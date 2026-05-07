package com.zenvix.core.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Standardizes explicit cross-platform Service tracking boundaries smoothly wrapping 
 * thread-safe Retries, automated Shutdown hooks, and physical background Process isolation natively.
 */
public abstract class ServiceManager {

    public enum ServiceStatus { STOPPED, STARTING, RUNNING, STOPPING, CRASHED, UNKNOWN }
    
    public static class ServiceMetrics {
        public long pid;
        public double cpuPercent;
        public long ramMB;
        public long uptime;
        public int connections;
    }

    protected Process currentProcess;
    protected ServiceStatus currentStatus = ServiceStatus.STOPPED;

    public abstract void start() throws ZenviXException;
    public abstract void stop() throws ZenviXException;
    public abstract void restart() throws ZenviXException;
    public abstract ServiceStatus getStatus();
    public abstract String getLogs();
    public abstract ServiceMetrics getMetrics();

    protected Process buildProcess(List<String> command, Map<String, String> env) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (env != null) pb.environment().putAll(env);
        return pb.start();
    }

    protected void startOutputCapture(Process p, Consumer<String> lineHandler) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) lineHandler.accept(line);
            } catch (Exception e) {}
        }).start();
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) lineHandler.accept(line);
            } catch (Exception e) {}
        }).start();
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected <T> T retryWithBackoff(Callable<T> action, int maxAttempts, long initialDelayMs) throws Exception {
        int attempt = 0;
        long delay = initialDelayMs;
        while (true) {
            try {
                return action.call();
            } catch (Exception e) {
                if (++attempt >= maxAttempts) throw e;
                Thread.sleep(delay);
                delay *= 2;
            }
        }
    }

    protected void writePidFile(Path pidFile, long pid) throws Exception {
        Files.writeString(pidFile, String.valueOf(pid));
    }

    protected long readPidFile(Path pidFile) {
        try {
            return Long.parseLong(Files.readString(pidFile).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    protected void addShutdownHook(Runnable shutdownAction) {
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownAction));
    }
}
