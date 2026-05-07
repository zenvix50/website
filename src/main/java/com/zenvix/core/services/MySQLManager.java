package com.zenvix.core.services;

import java.util.Arrays;

public class MySQLManager extends ServiceManager {

    private int port = 3306;

    @Override
    public void start() throws ZenviXException {
        if (!isPortAvailable(port)) {
            throw new ZenviXException.PortConflictException("MySQL port is busy.", "Port " + port + " is bound.", "Change MySQL port.");
        }
        currentStatus = ServiceStatus.STARTING;
        try {
            currentProcess = buildProcess(Arrays.asList("mysqld", "--port=" + port), null);
            startOutputCapture(currentProcess, System.out::println);
            currentStatus = ServiceStatus.RUNNING;
        } catch (Exception e) {
            currentStatus = ServiceStatus.CRASHED;
            throw new ZenviXException.ServiceStartException("Failed to start MySQL", e.getMessage(), "Check logs");
        }
    }

    @Override
    public void stop() throws ZenviXException {
        currentStatus = ServiceStatus.STOPPING;
        if (currentProcess != null) {
            currentProcess.destroy();
            currentStatus = ServiceStatus.STOPPED;
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
        return "MySQL logs...";
    }

    @Override
    public ServiceMetrics getMetrics() {
        ServiceMetrics m = new ServiceMetrics();
        m.pid = currentProcess != null ? currentProcess.pid() : -1;
        return m;
    }
}
