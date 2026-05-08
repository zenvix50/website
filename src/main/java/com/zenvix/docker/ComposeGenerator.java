package com.zenvix.docker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds standard Compose configurations dynamically mapping specific zenviX 
 * service payloads flawlessly formatting safe YAML strings.
 */
public class ComposeGenerator {

    public enum ServiceType { MYSQL, POSTGRESQL, REDIS, NGINX, TOMCAT }

    public static class ServiceConfig {
        public final ServiceType type;
        public final int port;
        public final String password; 

        public ServiceConfig(ServiceType type, int port, String password) {
            this.type = type;
            this.port = port;
            this.password = password;
        }
    }

    public boolean generateComposeFile(List<ServiceConfig> services, Path outputPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("version: '3.8'\n\nservices:\n");

        for (ServiceConfig service : services) {
            switch (service.type) {
                case MYSQL:
                    sb.append("  mysql:\n")
                      .append("    image: mysql:8.0\n")
                      .append("    container_name: zenvix-mysql\n")
                      .append("    environment:\n")
                      .append("      MYSQL_ROOT_PASSWORD: ").append(service.password).append("\n")
                      .append("    ports:\n")
                      .append("      - \"").append(service.port).append(":3306\"\n")
                      .append("    restart: unless-stopped\n\n");
                    break;
                case POSTGRESQL:
                    sb.append("  postgresql:\n")
                      .append("    image: postgres:16\n")
                      .append("    container_name: zenvix-postgresql\n")
                      .append("    environment:\n")
                      .append("      POSTGRES_PASSWORD: ").append(service.password).append("\n")
                      .append("    ports:\n")
                      .append("      - \"").append(service.port).append(":5432\"\n")
                      .append("    restart: unless-stopped\n\n");
                    break;
                case REDIS:
                    sb.append("  redis:\n")
                      .append("    image: redis:7\n")
                      .append("    container_name: zenvix-redis\n")
                      .append("    ports:\n")
                      .append("      - \"").append(service.port).append(":6379\"\n")
                      .append("    restart: unless-stopped\n\n");
                    break;
                case NGINX:
                    sb.append("  nginx:\n")
                      .append("    image: nginx:latest\n")
                      .append("    container_name: zenvix-nginx\n")
                      .append("    ports:\n")
                      .append("      - \"").append(service.port).append(":80\"\n")
                      .append("      - \"443:443\"\n")
                      .append("    restart: unless-stopped\n\n");
                    break;
                case TOMCAT:
                    sb.append("  tomcat:\n")
                      .append("    image: tomcat:11\n")
                      .append("    container_name: zenvix-tomcat\n")
                      .append("    ports:\n")
                      .append("      - \"").append(service.port).append(":8080\"\n")
                      .append("    restart: unless-stopped\n\n");
                    break;
            }
        }

        try {
            if (outputPath.getParent() != null && !Files.exists(outputPath.getParent())) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.write(outputPath, sb.toString().getBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
