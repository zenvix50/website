package com.zenvix.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MavenManagerTest {

    @TempDir
    Path tempDir;

    private MavenManager mavenManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        mavenManager = spy(new MavenManager(tempDir.toString()));
    }

    @Test
    public void testAutoInstall_downloadsMavenIfMissing() throws Exception {
        doNothing().when(mavenManager).downloadAndExtract(anyString());
        
        mavenManager.autoInstallIfMissing();
        
        verify(mavenManager).downloadAndExtract("https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip");
    }

    @Test
    public void testBuild_runsMavenGoalAndStreamsOutput() throws Exception {
        doNothing().when(mavenManager).autoInstallIfMissing();
        
        doReturn(mockPb).when(mavenManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        
        String logOutput = "[INFO] Scanning for projects...\n[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/test/artifact/1.0/artifact-1.0.jar\n[INFO] BUILD SUCCESS\n";
        InputStream is = new ByteArrayInputStream(logOutput.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);
        when(mockProcess.exitValue()).thenReturn(0);

        List<String> outputLines = new ArrayList<>();
        List<String> progressLines = new ArrayList<>();

        mavenManager.runBuild(tempDir.toString(), "clean install", outputLines::add, progressLines::add);

        assertTrue(outputLines.contains("[INFO] BUILD SUCCESS"));
        assertTrue(progressLines.stream().anyMatch(l -> l.contains("Downloading") && l.contains("org/test/artifact/1.0/artifact-1.0.jar")));
        
        assertEquals(1, mavenManager.getHistory().size());
        assertEquals("SUCCESS", mavenManager.getHistory().get(0).result);
    }

    @Test
    public void testCancel_cancelsBuildProcess() throws Exception {
        java.lang.reflect.Field field = MavenManager.class.getDeclaredField("activeBuildProcess");
        field.setAccessible(true);
        field.set(mavenManager, mockProcess);

        when(mockProcess.isAlive()).thenReturn(true);
        mavenManager.cancelBuild();

        verify(mockProcess).destroy();
    }

    @Test
    public void testHistory_recordsBuildResult() throws Exception {
        doNothing().when(mavenManager).autoInstallIfMissing();
        doReturn(mockPb).when(mavenManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.exitValue()).thenReturn(0);

        mavenManager.runBuild(tempDir.toString(), "test", null, null);

        MavenManager newManager = new MavenManager(tempDir.toString());
        assertEquals(1, newManager.getHistory().size());
        assertEquals("test", newManager.getHistory().get(0).goal);
        assertEquals("SUCCESS", newManager.getHistory().get(0).result);
    }

    @Test
    public void testSettingsEdit_updatesSettingsXml() throws Exception {
        mavenManager.updateSettings("/my/local/repo", "<mirror><id>central</id><url>http://mirror</url><mirrorOf>central</mirrorOf></mirror>");
        
        String xml = new String(Files.readAllBytes(Paths.get(mavenManager.getSettingsPath())));
        assertTrue(xml.contains("<localRepository>/my/local/repo</localRepository>"));
        assertTrue(xml.contains("<id>central</id>"));
        
        mavenManager.updateSettings("/my/new/repo", null);
        xml = new String(Files.readAllBytes(Paths.get(mavenManager.getSettingsPath())));
        assertTrue(xml.contains("<localRepository>/my/new/repo</localRepository>"));
        assertFalse(xml.contains("<localRepository>/my/local/repo</localRepository>"));
    }
}
