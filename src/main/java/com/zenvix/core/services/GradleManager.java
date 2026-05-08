package com.zenvix.core.services;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GradleManager handles Gradle installations, running builds, task progress tracking,
 * Gradle Wrapper resolution, and build history logging.
 */
public class GradleManager extends ServiceManager {

    private String baseDir = "zenvix";
    private Process activeBuildProcess;
    private final List<BuildRecord> buildHistory = new CopyOnWriteArrayList<>();

    public GradleManager() {
        this("zenvix");
    }

    public GradleManager(String baseDir) {
        this.baseDir = baseDir;
        loadHistory();
    }

    public static class BuildRecord {
        public String timestamp;
        public String task;
        public String result;
        public long durationMs;

        public BuildRecord(String timestamp, String task, String result, long durationMs) {
            this.timestamp = timestamp;
            this.task = task;
            this.result = result;
            this.durationMs = durationMs;
        }

        public String toJson() {
            return String.format("{\"timestamp\":\"%s\", \"task\":\"%s\", \"result\":\"%s\", \"durationMs\":%d}", timestamp, task, result, durationMs);
        }
    }

    // --- Path Management ---

    public String getGradleHome() {
        return Paths.get(baseDir, "gradle").toAbsolutePath().toString();
    }

    public String getBinPath() {
        String ext = System.getProperty("os.name").toLowerCase().contains("win") ? ".bat" : "";
        return Paths.get(getGradleHome(), "bin", "gradle" + ext).toString();
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }

    // --- History Persistence ---

    private void loadHistory() {
        Path historyFile = Paths.get(baseDir, "config", "gradle-history.json");
        if (Files.exists(historyFile)) {
            try {
                String content = new String(Files.readAllBytes(historyFile));
                Matcher m = Pattern.compile("\\{\"timestamp\":\"(.*?)\",\\s*\"task\":\"(.*?)\",\\s*\"result\":\"(.*?)\",\\s*\"durationMs\":(\\d+)\\}").matcher(content);
                while (m.find()) {
                    buildHistory.add(new BuildRecord(m.group(1), m.group(2), m.group(3), Long.parseLong(m.group(4))));
                }
            } catch (Exception e) { /* ignore parse errors */ }
        }
    }

    private void saveHistory() {
        try {
            Path configDir = Paths.get(baseDir, "config");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path historyFile = configDir.resolve("gradle-history.json");
            
            while (buildHistory.size() > 50) {
                buildHistory.remove(0);
            }
            
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < buildHistory.size(); i++) {
                sb.append("  ").append(buildHistory.get(i).toJson());
                if (i < buildHistory.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            
            Files.write(historyFile, sb.toString().getBytes());
        } catch (Exception e) { /* ignore write errors */ }
    }

    // --- Auto Installation ---

    public void autoInstallIfMissing() throws Exception {
        if (!Files.exists(Paths.get(getBinPath()))) {
            String url = "https://services.gradle.org/distributions/gradle-8.6-bin.zip";
            downloadAndExtract(url);
        }
    }

    protected void downloadAndExtract(String downloadUrl) throws Exception {
        Path archivePath = Paths.get(baseDir, "gradle", "temp_gradle.zip");
        Files.createDirectories(archivePath.getParent());

        URL url = new URL(downloadUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, archivePath, StandardCopyOption.REPLACE_EXISTING);
        }

        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(archivePath.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                int slashIdx = name.indexOf("/");
                if (slashIdx != -1 && slashIdx < name.length() - 1) {
                    name = name.substring(slashIdx + 1);
                    File file = new File(getGradleHome(), name);
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        file.getParentFile().mkdirs();
                        Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        
        Files.deleteIfExists(archivePath);
        
        File gradleBin = new File(Paths.get(getGradleHome(), "bin", "gradle").toString());
        if (gradleBin.exists()) gradleBin.setExecutable(true);
    }

    // --- Build Console ---

    public void runBuild(String projectDir, String tasks, Consumer<String> outputLineCallback, Consumer<String> progressCallback) throws Exception {
        if (activeBuildProcess != null && activeBuildProcess.isAlive()) {
            throw new Exception("A build is already running.");
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        File projDir = new File(projectDir);
        
        List<String> cmd = new ArrayList<>();
        
        // Gradle Wrapper Detection
        File wrapperBat = new File(projDir, "gradlew.bat");
        File wrapperSh = new File(projDir, "gradlew");
        
        if (isWindows && wrapperBat.exists()) {
            cmd.add(wrapperBat.getAbsolutePath());
        } else if (!isWindows && wrapperSh.exists()) {
            wrapperSh.setExecutable(true);
            cmd.add(wrapperSh.getAbsolutePath());
        } else {
            autoInstallIfMissing();
            cmd.add(getBinPath());
        }

        cmd.addAll(Arrays.asList(tasks.split("\\s+")));

        ProcessBuilder pb = createProcessBuilder(cmd.toArray(new String[0]));
        pb.directory(projDir);
        pb.environment().put("GRADLE_HOME", getGradleHome());
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        activeBuildProcess = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeBuildProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (outputLineCallback != null) outputLineCallback.accept(line);
                
                if (progressCallback != null) {
                    if (line.startsWith("> Task :")) {
                        String taskInfo = line.substring("> Task :".length()).trim();
                        progressCallback.accept("Executing task: " + taskInfo);
                    }
                }
            }
        }

        activeBuildProcess.waitFor();
        int exitCode = activeBuildProcess.exitValue();
        String result = (exitCode == 0) ? "SUCCESS" : "FAILURE";
        long duration = System.currentTimeMillis() - startTime;
        
        BuildRecord record = new BuildRecord(LocalDateTime.now().toString(), tasks, result, duration);
        buildHistory.add(record);
        saveHistory();

        if (exitCode != 0) {
            throw new Exception("BUILD " + result + " with exit code " + exitCode);
        }
    }

    public void cancelBuild() {
        if (activeBuildProcess != null && activeBuildProcess.isAlive()) {
            activeBuildProcess.destroy();
            try {
                activeBuildProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) { /* ignore */ }
            if (activeBuildProcess.isAlive()) activeBuildProcess.destroyForcibly();
        }
    }

    // --- ServiceManager Overrides ---

    @Override
    public void start() throws Exception {
        autoInstallIfMissing();
    }

    @Override
    public void stop() throws Exception {
        cancelBuild();
    }

    @Override
    public void restart() throws Exception {
        stop();
        start();
    }

    @Override
    public String getStatus() throws Exception {
        if (activeBuildProcess != null && activeBuildProcess.isAlive()) {
            return "RUNNING_BUILD";
        }
        return Files.exists(Paths.get(getBinPath())) ? "READY" : "NOT_INSTALLED";
    }

    @Override
    public String getLogs() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (BuildRecord rec : buildHistory) {
            sb.append(String.format("[%s] %s -> %s (%d ms)\n", rec.timestamp, rec.task, rec.result, rec.durationMs));
        }
        return sb.toString();
    }

    @Override
    public String getMetrics() throws Exception {
        long successCount = buildHistory.stream().filter(b -> "SUCCESS".equals(b.result)).count();
        return "Total Builds: " + buildHistory.size() + "\nSuccessful: " + successCount;
    }
    
    public List<BuildRecord> getHistory() {
        return new ArrayList<>(buildHistory);
    }
}
