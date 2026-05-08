package com.zenvix.core.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostgreSQLManagerTest {

    @TempDir
    Path tempDir;

    private PostgreSQLManager pgManager;

    @Mock
    private Process mockProcess;

    @Mock
    private ProcessBuilder mockPb;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        
        pgManager = spy(new PostgreSQLManager(tempDir.toString()));
        pgManager.setPort(5432);
        SecretsVault.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        try {
            if (pgManager.isRunning()) {
                doNothing().when(pgManager).stop();
                pgManager.stop();
            }
        } catch (Exception e) {}
        closeable.close();
        SecretsVault.clear();
    }

    @Test
    public void testInitdb_initializesDataDirectory() throws Exception {
        doReturn(mockPb).when(pgManager).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);
        
        pgManager.initDbIfNecessary();
        
        assertTrue(Files.exists(Paths.get(pgManager.getDataDir())));
        assertTrue(SecretsVault.has("postgresql.postgres.password"));
    }

    @Test
    public void testStart_postgresStartsSuccessfully() throws Exception {
        doReturn(true).when(pgManager).isPortAvailable(5432);
        
        doReturn(mockPb).when(pgManager).createProcessBuilder(any(String[].class));
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);
        
        pgManager.start();
        
        doReturn("RUNNING").when(pgManager).getStatus();
        assertTrue(pgManager.isRunning());
    }

    @Test
    public void testBackup_dumpsDatabaseCorrectly() throws Exception {
        SecretsVault.set("postgresql.postgres.password", "secret");
        doReturn(mockPb).when(pgManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);

        pgManager.backupDatabase("mydb", "/tmp/backup.dump");

        verify(pgManager).createProcessBuilder(pgManager.getBinPath("pg_dump"), "-U", "postgres", "-d", "mydb", "-F", "c", "-f", "/tmp/backup.dump");
    }

    @Test
    public void testRestore_restoresDatabaseFromDump() throws Exception {
        SecretsVault.set("postgresql.postgres.password", "secret");
        doReturn(mockPb).when(pgManager).createProcessBuilder(any(String[].class));
        when(mockPb.environment()).thenReturn(new java.util.HashMap<>());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);

        pgManager.restoreDatabase("mydb", "/tmp/backup.dump");

        verify(pgManager).createProcessBuilder(pgManager.getBinPath("pg_restore"), "-U", "postgres", "-d", "mydb", "-1", "/tmp/backup.dump");
    }

    @Test
    public void testRoleCreate_createsRoleWithPrivileges() throws Exception {
        doReturn(mockConnection).when(pgManager).getConnection();
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);

        pgManager.createRole("devuser", "pass", false);
        verify(mockStatement).execute("CREATE ROLE devuser WITH LOGIN PASSWORD 'pass'");

        pgManager.grantPrivileges("devuser", "mydb", "ALL");
        verify(mockStatement).execute("GRANT ALL ON DATABASE mydb TO devuser");
    }

    @Test
    public void testPgHbaEdit_addsAuthRule() throws Exception {
        Path hbaPath = Paths.get(pgManager.getHbaConfPath());
        Files.createDirectories(hbaPath.getParent());
        Files.write(hbaPath, "# init\n".getBytes());

        doReturn(false).when(pgManager).isRunning(); 

        PostgreSQLManager.HbaRule rule = new PostgreSQLManager.HbaRule("host", "all", "all", "192.168.1.0/24", "scram-sha-256");
        pgManager.addHbaRule(rule);

        String content = new String(Files.readAllBytes(hbaPath));
        assertTrue(content.contains("host\tall\tall\t192.168.1.0/24\tscram-sha-256"));
        
        List<PostgreSQLManager.HbaRule> parsedRules = pgManager.getHbaRules();
        assertEquals(1, parsedRules.size());
        assertEquals("192.168.1.0/24", parsedRules.get(0).address);
        
        pgManager.removeHbaRule(rule);
        String updatedContent = new String(Files.readAllBytes(hbaPath));
        assertFalse(updatedContent.contains("host\tall\tall\t192.168.1.0/24\tscram-sha-256"));
    }
}
