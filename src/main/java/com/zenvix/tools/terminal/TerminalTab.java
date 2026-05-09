package com.zenvix.tools.terminal;

import com.zenvix.tools.EnvironmentEditor;

import java.io.*;
import java.util.Map;
import java.util.function.Consumer;

/**
 * TerminalTab manages a single shell process and handles bidirectional I/O natively mapping 
 * standard output streams across background daemon threads smoothly rendering OS terminals.
 */
public class TerminalTab {
    private final String tabId;
    private Process process;
    private BufferedWriter writer;
    private Thread outputThread;
    private Thread errorThread;
    private Consumer<String> onOutput;
    private Consumer<String> onTitleChange;
    private int fontSize = 12;

    public TerminalTab(String tabId) {
        this.tabId = tabId;
    }

    public void startShell(EnvironmentEditor envEditor, String projectId, Consumer<String> onOutput) throws Exception {
        this.onOutput = onOutput;
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb = new ProcessBuilder();
        
        if (os.contains("win")) {
            pb.command("cmd.exe", "/K");
        } else if (os.contains("mac")) {
            pb.command("/bin/zsh");
        } else {
            pb.command("/bin/bash");
        }

        Map<String, String> env = pb.environment();
        if (envEditor != null) {
            Map<String, String> zenvixEnv = envEditor.getEffectiveEnv(projectId);
            env.putAll(zenvixEnv);
            
            // Ensures correct PATH hierarchy where custom parameters override standard OS routes cleanly
            if (zenvixEnv.containsKey("PATH")) {
                env.put("PATH", zenvixEnv.get("PATH") + File.pathSeparator + env.getOrDefault("PATH", ""));
            }
        }

        process = pb.start();
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        outputThread = new Thread(() -> streamOutput(process.getInputStream()));
        errorThread = new Thread(() -> streamOutput(process.getErrorStream()));
        
        outputThread.setDaemon(true);
        errorThread.setDaemon(true);
        outputThread.start();
        errorThread.start();
    }

    private void streamOutput(InputStream is) {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                if (onOutput != null) {
                    onOutput.accept(new String(buffer, 0, length));
                }
            }
        } catch (IOException e) { /* expected when process closes */ }
    }

    public void sendInput(String input) throws IOException {
        if (writer != null) {
            writer.write(input);
            writer.flush();
        }
    }

    public void sendCtrlC() throws IOException {
        // ETX character intercepts SIGINT gracefully for standard terminal inputs
        sendInput("\u0003"); 
    }

    public void setOnTitleChange(Consumer<String> onTitleChange) {
        this.onTitleChange = onTitleChange;
    }

    public void close() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        if (outputThread != null) outputThread.interrupt();
        if (errorThread != null) errorThread.interrupt();
    }

    public String getTabId() {
        return tabId;
    }

    public void increaseFontSize() {
        fontSize += 2;
    }

    public void decreaseFontSize() {
        if (fontSize > 6) fontSize -= 2;
    }

    public void resetFontSize() {
        fontSize = 12;
    }

    public int getFontSize() {
        return fontSize;
    }

    public Process getProcess() {
        return process;
    }
}
