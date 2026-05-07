package com.zenvix.tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EnvironmentEditor manages global and per-project environment variables,
 * applying an inheritance model where project variables override globals.
 * It provides .env file import/export capabilities and JSON persistence.
 */
public class EnvironmentEditor {

    private String baseDir = "zenvix";
    private EnvironmentConfig config = new EnvironmentConfig();

    public EnvironmentEditor() {
        this("zenvix");
    }

    public EnvironmentEditor(String baseDir) {
        this.baseDir = baseDir;
        loadConfig();
    }

    // --- Core Management ---

    public void setGlobalVar(String key, String value) {
        config.global.put(key, value);
        saveConfig();
    }

    public void removeGlobalVar(String key) {
        config.global.remove(key);
        saveConfig();
    }

    public void setProjectVar(String projectId, String key, String value) {
        config.projects.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>()).put(key, value);
        saveConfig();
    }

    public void removeProjectVar(String projectId, String key) {
        Map<String, String> projectVars = config.projects.get(projectId);
        if (projectVars != null) {
            projectVars.remove(key);
            if (projectVars.isEmpty()) {
                config.projects.remove(projectId);
            }
            saveConfig();
        }
    }

    public Map<String, String> getGlobalVars() {
        return new HashMap<>(config.global);
    }

    public Map<String, String> getProjectVars(String projectId) {
        return new HashMap<>(config.projects.getOrDefault(projectId, new ConcurrentHashMap<>()));
    }

    // --- Inheritance Model ---

    public Map<String, String> getEffectiveEnv(String projectId) {
        Map<String, String> effective = new HashMap<>(config.global);
        
        if (projectId != null) {
            Map<String, String> projectVars = config.projects.get(projectId);
            if (projectVars != null) {
                effective.putAll(projectVars);
            }
        }
        
        return effective;
    }

    public void applyToProcessBuilder(ProcessBuilder pb, String projectId) {
        Map<String, String> effective = getEffectiveEnv(projectId);
        pb.environment().putAll(effective);
    }

    // --- Import / Export (.env) ---

    public void exportToEnvFile(String projectId, Path outputPath) throws IOException {
        Map<String, String> env = getEffectiveEnv(projectId);
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, String> entry : env.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        
        Files.write(outputPath, sb.toString().getBytes());
    }

    public void importFromEnvFile(Path inputPath, String projectId) throws IOException {
        List<String> lines = Files.readAllLines(inputPath);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            int idx = line.indexOf("=");
            if (idx > 0) {
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                
                if (projectId == null) {
                    config.global.put(key, value);
                } else {
                    config.projects.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>()).put(key, value);
                }
            }
        }
        saveConfig();
    }

    // --- Persistence (Zero Dependency) ---

    private void loadConfig() {
        Path configPath = Paths.get(baseDir, "config", "environment.json");
        if (Files.exists(configPath)) {
            try {
                String json = new String(Files.readAllBytes(configPath));
                parseJson(json);
            } catch (Exception e) { /* ignore parse errors */ }
        }
    }

    private void saveConfig() {
        try {
            Path configDir = Paths.get(baseDir, "config");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path configPath = configDir.resolve("environment.json");
            
            Files.write(configPath, toJson().getBytes());
        } catch (Exception e) { /* ignore write errors */ }
    }

    protected String toJson() {
        StringBuilder sb = new StringBuilder("{\n");
        
        sb.append("  \"global\": {\n");
        int gSize = config.global.size();
        int gCount = 0;
        for (Map.Entry<String, String> entry : config.global.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\": \"")
              .append(escapeJson(entry.getValue())).append("\"");
            if (++gCount < gSize) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");
        
        sb.append("  \"projects\": {\n");
        int pSize = config.projects.size();
        int pCount = 0;
        for (Map.Entry<String, Map<String, String>> projEntry : config.projects.entrySet()) {
            sb.append("    \"").append(projEntry.getKey()).append("\": {\n");
            
            Map<String, String> vars = projEntry.getValue();
            int vSize = vars.size();
            int vCount = 0;
            for (Map.Entry<String, String> varEntry : vars.entrySet()) {
                sb.append("      \"").append(varEntry.getKey()).append("\": \"")
                  .append(escapeJson(varEntry.getValue())).append("\"");
                if (++vCount < vSize) sb.append(",");
                sb.append("\n");
            }
            
            sb.append("    }");
            if (++pCount < pSize) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    protected void parseJson(String json) {
        config.global.clear();
        config.projects.clear();

        int globalStart = json.indexOf("\"global\"");
        int projectsStart = json.indexOf("\"projects\"");
        
        if (globalStart != -1 && projectsStart != -1) {
            String gBlock = json.substring(globalStart, projectsStart);
            Matcher pair = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(gBlock);
            while (pair.find()) {
                config.global.put(pair.group(1), pair.group(2).replace("\\\"", "\"").replace("\\\\", "\\"));
            }
            
            String pBlock = json.substring(projectsStart);
            Matcher proj = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^{}]*)\\}").matcher(pBlock);
            while (proj.find()) {
                String projectId = proj.group(1);
                String varsBlock = proj.group(2);
                Map<String, String> vars = new ConcurrentHashMap<>();
                Matcher varPair = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(varsBlock);
                while (varPair.find()) {
                    vars.put(varPair.group(1), varPair.group(2).replace("\\\"", "\"").replace("\\\\", "\\"));
                }
                config.projects.put(projectId, vars);
            }
        }
    }
}
