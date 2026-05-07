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
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TomcatManagerTest {

    @TempDir
    Path tempDir;

    private TomcatManager tomcatManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        
        tomcatManager = spy(new TomcatManager(tempDir.toString()));
        tomcatManager.setHttpPort(8080);
        tomcatManager.setVersion("9");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tomcatManager.isRunning()) {
            tomcatManager.stop();
        }
        closeable.close();
    }

    @Test
    public void testStart_successfullyStartsTomcat() throws Exception {
        doReturn(true).when(tomcatManager).isPortAvailable(anyInt());
        
        doReturn(mockPb).when(tomcatManager).createProcessBuilder(any());
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        
        String startupLog = "INFO: Server version: Apache Tomcat/9.0\nINFO: Server startup in 1000 ms\n";
        InputStream is = new ByteArrayInputStream(startupLog.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);
        when(mockProcess.isAlive()).thenReturn(true);
        doReturn(12345L).when(tomcatManager).getPidOfProcess(mockProcess);

        tomcatManager.start();

        assertTrue(tomcatManager.isRunning());
        assertEquals("RUNNING", tomcatManager.getStatus());
        verify(tomcatManager, times(1)).doStart();
    }

    @Test
    public void testStop_gracefullyStopsTomcat() throws Exception {
        // Setup initial start state
        testStart_successfullyStartsTomcat();
        
        Process stopProcess = mock(Process.class);
        ProcessBuilder stopPb = mock(ProcessBuilder.class);
        doReturn(stopPb).when(tomcatManager).createProcessBuilder(any());
        when(stopPb.environment()).thenReturn(new java.util.HashMap<>());
        when(stopPb.start()).thenReturn(stopProcess);

        tomcatManager.stop();

        assertFalse(tomcatManager.isRunning());
        assertEquals("STOPPED", tomcatManager.getStatus());
        verify(mockProcess, atLeastOnce()).destroy();
    }

    @Test
    public void testRestart_stopsAndStartsSuccessfully() throws Exception {
        doNothing().when(tomcatManager).stop();
        doNothing().when(tomcatManager).start();
        
        tomcatManager.restart();
        
        verify(tomcatManager, times(1)).stop();
        verify(tomcatManager, times(1)).start();
    }

    @Test
    public void testPortConflict_throwsPortConflictException() throws Exception {
        doReturn(false).when(tomcatManager).isPortAvailable(tomcatManager.getHttpPort());

        assertThrows(TomcatManager.PortConflictException.class, () -> {
            tomcatManager.start();
        });
        
        verify(tomcatManager, never()).doStart();
    }

    @Test
    public void testWARDeploy_deploysAndUndeploys() throws Exception {
        File dummyWar = tempDir.resolve("dummy.war").toFile();
        Files.write(dummyWar.toPath(), "dummy content".getBytes());

        tomcatManager.deployWar("testapp", dummyWar.getAbsolutePath());

        Path deployedWarPath = Paths.get(tomcatManager.getCatalinaHome(), "webapps", "testapp.war");
        assertTrue(Files.exists(deployedWarPath));

        tomcatManager.undeployWar("testapp");
        assertFalse(Files.exists(deployedWarPath));
    }

    @Test
    public void testRetryLogic_retriesOnFailure() throws Exception {
        doReturn(true).when(tomcatManager).isPortAvailable(anyInt());
        
        doThrow(new RuntimeException("Simulated failure 1"))
            .doThrow(new RuntimeException("Simulated failure 2"))
            .doNothing().when(tomcatManager).doStart();

        tomcatManager.start();

        verify(tomcatManager, times(3)).doStart();
    }
}
