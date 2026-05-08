package com.zenvix.setup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates synchronous onboarding architectures gracefully stepping users 
 * through Master Password generation and config payloads accurately explicitly natively.
 */
public class FirstRunWizard {

    private final Path configDir = Paths.get("zenvix", "config");
    private final Path configFile = configDir.resolve("config.json");
    private final Path gitignoreFile = configDir.resolve(".gitignore");
    
    private final ServiceDetector serviceDetector;
    private WizardStep currentStep = WizardStep.WELCOME;
    
    private Map<String, ServiceDetector.DetectedService> detectedServices = new HashMap<>();
    private String masterPassword;
    private List<String> selectedServices = new ArrayList<>();
    private Path installPath = Paths.get("zenvix");

    public FirstRunWizard(ServiceDetector serviceDetector) {
        this.serviceDetector = serviceDetector;
    }

    public boolean isFirstRun() {
        return !Files.exists(configFile);
    }

    public WizardStep getCurrentStep() {
        return currentStep;
    }

    public void advanceStep() {
        WizardStep[] steps = WizardStep.values();
        if (currentStep.ordinal() < steps.length - 1) {
            currentStep = steps[currentStep.ordinal() + 1];
            executeStepLogic();
        }
    }
    
    private void executeStepLogic() {
        if (currentStep == WizardStep.DETECT_INSTALLATIONS) {
            detectedServices = serviceDetector.detectAll();
        } else if (currentStep == WizardStep.COMPLETE) {
            completeSetup();
        }
    }

    public void setInstallPath(Path path) {
        this.installPath = path;
    }

    public void setMasterPassword(String password) {
        this.masterPassword = password;
    }

    public void setSelectedServices(List<String> services) {
        this.selectedServices = new ArrayList<>(services);
    }
    
    public Map<String, ServiceDetector.DetectedService> getDetectedServices() {
        return detectedServices;
    }

    protected void completeSetup() {
        try {
            Files.createDirectories(configDir);
            
            initializeSecretsVault(masterPassword);
            
            if (!Files.exists(gitignoreFile)) {
                Files.writeString(gitignoreFile, "secrets.vault\n*.log\n");
            }
            
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"installPath\": \"").append(installPath.toString().replace("\\", "\\\\")).append("\",\n");
            json.append("  \"enabledServices\": [\n");
            for (int i = 0; i < selectedServices.size(); i++) {
                json.append("    \"").append(selectedServices.get(i)).append("\"");
                if (i < selectedServices.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
            json.append("}\n");
            
            Files.writeString(configFile, json.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initializeSecretsVault(String password) throws Exception {
        Path vaultPath = configDir.resolve("secrets.vault");
        Files.writeString(vaultPath, "ENCRYPTED_VAULT_MOCK");
    }
}
