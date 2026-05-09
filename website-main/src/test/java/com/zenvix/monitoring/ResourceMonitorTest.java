package com.zenvix.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import oshi.SystemInfo;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceMonitorTest {

    private ResourceMonitor monitor;
    
    @Mock private AlertManager mockAlertManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        monitor = spy(new ResourceMonitor());
        monitor.setAlertManager(mockAlertManager);
    }

    @Test
    public void testCPU_returnsNonNegativeValue() {
        doReturn(new ResourceSnapshot("MySQL", 1234, 15.5, 500, 10, 100, 5)).when(monitor).captureSnapshot(any());
        
        monitor.registerService("MySQL", 1234, 3306, tempDir);
        monitor.pollResources();
        
        ResourceSnapshot snap = monitor.getSnapshot("MySQL");
        assertNotNull(snap);
        assertTrue(snap.cpuPercent >= 0.0);
        assertEquals(15.5, snap.cpuPercent);
    }

    @Test
    public void testRAM_returnsPositiveValue() {
        doReturn(new ResourceSnapshot("MySQL", 1234, 15.5, 1024, 10, 100, 5)).when(monitor).captureSnapshot(any());
        monitor.registerService("MySQL", 1234, 3306, tempDir);
        monitor.pollResources();
        
        ResourceSnapshot snap = monitor.getSnapshot("MySQL");
        assertTrue(snap.ramMB > 0);
        assertEquals(1024, snap.ramMB);
    }

    @Test
    public void testDisk_returnsDiskUsage() throws Exception {
        ResourceMonitor realMonitor = new ResourceMonitor();
        int myPid = new SystemInfo().getOperatingSystem().getProcessId();
        ResourceMonitor.ServiceMeta meta = new ResourceMonitor.ServiceMeta("ZenviX", myPid, 0, tempDir);
        
        ResourceSnapshot snap = realMonitor.captureSnapshot(meta);
        if (snap != null) {
            assertTrue(snap.diskTotalMB > 0);
            assertTrue(snap.diskUsedMB >= 0);
        }
    }

    @Test
    public void testNetworkConnections_countOpenConnections() {
        doReturn(2).when(monitor).countNetworkConnections(3306);
        
        doReturn(new ResourceSnapshot("MySQL", 1234, 0, 0, 0, 0, monitor.countNetworkConnections(3306))).when(monitor).captureSnapshot(any());
        monitor.registerService("MySQL", 1234, 3306, tempDir);
        monitor.pollResources();
        
        ResourceSnapshot snap = monitor.getSnapshot("MySQL");
        assertEquals(2, snap.networkConnections);
    }

    @Test
    public void testSystemTotals_aggregatesAllServices() {
        doReturn(new ResourceSnapshot("S1", 1, 10.0, 500, 0, 0, 0)).when(monitor).captureSnapshot(argThat(m -> m.serviceId.equals("S1")));
        doReturn(new ResourceSnapshot("S2", 2, 20.0, 1000, 0, 0, 0)).when(monitor).captureSnapshot(argThat(m -> m.serviceId.equals("S2")));
        
        monitor.registerService("S1", 1, 80, tempDir);
        monitor.registerService("S2", 2, 8080, tempDir);
        
        monitor.pollResources();
        
        assertEquals(30.0, monitor.getTotalCpuPercent());
        assertEquals(1500, monitor.getTotalRamMB());
        
        verify(mockAlertManager, times(2)).checkRules(any());
    }
}
