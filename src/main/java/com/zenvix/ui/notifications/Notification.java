package com.zenvix.ui.notifications;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Standard POJO representing structural Notification blocks safely tracking read statuses.
 */
public class Notification {
    public enum Type { INFO, WARNING, ERROR, CRASH }

    public final String id;
    public final Type type;
    public final String title;
    public final String message;
    public final LocalDateTime timestamp;
    public boolean read;

    public Notification(Type type, String title, String message) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }
}
