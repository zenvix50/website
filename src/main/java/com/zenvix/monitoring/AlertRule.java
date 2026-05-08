package com.zenvix.monitoring;

import java.util.UUID;

/**
 * Encapsulates highly dynamic alerting constraints natively bridging metrics 
 * against specific mathematical thresholds mapping securely to executable logic.
 */
public class AlertRule {
    public enum Metric { CPU, RAM, DISK }
    public enum Operator { GT, LT, EQ }
    public enum Action { NOTIFY_WARNING, NOTIFY_ERROR, RESTART, LOG }

    public String id;
    public String serviceId; 
    public Metric metric;
    public Operator operator;
    public double thresholdValue;
    public Action action;
    public int cooldownMinutes;
    public int requiredConsecutiveHits; 

    public AlertRule(String serviceId, Metric metric, Operator operator, double thresholdValue, Action action, int cooldownMinutes, int requiredConsecutiveHits) {
        this.id = UUID.randomUUID().toString();
        this.serviceId = serviceId;
        this.metric = metric;
        this.operator = operator;
        this.thresholdValue = thresholdValue;
        this.action = action;
        this.cooldownMinutes = cooldownMinutes;
        this.requiredConsecutiveHits = requiredConsecutiveHits;
    }
}
