package com.zenvix.notifications;

public class NotifierFactory {
    public static OsNotifier createNotifier() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return new WindowsNotifier();
        if (os.contains("mac")) return new MacOSNotifier();
        return new LinuxNotifier();
    }
}
