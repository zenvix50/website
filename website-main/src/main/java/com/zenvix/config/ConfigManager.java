package com.zenvix.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Automates Atomic JSON parsing cleanly routing safe configuration writes seamlessly
 * preventing corruption across physical hardware faults securely globally.
 */
public class ConfigManager {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path configDir;
    private final List<ConfigMigration> migrations = new ArrayList<>();
    public final int CURRENT_SCHEMA_VERSION = 2;

    public ConfigManager(Path configDir) {
        this.configDir = configDir;
        try { Files.createDirectories(configDir); } catch (Exception e) {}
    }

    public void addMigration(ConfigMigration migration) {
        migrations.add(migration);
    }

    public <T> T loadConfig(String filename, Class<T> clazz) throws Exception {
        Path file = configDir.resolve(filename);
        if (!Files.exists(file)) {
            return clazz.getDeclaredConstructor().newInstance();
        }

        Path checksumFile = configDir.resolve(filename + ".sha256");
        if (Files.exists(checksumFile)) {
            String expected = Files.readString(checksumFile);
            String actual = calculateChecksum(file);
            if (!expected.equals(actual)) {
                throw new Exception("Config checksum mismatch: " + filename + " is corrupted.");
            }
        }

        JsonNode rootNode = mapper.readTree(file.toFile());
        if (rootNode.has("schemaVersion") && rootNode instanceof ObjectNode) {
            int version = rootNode.get("schemaVersion").asInt();
            if (version > CURRENT_SCHEMA_VERSION) {
                throw new Exception("Config version newer than supported by this zenviX version.");
            } else if (version < CURRENT_SCHEMA_VERSION) {
                rootNode = runMigrations((ObjectNode) rootNode, version);
                saveConfig(filename, rootNode); 
            }
        }

        return mapper.treeToValue(rootNode, clazz);
    }

    public void saveConfig(String filename, Object configObj) throws Exception {
        Path file = configDir.resolve(filename);
        Path tempFile = configDir.resolve(filename + ".tmp");

        mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), configObj);
        
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        
        String checksum = calculateChecksum(file);
        Files.writeString(configDir.resolve(filename + ".sha256"), checksum);
    }

    private ObjectNode runMigrations(ObjectNode config, int currentVersion) {
        migrations.sort(Comparator.comparingInt(ConfigMigration::fromVersion));
        for (ConfigMigration m : migrations) {
            if (m.fromVersion() >= currentVersion) {
                m.migrate(config);
                config.put("schemaVersion", m.toVersion());
                currentVersion = m.toVersion();
            }
        }
        return config;
    }

    private String calculateChecksum(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(file));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
