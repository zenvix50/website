package com.zenvix.monitoring;

import com.zenvix.ui.notifications.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates snapshot limits globally traversing metric constraints seamlessly piping 
 * alert events natively across UI environments explicitly safely intercepting spam cleanly.
 */
public class AlertManager {

    private final List<AlertRule> rules = new ArrayList<>();
    private final ObservableList<AlertEvent> history = FXCollections.observableArrayList();
    private final Map<String, Instant> lastFiredAt = new ConcurrentHashMap<>();
    
    private final Map<String, Integer> consecutiveHits = new ConcurrentHashMap<>();

    private final NotificationManager notificationManager;
    private final Path configPath = Paths.get("zenvix", "config", "alerts.json");

    public AlertManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        loadRules();
    }

    private void loadRules() {
        if (!Files.exists(configPath)) {
            rules.add(new AlertRule("ANY", AlertRule.Metric.RAM, AlertRule.Operator.GT, 1024.0, AlertRule.Action.NOTIFY_WARNING, 5, 1));
            rules.add(new AlertRule("ANY", AlertRule.Metric.CPU, AlertRule.Operator.GT, 80.0, AlertRule.Action.NOTIFY_WARNING, 5, 3));
            rules.add(new AlertRule("ANY", AlertRule.Metric.DISK, AlertRule.Operator.GT, 90.0, AlertRule.Action.NOTIFY_ERROR, 60, 1));
            saveRules();
        } else {
            try {
                String content = new String(Files.readAllBytes(configPath));
                if (rules.isEmpty()) {
                    rules.add(new AlertRule("ANY", AlertRule.Metric.RAM, AlertRule.Operator.GT, 1024.0, AlertRule.Action.NOTIFY_WARNING, 5, 1));
                    rules.add(new AlertRule("ANY", AlertRule.Metric.CPU, AlertRule.Operator.GT, 80.0, AlertRule.Action.NOTIFY_WARNING, 5, 3));
                    rules.add(new AlertRule("ANY", AlertRule.Metric.DISK, AlertRule.Operator.GT, 90.0, AlertRule.Action.NOTIFY_ERROR, 60, 1));
                }
            } catch (IOException e) {}
        }
    }

    private void saveRules() {
        try {
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            Files.write(configPath, "[ { \"info\": \"mock serialized rules\" } ]".getBytes());
        } catch (IOException e) {}
    }

    public void checkRules(ResourceSnapshot snapshot) {
        for (AlertRule rule : rules) {
            if (!rule.serviceId.equals("ANY") && !rule.serviceId.equals(snapshot.serviceId)) {
                continue;
            }

            double actualValue = extractMetric(rule.metric, snapshot);
            boolean conditionMet = evaluateCondition(actualValue, rule.operator, rule.thresholdValue);

            String hitKey = rule.id + "_" + snapshot.serviceId;

            if (conditionMet) {
                int hits = consecutiveHits.getOrDefault(hitKey, 0) + 1;
                consecutiveHits.put(hitKey, hits);

                if (hits >= rule.requiredConsecutiveHits) {
                    triggerAlert(rule, snapshot, actualValue);
                }
            } else {
                consecutiveHits.put(hitKey, 0); 
            }
        }
    }

    private double extractMetric(AlertRule.Metric metric, ResourceSnapshot snapshot) {
        switch (metric) {
            case CPU: return snapshot.cpuPercent;
            case RAM: return snapshot.ramMB;
            case DISK: 
                if (snapshot.diskTotalMB == 0) return 0.0;
                return (snapshot.diskUsedMB / (double) snapshot.diskTotalMB) * 100.0;
            default: return 0.0;
        }
    }

    private boolean evaluateCondition(double actual, AlertRule.Operator operator, double threshold) {
        switch (operator) {
            case GT: return actual > threshold;
            case LT: return actual < threshold;
            case EQ: return Math.abs(actual - threshold) < 0.001;
            default: return false;
        }
    }

    private void triggerAlert(AlertRule rule, ResourceSnapshot snapshot, double actualValue) {
        String hitKey = rule.id + "_" + snapshot.serviceId;
        
        Instant lastFired = lastFiredAt.get(hitKey);
        if (lastFired != null && Instant.now().isBefore(lastFired.plus(rule.cooldownMinutes, ChronoUnit.MINUTES))) {
            return; 
        }

        lastFiredAt.put(hitKey, Instant.now());
        consecutiveHits.put(hitKey, 0); 

        String message = String.format("%s exceeded threshold: %.2f (Rule: %s %s %.2f)", 
            rule.metric, actualValue, rule.metric, rule.operator, rule.thresholdValue);

        AlertEvent event = new AlertEvent(rule.id, snapshot.serviceId, message, actualValue);
        
        Platform.runLater(() -> {
            history.add(0, event);
            if (history.size() > 200) {
                history.remove(200, history.size());
            }
        });

        if (notificationManager != null) {
            String title = "Alert: " + snapshot.serviceId;
            switch (rule.action) {
                case NOTIFY_WARNING:
                    notificationManager.notifyWarning(title, message);
                    break;
                case NOTIFY_ERROR:
                    notificationManager.notifyError(title, message);
                    break;
                case RESTART:
                    notificationManager.notifyWarning(title, message + " - Initiating Restart");
                    break;
                case LOG:
                    System.out.println("LOG ALERT: " + title + " - " + message);
                    break;
            }
        }
    }

    public ObservableList<AlertEvent> getHistory() {
        return history;
    }
    
    public List<AlertRule> getRules() {
        return rules;
    }
    
    protected void addRule(AlertRule rule) {
        rules.add(rule);
    }
    protected void clearRules() {
        rules.clear();
    }
    protected void clearCooldowns() {
        lastFiredAt.clear();
        consecutiveHits.clear();
    }
}
