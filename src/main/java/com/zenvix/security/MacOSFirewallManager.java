package com.zenvix.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MacOSFirewallManager implements FirewallManager {

    @Override
    public boolean addRule(int port, String protocol, String serviceName) {
        String pfRule = "pass in proto " + protocol.toLowerCase() + " from any to any port " + port;
        return executeCommand("sudo", "sh", "-c", "echo '" + pfRule + "' | pfctl -a \"com.apple/zenviX-" + serviceName + "\" -f -");
    }

    @Override
    public boolean removeRule(int port, String protocol, String serviceName) {
        return executeCommand("sudo", "pfctl", "-a", "com.apple/zenviX-" + serviceName, "-F", "all");
    }

    @Override
    public List<String> listRules() {
        List<String> rules = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("sudo", "pfctl", "-a", "*", "-s", "Anchors").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("zenviX-")) {
                    rules.add(line.trim());
                }
            }
        } catch (Exception e) {}
        return rules;
    }

    @Override
    public boolean hasElevatedPrivileges() {
        try {
            Process p = new ProcessBuilder("id", "-u").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String uid = reader.readLine();
            return "0".equals(uid);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void requestElevation() {
        if (!hasElevatedPrivileges()) {
            System.err.println("zenviX requires elevated privileges to manage macOS firewall rules. Please run as root/sudo.");
        }
    }

    protected boolean executeCommand(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) { return false; }
    }
}
