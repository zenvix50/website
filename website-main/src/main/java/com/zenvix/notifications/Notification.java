package com.zenvix.notifications;

import java.time.LocalDateTime;

/**
 * Immutable Notification payload tracking discrete OS alerts robustly accurately.
 */
public class Notification {
    public enum NotificationType { INFO, WARNING, ERROR, CRASH }
    
    public final NotificationType type;
    public final String title;
    public final String message;
    public final LocalDateTime timestamp;

    public Notification(NotificationType type, String title, String message) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
