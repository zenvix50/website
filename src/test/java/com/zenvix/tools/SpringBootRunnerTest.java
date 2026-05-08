package com.zenvix.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringBootRunnerTest {

    @TempDir
    Path tempDir;

    private SpringBootRunner runner;

    @Mock
    private Process mockProcess1;

    @Mock
    private Process mockProcess2;

    @Mock
    private ProcessBuilder mockPb;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        runner = spy(new SpringBootRunner(tempDir.toString()));
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testStart_startsSpringBootApp() throws Exception {
        runner.addOrUpdateApp(new SpringBootRunner.AppConfig("app1", "app1.jar", 8080, "dev", "-Xmx512m"));
        
        doReturn(mockPb).when(runner).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess1);
        
        String output = "  .   ____          _            __ _ _\n" +
                        " /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\\n" +
                        "( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\\n" +
                        " \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )\n" +
                        "  '  |____| .__|_| |_|_| |_\\__, | / / / /\n" +
                        " =========|_|==============|___/=/_/_/_/\n" +
                        " :: Spring Boot ::\n" +
                        "LiveReload server is running on port 35729\n";
        InputStream is = new ByteArrayInputStream(output.getBytes());
        when(mockProcess1.getInputStream()).thenReturn(is);
        when(mockProcess1.isAlive()).thenReturn(true);

        List<String> logs = new ArrayList<>();
        List<String> devToolsLogs = new ArrayList<>();

        runner.startApp("app1", logs::add, devToolsLogs::add);

        assertTrue(runner.isAppRunning("app1"));
        Thread.sleep(100); // Allow thread to read

        assertTrue(logs.stream().anyMatch(l -> l.contains(":: Spring Boot ::")));
        assertTrue(devToolsLogs.stream().anyMatch(l -> l.contains("LiveReload server is running")));
        
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(runner).createProcessBuilder(captor.capture());
        String[] cmd = captor.getValue();
        List<String> cmdList = java.util.Arrays.asList(cmd);
        assertTrue(cmdList.contains("-Xmx512m"));
        assertTrue(cmdList.contains("-jar"));
        assertTrue(cmdList.contains("app1.jar"));
        assertTrue(cmdList.contains("--spring.profiles.active=dev"));
        assertTrue(cmdList.contains("--server.port=8080"));
    }

    @Test
    public void testStop_stopsAppGracefully() throws Exception {
        runner.addOrUpdateApp(new SpringBootRunner.AppConfig("app1", "app1.jar", 8080, "", ""));
        
        doReturn(mockPb).when(runner).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess1);
        when(mockProcess1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess1.isAlive()).thenReturn(true);

        runner.startApp("app1", null, null);
        
        runner.stopApp("app1");
        
        verify(mockProcess1).destroy();
        verify(mockProcess1).waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        assertFalse(runner.isAppRunning("app1"));
    }

    @Test
    public void testMultipleApps_runSimultaneously() throws Exception {
        runner.addOrUpdateApp(new SpringBootRunner.AppConfig("app1", "app1.jar", 8080, "", ""));
        runner.addOrUpdateApp(new SpringBootRunner.AppConfig("app2", "app2.jar", 8081, "", ""));
        
        doReturn(mockPb).when(runner).createProcessBuilder(any(String[].class));
        
        when(mockPb.start()).thenReturn(mockProcess1).thenReturn(mockProcess2);
        
        when(mockProcess1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess1.isAlive()).thenReturn(true);
        when(mockProcess2.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess2.isAlive()).thenReturn(true);

        runner.startApp("app1", null, null);
        runner.startApp("app2", null, null);

        assertTrue(runner.isAppRunning("app1"));
        assertTrue(runner.isAppRunning("app2"));
    }

    @Test
    public void testActuatorHealth_pollsHealthEndpoint() {
        assertEquals("UP", runner.parseHealthStatus("{\"status\":\"UP\"}"));
        assertEquals("DOWN", runner.parseHealthStatus("{\"status\":\"DOWN\"}"));
        assertEquals("OUT_OF_SERVICE", runner.parseHealthStatus("{\"status\":\"OUT_OF_SERVICE\"}"));
        assertEquals("UNKNOWN", runner.parseHealthStatus("{}"));
    }

    @Test
    public void testProfileSwitch_appliesCorrectProfile() throws Exception {
        runner.addOrUpdateApp(new SpringBootRunner.AppConfig("app1", "app1.jar", 8080, "prod", ""));
        
        doReturn(mockPb).when(runner).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess1);
        when(mockProcess1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        runner.startApp("app1", null, null);
        
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(runner).createProcessBuilder(captor.capture());
        
        List<String> cmdList = java.util.Arrays.asList(captor.getValue());
        assertTrue(cmdList.contains("--spring.profiles.active=prod"));
    }
}
