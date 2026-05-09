package com.zenvix.core.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MySQLManagerTest {

    @TempDir
    Path tempDir;

    private MySQLManager mysqlManager;

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
        
        mysqlManager = spy(new MySQLManager(tempDir.toString()));
        mysqlManager.setPort(3306);
        SecretsVault.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mysqlManager.isRunning()) {
            // Need to mock stop cleanly if we are 'running'
            doNothing().when(mysqlManager).stop();
            mysqlManager.stop();
        }
        closeable.close();
        SecretsVault.clear();
    }

    @Test
    public void testStart_mysqlStartsSuccessfully() throws Exception {
        doReturn(true).when(mysqlManager).isPortAvailable(3306);
        
        doReturn(mockPb).when(mysqlManager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectErrorStream(true)).thenReturn(mockPb);
        
        doAnswer(invocation -> {
            Files.createDirectories(new File(mysqlManager.getPidFile()).getParentFile().toPath());
            Files.write(new File(mysqlManager.getPidFile()).toPath(), "12345".getBytes());
            return mockProcess;
        }).when(mockPb).start();

        when(mockProcess.isAlive()).thenReturn(true);

        mysqlManager.start();

        assertTrue(mysqlManager.isRunning());
        assertEquals("RUNNING", mysqlManager.getStatus());
    }

    @Test
    public void testPortConflict_throwsException() throws Exception {
        doReturn(false).when(mysqlManager).isPortAvailable(3306);

        Exception exception = assertThrows(Exception.class, () -> {
            mysqlManager.start();
        });
        assertTrue(exception.getMessage().contains("already in use"));
    }

    @Test
    public void testImport_importsSQLFile() throws Exception {
        SecretsVault.set("mysql.root.password", "secret");
        
        doReturn(mockPb).when(mysqlManager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectInput(any(File.class))).thenReturn(mockPb);
        when(mockPb.redirectErrorStream(true)).thenReturn(mockPb);
        
        // Return a process with an empty input stream so drain logic doesn't NPE
        java.io.ByteArrayInputStream emptyStream = new java.io.ByteArrayInputStream(new byte[0]);
        when(mockProcess.getInputStream()).thenReturn(emptyStream);
        
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);

        File dummySql = tempDir.resolve("dump.sql").toFile();
        Files.write(dummySql.toPath(), "SELECT 1;".getBytes());

        mysqlManager.importDatabase("testdb", dummySql.getAbsolutePath());

        verify(mysqlManager).createProcessBuilder(mysqlManager.getBinPath("mysql"), "-u", "root", "-psecret", "-P", "3306", "-h", "127.0.0.1", "testdb");
    }

    @Test
    public void testExport_dumpsDatabaseToFile() throws Exception {
        SecretsVault.set("mysql.root.password", "secret");
        
        doReturn(mockPb).when(mysqlManager).createProcessBuilder(any(String[].class));
        when(mockPb.redirectOutput(any(File.class))).thenReturn(mockPb);
        when(mockPb.redirectErrorStream(false)).thenReturn(mockPb);
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.exitValue()).thenReturn(0);

        File outputSql = tempDir.resolve("out.sql").toFile();

        mysqlManager.exportDatabase("testdb", outputSql.getAbsolutePath());

        verify(mysqlManager).createProcessBuilder(mysqlManager.getBinPath("mysqldump"), "-u", "root", "-psecret", "-P", "3306", "-h", "127.0.0.1", "--routines", "--triggers", "testdb");
    }

    @Test
    public void testUserCreate_createsUserWithPrivileges() throws Exception {
        doReturn(mockConnection).when(mysqlManager).getConnection();
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);

        mysqlManager.createUser("newuser", "password123");
        verify(mockStatement).execute("CREATE USER 'newuser'@'%' IDENTIFIED BY 'password123'");

        mysqlManager.grantPrivileges("newuser", "testdb", "ALL PRIVILEGES");
        verify(mockStatement).execute("GRANT ALL PRIVILEGES ON testdb.* TO 'newuser'@'%'");
        verify(mockStatement).execute("FLUSH PRIVILEGES");
    }

    @Test
    public void testPasswordChange_updatesVaultEntry() throws Exception {
        doReturn(mockConnection).when(mysqlManager).getConnection();
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);

        mysqlManager.setIsMariaDB(false); // testing MySQL 8
        mysqlManager.changeRootPassword("new_strong_password");

        verify(mockStatement).execute("ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'new_strong_password'");
        assertEquals("new_strong_password", SecretsVault.get("mysql.root.password"));
    }
}
