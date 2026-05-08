package com.zenvix.core.services;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * H2Manager handles the lifecycle, configuration, and administration of an embedded H2 database server.
 * Supports both in-memory and file modes, and provides an admin web console.
 */
public class H2Manager extends ServiceManager {

    private String baseDir = "zenvix";
    private int webPort = 8082;
    private int tcpPort = 9092;
    private String mode = "file"; // "memory" or "file"
    private String dbPath = ""; 

    private Process h2Process;
    private boolean isRunning = false;

    public H2Manager() {
        this("zenvix");
    }

    public H2Manager(String baseDir) {
        this.baseDir = baseDir;
        this.dbPath = Paths.get(baseDir, "h2", "data", "zenviXdb").toAbsolutePath().toString().replace('\\', '/');
        loadConfig();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) stop();
            } catch (Exception e) {
                System.err.println("Failed to stop H2 database gracefully on shutdown: " + e.getMessage());
            }
        }));
    }

    protected void loadConfig() {
        Path configPath = Paths.get(baseDir, "config", "h2.json");
        if (Files.exists(configPath)) {
            try {
                String json = new String(Files.readAllBytes(configPath));
                
                Matcher modeMatcher = Pattern.compile("\"mode\"\\s*:\\s*\"(.*?)\"").matcher(json);
                if (modeMatcher.find()) this.mode = modeMatcher.group(1);
                
                Matcher dbPathMatcher = Pattern.compile("\"dbPath\"\\s*:\\s*\"(.*?)\"").matcher(json);
                if (dbPathMatcher.find()) this.dbPath = dbPathMatcher.group(1);

            } catch (IOException e) {
                System.err.println("Error reading h2.json: " + e.getMessage());
            }
        }
    }

    public String getH2JarPath() {
        return Paths.get(baseDir, "h2", "h2-2.2.jar").toAbsolutePath().toString();
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }
    
    protected String getJavaCommand() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
            return Paths.get(javaHome, "bin", "java" + exeExt).toString();
        }
        return "java";
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
        if (!isPortAvailable(webPort)) throw new Exception("Web port " + webPort + " is already in use.");
        if (!isPortAvailable(tcpPort)) throw new Exception("TCP port " + tcpPort + " is already in use.");

        if ("file".equalsIgnoreCase(mode)) {
            File dataDir = new File(dbPath).getParentFile();
            if (dataDir != null && !dataDir.exists()) dataDir.mkdirs();
        }

        Files.createDirectories(Paths.get(baseDir, "h2", "logs"));

        List<String> command = new ArrayList<>(Arrays.asList(
            getJavaCommand(),
            "-cp", getH2JarPath(),
            "org.h2.tools.Server",
            "-web", "-webPort", String.valueOf(webPort), "-webAllowOthers",
            "-tcp", "-tcpPort", String.valueOf(tcpPort), "-tcpAllowOthers"
        ));

        ProcessBuilder pb = createProcessBuilder(command.toArray(new String[0]));
        pb.redirectErrorStream(true);
        
        h2Process = pb.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(h2Process.getInputStream()));
        Path logFile = Paths.get(baseDir, "h2", "logs", "h2.log");
        
        boolean[] started = {false};
        Thread logThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    Files.write(logFile, (line + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    if (line.contains("Web Console server running") || line.contains("TCP server running")) {
                        started[0] = true;
                    }
                }
            } catch (Exception e) {}
        });
        logThread.start();
        
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 15000) {
            if (started[0]) {
                break;
            }
            if (!h2Process.isAlive()) {
                throw new Exception("H2 process terminated prematurely.");
            }
            Thread.sleep(200);
        }
        
        if (!started[0] && !h2Process.isAlive()) {
            throw new Exception("H2 failed to start.");
        }
        
        this.isRunning = true;
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        List<String> command = new ArrayList<>(Arrays.asList(
            getJavaCommand(),
            "-cp", getH2JarPath(),
            "org.h2.tools.Server",
            "-tcpShutdown", "tcp://localhost:" + tcpPort
        ));

        ProcessBuilder pb = createProcessBuilder(command.toArray(new String[0]));
        Process stopProc = pb.start();
        stopProc.waitFor(10, TimeUnit.SECONDS);

        if (h2Process != null && h2Process.isAlive()) {
            h2Process.destroy();
            h2Process.waitFor(5, TimeUnit.SECONDS);
            if (h2Process.isAlive()) h2Process.destroyForcibly();
        }
        
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
        return h2Process != null && h2Process.isAlive();
    }

    @Override
    public String getLogs() throws Exception {
        Path logFile = Paths.get(baseDir, "h2", "logs", "h2.log");
        if (!Files.exists(logFile)) return "";
        List<String> lines = Files.readAllLines(logFile);
        int tail = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
    }

    @Override
    public String getMetrics() throws Exception {
        return "Metrics via JDBC / JMX not fully implemented for H2.";
    }

    // --- JDBC URL Generation ---

    public String getJdbcUrlInMemory() {
        return "jdbc:h2:mem:zenviXdb;DB_CLOSE_DELAY=-1";
    }

    public String getJdbcUrlFile() {
        return "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE";
    }

    public String getJdbcUrlTcp() {
        return "jdbc:h2:tcp://localhost:" + tcpPort + "/~/zenviXdb";
    }

    public String getActiveJdbcUrl() {
        if ("memory".equalsIgnoreCase(mode)) {
            return getJdbcUrlInMemory();
        } else {
            return getJdbcUrlFile();
        }
    }

    // --- Console Access ---

    public void launchWebConsole() throws Exception {
        if (!isRunning()) {
            start();
        }
        String url = "http://localhost:" + webPort;
        openBrowser(url);
    }

    protected void openBrowser(String url) throws Exception {
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();
            if (os.contains("win")) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                rt.exec("open " + url);
            } else {
                rt.exec("xdg-open " + url);
            }
        }
    }

    // --- Getters and Setters ---
    
    public int getWebPort() { return webPort; }
    public void setWebPort(int webPort) { this.webPort = webPort; }
    public int getTcpPort() { return tcpPort; }
    public void setTcpPort(int tcpPort) { this.tcpPort = tcpPort; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getDbPath() { return dbPath; }
    public void setDbPath(String dbPath) { this.dbPath = dbPath; }
}
