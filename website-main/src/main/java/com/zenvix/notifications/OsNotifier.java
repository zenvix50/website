package com.zenvix.notifications;

import com.zenvix.notifications.Notification.NotificationType;

/**
 * Ensures strict polymorphic bounds encapsulating raw explicit OS execution logic natively.
 */
public interface OsNotifier {
    void send(String title, String message, NotificationType type);
}
