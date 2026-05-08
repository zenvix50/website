package com.zenvix.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single dependency artifact natively modeled for tree UI bounds.
 */
public class DependencyNode {
    public String groupId;
    public String artifactId;
    public String version;
    public String scope;
    public String license = "Unknown";
    public boolean hasConflict = false;
    public List<DependencyNode> children = new ArrayList<>();

    public DependencyNode(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public String getDisplayName() {
        return groupId + ":" + artifactId + ":" + version + (scope != null && !scope.isEmpty() ? " [" + scope + "]" : "");
    }
}
