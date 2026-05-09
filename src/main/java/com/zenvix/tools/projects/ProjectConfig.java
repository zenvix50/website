package com.zenvix.tools.projects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a registered Java project configuration natively in zenviX.
 */
public class ProjectConfig {
    public String id;
    public String name;
    public String path;
    public String buildTool; // MAVEN, GRADLE, UNKNOWN
    public String mainJar;
    public String jvmArgs;
    public String profile;
    public Map<String, String> envVars = new HashMap<>();
    public List<String> associatedServices = new ArrayList<>();

    public ProjectConfig() {
        this.id = UUID.randomUUID().toString();
    }
}
