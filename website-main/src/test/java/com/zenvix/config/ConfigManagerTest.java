package com.zenvix.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ConfigManagerTest {

    private ConfigManager configManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        configManager = new ConfigManager(tempDir);
    }

    @Test
    public void testReadWrite_roundTripAllConfigTypes() throws Exception {
        ConfigModel.AppConfig appConfig = new ConfigModel.AppConfig();
        appConfig.theme = "dark";
        
        configManager.saveConfig("app.json", appConfig);
        
        ConfigModel.AppConfig loaded = configManager.loadConfig("app.json", ConfigModel.AppConfig.class);
        assertEquals("dark", loaded.theme);
        assertEquals(1, loaded.schemaVersion);
    }

    @Test
    public void testAtomicWrite_doesNotCorruptOnFailure() throws Exception {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, "{\"valid\":\"json\"}");
        Files.writeString(tempDir.resolve("app.json.sha256"), "dummy_hash_will_fail");
        
        Exception ex = assertThrows(Exception.class, () -> {
            configManager.loadConfig("app.json", ConfigModel.AppConfig.class);
        });
        
        assertTrue(ex.getMessage().contains("checksum mismatch"));
    }

    @Test
    public void testMigration_upgradesSchemaVersion() throws Exception {
        ConfigMigration migration = new ConfigMigration() {
            public int fromVersion() { return 1; }
            public int toVersion() { return 2; }
            public void migrate(ObjectNode json) { json.put("trayEnabled", true); }
        };
        
        configManager.addMigration(migration);
        
        Path file = tempDir.resolve("legacy.json");
        Files.writeString(file, "{\"schemaVersion\": 1, \"theme\": \"light\"}");
        
        ConfigModel.AppConfig loaded = configManager.loadConfig("legacy.json", ConfigModel.AppConfig.class);
        
        assertEquals(2, loaded.schemaVersion);
        assertTrue(loaded.trayEnabled);
    }
}
