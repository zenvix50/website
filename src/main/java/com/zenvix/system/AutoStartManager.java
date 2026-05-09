package com.zenvix.system;

/**
 * Formalizes explicit cross-platform physical AutoStart mappings delegating
 * specific system integration paths dynamically safely across hardware limits.
 */
public interface AutoStartManager {
    boolean isAutoStartEnabled();
    void setAutoStart(boolean enabled, String exePath);

    static AutoStartManager create() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return new WindowsAutoStart();
        if (os.contains("mac")) return new MacOSAutoStart();
        return new LinuxAutoStart();
    }
}
