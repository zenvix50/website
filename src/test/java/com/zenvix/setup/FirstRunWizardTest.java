package com.zenvix.setup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FirstRunWizardTest {

    private FirstRunWizard wizard;
    private ServiceDetector mockDetector;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        mockDetector = spy(new ServiceDetector());
        wizard = spy(new FirstRunWizard(mockDetector));
        
        Path mockConfigDir = tempDir.resolve("config");
        
        java.lang.reflect.Field configDirField = FirstRunWizard.class.getDeclaredField("configDir");
        configDirField.setAccessible(true);
        configDirField.set(wizard, mockConfigDir);
        
        java.lang.reflect.Field configFileField = FirstRunWizard.class.getDeclaredField("configFile");
        configFileField.setAccessible(true);
        configFileField.set(wizard, mockConfigDir.resolve("config.json"));
        
        java.lang.reflect.Field gitignoreFileField = FirstRunWizard.class.getDeclaredField("gitignoreFile");
        gitignoreFileField.setAccessible(true);
        gitignoreFileField.set(wizard, mockConfigDir.resolve(".gitignore"));
    }

    @Test
    public void testDetection_detectsInstalledServices() throws Exception {
        File dummyDir = Files.createTempDirectory("dummyBin").toFile();
        File dummyJava = new File(dummyDir, "java");
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            dummyJava = new File(dummyDir, "java.exe");
        }
        dummyJava.createNewFile();
        dummyJava.setExecutable(true);

        doReturn(dummyDir.getAbsolutePath()).when(mockDetector).getSystemPath();
        doReturn("17.0.1").when(mockDetector).getVersion(anyString());
        
        wizard.advanceStep(); 
        assertEquals(WizardStep.DETECT_INSTALLATIONS, wizard.getCurrentStep());
        
        Map<String, ServiceDetector.DetectedService> detected = wizard.getDetectedServices();
        assertTrue(detected.containsKey("java"));
        assertEquals(dummyJava.getAbsolutePath(), detected.get("java").path);
        assertEquals("17.0.1", detected.get("java").version);
    }

    @Test
    public void testMasterPassword_initializesVault() throws Exception {
        wizard.setMasterPassword("SuperSecret123!");
        
        while(wizard.getCurrentStep() != WizardStep.COMPLETE) {
            wizard.advanceStep();
        }
        
        Path vaultFile = tempDir.resolve("config").resolve("secrets.vault");
        assertTrue(Files.exists(vaultFile), "Secrets vault should be generated");
    }

    @Test
    public void testGitignore_generatesGitignoreFile() throws Exception {
        while(wizard.getCurrentStep() != WizardStep.COMPLETE) {
            wizard.advanceStep();
        }
        
        Path gitignore = tempDir.resolve("config").resolve(".gitignore");
        assertTrue(Files.exists(gitignore));
        String content = Files.readString(gitignore);
        assertTrue(content.contains("secrets.vault"));
    }

    @Test
    public void testConfig_createsDefaultConfigJson() throws Exception {
        wizard.setInstallPath(tempDir.resolve("custom_zenvix"));
        
        while(wizard.getCurrentStep() != WizardStep.COMPLETE) {
            wizard.advanceStep();
        }
        
        Path config = tempDir.resolve("config").resolve("config.json");
        assertTrue(Files.exists(config));
        String content = Files.readString(config);
        assertTrue(content.contains("custom_zenvix"));
    }

    @Test
    public void testServiceSelection_enablesSelectedServices() throws Exception {
        wizard.setSelectedServices(Arrays.asList("MySQL", "Redis", "Tomcat"));
        
        while(wizard.getCurrentStep() != WizardStep.COMPLETE) {
            wizard.advanceStep();
        }
        
        Path config = tempDir.resolve("config").resolve("config.json");
        String content = Files.readString(config);
        assertTrue(content.contains("\"MySQL\""));
        assertTrue(content.contains("\"Redis\""));
        assertTrue(content.contains("\"Tomcat\""));
    }
}
