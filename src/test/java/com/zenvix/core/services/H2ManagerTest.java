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
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class H2ManagerTest {

    @TempDir
    Path tempDir;

    private H2Manager h2Manager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        h2Manager = spy(new H2Manager(tempDir.toString()));
        h2Manager.setWebPort(8082);
        h2Manager.setTcpPort(9092);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (h2Manager.isRunning()) {
            doNothing().when(h2Manager).stop();
            h2Manager.stop();
        }
        closeable.close();
    }

    @Test
    public void testStart_inMemoryMode_startsSuccessfully() throws Exception {
        h2Manager.setMode("memory");
        doReturn(true).when(h2Manager).isPortAvailable(anyInt());
        
        doReturn(mockPb).when(h2Manager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectErrorStream(true)).thenReturn(mockPb);
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.isAlive()).thenReturn(true);

        String startupLog = "Web Console server running at http://localhost:8082\nTCP server running at tcp://localhost:9092\n";
        InputStream is = new ByteArrayInputStream(startupLog.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);

        h2Manager.start();

        assertTrue(h2Manager.isRunning());
        assertEquals("RUNNING", h2Manager.getStatus());
    }

    @Test
    public void testStart_fileMode_startsSuccessfully() throws Exception {
        h2Manager.setMode("file");
        String dbFile = tempDir.resolve("h2/data/mydb").toString().replace('\\', '/');
        h2Manager.setDbPath(dbFile);
        
        doReturn(true).when(h2Manager).isPortAvailable(anyInt());
        
        doReturn(mockPb).when(h2Manager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectErrorStream(true)).thenReturn(mockPb);
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.isAlive()).thenReturn(true);

        String startupLog = "Web Console server running at http://localhost:8082\nTCP server running at tcp://localhost:9092\n";
        InputStream is = new ByteArrayInputStream(startupLog.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);

        h2Manager.start();

        assertTrue(h2Manager.isRunning());
        assertTrue(new File(dbFile).getParentFile().exists());
    }

    @Test
    public void testJdbcUrlGeneration_generatesCorrectUrls() {
        h2Manager.setMode("memory");
        assertEquals("jdbc:h2:mem:zenviXdb;DB_CLOSE_DELAY=-1", h2Manager.getActiveJdbcUrl());
        assertEquals("jdbc:h2:mem:zenviXdb;DB_CLOSE_DELAY=-1", h2Manager.getJdbcUrlInMemory());

        String dbFile = tempDir.resolve("h2/data/zenviXdb").toString().replace('\\', '/');
        h2Manager.setMode("file");
        h2Manager.setDbPath(dbFile);
        
        assertEquals("jdbc:h2:file:" + dbFile + ";AUTO_SERVER=TRUE", h2Manager.getActiveJdbcUrl());
        assertEquals("jdbc:h2:file:" + dbFile + ";AUTO_SERVER=TRUE", h2Manager.getJdbcUrlFile());
        
        assertEquals("jdbc:h2:tcp://localhost:9092/~/zenviXdb", h2Manager.getJdbcUrlTcp());
    }

    @Test
    public void testConsoleStart_launchesWebConsole() throws Exception {
        doReturn(true).when(h2Manager).isRunning();
        doNothing().when(h2Manager).openBrowser(anyString());

        h2Manager.launchWebConsole();

        verify(h2Manager).openBrowser("http://localhost:8082");
    }

    @Test
    public void testStop_shutsDownGracefully() throws Exception {
        doReturn(true).when(h2Manager).isRunning();
        
        java.lang.reflect.Field field = H2Manager.class.getDeclaredField("h2Process");
        field.setAccessible(true);
        field.set(h2Manager, mockProcess);
        when(mockProcess.isAlive()).thenReturn(true);
        
        ProcessBuilder stopPb = mock(ProcessBuilder.class);
        doReturn(stopPb).when(h2Manager).createProcessBuilder(any(String[].class));
        Process stopProc = mock(Process.class);
        when(stopPb.start()).thenReturn(stopProc);

        h2Manager.stop();

        verify(mockProcess).destroy();
        verify(stopProc).waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        assertFalse(h2Manager.isRunning());
    }
}
