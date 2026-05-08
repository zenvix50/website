package com.zenvix.monitoring;

import com.zenvix.core.services.ServiceManager;
import com.zenvix.ui.notifications.NotificationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HealthMonitorTest {

    private HealthMonitor monitor;
    
    @Mock private NotificationManager mockNotifier;
    @Mock private ServiceManager mockServiceManager;

    @BeforeEach
    public void setUp() {
        monitor = spy(new HealthMonitor(mockNotifier));
    }

    @Test
    public void testHealthCheck_detectsRunningService() {
        monitor.registerService("TestDB", mockServiceManager, "MYSQL", 3306, false);
        monitor.setExpectedRunning("TestDB", true);

        doReturn(new HealthCheckResult(true, "OK")).when(monitor).checkService(any());

        monitor.triggerChecksNow();

        assertEquals(HealthState.HEALTHY, monitor.getServiceState("TestDB"));
        verify(mockNotifier, never()).notifyCrash(any());
    }

    @Test
    public void testCrashDetection_detectsPortClosed() {
        monitor.registerService("TestDB", mockServiceManager, "MYSQL", 3306, false);
        monitor.setExpectedRunning("TestDB", true);

        doReturn(new HealthCheckResult(true, "OK")).when(monitor).checkService(any());
        monitor.triggerChecksNow();

        doReturn(new HealthCheckResult(false, "Fail")).when(monitor).checkService(any());
        monitor.triggerChecksNow();

        assertEquals(HealthState.CRASHED, monitor.getServiceState("TestDB"));
        verify(mockNotifier).notifyCrash("TestDB");
    }

    @Test
    public void testAutoRestart_restartsAfterCrash() throws Exception {
        monitor.registerService("TestDB", mockServiceManager, "MYSQL", 3306, true);
        monitor.setExpectedRunning("TestDB", true);

        doReturn(new HealthCheckResult(true, "OK")).when(monitor).checkService(any());
        monitor.triggerChecksNow();

        doReturn(new HealthCheckResult(false, "Fail")).when(monitor).checkService(any());
        monitor.triggerChecksNow();

        Thread.sleep(200);

        verify(mockNotifier).notifyCrash("TestDB");
        verify(mockServiceManager).stop();
    }

    @Test
    public void testCircuitBreaker_opensAfter5Failures() {
        monitor.registerService("TestDB", mockServiceManager, "MYSQL", 3306, false);
        monitor.setExpectedRunning("TestDB", true);

        doReturn(new HealthCheckResult(false, "Fail")).when(monitor).checkService(any());

        for (int i = 0; i < 5; i++) {
            monitor.triggerChecksNow();
        }

        assertEquals(HealthState.CIRCUIT_OPEN, monitor.getServiceState("TestDB"));
        verify(mockNotifier).notifyWarning(eq("Circuit Breaker Open"), anyString());
    }

    @Test
    public void testCircuitBreaker_stopsCheckingAfterOpen() {
        monitor.registerService("TestDB", mockServiceManager, "MYSQL", 3306, false);
        monitor.setExpectedRunning("TestDB", true);

        doReturn(new HealthCheckResult(false, "Fail")).when(monitor).checkService(any());

        for (int i = 0; i < 5; i++) {
            monitor.triggerChecksNow();
        }

        assertEquals(HealthState.CIRCUIT_OPEN, monitor.getServiceState("TestDB"));

        monitor.triggerChecksNow();
        
        verify(monitor, times(5)).checkService(any());
    }
}
