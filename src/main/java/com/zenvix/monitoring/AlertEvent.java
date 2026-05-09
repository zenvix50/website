package com.zenvix.monitoring;

import java.time.LocalDateTime;

/**
 * Securely encapsulates historical trigger states tracking physical thresholds 
 * accurately mapping metrics onto explicit physical event timelines natively.
 */
public class AlertEvent {
    public final String ruleId;
    public final String serviceId;
    public final String message;
    public final double actualValue;
    public final LocalDateTime timestamp;

    public AlertEvent(String ruleId, String serviceId, String message, double actualValue) {
        this.ruleId = ruleId;
        this.serviceId = serviceId;
        this.message = message;
        this.actualValue = actualValue;
        this.timestamp = LocalDateTime.now();
    }
}
