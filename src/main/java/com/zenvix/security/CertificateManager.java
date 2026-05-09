package com.zenvix.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages physical X509 PKCS12 Certificate bounds generating 4096-bit RSA endpoints securely 
 * editing Apache Tomcat and Nginx XML pipelines effectively enabling valid HTTP protocols.
 */
public class CertificateManager {
    
    private final Path keystorePath = Paths.get("zenvix", "config", "zenvix-keystore.p12");
    private final String storePass = "zenvixSec123!"; 
    
    public boolean generateCertificate(int validityDays) {
        try {
            if (!Files.exists(keystorePath.getParent())) {
                Files.createDirectories(keystorePath.getParent());
            }
            if (Files.exists(keystorePath)) {
                Files.delete(keystorePath); 
            }
            
            String[] cmd = {
                "keytool", "-genkeypair",
                "-alias", "zenvix",
                "-keyalg", "RSA",
                "-keysize", "4096",
                "-keystore", keystorePath.toAbsolutePath().toString(),
                "-storepass", storePass,
                "-validity", String.valueOf(validityDays),
                "-dname", "CN=localhost, O=zenviX, C=US",
                "-storetype", "PKCS12"
            };
            
            return executeCommand(cmd);
        } catch (Exception e) {
            return false;
        }
    }
    
    public long getDaysUntilExpiry() {
        if (!Files.exists(keystorePath)) return -1;
        try (java.io.InputStream is = Files.newInputStream(keystorePath)) {
            java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
            ks.load(is, storePass.toCharArray());
            java.security.cert.Certificate cert = ks.getCertificate("zenvix");
            if (cert instanceof java.security.cert.X509Certificate) {
                java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cert;
                long diff = x509.getNotAfter().getTime() - System.currentTimeMillis();
                return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);
            }
        } catch (Exception e) {}
        return -1;
    }
    
    public boolean configureTomcatHTTPS(Path serverXmlPath) {
        if (!Files.exists(serverXmlPath)) return false;
        try {
            String xml = new String(Files.readAllBytes(serverXmlPath));
            if (!xml.contains("port=\"8443\"")) {
                String connector = String.format(
                    "\n    <Connector port=\"8443\" protocol=\"org.apache.coyote.http11.Http11NioProtocol\"\n" +
                    "               maxThreads=\"150\" SSLEnabled=\"true\">\n" +
                    "        <SSLHostConfig>\n" +
                    "            <Certificate certificateKeystoreFile=\"%s\"\n" +
                    "                         certificateKeystorePassword=\"%s\"\n" +
                    "                         type=\"RSA\" />\n" +
                    "        </SSLHostConfig>\n" +
                    "    </Connector>\n", 
                    keystorePath.toAbsolutePath().toString().replace("\\", "/"), storePass);
                
                xml = xml.replace("</Service>", connector + "</Service>");
                Files.write(serverXmlPath, xml.getBytes());
                return true;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public boolean configureNginxHTTPS(Path nginxConfPath, Path crtPath, Path keyPath) {
        if (!Files.exists(nginxConfPath)) return false;
        try {
            String conf = new String(Files.readAllBytes(nginxConfPath));
            if (!conf.contains("ssl_certificate")) {
                String sslDirectives = String.format(
                    "\n    listen 443 ssl;\n" +
                    "    ssl_certificate \"%s\";\n" +
                    "    ssl_certificate_key \"%s\";\n",
                    crtPath.toAbsolutePath().toString().replace("\\", "/"), 
                    keyPath.toAbsolutePath().toString().replace("\\", "/"));
                
                conf = conf.replace("listen       80;", "listen       80;" + sslDirectives);
                Files.write(nginxConfPath, conf.getBytes());
                return true;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public boolean extractPemForNginx(Path crtPath, Path keyPath) {
        if (!Files.exists(keystorePath)) return false;
        try (java.io.InputStream is = Files.newInputStream(keystorePath)) {
            java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
            ks.load(is, storePass.toCharArray());
            
            java.security.cert.Certificate cert = ks.getCertificate("zenvix");
            String certPem = "-----BEGIN CERTIFICATE-----\n" +
                             java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded()) +
                             "\n-----END CERTIFICATE-----\n";
            Files.write(crtPath, certPem.getBytes());
            
            java.security.Key key = ks.getKey("zenvix", storePass.toCharArray());
            String keyPem = "-----BEGIN PRIVATE KEY-----\n" +
                            java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key.getEncoded()) +
                            "\n-----END PRIVATE KEY-----\n";
            Files.write(keyPath, keyPem.getBytes());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    protected boolean executeCommand(String... command) throws Exception {
        Process p = new ProcessBuilder(command).start();
        return p.waitFor() == 0;
    }
}
