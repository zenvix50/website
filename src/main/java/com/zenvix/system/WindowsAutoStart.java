package com.zenvix.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WindowsAutoStart implements AutoStartManager {
    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "zenviX";

    @Override
    public boolean isAutoStartEnabled() {
        try {
            Process p = new ProcessBuilder("reg", "query", REG_KEY, "/v", VALUE_NAME).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(VALUE_NAME)) return true;
            }
        } catch (Exception e) {}
        return false;
    }

    @Override
    public void setAutoStart(boolean enabled, String exePath) {
        try {
            if (enabled) {
                new ProcessBuilder("reg", "add", REG_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", exePath, "/f").start().waitFor();
            } else {
                new ProcessBuilder("reg", "delete", REG_KEY, "/v", VALUE_NAME, "/f").start().waitFor();
            }
        } catch (Exception e) {}
    }
}
