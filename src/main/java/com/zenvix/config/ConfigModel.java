package com.zenvix.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates global POJO mapping models cleanly tracking dynamic physical JSON nodes automatically!
 */
public class ConfigModel {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppConfig {
        public int schemaVersion = 1;
        public String theme = "light";
        public String language = "en_US";
        public int fontSize = 14;
        public String updateMode = "prompted";
        public boolean autostart = false;
        public boolean trayEnabled = true;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceConfig {
        public String serviceId;
        public int port;
        public String version;
        public boolean autostart;
        public List<String> customFlags;
        public String vaultKeyRef;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectConfig {
        public String id;
        public String name;
        public String path;
        public String buildTool;
        public String mainJar;
        public List<String> jvmArgs;
        public String profile;
        public Map<String, String> envVars;
        public List<String> associatedServices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchedulerConfig {
        public List<com.zenvix.scheduler.ScheduledJob> jobs;
    }
}
