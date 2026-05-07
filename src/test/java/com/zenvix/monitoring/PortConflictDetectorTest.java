package com.zenvix.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortConflictDetectorTest {

    private PortConflictDetector detector;
    private PortConflictDetector spyDetector;

    @BeforeEach
    public void setUp() {
        detector = new PortConflictDetector();
        spyDetector = spy(detector);
    }

    @Test
    public void testPortFree_returnsAvailable() throws Exception {
        int freePort = 0;
        try (ServerSocket s = new ServerSocket(0)) {
            freePort = s.getLocalPort();
        }
        
        PortScanResult result = detector.checkPort(freePort);
        
        assertFalse(result.inUse);
        assertEquals(freePort, result.port);
    }

    @Test
    public void testPortOccupied_returnsConflict() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            int occupiedPort = s.getLocalPort();
            
            doReturn(new PortScanResult(occupiedPort, true, "java", 1234)).when(spyDetector).identifyOccupant(occupiedPort);
            
            PortScanResult result = spyDetector.checkPort(occupiedPort);
            
            assertTrue(result.inUse);
            assertEquals("java", result.occupyingProcessName);
            assertEquals(1234, result.occupyingPid);
        }
    }

    @Test
    public void testParallelScan_checksAllPortsSimultaneously() throws Exception {
        int p1, p2, p3;
        try (ServerSocket s1 = new ServerSocket(0); ServerSocket s2 = new ServerSocket(0); ServerSocket s3 = new ServerSocket(0)) {
            p1 = s1.getLocalPort();
            p2 = s2.getLocalPort();
            p3 = s3.getLocalPort();
            
            doReturn(new PortScanResult(p1, true, "java", 1)).when(spyDetector).identifyOccupant(p1);
            doReturn(new PortScanResult(p2, true, "java", 2)).when(spyDetector).identifyOccupant(p2);
            doReturn(new PortScanResult(p3, true, "java", 3)).when(spyDetector).identifyOccupant(p3);
            
            List<PortScanResult> results = spyDetector.checkPortsInParallel(Arrays.asList(p1, p2, p3));
            
            assertEquals(3, results.size());
            assertTrue(results.get(0).inUse);
            assertTrue(results.get(1).inUse);
            assertTrue(results.get(2).inUse);
        }
    }

    @Test
    public void testKillProcess_killsOccupyingProcess() {
        doReturn(true).when(spyDetector).killNative(1234);
        
        boolean killed = spyDetector.killProcess(1234);
        
        assertTrue(killed);
        verify(spyDetector).killNative(1234);
    }
}
