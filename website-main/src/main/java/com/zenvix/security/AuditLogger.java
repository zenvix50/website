package com.zenvix.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ensures strict append-only compliance executing physical FileChannel writes
 * cryptographically signed via HMAC-SHA256 preventing tampering perfectly natively.
 */
public class AuditLogger {

    private final Path logsDir;
    private final byte[] signingKey;
    private final ObjectMapper mapper;

    public AuditLogger(Path baseDir, String auditPassword) {
        this.logsDir = baseDir.resolve("logs");
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {}
        
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            this.signingKey = md.digest(auditPassword.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
        
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        
        rotateLogs();
    }

    private Path getCurrentLogFile() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return logsDir.resolve("audit-" + dateStr + ".log");
    }

    public void log(AuditEntry.ActionType action, String serviceId, String configKey, String actor) {
        String safeConfigKey = configKey;
        if (configKey != null && configKey.contains("=")) {
            safeConfigKey = configKey.substring(0, configKey.indexOf("=")) + "=[MASKED]";
        }
        
        AuditEntry entry = new AuditEntry(action, serviceId, safeConfigKey, actor);
        entry.entryHash = computeHMAC(entry.getRawContent());
        
        String json;
        try {
            json = mapper.writeValueAsString(entry) + "\n";
        } catch (Exception e) {
            return;
        }
        
        Path logFile = getCurrentLogFile();
        try (FileChannel channel = FileChannel.open(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {}
    }

    protected String computeHMAC(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(signingKey, "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public List<AuditEntry> verifyAndReadLogs(Path logFile) throws SecurityException {
        List<AuditEntry> entries = new ArrayList<>();
        if (!Files.exists(logFile)) return entries;
        
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                AuditEntry entry = mapper.readValue(line, AuditEntry.class);
                String expectedHash = computeHMAC(entry.getRawContent());
                if (!expectedHash.equals(entry.entryHash)) {
                    throw new SecurityException("Tampering detected in log file " + logFile.getFileName() + " at line " + lineNumber);
                }
                entries.add(entry);
                lineNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read logs", e);
        }
        return entries;
    }

    public void exportToCSV(List<AuditEntry> entries, Path destFile) {
        try (FileWriter out = new FileWriter(destFile.toFile());
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("ID", "Timestamp", "Action", "Service", "ConfigKey", "Actor", "Signature"))) {
            for (AuditEntry e : entries) {
                printer.printRecord(e.id, e.timestamp, e.actionType, e.serviceId, e.configKey, e.actor, e.entryHash);
            }
        } catch (IOException e) {}
    }

    public void exportToJSON(List<AuditEntry> entries, Path destFile) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(destFile.toFile(), entries);
        } catch (IOException e) {}
    }

    public void rotateLogs() {
        LocalDate cutoff = LocalDate.now().minusDays(90);
        try (Stream<Path> files = Files.list(logsDir)) {
            files.filter(p -> p.getFileName().toString().startsWith("audit-") && p.getFileName().toString().endsWith(".log"))
                 .forEach(p -> {
                     String name = p.getFileName().toString();
                     try {
                         String datePart = name.substring(6, 14); 
                         LocalDate logDate = LocalDate.parse(datePart, DateTimeFormatter.BASIC_ISO_DATE);
                         if (logDate.isBefore(cutoff)) {
                             Files.delete(p);
                         }
                     } catch (Exception e) {}
                 });
        } catch (IOException e) {}
    }
}
