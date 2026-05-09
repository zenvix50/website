package com.zenvix.tools.terminal;

import com.zenvix.tools.EnvironmentEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BuiltInTerminalTest {

    @Mock
    private EnvironmentEditor mockEnvEditor;

    private BuiltInTerminal terminal;

    @BeforeEach
    public void setUp() {
        terminal = new BuiltInTerminal(mockEnvEditor, "proj1");
    }

    @AfterEach
    public void tearDown() {
        terminal.closeAll();
    }

    @Test
    public void testShellLaunch_launchesOSNativeShell() throws Exception {
        when(mockEnvEditor.getEffectiveEnv(anyString())).thenReturn(new HashMap<>());
        
        TerminalTab tab = terminal.openNewTab(msg -> {});
        assertNotNull(tab.getProcess());
        assertTrue(tab.getProcess().isAlive());
        
        assertNotNull(tab.getTabId());
    }

    @Test
    public void testEnvironment_injectsZenviXEnv() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("JAVA_HOME", "/custom/java/path");
        envVars.put("MAVEN_HOME", "/custom/maven");
        when(mockEnvEditor.getEffectiveEnv(anyString())).thenReturn(envVars);

        TerminalTab tab = terminal.openNewTab(msg -> {});
        
        verify(mockEnvEditor, atLeastOnce()).getEffectiveEnv("proj1");
        assertTrue(tab.getProcess().isAlive());
    }

    @Test
    public void testMultiTab_opensAndClosesIndependently() throws Exception {
        when(mockEnvEditor.getEffectiveEnv(anyString())).thenReturn(new HashMap<>());

        TerminalTab tab1 = terminal.openNewTab(msg -> {});
        TerminalTab tab2 = terminal.openNewTab(msg -> {});
        
        assertEquals(2, terminal.getTabs().size());
        assertEquals(tab2, terminal.getActiveTab());
        
        terminal.previousTab();
        assertEquals(tab1, terminal.getActiveTab());
        
        terminal.nextTab();
        assertEquals(tab2, terminal.getActiveTab());

        terminal.closeTab(tab1.getTabId());
        assertEquals(1, terminal.getTabs().size());
        assertEquals(tab2, terminal.getActiveTab());
    }

    @Test
    public void testFontSize_adjustsFontSizeCorrectly() {
        TerminalTab tab = new TerminalTab("test");
        assertEquals(12, tab.getFontSize());
        
        tab.increaseFontSize();
        assertEquals(14, tab.getFontSize());
        
        tab.decreaseFontSize();
        assertEquals(12, tab.getFontSize());
        
        tab.resetFontSize();
        assertEquals(12, tab.getFontSize());
        
        tab.decreaseFontSize();
        tab.decreaseFontSize();
        tab.decreaseFontSize();
        tab.decreaseFontSize();
        assertEquals(6, tab.getFontSize()); // Safely truncates without passing 0 implicitly 
    }
}
