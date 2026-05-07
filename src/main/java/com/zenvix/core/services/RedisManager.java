package com.zenvix.core.services;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RedisManager handles the lifecycle, configuration, and administration of a Redis 7.x server.
 * Implements a Lettuce async client for key-value browsing and memory statistics.
 */
public class RedisManager extends ServiceManager {

    private String baseDir = "zenvix";
    private int port = 6379;
    private Process redisProcess;
    private boolean isRunning = false;

    public RedisManager() {
        this("zenvix");
    }

    public RedisManager(String baseDir) {
        this.baseDir = baseDir;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) stop();
            } catch (Exception e) {
                System.err.println("Failed to stop Redis gracefully on shutdown: " + e.getMessage());
            }
        }));
    }

    // --- Path Management ---

    public String getRedisHome() {
        return Paths.get(baseDir, "redis").toAbsolutePath().toString();
    }

    public String getBinPath(String binName) {
        String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        return Paths.get(getRedisHome(), "bin", binName + exeExt).toString();
    }

    public String getConfigPath() {
        return Paths.get(getRedisHome(), "redis.conf").toAbsolutePath().toString();
    }

    public String getLogPath() {
        return Paths.get(getRedisHome(), "redis.log").toAbsolutePath().toString();
    }

    public String getPidFile() {
        Path config = Paths.get(getConfigPath());
        if (Files.exists(config)) {
            try {
                for (String line : Files.readAllLines(config)) {
                    if (line.trim().startsWith("pidfile")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) return parts[1];
                    }
                }
            } catch (IOException e) { /* ignore */ }
        }
        return Paths.get(getRedisHome(), "redis.pid").toAbsolutePath().toString();
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }
    
    protected ProcessBuilder createProcessBuilder(List<String> command) {
        return new ProcessBuilder(command);
    }

    // --- Lifecycle Management ---

    @Override
    public void start() throws Exception {
        if (!isPortAvailable(port)) {
            throw new Exception("Port " + port + " is already in use.");
        }

        Files.createDirectories(Paths.get(getRedisHome(), "bin"));
        
        String conf = getConfigPath();
        if (!Files.exists(Paths.get(conf))) {
            Files.write(Paths.get(conf), ("port " + port + "\npidfile " + getPidFile().replace("\\", "/") + "\nlogfile " + getLogPath().replace("\\", "/") + "\n").getBytes());
        }

        ProcessBuilder pb = createProcessBuilder(getBinPath("redis-server"), conf);
        pb.redirectErrorStream(true);
        redisProcess = pb.start();

        long startTime = System.currentTimeMillis();
        boolean started = false;
        while (System.currentTimeMillis() - startTime < 15000) {
            if (isRunningInternal()) {
                started = true;
                break;
            }
            if (!redisProcess.isAlive()) {
                throw new Exception("Redis failed to start. Check logs.");
            }
            Thread.sleep(200);
        }

        if (!started) {
            redisProcess.destroyForcibly();
            throw new Exception("Redis startup timed out.");
        }

        this.isRunning = true;
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        String password = SecretsVault.get("redis.password");
        List<String> cmd = new ArrayList<>(Arrays.asList(getBinPath("redis-cli"), "-p", String.valueOf(port)));
        if (password != null && !password.isEmpty()) {
            cmd.add("-a"); cmd.add(password);
        }
        cmd.add("SHUTDOWN");
        
        Process p = createProcessBuilder(cmd).start();
        p.waitFor(10, TimeUnit.SECONDS);

        if (redisProcess != null && redisProcess.isAlive()) {
            redisProcess.destroy();
            redisProcess.waitFor(5, TimeUnit.SECONDS);
            if (redisProcess.isAlive()) redisProcess.destroyForcibly();
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
        return isRunningInternal();
    }
    
    private boolean isRunningInternal() {
        if (redisProcess != null && redisProcess.isAlive()) return true;
        File pidFile = new File(getPidFile());
        if (pidFile.exists()) {
            try {
                String pid = new String(Files.readAllBytes(pidFile.toPath())).trim();
                return !pid.isEmpty();
            } catch (IOException e) { }
        }
        return false;
    }

    @Override
    public String getLogs() throws Exception {
        Path logPath = Paths.get(getLogPath());
        if (!Files.exists(logPath)) return "";
        List<String> lines = Files.readAllLines(logPath);
        int tail = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
    }

    @Override
    public String getMetrics() throws Exception {
        return "Metrics generated via built-in getMemoryStats().";
    }

    // --- CLI Execution ---

    public String executeCliCommand(String command) throws Exception {
        String password = SecretsVault.get("redis.password");
        List<String> cmd = new ArrayList<>(Arrays.asList(getBinPath("redis-cli"), "-p", String.valueOf(port)));
        if (password != null && !password.isEmpty()) {
            cmd.add("-a"); cmd.add(password);
        }
        cmd.addAll(Arrays.asList(command.split("\\s+")));
        
        ProcessBuilder pb = createProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append("\n");
        }
        p.waitFor();
        return out.toString().trim();
    }

    // --- Lettuce Async Client: Key-Value Browser & Stats ---

    protected RedisClient getRedisClient() {
        String password = SecretsVault.get("redis.password");
        String auth = (password != null && !password.isEmpty()) ? password + "@" : "";
        String uri = "redis://" + auth + "localhost:" + port;
        return RedisClient.create(uri);
    }

    public List<String> listKeys(String pattern) throws Exception {
        try (RedisClient client = getRedisClient();
             StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> async = connection.async();
            ScanArgs args = ScanArgs.Builder.matches(pattern).limit(100);
            KeyScanCursor<String> cursor = async.scan(args).get();
            return cursor.getKeys();
        }
    }

    public String getValue(String key) throws Exception {
        try (RedisClient client = getRedisClient();
             StatefulRedisConnection<String, String> connection = client.connect()) {
            return connection.async().get(key).get();
        }
    }

    public void setValue(String key, String value, long ttlSeconds) throws Exception {
        try (RedisClient client = getRedisClient();
             StatefulRedisConnection<String, String> connection = client.connect()) {
            if (ttlSeconds > 0) {
                connection.async().setex(key, ttlSeconds, value).get();
            } else {
                connection.async().set(key, value).get();
            }
        }
    }

    public void deleteKey(String key) throws Exception {
        try (RedisClient client = getRedisClient();
             StatefulRedisConnection<String, String> connection = client.connect()) {
            connection.async().del(key).get();
        }
    }

    public long getTtl(String key) throws Exception {
        try (RedisClient client = getRedisClient();
             StatefulRedisConnection<String, String> connection = client.connect()) {
            return connection.async().ttl(key).get();
        }
    }

    public Map<String, String> getMemoryStats() throws Exception {
        Map<String, String> stats = new HashMap<>();
        try (RedisClient client = getRedisClient();
             StatefulRedisConnection<String, String> connection = client.connect()) {
            
            String infoMemory = connection.async().info("memory").get();
            for (String line : infoMemory.split("\r?\n")) {
                if (line.contains(":")) {
                    String[] kv = line.split(":", 2);
                    stats.put(kv[0], kv[1]);
                }
            }
            
            String infoStats = connection.async().info("stats").get();
            for (String line : infoStats.split("\r?\n")) {
                if (line.startsWith("keyspace_hits:") || line.startsWith("keyspace_misses:")) {
                    String[] kv = line.split(":", 2);
                    stats.put(kv[0], kv[1]);
                }
            }
        }
        return stats;
    }

    // --- redis.conf Editor ---

    public void editConfig(Map<String, String> newSettings) throws Exception {
        Path confPath = Paths.get(getConfigPath());
        Path tempPath = Paths.get(getConfigPath() + ".tmp");
        
        List<String> lines = Files.exists(confPath) ? Files.readAllLines(confPath) : new ArrayList<>();
        List<String> newLines = new ArrayList<>();
        
        Set<String> processedKeys = new HashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                newLines.add(line);
                continue;
            }
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length > 0 && newSettings.containsKey(parts[0])) {
                newLines.add(parts[0] + " " + newSettings.get(parts[0]));
                processedKeys.add(parts[0]);
            } else {
                newLines.add(line);
            }
        }
        
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            if (!processedKeys.contains(entry.getKey())) {
                newLines.add(entry.getKey() + " " + entry.getValue());
            }
        }
        
        Files.write(tempPath, newLines);
        
        boolean requiresRestart = false;
        if (isRunning()) {
            for (Map.Entry<String, String> entry : newSettings.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                
                if (key.equals("bind") || key.equals("port") || key.equals("requirepass")) {
                    requiresRestart = true;
                } else {
                    try {
                        String out = executeCliCommand("CONFIG SET " + key + " " + val);
                        if (out.toLowerCase().contains("err")) {
                            Files.deleteIfExists(tempPath);
                            throw new Exception("Validation failed for config " + key + ": " + out);
                        }
                    } catch (Exception e) {
                        Files.deleteIfExists(tempPath);
                        throw new Exception("Failed to apply config live: " + e.getMessage());
                    }
                }
            }
        }
        
        Files.move(tempPath, confPath, StandardCopyOption.REPLACE_EXISTING);
        
        if (newSettings.containsKey("requirepass")) {
            SecretsVault.set("redis.password", newSettings.get("requirepass"));
        }
        
        if (requiresRestart) {
            throw new RestartRequiredException("Changes saved. Restart Redis for changes to port, bind, or requirepass to take effect.");
        }
    }

    public static class RestartRequiredException extends Exception {
        public RestartRequiredException(String message) { super(message); }
    }

    // --- Getters and Setters ---
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
