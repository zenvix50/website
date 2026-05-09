package com.zenvix.ui.notifications;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.function.Consumer;

/**
 * Core manager responsible for integrating native OS Toast messaging bindings alongside 
 * the in-app history collections executing Alert popups natively.
 */
public class NotificationManager {

    private final ObservableList<Notification> history = FXCollections.observableArrayList();
    private NotificationBell bell;
    
    private Consumer<String> onRestartService;
    private Consumer<String> onViewLogs;

    public void setBell(NotificationBell bell) {
        this.bell = bell;
        this.bell.setManager(this);
    }

    public void setCrashCallbacks(Consumer<String> onRestartService, Consumer<String> onViewLogs) {
        this.onRestartService = onRestartService;
        this.onViewLogs = onViewLogs;
    }

    public ObservableList<Notification> getHistory() {
        return history;
    }

    public void notifyInfo(String title, String message) {
        addNotification(new Notification(Notification.Type.INFO, title, message));
    }

    public void notifyWarning(String title, String message) {
        addNotification(new Notification(Notification.Type.WARNING, title, message));
    }

    public void notifyError(String title, String message) {
        addNotification(new Notification(Notification.Type.ERROR, title, message));
    }

    public void notifyCrash(String serviceName) {
        String title = serviceName + " Crashed!";
        String message = "Service terminated unexpectedly.";
        addNotification(new Notification(Notification.Type.CRASH, title, message));
        showCrashDialog(serviceName);
    }

    private void addNotification(Notification notification) {
        Platform.runLater(() -> {
            history.add(0, notification); 
            if (history.size() > 50) {
                history.remove(50, history.size());
            }
            if (bell != null) {
                bell.incrementUnread();
            }
            sendOSToast(notification.title, notification.message, notification.type);
        });
    }

    public void markAllAsRead() {
        for (Notification n : history) {
            n.read = true;
        }
        if (bell != null) {
            bell.resetUnread();
        }
    }

    protected void sendOSToast(String title, String message, Notification.Type type) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                sendWindowsToast(title, message, type);
            } else if (os.contains("mac")) {
                sendMacOSToast(title, message);
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                sendLinuxToast(title, message);
            }
        } catch (Exception e) {
            // Execution swallowed safely blocking native architecture failures naturally
        }
    }

    protected void sendWindowsToast(String title, String message, Notification.Type type) throws Exception {
        String psCommand = 
            "[reflection.assembly]::loadwithpartialname('System.Windows.Forms') | Out-Null; " +
            "$n = new-object system.windows.forms.notifyicon; " +
            "$n.icon = [system.drawing.systemicons]::information; " +
            "$n.visible = $true; " +
            "$n.showballoontip(30000, '" + title.replace("'", "''") + "', '" + message.replace("'", "''") + "', [system.windows.forms.tooltipicon]::info); " +
            "Start-Sleep -s 2; " + 
            "$n.dispose();";

        executeCommand("powershell.exe", "-Command", psCommand);
    }

    protected void sendMacOSToast(String title, String message) throws Exception {
        String script = String.format("display notification \"%s\" with title \"zenviX\" subtitle \"%s\"", 
                                      message.replace("\"", "\\\""), 
                                      title.replace("\"", "\\\""));
        executeCommand("osascript", "-e", script);
    }

    protected void sendLinuxToast(String title, String message) throws Exception {
        executeCommand("notify-send", "-a", "zenviX", title, message);
    }

    protected void executeCommand(String... command) throws Exception {
        new ProcessBuilder(command).start();
    }

    protected void showCrashDialog(String serviceName) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showCrashDialog(serviceName));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Service Crashed");
        alert.setHeaderText(serviceName + " crashed!");
        alert.setContentText("The service terminated unexpectedly. Restart automatically?");

        ButtonType btnRestart = new ButtonType("Restart Now");
        ButtonType btnLogs = new ButtonType("View Logs");
        ButtonType btnIgnore = new ButtonType("Ignore", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnRestart, btnLogs, btnIgnore);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnRestart && onRestartService != null) {
                onRestartService.accept(serviceName);
            } else if (type == btnLogs && onViewLogs != null) {
                onViewLogs.accept(serviceName);
            }
        });
    }
}
