package com.zenvix.security;

import java.util.List;

/**
 * Standard interface executing unified boundary mapping resolving physical 
 * port configurations natively across specific OS implementations.
 */
public interface FirewallManager {
    boolean addRule(int port, String protocol, String serviceName);
    boolean removeRule(int port, String protocol, String serviceName);
    List<String> listRules();
    boolean hasElevatedPrivileges();
    void requestElevation();
}
