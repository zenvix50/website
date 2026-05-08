package com.zenvix.core.services;

import java.util.Arrays;

public class NginxManager extends ServiceManager {

    private int port = 80;

    @Override
    public void start() throws ZenviXException {
        if (!isPortAvailable(port)) {
            throw new ZenviXException.PortConflictException("Nginx port is busy.", "Port " + port + " is bound.", "Change Nginx port.");
        }
        currentStatus = ServiceStatus.STARTING;
        try {
            currentProcess = buildProcess(Arrays.asList("nginx"), null);
            startOutputCapture(currentProcess, System.out::println);
            currentStatus = ServiceStatus.RUNNING;
        } catch (Exception e) {
            currentStatus = ServiceStatus.CRASHED;
            throw new ZenviXException.ServiceStartException("Failed to start Nginx", e.getMessage(), "Check logs");
        }
    }

    @Override
    public void stop() throws ZenviXException {
        currentStatus = ServiceStatus.STOPPING;
        try {
            buildProcess(Arrays.asList("nginx", "-s", "stop"), null);
            if (currentProcess != null) currentProcess.destroy();
            currentStatus = ServiceStatus.STOPPED;
        } catch (Exception e) {
            throw new ZenviXException.ServiceStopException("Failed to stop Nginx", e.getMessage(), "Manually kill process");
        }
    }

    @Override
    public void restart() throws ZenviXException {
        stop();
        start();
    }

    @Override
    public ServiceStatus getStatus() {
        return currentStatus;
    }

    @Override
    public String getLogs() {
        return "Nginx logs...";
    }

    @Override
    public ServiceMetrics getMetrics() {
        ServiceMetrics m = new ServiceMetrics();
        m.pid = currentProcess != null ? currentProcess.pid() : -1;
        return m;
    }
}
