package com.zenvix.notifications;

import com.zenvix.notifications.Notification.NotificationType;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Standardizes global Application notification events securely tracking specific
 * user interactions flawlessly routing background Toast commands across hardware securely.
 */
public class NotificationManager {

    private final OsNotifier osNotifier;
    private final List<Notification> history = new LinkedList<>();

    public NotificationManager(OsNotifier osNotifier) {
        this.osNotifier = osNotifier;
    }

    public NotificationManager() {
        this(NotifierFactory.createNotifier());
    }

    public void notify(NotificationType type, String title, String message) {
        Notification n = new Notification(type, title, message);
        addInAppNotification(n);
        osNotifier.send(title, message, type);
    }

    public void notifyCrash(String serviceName) {
        String msg = serviceName + " has unexpectedly crashed.";
        notify(NotificationType.CRASH, "Service Crashed", msg);
        showCrashDialog(serviceName);
    }

    public void addInAppNotification(Notification n) {
        synchronized (history) {
            history.add(0, n);
            if (history.size() > 50) {
                history.remove(history.size() - 1);
            }
        }
    }

    public List<Notification> getHistory() {
        synchronized (history) {
            return Collections.unmodifiableList(history);
        }
    }

    protected void showCrashDialog(String serviceName) {
        if (Platform.isFxApplicationThread()) {
            displayCrashAlert(serviceName);
        } else {
            try {
                Platform.runLater(() -> displayCrashAlert(serviceName));
            } catch (IllegalStateException e) {
            }
        }
    }

    private void displayCrashAlert(String serviceName) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Service Crash Detected");
        alert.setHeaderText(serviceName + " Crashed!");
        alert.setContentText("The service has stopped unexpectedly. Please check the logs.");
        alert.show();
    }
}
