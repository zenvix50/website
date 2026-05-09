package com.zenvix.docker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates dynamic Docker execution pipelines automatically detecting active
 * socket daemons and integrating safe lifecycle container hooks naturally natively.
 */
public class DockerManager {

    private boolean isDockerAvailable = false;

    public DockerManager() {
        this.isDockerAvailable = checkDockerAvailability();
    }

    protected boolean checkDockerAvailability() {
        return executeCommand("docker", "info");
    }

    public boolean isAvailable() {
        return isDockerAvailable;
    }

    public boolean startContainer(String containerName) {
        if (!isAvailable()) return false;
        return executeCommand("docker", "start", containerName);
    }

    public boolean stopContainer(String containerName) {
        if (!isAvailable()) return false;
        return executeCommand("docker", "stop", containerName);
    }

    public boolean restartContainer(String containerName) {
        if (!isAvailable()) return false;
        return executeCommand("docker", "restart", containerName);
    }

    public List<String> getRunningZenvixContainers() {
        List<String> containers = new ArrayList<>();
        if (!isAvailable()) return containers;

        try {
            Process p = new ProcessBuilder("docker", "ps", "--filter", "name=zenvix-", "--format", "{{.Names}}").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                containers.add(line.trim());
            }
        } catch (Exception e) {}
        return containers;
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
