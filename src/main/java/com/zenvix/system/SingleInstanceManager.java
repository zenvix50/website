package com.zenvix.system;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ensures strict global Singleton bounds utilizing raw OS FileChannel locks
 * securely intercepting duplicate executions routing background IPC alerts cleanly!
 */
public class SingleInstanceManager {

    private final Path lockFile;
    private final int ipcPort;
    private FileChannel channel;
    private FileLock lock;
    private ServerSocket ipcSocket;

    public SingleInstanceManager(Path configDir, int port) {
        this.lockFile = configDir.resolve("zenvix.lock");
        this.ipcPort = port;
    }

    public SingleInstanceManager() {
        this(Paths.get("zenvix"), 49152);
    }

    public boolean checkAndLock() {
        try {
            Files.createDirectories(lockFile.getParent());
            File file = lockFile.toFile();
            
            channel = new RandomAccessFile(file, "rw").getChannel();
            lock = channel.tryLock();
            
            if (lock == null) {
                channel.close();
                notifyRunningInstance();
                showAlreadyRunningDialog();
                return false;
            }
            
            setupIpcListener();
            Runtime.getRuntime().addShutdownHook(new Thread(this::release));
            return true;
            
        } catch (Exception e) {
            return true; 
        }
    }

    private void notifyRunningInstance() {
        try (Socket socket = new Socket("localhost", ipcPort)) {
            socket.getOutputStream().write("BRING_TO_FRONT\n".getBytes());
        } catch (Exception e) {}
    }

    private void setupIpcListener() {
        new Thread(() -> {
            try {
                ipcSocket = new ServerSocket(ipcPort);
                while (!ipcSocket.isClosed()) {
                    try (Socket client = ipcSocket.accept()) {
                        bringToFront();
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    protected void bringToFront() {
        if (Platform.isFxApplicationThread()) {
            System.out.println("Bringing primary instance to front.");
        } else {
            try {
                Platform.runLater(this::bringToFront);
            } catch (IllegalStateException e) {}
        }
    }

    protected void showAlreadyRunningDialog() {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("zenviX");
            alert.setHeaderText("zenviX is already running");
            alert.setContentText("Another instance of zenviX is already open.");
            alert.showAndWait();
        } else {
            System.out.println("zenviX is already running!");
        }
    }

    public void release() {
        try {
            if (lock != null) lock.release();
            if (channel != null) channel.close();
            if (ipcSocket != null) ipcSocket.close();
            Files.deleteIfExists(lockFile);
        } catch (Exception e) {}
    }
}
