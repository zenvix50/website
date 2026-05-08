package com.zenvix.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EnvironmentEditorTest {

    @TempDir
    Path tempDir;

    private EnvironmentEditor editor;

    @BeforeEach
    public void setUp() {
        editor = new EnvironmentEditor(tempDir.toString());
    }

    @Test
    public void testGlobalVars_appliedToAllProcesses() {
        editor.setGlobalVar("DB_HOST", "localhost");
        editor.setGlobalVar("DEBUG", "true");

        Map<String, String> effective = editor.getEffectiveEnv(null);
        assertEquals("localhost", effective.get("DB_HOST"));
        assertEquals("true", effective.get("DEBUG"));

        ProcessBuilder pb = new ProcessBuilder();
        editor.applyToProcessBuilder(pb, null);
        assertEquals("localhost", pb.environment().get("DB_HOST"));
    }

    @Test
    public void testProjectVars_overrideGlobalVars() {
        editor.setGlobalVar("DB_HOST", "localhost");
        editor.setGlobalVar("DB_PORT", "3306");

        editor.setProjectVar("proj1", "DB_HOST", "192.168.1.100");
        editor.setProjectVar("proj1", "APP_ENV", "prod");

        Map<String, String> effective = editor.getEffectiveEnv("proj1");
        
        assertEquals("192.168.1.100", effective.get("DB_HOST")); 
        assertEquals("3306", effective.get("DB_PORT")); 
        assertEquals("prod", effective.get("APP_ENV")); 
    }

    @Test
    public void testMerge_correctInheritanceModel() {
        editor.setGlobalVar("A", "globalA");
        editor.setGlobalVar("B", "globalB");
        
        editor.setProjectVar("proj1", "B", "projB");
        editor.setProjectVar("proj2", "C", "projC");

        Map<String, String> env1 = editor.getEffectiveEnv("proj1");
        assertEquals("globalA", env1.get("A"));
        assertEquals("projB", env1.get("B"));
        assertNull(env1.get("C"));

        Map<String, String> env2 = editor.getEffectiveEnv("proj2");
        assertEquals("globalA", env2.get("A"));
        assertEquals("globalB", env2.get("B"));
        assertEquals("projC", env2.get("C"));
    }

    @Test
    public void testExport_generatesEnvFile() throws IOException {
        editor.setGlobalVar("DB_HOST", "db.local");
        editor.setProjectVar("projX", "SECRET", "12345");

        Path exportPath = tempDir.resolve(".env");
        editor.exportToEnvFile("projX", exportPath);

        String content = new String(Files.readAllBytes(exportPath));
        assertTrue(content.contains("DB_HOST=db.local"));
        assertTrue(content.contains("SECRET=12345"));
    }

    @Test
    public void testImport_parsesEnvFile() throws IOException {
        Path importPath = tempDir.resolve("import.env");
        String content = "API_KEY=999\n" +
                         "URL=\"http://example.com\"\n" +
                         "# Comment line\n" +
                         "MODE='test'\n";
        Files.write(importPath, content.getBytes());

        editor.importFromEnvFile(importPath, "projY");

        Map<String, String> vars = editor.getProjectVars("projY");
        assertEquals("999", vars.get("API_KEY"));
        assertEquals("http://example.com", vars.get("URL")); 
        assertEquals("test", vars.get("MODE")); 
        assertFalse(vars.containsKey("# Comment line"));
    }

    @Test
    public void testPersistence_serializesToJsonCorrectly() {
        editor.setGlobalVar("GLOBAL_KEY", "value\\with\"quotes");
        editor.setProjectVar("p1", "PROJ_KEY", "123");

        EnvironmentEditor newEditor = new EnvironmentEditor(tempDir.toString());
        Map<String, String> global = newEditor.getGlobalVars();
        assertEquals("value\\with\"quotes", global.get("GLOBAL_KEY"));

        Map<String, String> proj = newEditor.getProjectVars("p1");
        assertEquals("123", proj.get("PROJ_KEY"));
    }
}
