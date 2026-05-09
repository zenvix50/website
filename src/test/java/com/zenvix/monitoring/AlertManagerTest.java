package com.zenvix.monitoring;

import com.zenvix.ui.notifications.NotificationManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
public class AlertManagerTest {

    private AlertManager alertManager;
    
    @Mock private NotificationManager mockNotifier;

    @BeforeEach
    public void setUp() {
        alertManager = new AlertManager(mockNotifier);
        alertManager.clearRules();
        alertManager.clearCooldowns();
    }

    @Test
    public void testRule_firesWhenThresholdExceeded() throws Exception {
        AlertRule rule = new AlertRule("MySQL", AlertRule.Metric.RAM, AlertRule.Operator.GT, 500.0, AlertRule.Action.NOTIFY_WARNING, 5, 1);
        alertManager.addRule(rule);
        
        ResourceSnapshot snap = new ResourceSnapshot("MySQL", 1234, 0.0, 600, 0, 0, 0);
        alertManager.checkRules(snap);
        
        verify(mockNotifier).notifyWarning(eq("Alert: MySQL"), anyString());
    }

    @Test
    public void testCooldown_doesNotRefireWithinCooldown() {
        AlertRule rule = new AlertRule("MySQL", AlertRule.Metric.RAM, AlertRule.Operator.GT, 500.0, AlertRule.Action.NOTIFY_WARNING, 5, 1);
        alertManager.addRule(rule);
        
        ResourceSnapshot snap = new ResourceSnapshot("MySQL", 1234, 0.0, 600, 0, 0, 0);
        
        alertManager.checkRules(snap); 
        alertManager.checkRules(snap); 
        
        verify(mockNotifier, times(1)).notifyWarning(anyString(), anyString());
    }

    @Test
    public void testHistory_storesAlertEvents() throws Exception {
        AlertRule rule = new AlertRule("MySQL", AlertRule.Metric.RAM, AlertRule.Operator.GT, 500.0, AlertRule.Action.NOTIFY_WARNING, 0, 1); 
        alertManager.addRule(rule);
        
        ResourceSnapshot snap = new ResourceSnapshot("MySQL", 1234, 0.0, 600, 0, 0, 0);
        alertManager.checkRules(snap);
        
        Thread.sleep(100); 
        
        Platform.runLater(() -> {
            assertEquals(1, alertManager.getHistory().size());
            assertEquals("MySQL", alertManager.getHistory().get(0).serviceId);
        });
        Thread.sleep(100);
    }

    @Test
    public void testDefaultRules_loadOnStartup() {
        AlertManager freshManager = new AlertManager(mockNotifier);
        assertTrue(freshManager.getRules().size() >= 3);
        boolean hasRamRule = freshManager.getRules().stream().anyMatch(r -> r.metric == AlertRule.Metric.RAM);
        assertTrue(hasRamRule);
    }

    @Test
    public void testConsecutiveHits_firesAfterRequirementMet() {
        AlertRule rule = new AlertRule("MySQL", AlertRule.Metric.CPU, AlertRule.Operator.GT, 80.0, AlertRule.Action.NOTIFY_WARNING, 5, 3);
        alertManager.addRule(rule);
        
        ResourceSnapshot snap = new ResourceSnapshot("MySQL", 1234, 85.0, 0, 0, 0, 0);
        
        alertManager.checkRules(snap); 
        verify(mockNotifier, never()).notifyWarning(anyString(), anyString());
        
        alertManager.checkRules(snap); 
        verify(mockNotifier, never()).notifyWarning(anyString(), anyString());
        
        alertManager.checkRules(snap); 
        verify(mockNotifier, times(1)).notifyWarning(anyString(), anyString());
    }
}
