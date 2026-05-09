package com.zenvix.monitoring;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks physical hardware bounds scaling Process tracking securely parsing OS process 
 * boundaries natively mapping telemetry accurately into Alert Managers securely.
 */
public class ResourceMonitor {

    private final SystemInfo systemInfo = new SystemInfo();
    private final OperatingSystem os = systemInfo.getOperatingSystem();
    
    private final Map<String, ResourceSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, ServiceMeta> managedServices = new ConcurrentHashMap<>();
    
    private ScheduledExecutorService scheduler;
    private AlertManager alertManager;

    public static class ServiceMeta {
        public String serviceId;
        public int pid;
        public int port;
        public Path dataDirectory;
        
        public long previousCpuTime = 0;
        public long previousUpTime = 0;

        public ServiceMeta(String serviceId, int pid, int port, Path dataDirectory) {
            this.serviceId = serviceId;
            this.pid = pid;
            this.port = port;
            this.dataDirectory = dataDirectory;
        }
    }

    public void setAlertManager(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    public void registerService(String serviceId, int pid, int port, Path dataDirectory) {
        managedServices.put(serviceId, new ServiceMeta(serviceId, pid, port, dataDirectory));
    }

    public void unregisterService(String serviceId) {
        managedServices.remove(serviceId);
        snapshots.remove(serviceId);
    }

    public void startMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(this::pollResources, 0, 5, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    protected void pollResources() {
        for (ServiceMeta meta : managedServices.values()) {
            ResourceSnapshot snapshot = captureSnapshot(meta);
            if (snapshot != null) {
                snapshots.put(meta.serviceId, snapshot);
                if (alertManager != null) {
                    alertManager.checkRules(snapshot);
                }
            }
        }
    }

    protected ResourceSnapshot captureSnapshot(ServiceMeta meta) {
        OSProcess process = os.getProcess(meta.pid);
        if (process == null) return null; 

        long currentCpuTime = process.getKernelTime() + process.getUserTime();
        long currentUpTime = process.getUpTime();
        
        double cpuPercent = 0.0;
        if (meta.previousUpTime > 0 && currentUpTime > meta.previousUpTime) {
            long cpuDelta = currentCpuTime - meta.previousCpuTime;
            long timeDelta = currentUpTime - meta.previousUpTime;
            int logicalCores = systemInfo.getHardware().getProcessor().getLogicalProcessorCount();
            cpuPercent = 100d * (cpuDelta / (double) timeDelta) / logicalCores;
        }
        
        meta.previousCpuTime = currentCpuTime;
        meta.previousUpTime = currentUpTime;

        long ramMB = process.getResidentSetSize() / (1024 * 1024);

        long diskUsedMB = 0;
        long diskTotalMB = 0;
        if (meta.dataDirectory != null && Files.exists(meta.dataDirectory)) {
            try {
                FileStore store = Files.getFileStore(meta.dataDirectory);
                diskTotalMB = store.getTotalSpace() / (1024 * 1024);
                long usable = store.getUsableSpace() / (1024 * 1024);
                diskUsedMB = diskTotalMB - usable;
            } catch (Exception e) { }
        }

        int networkConnections = countNetworkConnections(meta.port);

        return new ResourceSnapshot(meta.serviceId, meta.pid, cpuPercent, ramMB, diskUsedMB, diskTotalMB, networkConnections);
    }

    protected int countNetworkConnections(int port) {
        if (port <= 0) return 0;
        int count = 0;
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            Process p;
            if (osName.contains("win")) {
                p = new ProcessBuilder("netstat", "-ano").start();
            } else {
                p = new ProcessBuilder("netstat", "-an").start();
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String portStr = ":" + port;
            while ((line = reader.readLine()) != null) {
                if (line.contains(portStr) && line.contains("ESTABLISHED")) {
                    count++;
                }
            }
            p.waitFor(1, TimeUnit.SECONDS);
        } catch (Exception e) { }
        
        return count;
    }

    public ResourceSnapshot getSnapshot(String serviceId) {
        return snapshots.get(serviceId);
    }

    public double getTotalCpuPercent() {
        return snapshots.values().stream().mapToDouble(s -> s.cpuPercent).sum();
    }

    public long getTotalRamMB() {
        return snapshots.values().stream().mapToLong(s -> s.ramMB).sum();
    }
}
