package com.zenvix.core;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automates physical environment degradation checks gracefully preventing 
 * blocking UI threads during disconnected states seamlessly adapting workflows natively.
 */
public class OfflineModeManager {

    private final AtomicBoolean isOnline = new AtomicBoolean(true);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public OfflineModeManager() {
        scheduler.scheduleAtFixedRate(this::checkNetworkStatus, 0, 60, TimeUnit.SECONDS);
    }

    protected void checkNetworkStatus() {
        try {
            URL url = new URL("https://api.github.com");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            int responseCode = conn.getResponseCode();
            isOnline.set(responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            isOnline.set(false);
        }
    }

    public boolean isOnline() {
        return isOnline.get();
    }

    public boolean executeInternetRequiredFeature(Runnable action) {
        if (isOnline.get()) {
            action.run();
            return true;
        } else {
            showOfflineDialog();
            return false;
        }
    }

    protected void showOfflineDialog() {
        if (Platform.isFxApplicationThread()) {
            displayAlert();
        } else {
            Platform.runLater(this::displayAlert);
        }
    }

    private void displayAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Offline Mode");
        alert.setHeaderText("No Internet Connection");
        alert.setContentText("This feature requires internet access. zenviX is currently offline and running in local-only mode.");
        alert.showAndWait();
    }

    protected String getSystemPath() {
        return System.getenv("PATH");
    }

    public String detectSystemService(String serviceName) {
        String[] binaries;
        switch (serviceName.toLowerCase()) {
            case "mysql": binaries = new String[]{"mysql", "mysqld"}; break;
            case "postgres": binaries = new String[]{"postgres", "psql"}; break;
            case "redis": binaries = new String[]{"redis-server"}; break;
            case "tomcat": binaries = new String[]{"catalina", "catalina.sh", "catalina.bat"}; break;
            default: return null;
        }

        String pathEnv = getSystemPath();
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            for (String p : paths) {
                for (String binary : binaries) {
                    File f = new File(p, binary);
                    if (f.exists() && f.isFile() && f.canExecute()) {
                        return f.getAbsolutePath();
                    }
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        String[] exts = {".exe", ".bat", ".cmd"};
                        for (String ext : exts) {
                            File winF = new File(p, binary + ext);
                            if (winF.exists() && winF.isFile() && winF.canExecute()) {
                                return winF.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
