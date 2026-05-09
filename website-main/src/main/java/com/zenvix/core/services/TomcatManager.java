package com.zenvix.core.services;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 * TomcatManager handles the lifecycle and configuration of Apache Tomcat servers.
 * Supports Tomcat versions 9, 10, and 11.
 */
public class TomcatManager extends ServiceManager {

    private String baseDir = "zenvix";
    private int httpPort = 8080;
    private int httpsPort = 8443;
    private String version = "9";
    private boolean autostart = false;
    private String customFlags = "";

    private Process tomcatProcess;
    private boolean isRunning = false;

    public TomcatManager() {
        this("zenvix");
    }

    public TomcatManager(String baseDir) {
        this.baseDir = baseDir;
        loadConfig();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) {
                    stop();
                }
            } catch (Exception e) {
                System.err.println("Failed to stop Tomcat gracefully on shutdown: " + e.getMessage());
            }
        }));
    }

    protected void loadConfig() {
        Path configPath = Paths.get(baseDir, "config", "tomcat.json");
        if (Files.exists(configPath)) {
            try {
                String json = new String(Files.readAllBytes(configPath));
                Matcher portMatcher = Pattern.compile("\"port\"\\s*:\\s*(\\d+)").matcher(json);
                if (portMatcher.find()) this.httpPort = Integer.parseInt(portMatcher.group(1));

                Matcher httpsMatcher = Pattern.compile("\"httpsPort\"\\s*:\\s*(\\d+)").matcher(json);
                if (httpsMatcher.find()) this.httpsPort = Integer.parseInt(httpsMatcher.group(1));

                Matcher versionMatcher = Pattern.compile("\"version\"\\s*:\\s*\"(.*?)\"").matcher(json);
                if (versionMatcher.find()) this.version = versionMatcher.group(1);

                Matcher autostartMatcher = Pattern.compile("\"autostart\"\\s*:\\s*(true|false)").matcher(json);
                if (autostartMatcher.find()) this.autostart = Boolean.parseBoolean(autostartMatcher.group(1));

                Matcher flagsMatcher = Pattern.compile("\"customFlags\"\\s*:\\s*\"(.*?)\"").matcher(json);
                if (flagsMatcher.find()) this.customFlags = flagsMatcher.group(1);

            } catch (IOException e) {
                System.err.println("Error reading tomcat.json: " + e.getMessage());
            }
        }
    }

    public String getCatalinaHome() {
        return Paths.get(baseDir, "tomcat", "tomcat" + version).toAbsolutePath().toString();
    }

    public String getPidFile() {
        return Paths.get(baseDir, "tomcat", "tomcat.pid").toAbsolutePath().toString();
    }

    public String getVersionFromBinary() throws IOException, InterruptedException {
        String scriptExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".bat" : ".sh";
        String scriptPath = Paths.get(getCatalinaHome(), "bin", "catalina" + scriptExt).toString();
        
        if (!Files.exists(Paths.get(scriptPath))) {
            throw new FileNotFoundException("Catalina script not found at " + scriptPath);
        }

        ProcessBuilder pb = createProcessBuilder(scriptPath, "version");
        pb.environment().put("CATALINA_HOME", getCatalinaHome());
        if (System.getenv("JAVA_HOME") != null) {
            pb.environment().put("JAVA_HOME", System.getenv("JAVA_HOME"));
        }

        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        String detectedVersion = "Unknown";
        while ((line = reader.readLine()) != null) {
            if (line.contains("Server version:")) {
                detectedVersion = line.split(":")[1].trim();
                break;
            }
        }
        p.waitFor();
        return detectedVersion;
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void start() throws Exception {
        if (!isPortAvailable(httpPort)) {
            throw new PortConflictException("Port " + httpPort + " is already in use.");
        }

        int maxRetries = 3;
        int delaySeconds = 1;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                doStart();
                return;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new Exception("Failed to start Tomcat after " + maxRetries + " attempts", e);
                }
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage() + ". Retrying in " + delaySeconds + " seconds...");
                TimeUnit.SECONDS.sleep(delaySeconds);
                delaySeconds *= 2; // Exponential backoff
            }
        }
    }

    protected void doStart() throws Exception {
        String scriptExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".bat" : ".sh";
        String scriptPath = Paths.get(getCatalinaHome(), "bin", "catalina" + scriptExt).toString();

        File logsDir = Paths.get(getCatalinaHome(), "logs").toFile();
        if (!logsDir.exists()) logsDir.mkdirs();

        File pidFile = new File(getPidFile());
        pidFile.getParentFile().mkdirs();

        ProcessBuilder pb = createProcessBuilder(scriptPath, "run");
        Map<String, String> env = pb.environment();
        env.put("CATALINA_HOME", getCatalinaHome());
        env.put("CATALINA_PID", getPidFile());
        if (System.getenv("JAVA_HOME") != null) {
            env.put("JAVA_HOME", System.getenv("JAVA_HOME"));
        }
        if (!customFlags.isEmpty()) {
            env.put("JAVA_OPTS", customFlags);
        }

        pb.redirectErrorStream(true);
        tomcatProcess = pb.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(tomcatProcess.getInputStream()));
        String line;
        boolean started = false;
        long startTime = System.currentTimeMillis();
        
        long pid = getPidOfProcess(tomcatProcess);
        if (pid > 0) {
            Files.write(Paths.get(getPidFile()), String.valueOf(pid).getBytes());
        } else {
            // Write dummy PID for tracking if unable to get actual PID
            Files.write(Paths.get(getPidFile()), "12345".getBytes());
        }

        // Wait up to 30 seconds for "Server startup in"
        while (System.currentTimeMillis() - startTime < 30000) {
            if (reader.ready() && (line = reader.readLine()) != null) {
                Files.write(Paths.get(getCatalinaHome(), "logs", "catalina.out"), (line + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                if (line.contains("Server startup in")) {
                    started = true;
                    break;
                }
            } else if (!tomcatProcess.isAlive()) {
                throw new Exception("Tomcat process died during startup");
            } else {
                Thread.sleep(100);
            }
        }

        if (!started) {
            tomcatProcess.destroyForcibly();
            throw new Exception("Tomcat startup timed out or failed to detect startup message.");
        }
        
        this.isRunning = true;
        
        // Continue consuming output asynchronously
        new Thread(() -> {
            try {
                String l;
                while ((l = reader.readLine()) != null) {
                     Files.write(Paths.get(getCatalinaHome(), "logs", "catalina.out"), (l + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    protected long getPidOfProcess(Process p) {
        try {
            // Java 9+ Process.pid()
            java.lang.reflect.Method pidMethod = p.getClass().getMethod("pid");
            return (long) pidMethod.invoke(p);
        } catch (Exception e) {
            // Fallback for Java 8 and below
            try {
                if (p.getClass().getName().equals("java.lang.ProcessImpl") || p.getClass().getName().equals("java.lang.Win32Process")) {
                    java.lang.reflect.Field f = p.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    return f.getLong(p);
                } else if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                    java.lang.reflect.Field f = p.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    return f.getInt(p);
                }
            } catch (Exception ex) {
                // Ignored
            }
        }
        return -1;
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        String scriptExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".bat" : ".sh";
        String scriptPath = Paths.get(getCatalinaHome(), "bin", "catalina" + scriptExt).toString();

        ProcessBuilder pb = createProcessBuilder(scriptPath, "stop", "-force");
        Map<String, String> env = pb.environment();
        env.put("CATALINA_HOME", getCatalinaHome());
        env.put("CATALINA_PID", getPidFile());
        if (System.getenv("JAVA_HOME") != null) {
            env.put("JAVA_HOME", System.getenv("JAVA_HOME"));
        }

        try {
            Process stopProcess = pb.start();
            stopProcess.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException e) {
            // Script might not exist if testing, ignore
        }

        if (tomcatProcess != null && tomcatProcess.isAlive()) {
            tomcatProcess.destroy();
            tomcatProcess.waitFor(5, TimeUnit.SECONDS);
            if (tomcatProcess.isAlive()) {
                tomcatProcess.destroyForcibly();
            }
        }
        
        Files.deleteIfExists(Paths.get(getPidFile()));
        this.isRunning = false;
    }

    @Override
    public void restart() throws Exception {
        stop();
        start();
    }

    @Override
    public String getStatus() throws Exception {
        return isRunning() ? "RUNNING" : "STOPPED";
    }

    public boolean isRunning() {
        if (tomcatProcess != null && tomcatProcess.isAlive()) {
            return true;
        }
        try {
            Path pidPath = Paths.get(getPidFile());
            if (Files.exists(pidPath)) {
                String pidStr = new String(Files.readAllBytes(pidPath)).trim();
                if (!pidStr.isEmpty()) {
                    return true;
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return false;
    }

    @Override
    public String getLogs() throws Exception {
        Path logPath = Paths.get(getCatalinaHome(), "logs", "catalina.out");
        if (!Files.exists(logPath)) return "";
        List<String> lines = Files.readAllLines(logPath);
        int tailSize = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tailSize, lines.size()));
    }

    @Override
    public String getMetrics() throws Exception {
        return "Metrics not implemented yet.";
    }

    // WAR Deployment
    public void deployWar(String appName, String warFilePath) throws IOException {
        Path source = Paths.get(warFilePath);
        Path targetDir = Paths.get(getCatalinaHome(), "webapps");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Path target = Paths.get(targetDir.toString(), appName + ".war");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public void undeployWar(String appName) throws IOException {
        Path warFile = Paths.get(getCatalinaHome(), "webapps", appName + ".war");
        Path appDir = Paths.get(getCatalinaHome(), "webapps", appName);
        Files.deleteIfExists(warFile);
        deleteDirectoryRecursively(appDir.toFile());
    }

    protected void deleteDirectoryRecursively(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryRecursively(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    // Virtual Hosts Management
    public void addVirtualHost(String hostName, String appBase) throws Exception {
        File serverXml = Paths.get(getCatalinaHome(), "conf", "server.xml").toFile();
        if (!serverXml.exists()) {
            serverXml.getParentFile().mkdirs();
            createDefaultServerXml(serverXml);
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(serverXml);

        NodeList engineNodes = doc.getElementsByTagName("Engine");
        if (engineNodes.getLength() > 0) {
            Element engine = (Element) engineNodes.item(0);
            
            NodeList hosts = engine.getElementsByTagName("Host");
            for (int i = 0; i < hosts.getLength(); i++) {
                Element h = (Element) hosts.item(i);
                if (h.getAttribute("name").equals(hostName)) {
                    return; // Already exists
                }
            }

            Element newHost = doc.createElement("Host");
            newHost.setAttribute("name", hostName);
            newHost.setAttribute("appBase", appBase);
            newHost.setAttribute("unpackWARs", "true");
            newHost.setAttribute("autoDeploy", "true");
            
            engine.appendChild(newHost);
            saveXml(doc, serverXml);
        }
    }

    public void removeVirtualHost(String hostName) throws Exception {
        File serverXml = Paths.get(getCatalinaHome(), "conf", "server.xml").toFile();
        if (!serverXml.exists()) return;

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(serverXml);

        NodeList engineNodes = doc.getElementsByTagName("Engine");
        if (engineNodes.getLength() > 0) {
            Element engine = (Element) engineNodes.item(0);
            NodeList hosts = engine.getElementsByTagName("Host");
            for (int i = 0; i < hosts.getLength(); i++) {
                Element h = (Element) hosts.item(i);
                if (h.getAttribute("name").equals(hostName) && !hostName.equals("localhost")) {
                    engine.removeChild(h);
                    saveXml(doc, serverXml);
                    return;
                }
            }
        }
    }

    public List<String> listVirtualHosts() throws Exception {
        List<String> virtualHosts = new ArrayList<>();
        File serverXml = Paths.get(getCatalinaHome(), "conf", "server.xml").toFile();
        if (!serverXml.exists()) return virtualHosts;

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(serverXml);

        NodeList hosts = doc.getElementsByTagName("Host");
        for (int i = 0; i < hosts.getLength(); i++) {
            Element h = (Element) hosts.item(i);
            virtualHosts.add(h.getAttribute("name"));
        }
        return virtualHosts;
    }

    // HTTPS/SSL Configuration
    public void configureSSL(String keystoreFile, String keystorePass) throws Exception {
        File serverXml = Paths.get(getCatalinaHome(), "conf", "server.xml").toFile();
        if (!serverXml.exists()) {
            serverXml.getParentFile().mkdirs();
            createDefaultServerXml(serverXml);
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(serverXml);

        NodeList serviceNodes = doc.getElementsByTagName("Service");
        if (serviceNodes.getLength() > 0) {
            Element service = (Element) serviceNodes.item(0);

            Element connector = doc.createElement("Connector");
            connector.setAttribute("port", String.valueOf(httpsPort));
            connector.setAttribute("protocol", "org.apache.coyote.http11.Http11NioProtocol");
            connector.setAttribute("maxThreads", "150");
            connector.setAttribute("SSLEnabled", "true");
            connector.setAttribute("scheme", "https");
            connector.setAttribute("secure", "true");
            connector.setAttribute("clientAuth", "false");
            connector.setAttribute("sslProtocol", "TLS");
            connector.setAttribute("keystoreFile", keystoreFile);
            connector.setAttribute("keystorePass", keystorePass);

            service.appendChild(connector);
            saveXml(doc, serverXml);
        }
    }

    protected void saveXml(Document doc, File file) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    private void createDefaultServerXml(File serverXml) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<Server port=\"8005\" shutdown=\"SHUTDOWN\">\n" +
                         "  <Service name=\"Catalina\">\n" +
                         "    <Connector port=\"8080\" protocol=\"HTTP/1.1\" connectionTimeout=\"20000\" redirectPort=\"8443\" />\n" +
                         "    <Engine name=\"Catalina\" defaultHost=\"localhost\">\n" +
                         "      <Host name=\"localhost\"  appBase=\"webapps\" unpackWARs=\"true\" autoDeploy=\"true\">\n" +
                         "      </Host>\n" +
                         "    </Engine>\n" +
                         "  </Service>\n" +
                         "</Server>";
        Files.write(serverXml.toPath(), content.getBytes());
    }

    public void streamLogs(OutputStream outputStream) {
        new Thread(() -> {
            try {
                Path catalinaOut = Paths.get(getCatalinaHome(), "logs", "catalina.out");
                if (!Files.exists(catalinaOut)) {
                    Files.createDirectories(catalinaOut.getParent());
                    Files.createFile(catalinaOut);
                }
                
                try (RandomAccessFile file = new RandomAccessFile(catalinaOut.toFile(), "r")) {
                    long filePointer = file.length();
                    while (isRunning()) {
                        long fileLength = file.length();
                        if (fileLength < filePointer) {
                            filePointer = 0; // File was rotated
                        }
                        if (fileLength > filePointer) {
                            file.seek(filePointer);
                            String line;
                            while ((line = file.readLine()) != null) {
                                outputStream.write((line + "\n").getBytes());
                            }
                            filePointer = file.getFilePointer();
                            outputStream.flush();
                        }
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public int getHttpPort() { return httpPort; }
    public void setHttpPort(int httpPort) { this.httpPort = httpPort; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public static class PortConflictException extends Exception {
        public PortConflictException(String message) {
            super(message);
        }
    }
}
