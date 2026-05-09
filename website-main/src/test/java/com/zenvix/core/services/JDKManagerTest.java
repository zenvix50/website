package com.zenvix.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JDKManagerTest {

    @TempDir
    Path tempDir;

    private JDKManager jdkManager;

    @BeforeEach
    public void setUp() throws Exception {
        jdkManager = spy(new JDKManager(tempDir.toString()));
    }

    @Test
    public void testSwitch_switchesActiveJDK() throws Exception {
        jdkManager.switchVersion("21");
        
        String json = new String(Files.readAllBytes(tempDir.resolve("config/jdk.json")));
        assertTrue(json.contains("\"active\": \"21\""));
        assertEquals("21", jdkManager.getActiveVersion());
    }

    @Test
    public void testSwitch_rejectsInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> {
            jdkManager.switchVersion("99");
        });
    }

    @Test
    public void testJavaHome_setsCorrectJavaHome() throws Exception {
        Path dummyJdk = tempDir.resolve("jdk/jdk17/bin");
        Files.createDirectories(dummyJdk);
        Files.write(dummyJdk.resolve("java"), new byte[0]);
        
        jdkManager.switchVersion("17");
        String javaHome = jdkManager.getJavaHome();
        assertEquals(tempDir.resolve("jdk/jdk17").toAbsolutePath().toString(), javaHome);
        
        ProcessBuilder pb = new ProcessBuilder();
        jdkManager.configureProcessBuilder(pb);
        assertEquals(javaHome, pb.environment().get("JAVA_HOME"));
        assertTrue(pb.environment().get("PATH").contains("jdk17"));
    }

    @Test
    public void testDownload_downloadsJDKFromAdoptium() throws Exception {
        String mockJsonResponse = "{\"binaries\":[{\"package\":{\"link\":\"https://github.com/fake-jdk.zip\"}}]}";
        doReturn(mockJsonResponse).when(jdkManager).fetchAdoptiumApi(anyString());
        doNothing().when(jdkManager).downloadAndExtract(anyString(), anyString());
        
        jdkManager.downloadJDK("17");
        
        verify(jdkManager).downloadAndExtract("https://github.com/fake-jdk.zip", "17");
    }

    @Test
    public void testDownload_throwsErrorIfLinkMissing() throws Exception {
        doReturn("{}").when(jdkManager).fetchAdoptiumApi(anyString());
        
        assertThrows(Exception.class, () -> {
            jdkManager.downloadJDK("17");
        });
    }

    @Test
    public void testDetect_detectsSystemInstalledJDKs() {
        List<String> jdks = jdkManager.detectSystemJDKs();
        assertNotNull(jdks);
    }

    @Test
    public void testFlags_validatesAndSavesJVMFlags() throws Exception {
        List<String> validFlags = Arrays.asList("-Xmx1024m", "-XX:+UseG1GC");
        jdkManager.setJvmFlags(validFlags);
        
        assertEquals(2, jdkManager.getJvmFlags().size());
        String json = new String(Files.readAllBytes(tempDir.resolve("config/jdk.json")));
        assertTrue(json.contains("\"-Xmx1024m\""));
        assertTrue(json.contains("\"-XX:+UseG1GC\""));
        
        List<String> invalidFlags = Arrays.asList("-XX:BadFlag#$");
        Exception exception = assertThrows(Exception.class, () -> {
            jdkManager.setJvmFlags(invalidFlags);
        });
        assertTrue(exception.getMessage().contains("Invalid JVM flag"));
    }
}
