package com.zenvix.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FirewallManagerTest {

    private WindowsFirewallManager winManager;
    private LinuxFirewallManager linManager;
    private MacOSFirewallManager macManager;

    @BeforeEach
    public void setUp() {
        winManager = spy(new WindowsFirewallManager());
        macManager = spy(new MacOSFirewallManager());
    }

    @Test
    public void testAddRule_addsFirewallRule() {
        doReturn(true).when(winManager).executeCommand((String[]) any());
        boolean success = winManager.addRule(8080, "TCP", "Tomcat");
        assertTrue(success);
        verify(winManager).executeCommand((String[]) any());
    }

    @Test
    public void testRemoveRule_removesFirewallRule() {
        // Construct securely bypassing physical UFW execution calls
        linManager = new LinuxFirewallManager() {
            @Override
            protected boolean checkCommand(String... cmd) {
                return false; 
            }
            @Override
            protected boolean executeCommand(String... cmd) {
                return true; 
            }
        };
        
        boolean success = linManager.removeRule(3306, "TCP", "MySQL");
        assertTrue(success);
    }

    @Test
    public void testListRules_listsZenviXRules() {
        List<String> rules = macManager.listRules();
        assertNotNull(rules);
    }

    @Test
    public void testWindowsCommand_buildsCorrectNetshCommand() {
        doReturn(true).when(winManager).executeCommand(
            "netsh", "advfirewall", "firewall", "add", "rule", 
            "name=zenviX-Test", "dir=in", "action=allow", "protocol=TCP", "localport=9999"
        );
        
        boolean success = winManager.addRule(9999, "TCP", "Test");
        assertTrue(success);
    }

    @Test
    public void testLinuxUFW_buildsCorrectUFWCommand() {
        linManager = new LinuxFirewallManager() {
            @Override
            protected boolean checkCommand(String... cmd) {
                return true; // Simulate UFW installed
            }
            @Override
            protected boolean executeCommand(String... cmd) {
                assertEquals("sudo", cmd[0]);
                assertEquals("ufw", cmd[1]);
                return true;
            }
        };
        boolean success = linManager.addRule(8080, "TCP", "Web");
        assertTrue(success);
    }
}
