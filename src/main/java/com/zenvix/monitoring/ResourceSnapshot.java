package com.zenvix.monitoring;

/**
 * Encapsulates immutable physical OS snapshot records freezing explicit metrics 
 * at precise timestamp boundaries supporting Alert arrays.
 */
public class ResourceSnapshot {
    public final String serviceId;
    public final int pid;
    public final double cpuPercent;
    public final long ramMB;
    public final long diskUsedMB;
    public final long diskTotalMB;
    public final int networkConnections;
    public final long timestamp;

    public ResourceSnapshot(String serviceId, int pid, double cpuPercent, long ramMB, long diskUsedMB, long diskTotalMB, int networkConnections) {
        this.serviceId = serviceId;
        this.pid = pid;
        this.cpuPercent = cpuPercent;
        this.ramMB = ramMB;
        this.diskUsedMB = diskUsedMB;
        this.diskTotalMB = diskTotalMB;
        this.networkConnections = networkConnections;
        this.timestamp = System.currentTimeMillis();
    }
}
