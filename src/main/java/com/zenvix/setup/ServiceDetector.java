package com.zenvix.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Sweeps physical system environments securely parsing local binary boundaries
 * abstracting the complexities of explicit OS specific toolchain mapping gracefully.
 */
public class ServiceDetector {

    public static class DetectedService {
        public final String name;
        public final String path;
        public final String version;
        
        public DetectedService(String name, String path, String version) {
            this.name = name;
            this.path = path;
            this.version = version;
        }
    }

    public Map<String, DetectedService> detectAll() {
        Map<String, DetectedService> detected = new HashMap<>();
        
        detect("java", "java -version", detected);
        detect("mysql", "mysql -V", detected);
        detect("postgres", "psql -V", detected);
        detect("redis", "redis-server -v", detected);
        
        String javaHome = getJavaHome();
        if (javaHome != null && !detected.containsKey("java")) {
            detected.put("java", new DetectedService("java", javaHome, "Unknown (JAVA_HOME)"));
        }
        
        return detected;
    }

    protected void detect(String name, String versionCmd, Map<String, DetectedService> detected) {
        String path = findInPath(name);
        if (path != null) {
            String version = getVersion(versionCmd);
            detected.put(name, new DetectedService(name, path, version));
        }
    }

    protected String findInPath(String binary) {
        String pathEnv = getSystemPath();
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            for (String p : paths) {
                File f = new File(p, binary);
                if (f.exists() && f.canExecute()) return f.getAbsolutePath();
                
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    File winF = new File(p, binary + ".exe");
                    if (winF.exists() && winF.canExecute()) return winF.getAbsolutePath();
                }
            }
        }
        return null;
    }

    protected String getSystemPath() {
        return System.getenv("PATH");
    }

    protected String getJavaHome() {
        return System.getenv("JAVA_HOME");
    }

    protected String getVersion(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd.split(" ")).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty()) {
                reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                line = reader.readLine(); 
            }
            return line != null ? line.trim() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
