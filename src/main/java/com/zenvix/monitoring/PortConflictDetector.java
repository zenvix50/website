package com.zenvix.monitoring;

import javafx.application.Platform;
import javafx.scene.control.*;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Ensures strict port boundary safety by validating active socket allocations across 
 * physical system states mapping occupying processes dynamically resolving conflicts securely.
 */
public class PortConflictDetector {

    private final OperatingSystem os = new SystemInfo().getOperatingSystem();

    public enum ConflictResolution { KILL_PROCESS, CHANGE_PORT, CANCEL }

    public static class ConflictResponse {
        public final ConflictResolution resolution;
        public final int newPort;

        public ConflictResponse(ConflictResolution resolution, int newPort) {
            this.resolution = resolution;
            this.newPort = newPort;
        }
    }

    public PortScanResult checkPort(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return new PortScanResult(port, false, null, -1);
        } catch (Exception e) {
            return identifyOccupant(port);
        }
    }

    public List<PortScanResult> checkPortsInParallel(List<Integer> ports) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(ports.size(), 10));
        List<Callable<PortScanResult>> tasks = new ArrayList<>();
        
        for (int port : ports) {
            tasks.add(() -> checkPort(port));
        }

        List<PortScanResult> results = new ArrayList<>();
        try {
            List<Future<PortScanResult>> futures = executor.invokeAll(tasks);
            for (Future<PortScanResult> future : futures) {
                results.add(future.get());
            }
        } catch (Exception e) {
        } finally {
            executor.shutdown();
        }
        return results;
    }

    protected PortScanResult identifyOccupant(int port) {
        int occupyingPid = -1;
        String occupyingName = "Unknown Process";

        String osName = System.getProperty("os.name").toLowerCase();
        try {
            Process p;
            if (osName.contains("win")) {
                p = new ProcessBuilder("cmd", "/c", "netstat -ano | findstr :" + port).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("LISTENING")) {
                        String[] parts = line.trim().split("\\s+");
                        occupyingPid = Integer.parseInt(parts[parts.length - 1]);
                        break;
                    }
                }
            } else {
                p = new ProcessBuilder("sh", "-c", "lsof -i :" + port + " -sTCP:LISTEN -t").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    occupyingPid = Integer.parseInt(line.trim());
                }
            }
        } catch (Exception e) {}

        if (occupyingPid > 0) {
            OSProcess process = os.getProcess(occupyingPid);
            if (process != null) {
                occupyingName = process.getName();
            }
        }

        return new PortScanResult(port, true, occupyingName, occupyingPid);
    }

    public boolean killProcess(int pid) {
        if (pid <= 0) return false;
        return killNative(pid);
    }
    
    protected boolean killNative(int pid) {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            Process p;
            if (osName.contains("win")) {
                p = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid)).start();
            } else {
                p = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
            }
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<ConflictResponse> showConflictDialog(PortScanResult result) {
        if (!Platform.isFxApplicationThread()) {
            return Optional.empty();
        }

        Alert dialog = new Alert(Alert.AlertType.WARNING);
        dialog.setTitle("Port Conflict Detected");
        dialog.setHeaderText("Port " + result.port + " is already in use.");
        dialog.setContentText("Occupied by: " + result.occupyingProcessName + " (PID: " + result.occupyingPid + ")");

        ButtonType killBtn = new ButtonType("Kill Process", ButtonBar.ButtonData.LEFT);
        ButtonType changeBtn = new ButtonType("Change Port...", ButtonBar.ButtonData.OTHER);
        ButtonType cancelBtn = new ButtonType("Cancel Start", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getButtonTypes().setAll(killBtn, changeBtn, cancelBtn);

        Optional<ButtonType> choice = dialog.showAndWait();

        if (!choice.isPresent() || choice.get() == cancelBtn) {
            return Optional.of(new ConflictResponse(ConflictResolution.CANCEL, result.port));
        }

        if (choice.get() == killBtn) {
            boolean success = killProcess(result.occupyingPid);
            if (success) {
                return Optional.of(new ConflictResponse(ConflictResolution.KILL_PROCESS, result.port));
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to kill process. Try changing the port instead.", ButtonType.OK);
                err.showAndWait();
                return Optional.of(new ConflictResponse(ConflictResolution.CANCEL, result.port));
            }
        }

        if (choice.get() == changeBtn) {
            TextInputDialog portDialog = new TextInputDialog(String.valueOf(result.port));
            portDialog.setTitle("Change Port");
            portDialog.setHeaderText("Enter a new port number:");
            Optional<String> newPortStr = portDialog.showAndWait();
            
            if (newPortStr.isPresent()) {
                try {
                    int np = Integer.parseInt(newPortStr.get());
                    return Optional.of(new ConflictResponse(ConflictResolution.CHANGE_PORT, np));
                } catch (NumberFormatException e) {}
            }
            return Optional.of(new ConflictResponse(ConflictResolution.CANCEL, result.port));
        }

        return Optional.empty();
    }
}
