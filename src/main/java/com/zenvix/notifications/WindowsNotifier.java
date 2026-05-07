package com.zenvix.notifications;

import com.zenvix.notifications.Notification.NotificationType;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Toolkit;

public class WindowsNotifier implements OsNotifier {
    @Override
    public void send(String title, String message, NotificationType type) {
        try {
            String script = "New-BurntToastNotification -Text '" + title.replace("'", "''") + "', '" + message.replace("'", "''") + "'";
            Process p = new ProcessBuilder("powershell", "-Command", script).start();
            if (p.waitFor() != 0) {
                useAwtFallback(title, message, type);
            }
        } catch (Exception e) {
            useAwtFallback(title, message, type);
        }
    }

    protected void useAwtFallback(String title, String message, NotificationType type) {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            TrayIcon trayIcon = new TrayIcon(image, "zenviX");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            
            TrayIcon.MessageType msgType = TrayIcon.MessageType.INFO;
            if (type == NotificationType.ERROR || type == NotificationType.CRASH) msgType = TrayIcon.MessageType.ERROR;
            else if (type == NotificationType.WARNING) msgType = TrayIcon.MessageType.WARNING;
            
            trayIcon.displayMessage(title, message, msgType);
            
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (Exception ignored) {}
                tray.remove(trayIcon);
            }).start();
        } catch (Exception e) {}
    }
}
