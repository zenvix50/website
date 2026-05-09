package com.zenvix.tools;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpringBootRunner manages multiple concurrent Spring Boot applications.
 * Supports Actuator health polling, DevTools reload detection, and multi-app configuration.
 */
public class SpringBootRunner {

    private String baseDir = "zenvix";
    private final Map<String, AppConfig> registry = new ConcurrentHashMap<>();
    private final Map<String, Process> runningApps = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> healthPollers = new ConcurrentHashMap<>();

    public SpringBootRunner() {
        this("zenvix");
    }

    public SpringBootRunner(String baseDir) {
        this.baseDir = baseDir;
        loadRegistry();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (String appName : runningApps.keySet()) {
                stopApp(appName);
            }
        }));
    }

    public static class AppConfig {
        public String name;
        public String jarPath;
        public int port;
        public String profile;
        public String jvmArgs;
        
        public AppConfig(String name, String jarPath, int port, String profile, String jvmArgs) {
            this.name = name; 
            this.jarPath = jarPath; 
            this.port = port; 
            this.profile = profile; 
            this.jvmArgs = jvmArgs;
        }
        
        public String toJson() {
            return String.format("{\"name\":\"%s\", \"jarPath\":\"%s\", \"port\":%d, \"profile\":\"%s\", \"jvmArgs\":\"%s\"}",
                    name.replace("\"", "\\\""), 
                    jarPath.replace("\\", "\\\\"), 
                    port, 
                    profile, 
                    jvmArgs.replace("\"", "\\\""));
        }
    }

    // --- Registry Management ---

    private void loadRegistry() {
        Path configPath = Paths.get(baseDir, "config", "springboot-apps.json");
        if (Files.exists(configPath)) {
            try {
                String content = new String(Files.readAllBytes(configPath));
                Matcher m = Pattern.compile("\\{\"name\":\"(.*?)\",\\s*\"jarPath\":\"(.*?)\",\\s*\"port\":(\\d+),\\s*\"profile\":\"(.*?)\",\\s*\"jvmArgs\":\"(.*?)\"\\}").matcher(content);
                while (m.find()) {
                    AppConfig cfg = new AppConfig(
                        m.group(1), 
                        m.group(2).replace("\\\\", "\\"), 
                        Integer.parseInt(m.group(3)), 
                        m.group(4), 
                        m.group(5).replace("\\\"", "\"")
                    );
                    registry.put(cfg.name, cfg);
                }
            } catch (Exception e) { /* ignore parse errors */ }
        }
    }

    public void saveRegistry() {
        try {
            Path configDir = Paths.get(baseDir, "config");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path configPath = configDir.resolve("springboot-apps.json");
            
            StringBuilder sb = new StringBuilder("[\n");
            List<AppConfig> apps = new ArrayList<>(registry.values());
            for (int i = 0; i < apps.size(); i++) {
                sb.append("  ").append(apps.get(i).toJson());
                if (i < apps.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            
            Files.write(configPath, sb.toString().getBytes());
        } catch (Exception e) { /* ignore write errors */ }
    }
    
    public void addOrUpdateApp(AppConfig config) {
        registry.put(config.name, config);
        saveRegistry();
    }
    
    public void removeApp(String name) {
        stopApp(name);
        registry.remove(name);
        saveRegistry();
    }

    public AppConfig getAppConfig(String name) {
        return registry.get(name);
    }

    // --- Process Management ---

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

    public void startApp(String appName, Consumer<String> outputCallback, Consumer<String> devToolsCallback) throws Exception {
        AppConfig config = registry.get(appName);
        if (config == null) throw new Exception("App not found in registry: " + appName);
        
        if (runningApps.containsKey(appName) && runningApps.get(appName).isAlive()) {
            throw new Exception("App is already running.");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(getJavaCommand());
        
        if (config.jvmArgs != null && !config.jvmArgs.isEmpty()) {
            cmd.addAll(Arrays.asList(config.jvmArgs.split("\\s+")));
        }
        
        cmd.add("-jar");
        cmd.add(config.jarPath);
        
        if (config.profile != null && !config.profile.isEmpty()) {
            cmd.add("--spring.profiles.active=" + config.profile);
        }
        cmd.add("--server.port=" + config.port);

        ProcessBuilder pb = createProcessBuilder(cmd.toArray(new String[0]));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        runningApps.put(appName, p);

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (outputCallback != null) outputCallback.accept(line);
                    
                    if (devToolsCallback != null && (line.contains("LiveReload server is running") || line.contains("Restarting"))) {
                        devToolsCallback.accept("DevTools Activity: " + line);
                    }
                }
            } catch (Exception e) { /* process died */ }
            runningApps.remove(appName);
            stopHealthPolling(appName);
        }).start();

        startHealthPolling(appName, config.port);
    }

    public void stopApp(String appName) {
        Process p = runningApps.get(appName);
        if (p != null && p.isAlive()) {
            p.destroy();
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                p.destroyForcibly();
            }
        }
        runningApps.remove(appName);
        stopHealthPolling(appName);
    }

    public void restartApp(String appName, Consumer<String> outputCallback, Consumer<String> devToolsCallback) throws Exception {
        stopApp(appName);
        startApp(appName, outputCallback, devToolsCallback);
    }

    public boolean isAppRunning(String appName) {
        Process p = runningApps.get(appName);
        return p != null && p.isAlive();
    }

    // --- Actuator Management ---

    private void startHealthPolling(String appName, int port) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        healthPollers.put(appName, scheduler);
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!runningApps.containsKey(appName)) {
                scheduler.shutdown();
                return;
            }
            try {
                getActuatorHealth(port);
            } catch (Exception e) { /* ignore unreachable */ }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void stopHealthPolling(String appName) {
        ScheduledExecutorService scheduler = healthPollers.remove(appName);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public String getActuatorHealth(int port) throws Exception {
        return httpGet("http://localhost:" + port + "/actuator/health");
    }

    public String getActuatorInfo(int port) throws Exception {
        return httpGet("http://localhost:" + port + "/actuator/info");
    }

    public String getActuatorEnv(int port) throws Exception {
        return httpGet("http://localhost:" + port + "/actuator/env");
    }

    protected String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            return response.toString();
        } catch (IOException e) {
            if (conn.getResponseCode() >= 400) {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = err.readLine()) != null) response.append(line);
                    return response.toString();
                } catch (Exception ignored) {}
            }
            throw e;
        }
    }

    public String parseHealthStatus(String actuatorHealthResponse) {
        if (actuatorHealthResponse == null) return "UNKNOWN";
        if (actuatorHealthResponse.contains("\"status\":\"UP\"") || actuatorHealthResponse.contains("\"status\": \"UP\"")) return "UP";
        if (actuatorHealthResponse.contains("\"status\":\"DOWN\"") || actuatorHealthResponse.contains("\"status\": \"DOWN\"")) return "DOWN";
        if (actuatorHealthResponse.contains("\"status\":\"OUT_OF_SERVICE\"") || actuatorHealthResponse.contains("\"status\": \"OUT_OF_SERVICE\"")) return "OUT_OF_SERVICE";
        return "UNKNOWN";
    }

    public Map<String, String> parseEnvVariables(String envJson) {
        Map<String, String> result = new HashMap<>();
        if (envJson == null) return result;
        
        Matcher profileMatcher = Pattern.compile("\"activeProfiles\"\\s*:\\s*\\[(.*?)\\]").matcher(envJson);
        if (profileMatcher.find()) result.put("activeProfiles", profileMatcher.group(1).replace("\"", ""));
        
        Matcher nameMatcher = Pattern.compile("\"spring.application.name\"\\s*:\\s*\\{[^}]*\"value\"\\s*:\\s*\"(.*?)\"").matcher(envJson);
        if (nameMatcher.find()) result.put("appName", nameMatcher.group(1));
        
        Matcher versionMatcher = Pattern.compile("\"version\"\\s*:\\s*\\{[^}]*\"value\"\\s*:\\s*\"(.*?)\"").matcher(envJson);
        if (versionMatcher.find()) result.put("version", versionMatcher.group(1));
        
        return result;
    }
}
