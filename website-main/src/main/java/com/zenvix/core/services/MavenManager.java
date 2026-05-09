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
 * MavenManager handles Apache Maven installations, dependency tracking,
 * configures settings.xml, runs builds, and parses output streams to the UI.
 */
public class MavenManager extends ServiceManager {

    private String baseDir = "zenvix";
    private Process activeBuildProcess;
    private final List<BuildRecord> buildHistory = new CopyOnWriteArrayList<>();

    public MavenManager() {
        this("zenvix");
    }

    public MavenManager(String baseDir) {
        this.baseDir = baseDir;
        loadHistory();
    }

    public static class BuildRecord {
        public String timestamp;
        public String goal;
        public String result;
        public long durationMs;

        public BuildRecord(String timestamp, String goal, String result, long durationMs) {
            this.timestamp = timestamp;
            this.goal = goal;
            this.result = result;
            this.durationMs = durationMs;
        }

        public String toJson() {
            return String.format("{\"timestamp\":\"%s\", \"goal\":\"%s\", \"result\":\"%s\", \"durationMs\":%d}", timestamp, goal, result, durationMs);
        }
    }

    // --- Path Management ---

    public String getMavenHome() {
        return Paths.get(baseDir, "maven").toAbsolutePath().toString();
    }

    public String getBinPath() {
        String ext = System.getProperty("os.name").toLowerCase().contains("win") ? ".cmd" : "";
        return Paths.get(getMavenHome(), "bin", "mvn" + ext).toString();
    }

    public String getSettingsPath() {
        return Paths.get(getMavenHome(), "conf", "settings.xml").toAbsolutePath().toString();
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }

    // --- History Persistence ---

    private void loadHistory() {
        Path historyFile = Paths.get(baseDir, "config", "maven-history.json");
        if (Files.exists(historyFile)) {
            try {
                String content = new String(Files.readAllBytes(historyFile));
                Matcher m = Pattern.compile("\\{\"timestamp\":\"(.*?)\",\\s*\"goal\":\"(.*?)\",\\s*\"result\":\"(.*?)\",\\s*\"durationMs\":(\\d+)\\}").matcher(content);
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
            Path historyFile = configDir.resolve("maven-history.json");
            
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
            String url = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip";
            downloadAndExtract(url);
        }
    }

    protected void downloadAndExtract(String downloadUrl) throws Exception {
        Path archivePath = Paths.get(baseDir, "maven", "temp_maven.zip");
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
                    File file = new File(getMavenHome(), name);
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
        
        File mvn = new File(Paths.get(getMavenHome(), "bin", "mvn").toString());
        if (mvn.exists()) mvn.setExecutable(true);
    }

    // --- Build Console ---

    public void runBuild(String projectDir, String goals, Consumer<String> outputLineCallback, Consumer<String> progressCallback) throws Exception {
        autoInstallIfMissing();
        
        if (activeBuildProcess != null && activeBuildProcess.isAlive()) {
            throw new Exception("A build is already running.");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(getBinPath());
        cmd.addAll(Arrays.asList(goals.split("\\s+")));

        ProcessBuilder pb = createProcessBuilder(cmd.toArray(new String[0]));
        pb.directory(new File(projectDir));
        pb.environment().put("MAVEN_HOME", getMavenHome());
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        activeBuildProcess = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeBuildProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (outputLineCallback != null) outputLineCallback.accept(line);
                
                if (progressCallback != null) {
                    if (line.contains("Downloading from ") || line.contains("Downloading: ")) {
                        String artifact = line.substring(line.indexOf(":") + 1).trim();
                        progressCallback.accept("Downloading " + artifact + "...");
                    } else if (line.contains("Downloaded from ") || line.contains("Downloaded: ")) {
                        String artifact = line.substring(line.indexOf(":") + 1).trim();
                        progressCallback.accept("Completed " + artifact);
                    }
                }
            }
        }

        activeBuildProcess.waitFor();
        int exitCode = activeBuildProcess.exitValue();
        String result = (exitCode == 0) ? "SUCCESS" : "FAILURE";
        long duration = System.currentTimeMillis() - startTime;
        
        BuildRecord record = new BuildRecord(LocalDateTime.now().toString(), goals, result, duration);
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

    // --- Settings Editor ---

    public void updateSettings(String localRepo, String mirrorsXmlSnippet) throws Exception {
        Path settingsPath = Paths.get(getSettingsPath());
        if (!Files.exists(settingsPath)) {
            Files.createDirectories(settingsPath.getParent());
            Files.write(settingsPath, "<settings>\n</settings>".getBytes());
        }
        
        String xml = new String(Files.readAllBytes(settingsPath));
        
        if (localRepo != null && !localRepo.isEmpty()) {
            if (xml.contains("<localRepository>")) {
                xml = xml.replaceAll("<localRepository>.*?</localRepository>", "<localRepository>" + localRepo + "</localRepository>");
            } else {
                xml = xml.replace("</settings>", "  <localRepository>" + localRepo + "</localRepository>\n</settings>");
            }
        }
        
        if (mirrorsXmlSnippet != null && !mirrorsXmlSnippet.isEmpty()) {
            if (xml.contains("<mirrors>")) {
                xml = xml.replaceAll("(?s)<mirrors>.*?</mirrors>", "<mirrors>\n" + mirrorsXmlSnippet + "\n  </mirrors>");
            } else {
                xml = xml.replace("</settings>", "  <mirrors>\n" + mirrorsXmlSnippet + "\n  </mirrors>\n</settings>");
            }
        }
        
        Files.write(settingsPath, xml.getBytes());
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
            sb.append(String.format("[%s] %s -> %s (%d ms)\n", rec.timestamp, rec.goal, rec.result, rec.durationMs));
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
