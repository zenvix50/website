package com.zenvix.exceptions;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Automates system-wide UncaughtException tracking flushing explicit StackTraces 
 * mapping natively to `zenvix/logs` safely orchestrating UI alerts asynchronously securely!
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Path logDir = Paths.get("zenvix", "logs");

    public void register() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        Path crashFile = writeCrashReport(t, e);
        showUserDialog(e, crashFile);
    }

    protected Path writeCrashReport(Thread t, Throwable e) {
        try {
            Files.createDirectories(logDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path crashFile = logDir.resolve("crash-" + timestamp + ".log");
            
            StringBuilder report = new StringBuilder();
            report.append("--- zenviX Crash Report ---\n");
            report.append("Date: ").append(LocalDateTime.now()).append("\n");
            report.append("Thread: ").append(t.getName()).append("\n");
            report.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
            report.append("JVM Version: ").append(System.getProperty("java.version")).append("\n");
            report.append("zenviX Version: 1.0.0-BETA\n\n");
            
            if (e instanceof ZenviXException) {
                ZenviXException ze = (ZenviXException) e;
                report.append("Error Code: ").append(ze.getErrorCode()).append("\n");
                report.append("User Message: ").append(ze.getUserFriendlyMessage()).append("\n");
                report.append("Technical: ").append(ze.getTechnicalDetails()).append("\n");
                report.append("Action: ").append(ze.getSuggestedAction()).append("\n\n");
            }
            
            report.append("--- Stack Trace ---\n");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            report.append(sw.toString()).append("\n");
            
            report.append("--- Recent Audit Logs ---\n");
            report.append(getRecentAuditLogs());
            
            Files.writeString(crashFile, report.toString());
            return crashFile;
        } catch (Exception ex) {
            return null;
        }
    }

    protected String getRecentAuditLogs() {
        Path auditLog = logDir.resolve("audit.log");
        if (Files.exists(auditLog)) {
            try {
                return Files.lines(auditLog)
                            .skip(Math.max(0, Files.lines(auditLog).count() - 100))
                            .collect(Collectors.joining("\n"));
            } catch (Exception e) {}
        }
        return "No recent audit logs available.";
    }

    protected void showUserDialog(Throwable e, Path crashFile) {
        Runnable showDialog = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("zenviX Critical Error");
            alert.setHeaderText("zenviX encountered an error.");
            alert.setContentText("Please copy the crash report and report it on GitHub.");

            TextArea textArea = new TextArea(crashFile != null ? crashFile.toAbsolutePath().toString() : "Unknown crash file");
            textArea.setEditable(false);
            textArea.setWrapText(true);

            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);

            alert.getDialogPane().setExpandableContent(expContent);
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            try {
                Platform.runLater(showDialog);
            } catch (IllegalStateException ex) {
            }
        }
    }
}
