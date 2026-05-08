package com.zenvix.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WindowsFirewallManager implements FirewallManager {

    @Override
    public boolean addRule(int port, String protocol, String serviceName) {
        String ruleName = "zenviX-" + serviceName;
        return executeCommand("netsh", "advfirewall", "firewall", "add", "rule", 
            "name=" + ruleName, "dir=in", "action=allow", "protocol=" + protocol.toUpperCase(), "localport=" + port);
    }

    @Override
    public boolean removeRule(int port, String protocol, String serviceName) {
        String ruleName = "zenviX-" + serviceName;
        return executeCommand("netsh", "advfirewall", "firewall", "delete", "rule", "name=" + ruleName);
    }

    @Override
    public List<String> listRules() {
        List<String> rules = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("netsh", "advfirewall", "firewall", "show", "rule", "name=all").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Rule Name:")) {
                    if (line.contains("zenviX-")) {
                        rules.add(line.replace("Rule Name:", "").trim());
                    }
                }
            }
        } catch (Exception e) {}
        return rules;
    }

    @Override
    public boolean hasElevatedPrivileges() {
        try {
            Class<?> shell32 = Class.forName("com.sun.jna.platform.win32.Shell32");
            Object instance = shell32.getField("INSTANCE").get(null);
            java.lang.reflect.Method method = shell32.getMethod("IsUserAnAdmin");
            return (Boolean) method.invoke(instance);
        } catch (Throwable t) {
            try {
                return new ProcessBuilder("net", "session").start().waitFor() == 0;
            } catch (Exception e) { return false; }
        }
    }

    @Override
    public void requestElevation() {
        if (!hasElevatedPrivileges()) {
            try {
                new ProcessBuilder("powershell", "-Command", "Start-Process", "java", "-ArgumentList", "'-jar zenvix.jar'", "-Verb", "RunAs").start();
                System.exit(0);
            } catch (Exception e) {}
        }
    }

    protected boolean executeCommand(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
