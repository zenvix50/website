package com.zenvix.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Main zenviX Control Panel window.
 * Manages global keyboard shortcuts mapping 1-9 directly across dynamic service components asynchronously!
 */
public class MainWindowController {

    @FXML private VBox servicesContainer;
    @FXML private Label heapIndicator;
    @FXML private Label lastActionLabel;
    @FXML private Label updateBadge;
    @FXML private Button themeToggleButton;
    @FXML private Button minimizeToTrayButton;

    private boolean isDarkMode = true;
    private final List<ServiceRowController> serviceRows = new ArrayList<>();

    private final KeyCombination startAllKey = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    private final KeyCombination stopAllKey = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    private final KeyCombination terminalKey = new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN);
    private final KeyCombination logsKey = new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN);
    private final KeyCombination helpKey = new KeyCodeCombination(KeyCode.F1);

    @FXML
    public void initialize() {
        startHeapMonitor();
        updateLastAction("Dashboard Loaded");
    }

    public void addServiceRow(String name, String version, String status, int port) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zenvix/ui/ServiceRow.fxml"));
            Node node = loader.load();
            ServiceRowController controller = loader.getController();
            
            controller.initData(name, version, status, port);
            controller.setMainController(this);
            
            serviceRows.add(controller);
            servicesContainer.getChildren().add(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (startAllKey.match(event)) {
            serviceRows.forEach(ServiceRowController::handleStart);
            updateLastAction("Started all services");
            event.consume();
        } else if (stopAllKey.match(event)) {
            serviceRows.forEach(ServiceRowController::handleStop);
            updateLastAction("Stopped all services");
            event.consume();
        } else if (terminalKey.match(event)) {
            openTerminal();
            event.consume();
        } else if (logsKey.match(event)) {
            openGlobalLogs();
            event.consume();
        } else if (helpKey.match(event)) {
            showHelpDialog();
            event.consume();
        } else if (event.isControlDown() && event.getCode().isDigitKey()) {
            int index = Integer.parseInt(event.getText()) - 1;
            if (index >= 0 && index < serviceRows.size()) {
                ServiceRowController row = serviceRows.get(index);
                if (row.isStopped()) row.handleStart();
                else row.handleStop();
                updateLastAction("Toggled service: " + row.getServiceName());
                event.consume();
            }
        }
    }

    @FXML
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        updateLastAction("Theme changed to " + (isDarkMode ? "Dark" : "Light"));
    }

    @FXML
    private void minimizeToTray() {
        updateLastAction("Minimized to tray");
    }

    public void updateLastAction(String action) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> lastActionLabel.setText(String.format("Last Action: %s at %s", action, timestamp)));
    }

    private void startHeapMonitor() {
        Thread monitor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Runtime rt = Runtime.getRuntime();
                    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                    long maxMB = rt.maxMemory() / 1024 / 1024;
                    Platform.runLater(() -> heapIndicator.setText(String.format("Heap: %dMB / %dMB", usedMB, maxMB)));
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    private void openTerminal() { /* System integration handled downstream */ }
    private void openGlobalLogs() { /* System integration handled downstream */ }
    
    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("zenviX Shortcuts");
        alert.setContentText("Ctrl+1-9 : Toggle Service\n" +
                             "Ctrl+Shift+S : Start All\n" +
                             "Ctrl+Shift+X : Stop All\n" +
                             "Ctrl+T : Open Terminal\n" +
                             "Ctrl+L : Open Logs\n" +
                             "F1 : This Help Menu");
        alert.show();
    }
}
