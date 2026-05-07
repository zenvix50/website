package com.zenvix.core.services;

/**
 * Abstract base class for all service managers in zenviX.
 */
public abstract class ServiceManager {
    public abstract void start() throws Exception;
    public abstract void stop() throws Exception;
    public abstract void restart() throws Exception;
    public abstract String getStatus() throws Exception;
    public abstract String getLogs() throws Exception;
    public abstract String getMetrics() throws Exception;
}
