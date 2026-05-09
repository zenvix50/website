package com.zenvix.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MacOSAutoStart implements AutoStartManager {
    protected Path plistPath = Paths.get(System.getProperty("user.home"), "Library", "LaunchAgents", "com.zenvix.plist");

    @Override
    public boolean isAutoStartEnabled() {
        return Files.exists(plistPath);
    }

    @Override
    public void setAutoStart(boolean enabled, String exePath) {
        try {
            if (enabled) {
                Files.createDirectories(plistPath.getParent());
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                             "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                             "<plist version=\"1.0\">\n" +
                             "<dict>\n" +
                             "  <key>Label</key>\n" +
                             "  <string>com.zenvix</string>\n" +
                             "  <key>ProgramArguments</key>\n" +
                             "  <array>\n" +
                             "    <string>" + exePath + "</string>\n" +
                             "  </array>\n" +
                             "  <key>RunAtLoad</key>\n" +
                             "  <true/>\n" +
                             "</dict>\n" +
                             "</plist>";
                Files.writeString(plistPath, xml);
            } else {
                Files.deleteIfExists(plistPath);
            }
        } catch (Exception e) {}
    }
}
