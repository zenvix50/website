package com.zenvix.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Controller for individual Service Rows in the zenviX control panel.
 * Handles inline port editing, state transitions natively simulating async GUI updates.
 */
public class ServiceRowController {

    @FXML private Label serviceNameLabel;
    @FXML private ComboBox<String> versionDropdown;
    @FXML private Label statusBadge;
    @FXML private Label portLabel;
    @FXML private TextField portField;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button restartButton;

    private MainWindowController mainController;

    public void setMainController(MainWindowController mainController) {
        this.mainController = mainController;
    }

    public void initData(String name, String version, String status, int port) {
        serviceNameLabel.setText(name);
        versionDropdown.getItems().addAll(version, "Latest", "LTS");
        versionDropdown.getSelectionModel().selectFirst();
        
        versionDropdown.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isStopped() && mainController != null) {
                mainController.updateLastAction("Stop service before changing version");
                versionDropdown.getSelectionModel().select(oldVal); 
            }
        });

        portLabel.setText(String.valueOf(port));
        portField.setText(String.valueOf(port));
        
        portField.setOnKeyPressed(this::handlePortFieldKey);
        portField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) cancelPortEdit();
        });

        updateStatus(status);
    }

    @FXML
    public void handleStart() {
        if (!isStopped()) return;
        updateStatus("STARTING");
        if (mainController != null) mainController.updateLastAction("Starting " + getServiceName());
        
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> updateStatus("RUNNING"));
        }).start();
    }

    @FXML
    public void handleStop() {
        if (isStopped()) return;
        updateStatus("STOPPING");
        if (mainController != null) mainController.updateLastAction("Stopping " + getServiceName());
        
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> updateStatus("STOPPED"));
        }).start();
    }

    @FXML
    public void handleRestart() {
        handleStop();
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(this::handleStart);
        }).start();
    }

    @FXML
    public void handleLogs() {
        if (mainController != null) mainController.updateLastAction("Opened logs for " + getServiceName());
    }

    @FXML
    public void handleConfig() {
        if (mainController != null) mainController.updateLastAction("Opened config for " + getServiceName());
    }

    @FXML
    private void handlePortClick(MouseEvent event) {
        portLabel.setVisible(false);
        portField.setVisible(true);
        portField.requestFocus();
        portField.selectAll();
    }

    private void handlePortFieldKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                int newPort = Integer.parseInt(portField.getText());
                if (newPort >= 1 && newPort <= 65535) {
                    portLabel.setText(String.valueOf(newPort));
                    if (mainController != null) mainController.updateLastAction("Updated port for " + getServiceName());
                } else {
                    portField.setText(portLabel.getText()); 
                }
            } catch (NumberFormatException e) {
                portField.setText(portLabel.getText());
            }
            cancelPortEdit();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            portField.setText(portLabel.getText());
            cancelPortEdit();
        }
    }

    private void cancelPortEdit() {
        portField.setVisible(false);
        portLabel.setVisible(true);
    }

    private void updateStatus(String status) {
        statusBadge.setText(status.toUpperCase());
        statusBadge.setAccessibleText("Service Status " + status);
        
        switch (status.toUpperCase()) {
            case "RUNNING":
                statusBadge.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 3 8;");
                startButton.setDisable(true);
                stopButton.setDisable(false);
                restartButton.setDisable(false);
                break;
            case "STOPPED":
                statusBadge.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 3 8;");
                startButton.setDisable(false);
                stopButton.setDisable(true);
                restartButton.setDisable(true);
                break;
            case "STARTING":
            case "STOPPING":
                statusBadge.setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: black; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 3 8;");
                startButton.setDisable(true);
                stopButton.setDisable(true);
                restartButton.setDisable(true);
                break;
            case "WARNING":
                statusBadge.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 3 8;");
                startButton.setDisable(true);
                stopButton.setDisable(false);
                restartButton.setDisable(false);
                break;
        }
    }

    public boolean isStopped() {
        return "STOPPED".equals(statusBadge.getText());
    }

    public String getServiceName() {
        return serviceNameLabel.getText();
    }
}
