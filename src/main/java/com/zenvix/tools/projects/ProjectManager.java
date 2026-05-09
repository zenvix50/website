package com.zenvix.tools.projects;

import com.zenvix.core.services.GradleManager;
import com.zenvix.core.services.MavenManager;
import com.zenvix.tools.SpringBootRunner;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ProjectManager handles importing, building, and running registered Java projects.
 */
public class ProjectManager {

    private String baseDir = "zenvix";
    private final Map<String, ProjectConfig> registry = new ConcurrentHashMap<>();
    private final MavenManager mavenManager;
    private final GradleManager gradleManager;
    private final SpringBootRunner springBootRunner;

    public ProjectManager(String baseDir, MavenManager mavenManager, GradleManager gradleManager, SpringBootRunner springBootRunner) {
        this.baseDir = baseDir;
        this.mavenManager = mavenManager;
        this.gradleManager = gradleManager;
        this.springBootRunner = springBootRunner;
        loadRegistry();
    }

    // --- JSON Persistence (Zero Dependencies) ---

    public String toJson(ProjectConfig config) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(config.id).append("\",");
        sb.append("\"name\":\"").append(config.name != null ? config.name.replace("\"", "\\\"") : "").append("\",");
        sb.append("\"path\":\"").append(config.path != null ? config.path.replace("\\", "\\\\") : "").append("\",");
        sb.append("\"buildTool\":\"").append(config.buildTool != null ? config.buildTool : "").append("\",");
        sb.append("\"mainJar\":\"").append(config.mainJar != null ? config.mainJar.replace("\\", "\\\\") : "").append("\",");
        sb.append("\"jvmArgs\":\"").append(config.jvmArgs != null ? config.jvmArgs : "").append("\",");
        sb.append("\"profile\":\"").append(config.profile != null ? config.profile : "").append("\",");
        
        sb.append("\"associatedServices\":[");
        for (int i = 0; i < config.associatedServices.size(); i++) {
            sb.append("\"").append(config.associatedServices.get(i)).append("\"");
            if (i < config.associatedServices.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    public ProjectConfig fromJson(String json) {
        ProjectConfig config = new ProjectConfig();
        config.id = extractJsonString(json, "id");
        config.name = extractJsonString(json, "name");
        config.path = extractJsonString(json, "path");
        config.buildTool = extractJsonString(json, "buildTool");
        config.mainJar = extractJsonString(json, "mainJar");
        config.jvmArgs = extractJsonString(json, "jvmArgs");
        config.profile = extractJsonString(json, "profile");
        
        Matcher m = Pattern.compile("\"associatedServices\"\\s*:\\s*\\[(.*?)\\]").matcher(json);
        if (m.find()) {
            String arr = m.group(1);
            if (!arr.trim().isEmpty()) {
                String[] items = arr.split(",");
                for (String item : items) {
                    config.associatedServices.add(item.replace("\"", "").trim());
                }
            }
        }
        return config;
    }
    
    private String extractJsonString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\\", "\\").replace("\\\"", "\"") : "";
    }

    private void saveRegistry() {
        try {
            Path configDir = Paths.get(baseDir, "config");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path configPath = configDir.resolve("projects.json");
            
            StringBuilder sb = new StringBuilder();
            for (ProjectConfig cfg : registry.values()) {
                sb.append(toJson(cfg)).append("\n");
            }
            Files.write(configPath, sb.toString().getBytes());
        } catch (Exception e) { /* ignore */ }
    }

    private void loadRegistry() {
        Path configPath = Paths.get(baseDir, "config", "projects.json");
        if (Files.exists(configPath)) {
            try {
                List<String> lines = Files.readAllLines(configPath);
                for (String line : lines) {
                    if (line.trim().startsWith("{")) {
                        ProjectConfig cfg = fromJson(line);
                        registry.put(cfg.id, cfg);
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }
    }

    // --- Registry API ---

    public ProjectConfig importProject(String directoryPath) throws Exception {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Invalid directory path");
        }

        ProjectConfig config = new ProjectConfig();
        config.path = dir.toAbsolutePath().toString();
        config.name = dir.getFileName().toString();

        Path pom = dir.resolve("pom.xml");
        Path gradle = dir.resolve("build.gradle");

        if (Files.exists(pom)) {
            config.buildTool = "MAVEN";
            String content = new String(Files.readAllBytes(pom));
            Matcher m = Pattern.compile("<artifactId>(.*?)</artifactId>").matcher(content);
            if (m.find()) config.name = m.group(1);
        } else if (Files.exists(gradle)) {
            config.buildTool = "GRADLE";
            Path settings = dir.resolve("settings.gradle");
            if (Files.exists(settings)) {
                String content = new String(Files.readAllBytes(settings));
                Matcher m = Pattern.compile("rootProject\\.name\\s*=\\s*['\"](.*?)['\"]").matcher(content);
                if (m.find()) config.name = m.group(1);
            }
        } else {
            config.buildTool = "UNKNOWN";
        }

        registry.put(config.id, config);
        saveRegistry();
        return config;
    }

    public void removeProject(String id) {
        registry.remove(id);
        saveRegistry();
    }

    public List<ProjectConfig> getProjects() {
        return new ArrayList<>(registry.values());
    }

    public ProjectConfig getProject(String id) {
        return registry.get(id);
    }

    public void openProjectFolder(String id) throws Exception {
        ProjectConfig config = registry.get(id);
        if (config != null && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(new File(config.path));
        }
    }

    public void associateService(String id, String serviceName) {
        ProjectConfig config = registry.get(id);
        if (config != null) {
            if (!config.associatedServices.contains(serviceName)) {
                config.associatedServices.add(serviceName);
                saveRegistry();
            }
        }
    }

    // --- Build & Run Execution ---

    public void buildAndRun(String id) throws Exception {
        ProjectConfig config = registry.get(id);
        if (config == null) throw new Exception("Project not found: " + id);

        if ("MAVEN".equals(config.buildTool)) {
            mavenManager.runBuild(config.path, "clean package -DskipTests", null, null);
        } else if ("GRADLE".equals(config.buildTool)) {
            gradleManager.runBuild(config.path, "clean build -x test", null, null);
        } else {
            throw new Exception("Unsupported build tool");
        }

        Path jarFile = findMainJar(config);
        if (jarFile == null) throw new Exception("Could not find generated JAR file");
        
        config.mainJar = jarFile.toAbsolutePath().toString();
        saveRegistry();

        SpringBootRunner.AppConfig appConfig = new SpringBootRunner.AppConfig(
                config.name,
                config.mainJar,
                8080,
                config.profile != null ? config.profile : "",
                config.jvmArgs != null ? config.jvmArgs : ""
        );
        
        springBootRunner.addOrUpdateApp(appConfig);
        springBootRunner.startApp(config.name, null, null);
    }

    private Path findMainJar(ProjectConfig config) throws Exception {
        Path searchDir = null;
        if ("MAVEN".equals(config.buildTool)) {
            searchDir = Paths.get(config.path, "target");
        } else if ("GRADLE".equals(config.buildTool)) {
            searchDir = Paths.get(config.path, "build", "libs");
        }
        
        if (searchDir != null && Files.exists(searchDir)) {
            List<Path> jars = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(searchDir, "*.jar")) {
                for (Path p : stream) {
                    if (!p.toString().endsWith("-javadoc.jar") && !p.toString().endsWith("-sources.jar") && !p.toString().endsWith("-plain.jar")) {
                        jars.add(p);
                    }
                }
            }
            if (!jars.isEmpty()) {
                jars.sort((p1, p2) -> Long.compare(p2.toFile().lastModified(), p1.toFile().lastModified()));
                return jars.get(0);
            }
        }
        return null;
    }

    // --- File Watcher ---

    public void watchProject(String id, java.util.function.Consumer<String> onJarChanged) throws Exception {
        ProjectConfig config = registry.get(id);
        if (config == null) return;
        
        Path searchDir = null;
        if ("MAVEN".equals(config.buildTool)) searchDir = Paths.get(config.path, "target");
        else if ("GRADLE".equals(config.buildTool)) searchDir = Paths.get(config.path, "build", "libs");
        
        if (searchDir == null) return;
        if (!Files.exists(searchDir)) Files.createDirectories(searchDir);

        WatchService watchService = FileSystems.getDefault().newWatchService();
        searchDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

        Thread watcherThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();
                        if (changed.toString().endsWith(".jar")) {
                            onJarChanged.accept("JAR changed: " + changed.toString());
                        }
                    }
                    if (!key.reset()) break;
                }
            } catch (Exception e) { /* Interrupted */ }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }
}
