package com.zenvix.security;

import com.zenvix.ui.notifications.NotificationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityManagerTest {

    private SecurityManager securityManager;
    private CertificateManager certManager;
    private FirewallManager firewallManager;
    
    @Mock private NotificationManager mockNotifier;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        securityManager = new SecurityManager(mockNotifier);
        
        certManager = spy(new CertificateManager());
        firewallManager = spy(new FirewallManager());
        
        try {
            java.lang.reflect.Field f1 = SecurityManager.class.getDeclaredField("certificateManager");
            f1.setAccessible(true);
            f1.set(securityManager, certManager);
            
            java.lang.reflect.Field f2 = SecurityManager.class.getDeclaredField("firewallManager");
            f2.setAccessible(true);
            f2.set(securityManager, firewallManager);
        } catch (Exception e) {}
    }

    @Test
    public void testCertGeneration_generatesValidCertificate() throws Exception {
        doReturn(true).when(certManager).executeCommand((String[]) any());
        
        boolean success = certManager.generateCertificate(365);
        assertTrue(success);
        
        verify(certManager).executeCommand((String[]) any());
    }

    @Test
    public void testExpiryCheck_detectsExpiringSoon() {
        doReturn(15L).when(certManager).getDaysUntilExpiry();
        
        securityManager.checkExpiryAndAlert();
        
        verify(mockNotifier).notifyWarning(contains("Expiring"), contains("15 days"));
    }

    @Test
    public void testTomcatHTTPS_configuresServer() throws Exception {
        Path serverXml = tempDir.resolve("server.xml");
        Files.write(serverXml, "<Service>\n    <Connector port=\"8080\" />\n</Service>".getBytes());
        
        boolean success = certManager.configureTomcatHTTPS(serverXml);
        assertTrue(success);
        
        String result = new String(Files.readAllBytes(serverXml));
        assertTrue(result.contains("port=\"8443\""));
        assertTrue(result.contains("SSLEnabled=\"true\""));
    }

    @Test
    public void testNginxHTTPS_configuresNginx() throws Exception {
        Path nginxConf = tempDir.resolve("nginx.conf");
        Files.write(nginxConf, "server {\n    listen       80;\n}".getBytes());
        
        Path crt = tempDir.resolve("cert.crt");
        Path key = tempDir.resolve("cert.key");
        
        boolean success = certManager.configureNginxHTTPS(nginxConf, crt, key);
        assertTrue(success);
        
        String result = new String(Files.readAllBytes(nginxConf));
        assertTrue(result.contains("listen       443 ssl;"));
        assertTrue(result.contains("ssl_certificate"));
    }

    @Test
    public void testFirewallAdd_addsPortException() throws Exception {
        doReturn(true).when(firewallManager).executeCommand((String[]) any());
        
        boolean success = firewallManager.addPortException(8080, "Tomcat");
        assertTrue(success);
        
        verify(firewallManager).executeCommand((String[]) any());
    }

    @Test
    public void testFirewallRemove_removesPortException() throws Exception {
        doReturn(true).when(firewallManager).executeCommand((String[]) any());
        
        boolean success = firewallManager.removePortException(8080, "Tomcat");
        assertTrue(success);
        
        verify(firewallManager).executeCommand((String[]) any());
    }
}
