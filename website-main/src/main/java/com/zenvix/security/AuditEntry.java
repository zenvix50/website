package com.zenvix.security;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Immutable audit trace object freezing physical execution records mapping 
 * exactly against the GDPR boundary compliance dynamically.
 */
public class AuditEntry {
    public enum ActionType {
        SERVICE_START, SERVICE_STOP, CONFIG_CHANGE, PASSWORD_CHANGE, VAULT_ACCESS
    }

    public final String id;
    public final ZonedDateTime timestamp;
    public final ActionType actionType;
    public final String serviceId;
    public final String configKey;
    public final String actor;
    public String entryHash;

    public AuditEntry() {
        this.id = null;
        this.timestamp = null;
        this.actionType = null;
        this.serviceId = null;
        this.configKey = null;
        this.actor = null;
    }

    public AuditEntry(ActionType actionType, String serviceId, String configKey, String actor) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = ZonedDateTime.now();
        this.actionType = actionType;
        this.serviceId = serviceId;
        this.configKey = configKey;
        this.actor = actor;
        this.entryHash = ""; 
    }

    public String getRawContent() {
        return id + "|" + timestamp.toString() + "|" + actionType + "|" + serviceId + "|" + configKey + "|" + actor;
    }
}
