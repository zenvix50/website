package com.zenvix.core.services;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MemcachedManager handles the lifecycle and administration of Memcached.
 * Uses raw TCP sockets to interact with Memcached for stats and flushes.
 */
public class MemcachedManager extends ServiceManager {

    private String baseDir = "zenvix";
    private int port = 11211;
    private int memoryMb = 64;
    private Process memcachedProcess;
    private boolean isRunning = false;

    public MemcachedManager() {
        this("zenvix");
    }

    public MemcachedManager(String baseDir) {
        this.baseDir = baseDir;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) stop();
            } catch (Exception e) {
                System.err.println("Failed to stop Memcached gracefully on shutdown: " + e.getMessage());
            }
        }));
    }

    // --- Path Management ---

    public String getMemcachedHome() {
        return Paths.get(baseDir, "memcached").toAbsolutePath().toString();
    }

    public String getBinPath() {
        String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        return Paths.get(getMemcachedHome(), "bin", "memcached" + exeExt).toString();
    }

    public String getLogPath() {
        return Paths.get(getMemcachedHome(), "memcached.log").toAbsolutePath().toString();
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

    // --- TCP Socket Commands ---
    
    protected Socket createSocket() throws IOException {
        return new Socket("127.0.0.1", port);
    }

    protected String sendCommand(String command) throws Exception {
        try (Socket socket = createSocket();
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.print(command + "\r\n");
            out.flush();
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                if (line.equals("END") || line.equals("OK") || line.equals("ERROR") || line.startsWith("CLIENT_ERROR") || line.startsWith("SERVER_ERROR")) {
                    break;
                }
            }
            return response.toString();
        }
    }

    public void flushAll() throws Exception {
        String response = sendCommand("flush_all");
        if (!response.contains("OK")) {
            throw new Exception("Failed to flush Memcached: " + response);
        }
    }

    public Map<String, String> getRawStats() throws Exception {
        String response = sendCommand("stats");
        Map<String, String> stats = new HashMap<>();
        
        for (String line : response.split("\n")) {
            if (line.startsWith("STAT ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) {
                    stats.put(parts[1], parts[2]);
                }
            }
        }
        return stats;
    }

    @Override
    public String getMetrics() throws Exception {
        Map<String, String> raw = getRawStats();
        
        long hits = Long.parseLong(raw.getOrDefault("get_hits", "0"));
        long misses = Long.parseLong(raw.getOrDefault("get_misses", "0"));
        long total = hits + misses;
        double hitRatio = total > 0 ? (double) hits / total * 100.0 : 0.0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Uptime (s): ").append(raw.getOrDefault("uptime", "0")).append("\n");
        sb.append("Current Items: ").append(raw.getOrDefault("curr_items", "0")).append("\n");
        sb.append("Memory Used (bytes): ").append(raw.getOrDefault("bytes", "0")).append("\n");
        sb.append("Total Connections: ").append(raw.getOrDefault("curr_connections", "0")).append("\n");
        sb.append("Evictions: ").append(raw.getOrDefault("evictions", "0")).append("\n");
        sb.append("Get Hits: ").append(hits).append("\n");
        sb.append("Get Misses: ").append(misses).append("\n");
        sb.append(String.format("Hit Ratio: %.2f%%", hitRatio)).append("\n");
        
        return sb.toString();
    }

    // --- Lifecycle Management ---

    @Override
    public void start() throws Exception {
        if (!isPortAvailable(port)) {
            throw new Exception("Port " + port + " is already in use.");
        }

        Files.createDirectories(Paths.get(getMemcachedHome(), "bin"));
        Path logFile = Paths.get(getLogPath());

        // We run it as a standard background process via Java rather than using -d to capture logs natively.
        ProcessBuilder pb = createProcessBuilder(
            getBinPath(),
            "-p", String.valueOf(port),
            "-m", String.valueOf(memoryMb)
        );
        
        pb.redirectErrorStream(true);
        memcachedProcess = pb.start();

        long startTime = System.currentTimeMillis();
        boolean started = false;
        
        while (System.currentTimeMillis() - startTime < 10000) {
            // Port will become unavailable once memcached binds to it
            if (!isPortAvailable(port)) {
                started = true;
                break;
            }
            if (!memcachedProcess.isAlive()) {
                throw new Exception("Memcached process terminated prematurely.");
            }
            Thread.sleep(100);
        }

        if (!started) {
            memcachedProcess.destroyForcibly();
            throw new Exception("Memcached startup timed out.");
        }

        this.isRunning = true;
        
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(memcachedProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Files.write(logFile, (line + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            } catch (Exception e) {}
        }).start();
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        if (memcachedProcess != null && memcachedProcess.isAlive()) {
            memcachedProcess.destroy();
            memcachedProcess.waitFor(5, TimeUnit.SECONDS);
            if (memcachedProcess.isAlive()) {
                memcachedProcess.destroyForcibly();
            }
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
        return memcachedProcess != null && memcachedProcess.isAlive();
    }

    @Override
    public String getLogs() throws Exception {
        Path logFile = Paths.get(getLogPath());
        if (!Files.exists(logFile)) return "";
        List<String> lines = Files.readAllLines(logFile);
        int tail = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
    }

    // --- Getters & Setters ---

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getMemoryMb() { return memoryMb; }
    public void setMemoryMb(int memoryMb) { this.memoryMb = memoryMb; }
}
