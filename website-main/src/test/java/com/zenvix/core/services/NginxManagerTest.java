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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NginxManagerTest {

    @TempDir
    Path tempDir;

    private NginxManager nginxManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        
        nginxManager = spy(new NginxManager(tempDir.toString()));
        nginxManager.setHttpPort(80);
        nginxManager.setHttpsPort(443);
        
        Path confPath = Paths.get(nginxManager.getConfigPath());
        Files.createDirectories(confPath.getParent());
        Files.write(confPath, "events {}\nhttp {\n}".getBytes());
        Files.createDirectories(Paths.get(nginxManager.getPidFile()).getParent());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (nginxManager.isRunning()) {
            nginxManager.stop();
        }
        closeable.close();
    }

    @Test
    public void testStart_nginxStartsOnConfiguredPort() throws Exception {
        doReturn(true).when(nginxManager).isPortAvailable(80);
        doReturn(true).when(nginxManager).validateConfig();
        
        doReturn(mockPb).when(nginxManager).createProcessBuilder(any(String[].class));
        
        doAnswer(invocation -> {
            Files.write(Paths.get(nginxManager.getPidFile()), "12345".getBytes());
            return mockProcess;
        }).when(mockPb).start();

        nginxManager.start();

        assertTrue(nginxManager.isRunning());
        assertEquals("RUNNING", nginxManager.getStatus());
        verify(nginxManager).validateConfig();
    }

    @Test
    public void testConfigReload_reloadsWithoutRestart() throws Exception {
        doReturn(true).when(nginxManager).validateConfig();
        
        doReturn(mockPb).when(nginxManager).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);

        nginxManager.reload();

        verify(nginxManager).createProcessBuilder(nginxManager.getBinaryPath(), "-s", "reload", "-c", nginxManager.getConfigPath());
        verify(nginxManager).validateConfig();
    }

    @Test
    public void testVirtualHostAdd_addsServerBlock() throws Exception {
        nginxManager.addVirtualHost("example.com", 8080, "/var/www/example");
        
        String conf = new String(Files.readAllBytes(Paths.get(nginxManager.getConfigPath())));
        
        assertTrue(conf.contains("server_name example.com;"));
        assertTrue(conf.contains("listen 8080;"));
        assertTrue(conf.contains("root /var/www/example;"));
    }

    @Test
    public void testSSLConfig_configuratesHTTPS() throws Exception {
        nginxManager.addVirtualHost("secure.com", 80, "/var/www/secure");
        
        doNothing().when(nginxManager).generateSelfSignedCert(anyString(), anyString(), anyString());
        
        nginxManager.configureSSL("secure.com", "/certs/secure.crt", "/certs/secure.key", true);
        
        String conf = new String(Files.readAllBytes(Paths.get(nginxManager.getConfigPath())));
        
        assertTrue(conf.contains("listen 443 ssl;"));
        assertTrue(conf.contains("ssl_certificate /certs/secure.crt;"));
        assertTrue(conf.contains("ssl_certificate_key /certs/secure.key;"));
    }

    @Test
    public void testInvalidConfig_throwsConfigException() throws Exception {
        doReturn(mockPb).when(nginxManager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectErrorStream(true)).thenReturn(mockPb);
        when(mockPb.start()).thenReturn(mockProcess);
        
        String errorLog = "nginx: [emerg] unknown directive \"invalid_directive\" in nginx.conf:5";
        InputStream is = new ByteArrayInputStream(errorLog.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);
        when(mockProcess.exitValue()).thenReturn(1); 

        assertThrows(NginxManager.ConfigException.class, () -> {
            nginxManager.validateConfig();
        });
    }

    @Test
    public void testStop_sendsStopSignalCorrectly() throws Exception {
        Files.write(Paths.get(nginxManager.getPidFile()), "12345".getBytes());
        
        doReturn(mockPb).when(nginxManager).createProcessBuilder(any(String[].class));
        
        doAnswer(invocation -> {
            Files.deleteIfExists(Paths.get(nginxManager.getPidFile()));
            return mockProcess;
        }).when(mockPb).start();

        nginxManager.stop();

        assertFalse(nginxManager.isRunning());
        verify(nginxManager).createProcessBuilder(nginxManager.getBinaryPath(), "-s", "stop", "-c", nginxManager.getConfigPath());
    }
}
