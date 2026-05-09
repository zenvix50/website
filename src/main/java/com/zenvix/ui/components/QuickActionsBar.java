package com.zenvix.ui.components;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * QuickActionsBar provides a horizontal toolbar for bulk service operations
 * orchestrating strict execution hierarchies seamlessly across background threading queues.
 */
public class QuickActionsBar {

    @FXML private HBox toolbarContainer;
    @FXML private Button btnStartAll;
    @FXML private Button btnStopAll;
    @FXML private Button btnRestartAll;
    @FXML private Button btnBackupConfig;
    @FXML private Button btnOpenTerminal;
    @FXML private Button btnOpenBrowser;

    private Consumer<String> statusUpdateCallback;
    private Runnable terminalCallback;
    private final List<ServiceDefinition> registeredServices = new ArrayList<>();

    public static class ServiceDefinition {
        public String id;
        public String category; // JDK, DB, NGINX, TOMCAT, SPRING_BOOT
        public boolean isRunning;
        public boolean hasUnsavedChanges;
        public Runnable startAction;
        public Runnable stopAction;

        public ServiceDefinition(String id, String category, Runnable startAction, Runnable stopAction) {
            this.id = id;
            this.category = category;
            this.startAction = startAction;
            this.stopAction = stopAction;
        }
    }

    public void setCallbacks(Consumer<String> statusUpdateCallback, Runnable terminalCallback) {
        this.statusUpdateCallback = statusUpdateCallback;
        this.terminalCallback = terminalCallback;
    }

    public void registerService(ServiceDefinition service) {
        registeredServices.add(service);
    }

    public void registerShortcuts(Scene scene) {
        KeyCombination startAllKey = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        KeyCombination stopAllKey = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        KeyCombination restartAllKey = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        KeyCombination backupKey = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN);
        KeyCombination terminalKey = new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (startAllKey.match(event) && !btnStartAll.isDisabled()) {
                handleStartAll();
                event.consume();
            } else if (stopAllKey.match(event) && !btnStopAll.isDisabled()) {
                handleStopAll();
                event.consume();
            } else if (restartAllKey.match(event) && !btnRestartAll.isDisabled()) {
                handleRestartAll();
                event.consume();
            } else if (backupKey.match(event) && !btnBackupConfig.isDisabled()) {
                handleBackupConfig();
                event.consume();
            } else if (terminalKey.match(event)) {
                handleOpenTerminal();
                event.consume();
            }
        });
    }

    @FXML
    public void handleStartAll() {
        if (statusUpdateCallback != null) statusUpdateCallback.accept("Starting... (Preparing)");
        setButtonsDisabled(true);

        new Thread(() -> {
            List<ServiceDefinition> ordered = getOrderedServices(true);
            int total = ordered.size();
            int current = 0;

            for (ServiceDefinition service : ordered) {
                if (service.isRunning) {
                    current++;
                    continue;
                }
                
                final int progress = ++current;
                Platform.runLater(() -> {
                    if (statusUpdateCallback != null) {
                        statusUpdateCallback.accept(String.format("Starting %s... (%d/%d)", service.id, progress, total));
                    }
                });

                try {
                    service.startAction.run();
                    service.isRunning = true;
                } catch (Exception e) {
                    boolean continueExecution = promptUserError("Failed to start " + service.id + ": " + e.getMessage() + "\nContinue starting remaining services?");
                    if (!continueExecution) break;
                }
            }

            Platform.runLater(() -> {
                if (statusUpdateCallback != null) statusUpdateCallback.accept("Start All Completed");
                setButtonsDisabled(false);
            });
        }).start();
    }

    @FXML
    public void handleStopAll() {
        if (hasUnsavedChanges() && !confirmAction("Stop All", "Some services have unsaved configurations. Are you sure you want to stop all?")) {
            return;
        }

        if (statusUpdateCallback != null) statusUpdateCallback.accept("Stopping... (Preparing)");
        setButtonsDisabled(true);

        new Thread(() -> {
            List<ServiceDefinition> ordered = getOrderedServices(false);
            int total = ordered.size();
            int current = 0;

            for (ServiceDefinition service : ordered) {
                if (!service.isRunning) {
                    current++;
                    continue;
                }
                
                final int progress = ++current;
                Platform.runLater(() -> {
                    if (statusUpdateCallback != null) {
                        statusUpdateCallback.accept(String.format("Stopping %s... (%d/%d)", service.id, progress, total));
                    }
                });

                try {
                    service.stopAction.run();
                    service.isRunning = false;
                } catch (Exception e) {
                    boolean continueExecution = promptUserError("Failed to stop " + service.id + "\nContinue stopping remaining services?");
                    if (!continueExecution) break;
                }
            }

            Platform.runLater(() -> {
                if (statusUpdateCallback != null) statusUpdateCallback.accept("Stop All Completed");
                setButtonsDisabled(false);
            });
        }).start();
    }

    @FXML
    public void handleRestartAll() {
        if (hasUnsavedChanges() && !confirmAction("Restart All", "Some services have unsaved configurations. Are you sure you want to restart all?")) {
            return;
        }
        
        setButtonsDisabled(true);
        new Thread(() -> {
            List<ServiceDefinition> reverseOrder = getOrderedServices(false);
            for (ServiceDefinition service : reverseOrder) {
                if (service.isRunning) {
                    try {
                        service.stopAction.run();
                        service.isRunning = false;
                    } catch (Exception e) {}
                }
            }

            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            List<ServiceDefinition> ordered = getOrderedServices(true);
            for (ServiceDefinition service : ordered) {
                try {
                    service.startAction.run();
                    service.isRunning = true;
                } catch (Exception e) {
                    boolean cont = promptUserError("Failed to start " + service.id + " during restart.\nContinue?");
                    if (!cont) break;
                }
            }

            Platform.runLater(() -> {
                if (statusUpdateCallback != null) statusUpdateCallback.accept("Restart All Completed");
                setButtonsDisabled(false);
            });
        }).start();
    }

    @FXML
    public void handleBackupConfig() {
        if (statusUpdateCallback != null) statusUpdateCallback.accept("Backing up configurations...");
        
        new Thread(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path backupsDir = Paths.get("zenvix", "config", "backups");
                if (!Files.exists(backupsDir)) Files.createDirectories(backupsDir);
                
                Path zipPath = backupsDir.resolve(timestamp + ".zip");
                Path configDir = Paths.get("zenvix", "config");

                if (Files.exists(configDir)) {
                    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                        Files.walk(configDir)
                             .filter(p -> !Files.isDirectory(p) && !p.startsWith(backupsDir))
                             .forEach(p -> {
                                 try {
                                     ZipEntry zipEntry = new ZipEntry(configDir.relativize(p).toString());
                                     zos.putNextEntry(zipEntry);
                                     Files.copy(p, zos);
                                     zos.closeEntry();
                                 } catch (IOException e) { /* execute through single file lock blocks safely */ }
                             });
                    }
                }
                
                Platform.runLater(() -> {
                    if (statusUpdateCallback != null) statusUpdateCallback.accept("Backup successful: " + zipPath.getFileName());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (statusUpdateCallback != null) statusUpdateCallback.accept("Backup failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void handleOpenTerminal() {
        if (terminalCallback != null) {
            terminalCallback.run();
        }
    }

    @FXML
    public void handleOpenBrowser() {
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:80")); 
            if (statusUpdateCallback != null) statusUpdateCallback.accept("Browser opened.");
        } catch (Exception e) {
            if (statusUpdateCallback != null) statusUpdateCallback.accept("Failed to open browser.");
        }
    }

    private List<ServiceDefinition> getOrderedServices(boolean forStart) {
        List<String> startOrder = Arrays.asList("JDK", "DB", "NGINX", "TOMCAT", "SPRING_BOOT");
        
        List<ServiceDefinition> sorted = new ArrayList<>(registeredServices);
        sorted.sort((s1, s2) -> {
            int idx1 = startOrder.indexOf(s1.category);
            int idx2 = startOrder.indexOf(s2.category);
            if (idx1 == -1) idx1 = 99;
            if (idx2 == -1) idx2 = 99;
            return forStart ? Integer.compare(idx1, idx2) : Integer.compare(idx2, idx1);
        });
        
        return sorted;
    }

    private void setButtonsDisabled(boolean disabled) {
        btnStartAll.setDisable(disabled);
        btnStopAll.setDisable(disabled);
        btnRestartAll.setDisable(disabled);
        btnBackupConfig.setDisable(disabled);
    }

    private boolean hasUnsavedChanges() {
        return registeredServices.stream().anyMatch(s -> s.hasUnsavedChanges);
    }

    private boolean confirmAction(String title, String content) {
        if (!Platform.isFxApplicationThread()) {
            java.util.concurrent.FutureTask<Boolean> query = new java.util.concurrent.FutureTask<>(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
                alert.setTitle(title);
                alert.setHeaderText("Unsaved Changes Detected");
                alert.showAndWait();
                return alert.getResult() == ButtonType.YES;
            });
            Platform.runLater(query);
            try { return query.get(); } catch (Exception e) { return false; }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
            alert.setTitle(title);
            alert.setHeaderText("Unsaved Changes Detected");
            alert.showAndWait();
            return alert.getResult() == ButtonType.YES;
        }
    }

    private boolean promptUserError(String message) {
        if (!Platform.isFxApplicationThread()) {
            java.util.concurrent.FutureTask<Boolean> query = new java.util.concurrent.FutureTask<>(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.YES, ButtonType.NO);
                alert.setTitle("Execution Error");
                alert.showAndWait();
                return alert.getResult() == ButtonType.YES;
            });
            Platform.runLater(query);
            try { return query.get(); } catch (Exception e) { return false; }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.YES, ButtonType.NO);
            alert.setTitle("Execution Error");
            alert.showAndWait();
            return alert.getResult() == ButtonType.YES;
        }
    }
}
