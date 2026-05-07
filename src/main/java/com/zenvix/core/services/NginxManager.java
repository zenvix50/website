package com.zenvix.core.services;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * NginxManager handles the lifecycle and configuration of an Nginx server.
 * Supports acting as a reverse proxy, static file server, and manages virtual hosts.
 */
public class NginxManager extends ServiceManager {

    private String baseDir;
    private int httpPort = 80;
    private int httpsPort = 443;

    public NginxManager() {
        this("zenvix");
    }

    public NginxManager(String baseDir) {
        this.baseDir = baseDir;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) stop();
            } catch (Exception e) {
                System.err.println("Failed to stop Nginx gracefully on shutdown: " + e.getMessage());
            }
        }));
    }

    // --- Path Resolution ---
    
    public String getNginxHome() {
        return Paths.get(baseDir, "nginx").toAbsolutePath().toString();
    }

    public String getBinaryPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Paths.get(getNginxHome(), "nginx.exe").toString();
        }
        return Paths.get(getNginxHome(), "bin", "nginx").toString();
    }

    public String getConfigPath() {
        return Paths.get(getNginxHome(), "conf", "nginx.conf").toAbsolutePath().toString();
    }

    public String getPidFile() {
        return Paths.get(getNginxHome(), "logs", "nginx.pid").toAbsolutePath().toString();
    }

    // --- Process Utilities ---

    protected ProcessBuilder createProcessBuilder(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(getNginxHome()));
        return pb;
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // --- Lifecycle Methods ---

    /**
     * Validates the Nginx configuration.
     * @return true if valid
     * @throws ConfigException if validation fails
     */
    public boolean validateConfig() throws Exception {
        String bin = getBinaryPath();
        String conf = getConfigPath();
        
        ProcessBuilder pb = createProcessBuilder(bin, "-t", "-c", conf);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        process.waitFor(10, TimeUnit.SECONDS);
        
        if (process.exitValue() != 0) {
            throw new ConfigException("Nginx configuration validation failed:\n" + output.toString());
        }
        return true;
    }

    @Override
    public void start() throws Exception {
        if (!isPortAvailable(httpPort)) {
            throw new Exception("Port " + httpPort + " is already in use.");
        }
        
        validateConfig();

        String bin = getBinaryPath();
        String conf = getConfigPath();
        
        ProcessBuilder pb = createProcessBuilder(bin, "-c", conf);
        Process process = pb.start();
        process.waitFor(5, TimeUnit.SECONDS); 
        
        // Wait a moment for PID file to be created
        Thread.sleep(1000); 
        if (!isRunning()) {
            throw new Exception("Nginx failed to start. Check error.log for details.");
        }
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        String bin = getBinaryPath();
        String conf = getConfigPath();
        
        // Use Nginx signal to stop
        ProcessBuilder pb = createProcessBuilder(bin, "-s", "stop", "-c", conf);
        Process process = pb.start();
        process.waitFor(10, TimeUnit.SECONDS);
        
        int retries = 5;
        while (isRunning() && retries > 0) {
            Thread.sleep(1000);
            retries--;
        }
        
        if (isRunning()) {
            throw new Exception("Failed to stop Nginx gracefully");
        }
    }

    @Override
    public void restart() throws Exception {
        stop();
        start();
    }

    /**
     * Reloads configuration without dropping connections.
     */
    public void reload() throws Exception {
        validateConfig();
        String bin = getBinaryPath();
        String conf = getConfigPath();
        
        ProcessBuilder pb = createProcessBuilder(bin, "-s", "reload", "-c", conf);
        Process process = pb.start();
        process.waitFor(10, TimeUnit.SECONDS);
        if (process.exitValue() != 0) {
            throw new Exception("Failed to reload Nginx configuration.");
        }
    }

    @Override
    public String getStatus() throws Exception {
        return isRunning() ? "RUNNING" : "STOPPED";
    }

    public boolean isRunning() {
        File pidFile = new File(getPidFile());
        if (pidFile.exists()) {
            try {
                String pidStr = new String(Files.readAllBytes(pidFile.toPath())).trim();
                return !pidStr.isEmpty();
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    @Override
    public String getLogs() throws Exception {
        Path errorLog = Paths.get(getNginxHome(), "logs", "error.log");
        if (!Files.exists(errorLog)) return "";
        List<String> lines = Files.readAllLines(errorLog);
        int tail = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
    }

    @Override
    public String getMetrics() throws Exception {
        return "Metrics not fully implemented. Consider querying Nginx stub_status module.";
    }

    /**
     * Streams both access.log and error.log to the provided OutputStreams.
     */
    public void streamLogs(OutputStream accessOut, OutputStream errorOut) {
        streamFile(Paths.get(getNginxHome(), "logs", "access.log"), accessOut);
        streamFile(Paths.get(getNginxHome(), "logs", "error.log"), errorOut);
    }
    
    private void streamFile(Path logFile, OutputStream out) {
        new Thread(() -> {
            try {
                if (!Files.exists(logFile)) {
                    Files.createDirectories(logFile.getParent());
                    Files.createFile(logFile);
                }
                try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
                    long pointer = file.length();
                    while (true) {
                        long len = file.length();
                        if (len < pointer) pointer = 0; 
                        if (len > pointer) {
                            file.seek(pointer);
                            String line;
                            while ((line = file.readLine()) != null) {
                                out.write((line + "\n").getBytes());
                            }
                            pointer = file.getFilePointer();
                            out.flush();
                        }
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- Configuration Editor Logic ---
    
    protected String readConfig() throws IOException {
        Path path = Paths.get(getConfigPath());
        if (!Files.exists(path)) return "events {}\nhttp {\n}";
        return new String(Files.readAllBytes(path));
    }
    
    protected void writeConfig(String content) throws IOException {
        Path path = Paths.get(getConfigPath());
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes());
    }

    public void addVirtualHost(String serverName, int port, String rootDir) throws Exception {
        String conf = readConfig();
        if (conf.contains("server_name " + serverName + ";")) {
            return; 
        }
        
        String serverBlock = "\n    server {\n" +
                             "        listen " + port + ";\n" +
                             "        server_name " + serverName + ";\n" +
                             "        root " + rootDir.replace("\\", "/") + ";\n" +
                             "        index index.html index.htm;\n" +
                             "    }\n";
                             
        int lastBrace = conf.lastIndexOf('}');
        if (lastBrace != -1) {
            conf = conf.substring(0, lastBrace) + serverBlock + conf.substring(lastBrace);
            writeConfig(conf);
        }
    }
    
    public void removeVirtualHost(String serverName) throws Exception {
        String conf = readConfig();
        String searchStr = "server_name " + serverName + ";";
        int nameIndex = conf.indexOf(searchStr);
        if (nameIndex == -1) return; 
        
        int serverStart = conf.lastIndexOf("server {", nameIndex);
        if (serverStart == -1) serverStart = conf.lastIndexOf("server{", nameIndex);
        if (serverStart == -1) return;
        
        int braceCount = 0;
        int endIndex = -1;
        for (int i = serverStart; i < conf.length(); i++) {
            if (conf.charAt(i) == '{') braceCount++;
            else if (conf.charAt(i) == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }
        
        if (endIndex != -1) {
            conf = conf.substring(0, serverStart) + conf.substring(endIndex + 1);
            writeConfig(conf);
        }
    }
    
    public void addUpstream(String name, List<String> servers) throws Exception {
        String conf = readConfig();
        if (conf.contains("upstream " + name + " {")) return;
        
        StringBuilder upstreamBlock = new StringBuilder("\n    upstream ").append(name).append(" {\n");
        for (String server : servers) {
            upstreamBlock.append("        server ").append(server).append(";\n");
        }
        upstreamBlock.append("    }\n");
        
        int lastBrace = conf.lastIndexOf('}');
        if (lastBrace != -1) {
            conf = conf.substring(0, lastBrace) + upstreamBlock.toString() + conf.substring(lastBrace);
            writeConfig(conf);
        }
    }

    /**
     * Configures SSL for a given server name. Can optionally auto-generate certificates.
     */
    public void configureSSL(String serverName, String certPath, String keyPath, boolean autoGenerate) throws Exception {
        if (autoGenerate) {
            generateSelfSignedCert(serverName, certPath, keyPath);
        }

        String conf = readConfig();
        String searchStr = "server_name " + serverName + ";";
        int nameIndex = conf.indexOf(searchStr);
        if (nameIndex == -1) throw new Exception("Virtual host " + serverName + " not found.");
        
        int serverStart = conf.lastIndexOf("server {", nameIndex);
        if (serverStart == -1) serverStart = conf.lastIndexOf("server{", nameIndex);
        if (serverStart == -1) return;
        
        int braceCount = 0;
        int endIndex = -1;
        for (int i = serverStart; i < conf.length(); i++) {
            if (conf.charAt(i) == '{') braceCount++;
            else if (conf.charAt(i) == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }
        
        if (endIndex != -1) {
            String before = conf.substring(0, endIndex);
            String after = conf.substring(endIndex);
            if (!before.contains("ssl_certificate ")) {
                String sslConfig = "\n        listen " + httpsPort + " ssl;\n" +
                                   "        ssl_certificate " + certPath.replace("\\", "/") + ";\n" +
                                   "        ssl_certificate_key " + keyPath.replace("\\", "/") + ";\n";
                conf = before + sslConfig + after;
                writeConfig(conf);
            }
        }
    }
    
    protected void generateSelfSignedCert(String serverName, String outCert, String outKey) throws Exception {
        File certFile = new File(outCert);
        certFile.getParentFile().mkdirs();
        
        ProcessBuilder pb = new ProcessBuilder("openssl", "req", "-x509", "-nodes", "-days", "365", 
            "-newkey", "rsa:2048", "-keyout", outKey, "-out", outCert, "-subj", "/CN=" + serverName);
        Process p = pb.start();
        p.waitFor(10, TimeUnit.SECONDS);
    }

    public static class ConfigException extends Exception {
        public ConfigException(String message) {
            super(message);
        }
    }

    // --- Getters & Setters ---

    public int getHttpPort() { return httpPort; }
    public void setHttpPort(int httpPort) { this.httpPort = httpPort; }
    public int getHttpsPort() { return httpsPort; }
    public void setHttpsPort(int httpsPort) { this.httpsPort = httpsPort; }
}
