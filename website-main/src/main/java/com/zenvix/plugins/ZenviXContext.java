package com.zenvix.plugins;

import com.zenvix.ui.notifications.NotificationManager;
import java.nio.file.Path;

/**
 * Maps global Context structures wrapping immutable pointers protecting global
 * states gracefully exposing endpoints correctly explicitly.
 */
public class ZenviXContext {
    private final NotificationManager notificationManager;
    private final Path configDirectory;
    private final Path dataDirectory;

    public ZenviXContext(NotificationManager notificationManager, Path configDirectory, Path dataDirectory) {
        this.notificationManager = notificationManager;
        this.configDirectory = configDirectory;
        this.dataDirectory = dataDirectory;
    }

    public NotificationManager getNotificationManager() { return notificationManager; }
    public Path getConfigDirectory() { return configDirectory; }
    public Path getDataDirectory() { return dataDirectory; }
}
