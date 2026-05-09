package com.zenvix.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuditLoggerTest {

    @TempDir
    Path tempDir;

    private AuditLogger logger;

    @BeforeEach
    public void setUp() {
        logger = new AuditLogger(tempDir, "super-secret-password");
    }

    private Path getCurrentLogFile() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return tempDir.resolve("logs").resolve("audit-" + dateStr + ".log");
    }

    @Test
    public void testLog_appendsEntryToFile() throws Exception {
        logger.log(AuditEntry.ActionType.SERVICE_START, "MySQL", null, "system");
        
        Path logFile = getCurrentLogFile();
        assertTrue(Files.exists(logFile));
        
        List<AuditEntry> entries = logger.verifyAndReadLogs(logFile);
        assertEquals(1, entries.size());
        assertEquals(AuditEntry.ActionType.SERVICE_START, entries.get(0).actionType);
        assertEquals("MySQL", entries.get(0).serviceId);
        
        logger.log(AuditEntry.ActionType.SERVICE_STOP, "MySQL", null, "system");
        entries = logger.verifyAndReadLogs(logFile);
        assertEquals(2, entries.size());
    }

    @Test
    public void testHMAC_entryHasValidSignature() {
        logger.log(AuditEntry.ActionType.VAULT_ACCESS, "Vault", null, "admin");
        
        Path logFile = getCurrentLogFile();
        List<AuditEntry> entries = logger.verifyAndReadLogs(logFile);
        
        AuditEntry entry = entries.get(0);
        String expectedHash = logger.computeHMAC(entry.getRawContent());
        
        assertEquals(expectedHash, entry.entryHash);
        assertFalse(entry.entryHash.isEmpty());
    }

    @Test
    public void testTamperDetection_detectsModifiedEntry() throws IOException {
        logger.log(AuditEntry.ActionType.SERVICE_START, "Tomcat", null, "user");
        Path logFile = getCurrentLogFile();
        
        String content = new String(Files.readAllBytes(logFile));
        content = content.replace("Tomcat", "Nginx");
        Files.write(logFile, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        
        assertThrows(SecurityException.class, () -> {
            logger.verifyAndReadLogs(logFile);
        });
    }

    @Test
    public void testExportJSON_exportsAllEntries() throws IOException {
        logger.log(AuditEntry.ActionType.PASSWORD_CHANGE, "Global", null, "admin");
        List<AuditEntry> entries = logger.verifyAndReadLogs(getCurrentLogFile());
        
        Path jsonFile = tempDir.resolve("export.json");
        logger.exportToJSON(entries, jsonFile);
        
        assertTrue(Files.exists(jsonFile));
        String jsonContent = new String(Files.readAllBytes(jsonFile));
        assertTrue(jsonContent.contains("PASSWORD_CHANGE"));
    }

    @Test
    public void testExportCSV_exportsAllEntries() throws IOException {
        logger.log(AuditEntry.ActionType.CONFIG_CHANGE, "Redis", "port", "admin");
        List<AuditEntry> entries = logger.verifyAndReadLogs(getCurrentLogFile());
        
        Path csvFile = tempDir.resolve("export.csv");
        logger.exportToCSV(entries, csvFile);
        
        assertTrue(Files.exists(csvFile));
        String csvContent = new String(Files.readAllBytes(csvFile));
        assertTrue(csvContent.contains("CONFIG_CHANGE"));
        assertTrue(csvContent.contains("Redis"));
    }

    @Test
    public void testRotation_createsNewFilePerDay() throws IOException {
        LocalDate oldDate = LocalDate.now().minusDays(95);
        Path oldLog = tempDir.resolve("logs").resolve("audit-" + oldDate.format(DateTimeFormatter.BASIC_ISO_DATE) + ".log");
        Files.write(oldLog, "old data".getBytes());
        
        LocalDate recentDate = LocalDate.now().minusDays(10);
        Path recentLog = tempDir.resolve("logs").resolve("audit-" + recentDate.format(DateTimeFormatter.BASIC_ISO_DATE) + ".log");
        Files.write(recentLog, "recent data".getBytes());
        
        logger.rotateLogs();
        
        assertFalse(Files.exists(oldLog), "Log older than 90 days should be deleted");
        assertTrue(Files.exists(recentLog), "Log within 90 days should be kept");
    }

    @Test
    public void testGDPR_noValuesInLog() {
        logger.log(AuditEntry.ActionType.CONFIG_CHANGE, "Database", "db.password=secret123", "admin");
        
        Path logFile = getCurrentLogFile();
        List<AuditEntry> entries = logger.verifyAndReadLogs(logFile);
        
        AuditEntry entry = entries.get(0);
        
        assertTrue(entry.configKey.contains("[MASKED]"));
        assertFalse(entry.configKey.contains("secret123"));
    }
}
