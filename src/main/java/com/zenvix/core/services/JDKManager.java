package com.zenvix.core.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDKManager handles multi-JDK installation, switching, and configuration.
 * Connects to Adoptium API for JDK downloads and manages JVM flags and environment variables.
 */
public class JDKManager extends ServiceManager {

    private String baseDir = "zenvix";
    private String activeVersion = "17";
    private List<String> jvmFlags = new ArrayList<>();

    public JDKManager() {
        this("zenvix");
    }

    public JDKManager(String baseDir) {
        this.baseDir = baseDir;
        loadConfig();
    }

    // --- Configuration Management ---

    private void loadConfig() {
        Path configPath = Paths.get(baseDir, "config", "jdk.json");
        if (Files.exists(configPath)) {
            try {
                String json = new String(Files.readAllBytes(configPath));
                Matcher activeMatcher = Pattern.compile("\"active\"\\s*:\\s*\"(.*?)\"").matcher(json);
                if (activeMatcher.find()) {
                    this.activeVersion = activeMatcher.group(1);
                }
                
                Matcher flagsMatcher = Pattern.compile("\"flags\"\\s*:\\s*\\[(.*?)\\]").matcher(json);
                if (flagsMatcher.find()) {
                    String flagsStr = flagsMatcher.group(1);
                    if (!flagsStr.trim().isEmpty()) {
                        String[] split = flagsStr.replace("\"", "").split(",");
                        for (String f : split) {
                            String trimmed = f.trim();
                            if (!trimmed.isEmpty()) jvmFlags.add(trimmed);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading jdk.json: " + e.getMessage());
            }
        }
    }

    public void saveConfig() throws IOException {
        Path configDir = Paths.get(baseDir, "config");
        if (!Files.exists(configDir)) Files.createDirectories(configDir);
        Path configPath = configDir.resolve("jdk.json");
        
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"active\": \"").append(activeVersion).append("\",\n");
        json.append("  \"flags\": [");
        for (int i = 0; i < jvmFlags.size(); i++) {
            json.append("\"").append(jvmFlags.get(i)).append("\"");
            if (i < jvmFlags.size() - 1) json.append(", ");
        }
        json.append("]\n}");
        
        Files.write(configPath, json.toString().getBytes());
    }

    // --- Version Switching ---

    public void switchVersion(String version) throws Exception {
        if (!Arrays.asList("8", "11", "17", "21").contains(version)) {
            throw new IllegalArgumentException("Unsupported JDK version: " + version);
        }
        this.activeVersion = version;
        saveConfig();
        
        System.setProperty("zenvix.java.home", getJavaHome());
    }

    public String getJavaHome() {
        Path jdkPath = Paths.get(baseDir, "jdk", "jdk" + activeVersion).toAbsolutePath();
        if (Files.exists(jdkPath)) {
            File dir = jdkPath.toFile();
            File[] files = dir.listFiles();
            if (files != null && files.length == 1 && files[0].isDirectory()) {
                return files[0].getAbsolutePath();
            }
            return jdkPath.toString();
        }
        
        List<String> systemJdks = detectSystemJDKs();
        if (!systemJdks.isEmpty()) {
            return systemJdks.get(0);
        }
        
        return System.getenv("JAVA_HOME");
    }

    // --- Adoptium Download ---

    public void downloadJDK(String version) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        String adoptOs = "linux";
        if (os.contains("win")) adoptOs = "windows";
        else if (os.contains("mac")) adoptOs = "mac";
        
        String urlStr = "https://api.adoptium.net/v3/assets/latest/" + version + "/hotspot?os=" + adoptOs + "&arch=x64&image_type=jdk";
        String response = fetchAdoptiumApi(urlStr);
        
        Matcher m = Pattern.compile("\"link\"\\s*:\\s*\"(https://[^\"]+)\"").matcher(response);
        if (m.find()) {
            String downloadUrl = m.group(1);
            downloadAndExtract(downloadUrl, version);
        } else {
            throw new Exception("Could not find download link in Adoptium API response.");
        }
    }

    protected String fetchAdoptiumApi(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("Adoptium API error: " + conn.getResponseCode());
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) response.append(line);
        }
        return response.toString();
    }

    protected void downloadAndExtract(String downloadUrl, String version) throws Exception {
        Path archivePath = Paths.get(baseDir, "jdk", "temp_jdk_archive");
        Files.createDirectories(archivePath.getParent());

        URL url = new URL(downloadUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, archivePath, StandardCopyOption.REPLACE_EXISTING);
        }

        Path extractDir = Paths.get(baseDir, "jdk", "jdk" + version);
        Files.createDirectories(extractDir);

        if (downloadUrl.endsWith(".zip")) {
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(archivePath.toFile()))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File file = new File(extractDir.toFile(), entry.getName());
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        file.getParentFile().mkdirs();
                        Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } else {
            Process p = new ProcessBuilder("tar", "-xzf", archivePath.toAbsolutePath().toString(), "-C", extractDir.toAbsolutePath().toString()).start();
            p.waitFor();
        }

        Files.deleteIfExists(archivePath);
    }

    // --- Process Context Injector ---

    public void configureProcessBuilder(ProcessBuilder pb) {
        String javaHome = getJavaHome();
        if (javaHome != null) {
            pb.environment().put("JAVA_HOME", javaHome);
            String pathEnv = pb.environment().get("PATH");
            if (pathEnv == null) pathEnv = System.getenv("PATH");
            String binPath = Paths.get(javaHome, "bin").toAbsolutePath().toString();
            pb.environment().put("PATH", binPath + File.pathSeparator + pathEnv);
        }
    }

    // --- System Detection ---

    public List<String> detectSystemJDKs() {
        List<String> jdks = new ArrayList<>();
        
        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null && (Files.exists(Paths.get(envJavaHome, "bin", "java")) || Files.exists(Paths.get(envJavaHome, "bin", "java.exe")))) {
            jdks.add(envJavaHome);
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        List<Path> commonPaths = new ArrayList<>();
        if (os.contains("win")) {
            commonPaths.add(Paths.get("C:\\Program Files\\Java"));
            commonPaths.add(Paths.get("C:\\Program Files\\Eclipse Adoptium"));
        } else if (os.contains("mac")) {
            commonPaths.add(Paths.get("/Library/Java/JavaVirtualMachines"));
        } else {
            commonPaths.add(Paths.get("/usr/lib/jvm"));
        }
        
        for (Path basePath : commonPaths) {
            if (Files.exists(basePath)) {
                try {
                    Files.list(basePath)
                         .filter(Files::isDirectory)
                         .filter(p -> Files.exists(p.resolve("bin/java")) || Files.exists(p.resolve("bin/java.exe")) || Files.exists(p.resolve("Contents/Home/bin/java")))
                         .forEach(p -> {
                             if (p.getFileName().toString().contains("jdk")) {
                                 if (Files.exists(p.resolve("Contents/Home"))) {
                                     jdks.add(p.resolve("Contents/Home").toAbsolutePath().toString());
                                 } else {
                                     jdks.add(p.toAbsolutePath().toString());
                                 }
                             }
                         });
                } catch (IOException e) { /* ignore */ }
            }
        }
        
        return jdks;
    }

    // --- JVM Flags Editor ---

    public void setJvmFlags(List<String> flags) throws Exception {
        for (String flag : flags) {
            if (flag.startsWith("-Xmx") || flag.startsWith("-Xms")) {
                System.out.println("WARNING: Heap memory setting detected (" + flag + "). Ensure system has sufficient memory.");
            }
            if (flag.startsWith("-XX:") && !flag.matches("-XX:[+\\-]?([a-zA-Z0-9]+(=.*)?)")) {
                throw new Exception("Invalid JVM flag format: " + flag);
            }
        }
        this.jvmFlags = new ArrayList<>(flags);
        saveConfig();
    }
    
    public List<String> getJvmFlags() {
        return new ArrayList<>(jvmFlags);
    }

    // --- ServiceManager Overrides ---

    @Override
    public void start() throws Exception {
        // JDK is not a runnable background service process.
    }

    @Override
    public void stop() throws Exception {
        // No-op
    }

    @Override
    public void restart() throws Exception {
        // No-op
    }

    @Override
    public String getStatus() throws Exception {
        return "ACTIVE JDK: " + activeVersion + " | JAVA_HOME: " + getJavaHome();
    }

    @Override
    public String getLogs() throws Exception {
        return "";
    }

    @Override
    public String getMetrics() throws Exception {
        return "System JDKs detected: " + detectSystemJDKs().size();
    }

    // --- Getters & Setters ---

    public String getActiveVersion() { return activeVersion; }
}
