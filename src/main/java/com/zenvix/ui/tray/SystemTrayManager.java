package com.zenvix.ui.tray;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Supplier;

/**
 * SystemTrayManager safely bridges the underlying OS AWT SystemTray hooks directly into 
 * the JavaFX runtime avoiding cross-thread collisions natively intercepting closure hooks.
 */
public class SystemTrayManager {

    private Stage mainStage;
    private TrayIcon trayIcon;
    private boolean firstTimeMinimized = true;
    protected boolean testingMode = false;
    
    private Runnable onStartAll;
    private Runnable onStopAll;
    private Runnable onExit;
    private Supplier<List<TrayService>> runningServicesSupplier;

    public static class TrayService {
        public String name;
        public Runnable stopAction;
        public TrayService(String name, Runnable stopAction) {
            this.name = name;
            this.stopAction = stopAction;
        }
    }

    public void initialize(Stage stage, Runnable onStartAll, Runnable onStopAll, Runnable onExit, Supplier<List<TrayService>> runningServicesSupplier) {
        this.mainStage = stage;
        this.onStartAll = onStartAll;
        this.onStopAll = onStopAll;
        this.onExit = onExit;
        this.runningServicesSupplier = runningServicesSupplier;

        if (!isSystemTraySupported() && !testingMode) {
            System.err.println("SystemTray is not supported on this platform.");
            return;
        }

        if (!testingMode) Platform.setImplicitExit(false);

        if (stage != null) {
            stage.setOnCloseRequest(this::handleCloseRequest);
        }

        setupTrayIcon();
    }

    private void handleCloseRequest(WindowEvent event) {
        if (isSystemTraySupported() || testingMode) {
            if (event != null) event.consume();
            minimizeToTray();
        }
    }

    private void setupTrayIcon() {
        if (testingMode) {
            trayIcon = new TrayIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
            updateContextMenu();
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        
        Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = ((BufferedImage) image).createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 16, 16);
        g2d.dispose();

        trayIcon = new TrayIcon(image, "zenviX");
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    restoreFromTray();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    updateContextMenu();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    updateContextMenu();
                }
            }
        });

        updateContextMenu();

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Failed to add tray icon.");
        }
    }

    protected void updateContextMenu() {
        if (trayIcon == null) return;
        
        PopupMenu popup = new PopupMenu();

        if (runningServicesSupplier != null) {
            List<TrayService> services = runningServicesSupplier.get();
            if (services != null && !services.isEmpty()) {
                Menu servicesMenu = new Menu("Running Services");
                for (TrayService ts : services) {
                    MenuItem stopItem = new MenuItem("Stop " + ts.name);
                    stopItem.addActionListener(e -> {
                        if (ts.stopAction != null) {
                            new Thread(ts.stopAction).start();
                        }
                    });
                    servicesMenu.add(stopItem);
                }
                popup.add(servicesMenu);
                popup.addSeparator();
            }
        }

        MenuItem startAllItem = new MenuItem("Start All");
        startAllItem.addActionListener(e -> {
            if (onStartAll != null) new Thread(onStartAll).start();
        });
        popup.add(startAllItem);

        MenuItem stopAllItem = new MenuItem("Stop All");
        stopAllItem.addActionListener(e -> {
            if (onStopAll != null) new Thread(onStopAll).start();
        });
        popup.add(stopAllItem);

        popup.addSeparator();

        MenuItem openItem = new MenuItem("Open zenviX");
        openItem.addActionListener(e -> restoreFromTray());
        popup.add(openItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> performExit());
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);
    }

    public void minimizeToTray() {
        if (testingMode) {
            if (mainStage != null) mainStage.hide();
            return;
        }
        Platform.runLater(() -> {
            if (mainStage != null) {
                mainStage.hide();
            }
            if (firstTimeMinimized && trayIcon != null) {
                trayIcon.displayMessage("zenviX", "zenviX is running in the background", TrayIcon.MessageType.INFO);
                firstTimeMinimized = false;
            }
        });
    }

    public void restoreFromTray() {
        if (testingMode) {
            if (mainStage != null) {
                mainStage.show();
                mainStage.toFront();
            }
            return;
        }
        Platform.runLater(() -> {
            if (mainStage != null) {
                mainStage.show();
                mainStage.toFront();
            }
        });
    }

    public void performExit() {
        if (onExit != null) {
            onExit.run();
        }
        
        if (!testingMode && trayIcon != null && isSystemTraySupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            Platform.exit();
        }
    }
    
    protected boolean isSystemTraySupported() {
        return !GraphicsEnvironment.isHeadless() && SystemTray.isSupported();
    }

    protected TrayIcon getTrayIcon() {
        return trayIcon;
    }
}
