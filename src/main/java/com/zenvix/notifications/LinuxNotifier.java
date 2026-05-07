package com.zenvix.notifications;

import com.zenvix.notifications.Notification.NotificationType;

public class LinuxNotifier implements OsNotifier {
    @Override
    public void send(String title, String message, NotificationType type) {
        try {
            Process p = new ProcessBuilder("which", "notify-send").start();
            if (p.waitFor() == 0) {
                new ProcessBuilder("notify-send", "-a", "zenviX", title, message).start();
            } else {
                new ProcessBuilder("zenity", "--notification", "--text=" + title + ": " + message).start();
            }
        } catch (Exception e) {}
    }
}
