package com.zenvix.notifications;

import com.zenvix.notifications.Notification.NotificationType;

public class MacOSNotifier implements OsNotifier {
    @Override
    public void send(String title, String message, NotificationType type) {
        String script = String.format("display notification \"%s\" with title \"zenviX\" subtitle \"%s\"", 
            message.replace("\"", "\\\""), 
            title.replace("\"", "\\\""));
        try {
            new ProcessBuilder("osascript", "-e", script).start();
        } catch (Exception e) {}
    }
}
