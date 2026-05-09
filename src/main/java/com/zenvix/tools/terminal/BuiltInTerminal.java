package com.zenvix.tools.terminal;

import com.zenvix.tools.EnvironmentEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * BuiltInTerminal orchestrates multiple TerminalTab instances seamlessly (up to 8 arrays).
 * Handles global operations swapping threads cleanly bridging multiple independent terminal buffers natively.
 */
public class BuiltInTerminal {
    private final List<TerminalTab> tabs = new ArrayList<>();
    private int activeTabIndex = -1;
    private final EnvironmentEditor envEditor;
    private final String projectId;
    private final int MAX_TABS = 8;

    public BuiltInTerminal(EnvironmentEditor envEditor, String projectId) {
        this.envEditor = envEditor;
        this.projectId = projectId;
    }

    public TerminalTab openNewTab(Consumer<String> onOutput) throws Exception {
        if (tabs.size() >= MAX_TABS) {
            throw new IllegalStateException("Maximum of " + MAX_TABS + " tabs reached.");
        }
        TerminalTab tab = new TerminalTab(UUID.randomUUID().toString());
        tab.startShell(envEditor, projectId, onOutput);
        tabs.add(tab);
        activeTabIndex = tabs.size() - 1;
        return tab;
    }

    public void closeTab(String tabId) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).getTabId().equals(tabId)) {
                tabs.get(i).close();
                tabs.remove(i);
                
                // Adjust index natively
                if (activeTabIndex >= tabs.size()) {
                    activeTabIndex = tabs.size() - 1;
                }
                break;
            }
        }
    }

    public TerminalTab getActiveTab() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }

    public TerminalTab nextTab() {
        if (tabs.isEmpty()) return null;
        activeTabIndex = (activeTabIndex + 1) % tabs.size();
        return tabs.get(activeTabIndex);
    }

    public TerminalTab previousTab() {
        if (tabs.isEmpty()) return null;
        activeTabIndex = (activeTabIndex - 1 + tabs.size()) % tabs.size();
        return tabs.get(activeTabIndex);
    }

    public void closeAll() {
        for (TerminalTab tab : tabs) {
            tab.close();
        }
        tabs.clear();
        activeTabIndex = -1;
    }

    public List<TerminalTab> getTabs() {
        return new ArrayList<>(tabs);
    }
}
