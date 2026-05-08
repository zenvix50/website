package com.zenvix.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LinuxAutoStart implements AutoStartManager {
    protected Path desktopFile = Paths.get(System.getProperty("user.home"), ".config", "autostart", "zenvix.desktop");

    @Override
    public boolean isAutoStartEnabled() {
        return Files.exists(desktopFile);
    }

    @Override
    public void setAutoStart(boolean enabled, String exePath) {
        try {
            if (enabled) {
                Files.createDirectories(desktopFile.getParent());
                String desktopEntry = "[Desktop Entry]\n" +
                                      "Type=Application\n" +
                                      "Name=zenviX\n" +
                                      "Exec=" + exePath + "\n" +
                                      "Hidden=false\n" +
                                      "NoDisplay=false\n" +
                                      "X-GNOME-Autostart-enabled=true\n";
                Files.writeString(desktopFile, desktopEntry);
            } else {
                Files.deleteIfExists(desktopFile);
            }
        } catch (Exception e) {}
    }
}
