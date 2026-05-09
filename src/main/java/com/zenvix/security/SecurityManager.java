package com.zenvix.security;

import com.zenvix.ui.notifications.NotificationManager;
import java.nio.file.Path;

/**
 * Main facade orchestrating the Security sub-components natively protecting 
 * socket firewalls cleanly integrating TLS bounds effortlessly dynamically.
 */
public class SecurityManager {

    private final CertificateManager certificateManager;
    private final FirewallManager firewallManager;
    private final NotificationManager notificationManager;

    public SecurityManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        this.certificateManager = new CertificateManager();
        
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            this.firewallManager = new WindowsFirewallManager();
        } else if (osName.contains("mac")) {
            this.firewallManager = new MacOSFirewallManager();
        } else {
            this.firewallManager = new LinuxFirewallManager();
        }
    }

    public boolean initializeCertificates() {
        if (certificateManager.getDaysUntilExpiry() <= 0) {
            return certificateManager.generateCertificate(365);
        }
        return true;
    }

    public void checkExpiryAndAlert() {
        long days = certificateManager.getDaysUntilExpiry();
        if (days > 0 && days <= 30) {
            notificationManager.notifyWarning("Certificate Expiring", "The zenviX SSL certificate expires in " + days + " days. Please regenerate it.");
        }
    }

    public boolean secureTomcat(Path serverXmlPath) {
        return certificateManager.configureTomcatHTTPS(serverXmlPath);
    }

    public boolean secureNginx(Path nginxConfPath, Path outCrtPath, Path outKeyPath) {
        boolean pemSuccess = certificateManager.extractPemForNginx(outCrtPath, outKeyPath);
        if (pemSuccess) {
            return certificateManager.configureNginxHTTPS(nginxConfPath, outCrtPath, outKeyPath);
        }
        return false;
    }

    public boolean openPort(int port, String serviceName) {
        return firewallManager.addRule(port, "TCP", serviceName);
    }

    public boolean closePort(int port, String serviceName) {
        return firewallManager.removeRule(port, "TCP", serviceName);
    }

    protected CertificateManager getCertificateManager() { return certificateManager; }
    protected FirewallManager getFirewallManager() { return firewallManager; }
}
