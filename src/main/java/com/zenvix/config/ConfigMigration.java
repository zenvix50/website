package com.zenvix.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Standard interfaces allowing smooth transitions mapping physical node additions across version increments securely!
 */
public interface ConfigMigration {
    int fromVersion();
    int toVersion();
    void migrate(ObjectNode configJson);
}
