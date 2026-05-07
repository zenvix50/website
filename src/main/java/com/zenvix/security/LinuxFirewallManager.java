package com.zenvix.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LinuxFirewallManager implements FirewallManager {

    private final boolean isUfwInstalled;

    public LinuxFirewallManager() {
        isUfwInstalled = checkCommand("which", "ufw");
    }

    @Override
    public boolean addRule(int port, String protocol, String serviceName) {
        String ruleComment = "zenviX-" + serviceName;
        if (isUfwInstalled) {
            return executeCommand("sudo", "ufw", "allow", "proto", protocol.toLowerCase(), "from", "any", "to", "any", "port", String.valueOf(port), "comment", ruleComment);
        } else {
            return executeCommand("sudo", "iptables", "-A", "INPUT", "-p", protocol.toLowerCase(), "--dport", String.valueOf(port), "-m", "comment", "--comment", ruleComment, "-j", "ACCEPT");
        }
    }

    @Override
    public boolean removeRule(int port, String protocol, String serviceName) {
        String ruleComment = "zenviX-" + serviceName;
        if (isUfwInstalled) {
            return executeCommand("sudo", "ufw", "delete", "allow", port + "/" + protocol.toLowerCase());
        } else {
            return executeCommand("sudo", "iptables", "-D", "INPUT", "-p", protocol.toLowerCase(), "--dport", String.valueOf(port), "-m", "comment", "--comment", ruleComment, "-j", "ACCEPT");
        }
    }

    @Override
    public List<String> listRules() {
        List<String> rules = new ArrayList<>();
        try {
            Process p;
            if (isUfwInstalled) {
                p = new ProcessBuilder("sudo", "ufw", "status").start();
            } else {
                p = new ProcessBuilder("sudo", "iptables", "-L", "-n").start();
            }
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
            System.err.println("zenviX requires elevated privileges to manage Linux firewall rules. Please run as root/sudo.");
        }
    }

    protected boolean checkCommand(String... cmd) {
        try {
            return new ProcessBuilder(cmd).start().waitFor() == 0;
        } catch (Exception e) { return false; }
    }

    protected boolean executeCommand(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) { return false; }
    }
}
