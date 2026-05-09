package com.zenvix.core.services;

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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GradleManagerTest {

    @TempDir
    Path tempDir;

    private GradleManager gradleManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        gradleManager = spy(new GradleManager(tempDir.toString()));
    }

    @Test
    public void testAutoInstall_downloadsGradleIfMissing() throws Exception {
        doNothing().when(gradleManager).downloadAndExtract(anyString());
        
        gradleManager.autoInstallIfMissing();
        
        verify(gradleManager).downloadAndExtract("https://services.gradle.org/distributions/gradle-8.6-bin.zip");
    }

    @Test
    public void testBuild_runsGradleTaskAndStreamsOutput() throws Exception {
        doNothing().when(gradleManager).autoInstallIfMissing();
        
        doReturn(mockPb).when(gradleManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        
        String logOutput = "Starting a Gradle Daemon...\n> Task :compileJava\n> Task :build SUCCESS\nBUILD SUCCESSFUL\n";
        InputStream is = new ByteArrayInputStream(logOutput.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);
        when(mockProcess.exitValue()).thenReturn(0);

        List<String> outputLines = new ArrayList<>();
        List<String> progressLines = new ArrayList<>();

        gradleManager.runBuild(tempDir.toString(), "build", outputLines::add, progressLines::add);

        assertTrue(outputLines.contains("> Task :build SUCCESS"));
        assertTrue(progressLines.stream().anyMatch(l -> l.contains("Executing task: compileJava")));
        
        assertEquals(1, gradleManager.getHistory().size());
        assertEquals("SUCCESS", gradleManager.getHistory().get(0).result);
    }

    @Test
    public void testWrapperDetection_usesWrapperIfPresent() throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        File wrapper = isWindows ? tempDir.resolve("gradlew.bat").toFile() : tempDir.resolve("gradlew").toFile();
        wrapper.createNewFile();

        doReturn(mockPb).when(gradleManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.exitValue()).thenReturn(0);

        gradleManager.runBuild(tempDir.toString(), "clean", null, null);

        org.mockito.ArgumentCaptor<String[]> captor = org.mockito.ArgumentCaptor.forClass(String[].class);
        verify(gradleManager).createProcessBuilder(captor.capture());
        
        String[] cmd = captor.getValue();
        assertEquals(wrapper.getAbsolutePath(), cmd[0]);
    }

    @Test
    public void testCancel_cancelsBuildProcess() throws Exception {
        java.lang.reflect.Field field = GradleManager.class.getDeclaredField("activeBuildProcess");
        field.setAccessible(true);
        field.set(gradleManager, mockProcess);

        when(mockProcess.isAlive()).thenReturn(true);
        gradleManager.cancelBuild();

        verify(mockProcess).destroy();
    }

    @Test
    public void testHistory_recordsBuildResult() throws Exception {
        doNothing().when(gradleManager).autoInstallIfMissing();
        doReturn(mockPb).when(gradleManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.exitValue()).thenReturn(0);

        gradleManager.runBuild(tempDir.toString(), "test", null, null);

        GradleManager newManager = new GradleManager(tempDir.toString());
        assertEquals(1, newManager.getHistory().size());
        assertEquals("test", newManager.getHistory().get(0).task);
        assertEquals("SUCCESS", newManager.getHistory().get(0).result);
    }
}
