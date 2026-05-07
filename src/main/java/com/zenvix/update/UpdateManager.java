package com.zenvix.update;

import com.zenvix.ui.notifications.NotificationManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Automates GitHub payload fetching parsing semantic arrays bridging background 
 * streaming IO gracefully integrating explicit OS rollout commands safely naturally.
 */
public class UpdateManager {

    private final String CURRENT_VERSION = "1.0.0";
    private final String REPO_OWNER = "zenviX-org";
    private final Path UPDATE_DIR = Paths.get("zenvix", "update");
    private final Path PREVIOUS_DIR = UPDATE_DIR.resolve("previous");
    
    private final NotificationManager notificationManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public enum UpdateMode { SILENT, PROMPTED, DISABLED }
    
    private UpdateMode updateMode = UpdateMode.PROMPTED; 

    public UpdateManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        try {
            Files.createDirectories(UPDATE_DIR);
            Files.createDirectories(PREVIOUS_DIR);
        } catch (Exception e) {}
    }

    public void startScheduling() {
        scheduler.schedule(this::checkForUpdates, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 24, 24, TimeUnit.HOURS);
    }

    public void checkForUpdates() {
        if (updateMode == UpdateMode.DISABLED) return;

        ReleaseInfo latest = fetchLatestRelease();
        if (latest != null && compareVersions(latest.version, CURRENT_VERSION) > 0) {
            handleNewUpdate(latest);
        }
    }

    protected ReleaseInfo fetchLatestRelease() {
        try {
            URL url = new URL("https://api.github.com/repos/" + REPO_OWNER + "/zenviX/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    String json = new String(is.readAllBytes());
                    String tagName = extractJsonField(json, "tag_name");
                    String body = extractJsonField(json, "body");
                    
                    String os = System.getProperty("os.name").toLowerCase();
                    String extension = os.contains("win") ? ".msi" : os.contains("mac") ? ".dmg" : ".deb";
                    
                    String downloadUrl = "";
                    String assetName = "";
                    
                    String[] parts = json.split("\"browser_download_url\":");
                    for (int i = 1; i < parts.length; i++) {
                        String urlField = parts[i].split("\"")[1];
                        if (urlField.endsWith(extension) || urlField.endsWith(".exe") || urlField.endsWith(".rpm")) {
                            downloadUrl = urlField;
                            assetName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
                            break;
                        }
                    }
                    
                    if (!downloadUrl.isEmpty()) {
                        return new ReleaseInfo(tagName.replace("v", ""), body, downloadUrl, assetName);
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }

    protected String extractJsonField(String json, String field) {
        String token = "\"" + field + "\":";
        int idx = json.indexOf(token);
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + token.length()) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end).replace("\\n", "\n");
    }

    public int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    protected void handleNewUpdate(ReleaseInfo release) {
        if (updateMode == UpdateMode.SILENT) {
            downloadUpdate(release);
        } else if (updateMode == UpdateMode.PROMPTED) {
            if (Platform.isFxApplicationThread()) {
                promptUserForUpdate(release);
            } else {
                Platform.runLater(() -> promptUserForUpdate(release));
            }
        }
    }

    protected void promptUserForUpdate(ReleaseInfo release) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("zenviX version " + release.version + " is available!");
        alert.setContentText("Would you like to download and install it now?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> downloadUpdate(release)).start();
        }
    }

    public boolean downloadUpdate(ReleaseInfo release) {
        try {
            Path versionDir = UPDATE_DIR.resolve(release.version);
            Files.createDirectories(versionDir);
            Path destFile = versionDir.resolve(release.assetName);
            
            URL url = new URL(release.downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            if (Platform.isFxApplicationThread()) {
                applyUpdate(destFile, release.version);
            } else {
                Platform.runLater(() -> applyUpdate(destFile, release.version));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean applyUpdate(Path installerPath, String newVersion) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            Files.createDirectories(PREVIOUS_DIR);
            
            Process p = null;
            if (os.contains("win")) {
                if (installerPath.toString().endsWith(".msi")) {
                    p = new ProcessBuilder("msiexec.exe", "/i", installerPath.toAbsolutePath().toString(), "/quiet").start();
                } else {
                    p = new ProcessBuilder(installerPath.toAbsolutePath().toString(), "/S").start();
                }
            } else if (os.contains("mac")) {
                p = new ProcessBuilder("hdiutil", "attach", installerPath.toAbsolutePath().toString()).start();
            } else {
                if (installerPath.toString().endsWith(".deb")) {
                    p = new ProcessBuilder("pkexec", "dpkg", "-i", installerPath.toAbsolutePath().toString()).start();
                } else {
                    p = new ProcessBuilder("pkexec", "rpm", "-U", installerPath.toAbsolutePath().toString()).start();
                }
            }
            if (p != null) {
                notificationManager.notifyInfo("Update Started", "zenviX will now restart to apply updates.");
                System.exit(0);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean rollback() {
        try {
            File[] files = PREVIOUS_DIR.toFile().listFiles();
            if (files != null && files.length > 0) {
                return applyUpdate(files[0].toPath(), "rollback");
            }
        } catch (Exception e) {}
        return false;
    }
    
    public void setUpdateMode(UpdateMode mode) {
        this.updateMode = mode;
    }
}
