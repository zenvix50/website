package com.zenvix.monitoring;

/**
 * Encapsulates precise socket validation states natively returning occupying
 * process signatures strictly isolating UI boundaries from deep OS executions seamlessly.
 */
public class PortScanResult {
    public final int port;
    public final boolean inUse;
    public final String occupyingProcessName;
    public final int occupyingPid;

    public PortScanResult(int port, boolean inUse, String occupyingProcessName, int occupyingPid) {
        this.port = port;
        this.inUse = inUse;
        this.occupyingProcessName = occupyingProcessName;
        this.occupyingPid = occupyingPid;
    }
}
