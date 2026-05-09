package com.zenvix.core.services;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisManagerTest {

    @TempDir
    Path tempDir;

    private RedisManager redisManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    @Mock
    private RedisClient mockClient;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection;

    @Mock
    private RedisAsyncCommands<String, String> mockAsync;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        
        redisManager = spy(new RedisManager(tempDir.toString()));
        redisManager.setPort(6379);
        SecretsVault.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (redisManager.isRunning()) {
            doNothing().when(redisManager).stop();
            redisManager.stop();
        }
        closeable.close();
        SecretsVault.clear();
    }

    @Test
    public void testStart_redisStartsOnConfiguredPort() throws Exception {
        doReturn(true).when(redisManager).isPortAvailable(6379);
        
        doReturn(mockPb).when(redisManager).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.isAlive()).thenReturn(true);
        
        doAnswer(invocation -> {
            Files.write(Paths.get(redisManager.getPidFile()), "12345".getBytes());
            return mockProcess;
        }).when(mockPb).start();

        redisManager.start();

        assertTrue(redisManager.isRunning());
        assertEquals("RUNNING", redisManager.getStatus());
        
        assertTrue(Files.exists(Paths.get(redisManager.getConfigPath())));
        String conf = new String(Files.readAllBytes(Paths.get(redisManager.getConfigPath())));
        assertTrue(conf.contains("port 6379"));
    }

    @Test
    public void testStop_shutsDownGracefully() throws Exception {
        Files.createDirectories(Paths.get(redisManager.getPidFile()).getParent());
        Files.write(Paths.get(redisManager.getPidFile()), "12345".getBytes());
        
        doReturn(mockPb).when(redisManager).createProcessBuilder(any(List.class));
        Process stopProc = mock(Process.class);
        when(mockPb.start()).thenReturn(stopProc);
        
        redisManager.stop();

        assertFalse(redisManager.isRunning());
        verify(stopProc).waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    public void testCLI_executesCommandAndReturnsOutput() throws Exception {
        SecretsVault.set("redis.password", "secret");
        
        doReturn(mockPb).when(redisManager).createProcessBuilder(any(List.class));
        when(mockPb.start()).thenReturn(mockProcess);
        
        String cliOutput = "OK\n";
        InputStream is = new ByteArrayInputStream(cliOutput.getBytes());
        when(mockProcess.getInputStream()).thenReturn(is);

        String result = redisManager.executeCliCommand("SET key value");

        assertEquals("OK", result);
    }

    @Test
    public void testKeyBrowser_listsSetsGetsDeletesKeys() throws Exception {
        doReturn(mockClient).when(redisManager).getRedisClient();
        when(mockClient.connect()).thenReturn(mockConnection);
        when(mockConnection.async()).thenReturn(mockAsync);
        
        when(mockAsync.set("testkey", "testval")).thenReturn(CompletableFuture.completedFuture("OK"));
        redisManager.setValue("testkey", "testval", 0);
        verify(mockAsync).set("testkey", "testval");
        
        when(mockAsync.get("testkey")).thenReturn(CompletableFuture.completedFuture("testval"));
        assertEquals("testval", redisManager.getValue("testkey"));
        
        when(mockAsync.del("testkey")).thenReturn(CompletableFuture.completedFuture(1L));
        redisManager.deleteKey("testkey");
        verify(mockAsync).del("testkey");
    }

    @Test
    public void testMemoryStats_returnsFormattedStats() throws Exception {
        doReturn(mockClient).when(redisManager).getRedisClient();
        when(mockClient.connect()).thenReturn(mockConnection);
        when(mockConnection.async()).thenReturn(mockAsync);
        
        String memoryInfo = "used_memory:1024\r\nused_memory_peak:2048\r\n";
        String statsInfo = "keyspace_hits:50\r\nkeyspace_misses:10\r\n";
        
        when(mockAsync.info("memory")).thenReturn(CompletableFuture.completedFuture(memoryInfo));
        when(mockAsync.info("stats")).thenReturn(CompletableFuture.completedFuture(statsInfo));
        
        Map<String, String> stats = redisManager.getMemoryStats();
        
        assertEquals("1024", stats.get("used_memory"));
        assertEquals("2048", CLI_helper_check(stats, "used_memory_peak")); // using direct check below
        assertEquals("2048", stats.get("used_memory_peak"));
        assertEquals("50", stats.get("keyspace_hits"));
        assertEquals("10", stats.get("keyspace_misses"));
    }

    private String CLI_helper_check(Map<String, String> map, String key) {
        return map.get(key);
    }

    @Test
    public void testConfigEdit_updatesRedisConf() throws Exception {
        Path confPath = Paths.get(redisManager.getConfigPath());
        Files.createDirectories(confPath.getParent());
        Files.write(confPath, "maxmemory 100mb\n".getBytes());

        Map<String, String> newSettings = new HashMap<>();
        newSettings.put("maxmemory", "200mb");
        newSettings.put("requirepass", "newpass");
        
        assertThrows(RedisManager.RestartRequiredException.class, () -> {
            redisManager.editConfig(newSettings);
        });

        String updatedConf = new String(Files.readAllBytes(confPath));
        assertTrue(updatedConf.contains("maxmemory 200mb"));
        assertTrue(updatedConf.contains("requirepass newpass"));
        assertEquals("newpass", SecretsVault.get("redis.password"));
    }
}
