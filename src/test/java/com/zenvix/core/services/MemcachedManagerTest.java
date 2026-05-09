package com.zenvix.core.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemcachedManagerTest {

    @TempDir
    Path tempDir;

    private MemcachedManager memcachedManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    @Mock
    private Socket mockSocket;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        memcachedManager = spy(new MemcachedManager(tempDir.toString()));
        memcachedManager.setPort(11211);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (memcachedManager.isRunning()) {
            memcachedManager.stop();
        }
        closeable.close();
    }

    @Test
    public void testStart_memcachedStartsOnPort11211() throws Exception {
        doReturn(true).when(memcachedManager).isPortAvailable(11211);
        
        doReturn(mockPb).when(memcachedManager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectErrorStream(true)).thenReturn(mockPb);
        
        doAnswer(invocation -> {
            // Once the process "starts", the port should look bound.
            doReturn(false).when(memcachedManager).isPortAvailable(11211);
            return mockProcess;
        }).when(mockPb).start();

        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        memcachedManager.start();

        assertTrue(memcachedManager.isRunning());
        assertEquals("RUNNING", memcachedManager.getStatus());
    }

    @Test
    public void testStop_shutsDownGracefully() throws Exception {
        java.lang.reflect.Field field = MemcachedManager.class.getDeclaredField("memcachedProcess");
        field.setAccessible(true);
        field.set(memcachedManager, mockProcess);
        when(mockProcess.isAlive()).thenReturn(true);

        memcachedManager.stop();

        verify(mockProcess).destroy();
        verify(mockProcess).waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        assertFalse(memcachedManager.isRunning());
    }

    @Test
    public void testFlush_flushesAllKeys() throws Exception {
        doReturn(mockSocket).when(memcachedManager).createSocket();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(baos);
        
        String response = "OK\r\n";
        InputStream is = new ByteArrayInputStream(response.getBytes());
        when(mockSocket.getInputStream()).thenReturn(is);
        
        memcachedManager.flushAll();
        
        String sentCmd = baos.toString();
        assertTrue(sentCmd.contains("flush_all"));
    }

    @Test
    public void testStats_parsesStatsResponse() throws Exception {
        doReturn(mockSocket).when(memcachedManager).createSocket();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(baos);
        
        String response = "STAT get_hits 100\r\nSTAT get_misses 25\r\nSTAT curr_items 50\r\nEND\r\n";
        InputStream is = new ByteArrayInputStream(response.getBytes());
        when(mockSocket.getInputStream()).thenReturn(is);
        
        Map<String, String> rawStats = memcachedManager.getRawStats();
        assertEquals("100", rawStats.get("get_hits"));
        assertEquals("25", rawStats.get("get_misses"));
        assertEquals("50", rawStats.get("curr_items"));

        // Reload stream mocks as getMetrics invokes getRawStats a second time natively
        doReturn(mockSocket).when(memcachedManager).createSocket();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(baos2);
        InputStream is2 = new ByteArrayInputStream(response.getBytes());
        when(mockSocket.getInputStream()).thenReturn(is2);

        String metrics = memcachedManager.getMetrics();
        assertTrue(metrics.contains("Get Hits: 100"));
        assertTrue(metrics.contains("Get Misses: 25"));
        assertTrue(metrics.contains("Current Items: 50"));
        assertTrue(metrics.contains("Hit Ratio: 80.00%"));
    }

    @Test
    public void testPortConflict_throwsException() throws Exception {
        doReturn(false).when(memcachedManager).isPortAvailable(11211);

        Exception exception = assertThrows(Exception.class, () -> {
            memcachedManager.start();
        });
        assertTrue(exception.getMessage().contains("already in use"));
    }
}
