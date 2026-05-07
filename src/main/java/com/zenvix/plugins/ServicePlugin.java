package com.zenvix.plugins;

import javafx.scene.Node;

/**
 * Standard contract mapping dynamic Third-Party closures natively allowing 
 * external packages to seamlessly mount into the zenviX architecture strictly securely.
 */
public interface ServicePlugin {

    public enum ServiceStatus { RUNNING, STOPPED, ERROR, STARTING, STOPPING }

    public static class ServiceException extends Exception {
        public ServiceException(String msg) { super(msg); }
        public ServiceException(String msg, Throwable cause) { super(msg, cause); }
    }

    String getId();           
    String getName();         
    String getVersion();      
    String getDescription();
    
    void initialize(ZenviXContext context); 
    void start() throws ServiceException;
    void stop() throws ServiceException;
    
    ServiceStatus getStatus();
    Node getControlPanelRow(); 
    void shutdown();           
}
