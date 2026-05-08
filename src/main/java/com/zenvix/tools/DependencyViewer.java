package com.zenvix.tools;

import com.zenvix.core.services.GradleManager;
import com.zenvix.core.services.MavenManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DependencyViewer evaluates deep workspace dependencies intercepting complex CLI outputs natively
 * generating structural Java trees identifying overlaps effortlessly.
 */
public class DependencyViewer {

    private final MavenManager mavenManager;
    private final GradleManager gradleManager;
    private final Map<String, Set<String>> artifactVersions = new HashMap<>();

    public DependencyViewer(MavenManager mavenManager, GradleManager gradleManager) {
        this.mavenManager = mavenManager;
        this.gradleManager = gradleManager;
    }

    // --- Maven Pipeline ---

    public DependencyNode parseMavenTree(String output) {
        artifactVersions.clear();
        DependencyNode root = new DependencyNode("root", "project", "1.0", "");
        List<NodeLevel> stack = new ArrayList<>();
        stack.add(new NodeLevel(root, -1));

        String[] lines = output.split("\\r?\\n");

        for (String line : lines) {
            line = line.replace("[INFO] ", "");
            int dashIndex = line.indexOf("- ");
            if (dashIndex < 0 || (!line.contains("+-") && !line.contains("\\-"))) continue;

            int level = dashIndex / 3;

            String text = line.substring(dashIndex + 2).trim();
            String[] parts = text.split(":");
            
            if (parts.length >= 4) {
                String groupId = parts[0];
                String artifactId = parts[1];
                String scope = parts[parts.length - 1];
                
                String version;
                if (parts.length == 4) {
                    version = parts[2];
                } else if (parts.length == 5) {
                    version = parts[3];
                } else if (parts.length >= 6) {
                    version = parts[4];
                } else {
                    version = "unknown";
                }

                DependencyNode node = new DependencyNode(groupId, artifactId, version, scope);
                trackVersion(groupId, artifactId, version);

                while (!stack.isEmpty() && stack.get(stack.size() - 1).level >= level) {
                    stack.remove(stack.size() - 1);
                }

                if (!stack.isEmpty()) {
                    stack.get(stack.size() - 1).node.children.add(node);
                }
                stack.add(new NodeLevel(node, level));
            }
        }
        detectConflicts(root);
        return root;
    }

    // --- Gradle Pipeline ---

    public DependencyNode parseGradleTree(String output) {
        artifactVersions.clear();
        DependencyNode root = new DependencyNode("root", "project", "1.0", "");
        List<NodeLevel> stack = new ArrayList<>();
        stack.add(new NodeLevel(root, -1));

        String[] lines = output.split("\\r?\\n");

        for (String line : lines) {
            int dashIndex = line.indexOf("--- ");
            if (dashIndex < 0) continue;

            int level = dashIndex / 5;

            String text = line.substring(dashIndex + 4).trim();
            String[] parts = text.split(":");
            
            if (parts.length >= 3) {
                String groupId = parts[0];
                String artifactId = parts[1];
                String versionPart = parts[2];
                
                if (versionPart.contains("->")) {
                    versionPart = versionPart.split("->")[1].trim();
                }
                if (versionPart.contains(" ")) {
                    versionPart = versionPart.split(" ")[0].trim();
                }

                DependencyNode node = new DependencyNode(groupId, artifactId, versionPart, "compile");
                trackVersion(groupId, artifactId, versionPart);

                while (!stack.isEmpty() && stack.get(stack.size() - 1).level >= level) {
                    stack.remove(stack.size() - 1);
                }

                if (!stack.isEmpty()) {
                    stack.get(stack.size() - 1).node.children.add(node);
                }
                stack.add(new NodeLevel(node, level));
            }
        }
        detectConflicts(root);
        return root;
    }

    private static class NodeLevel {
        DependencyNode node;
        int level;
        NodeLevel(DependencyNode node, int level) {
            this.node = node;
            this.level = level;
        }
    }

    // --- Analytics ---

    private void trackVersion(String groupId, String artifactId, String version) {
        String key = groupId + ":" + artifactId;
        artifactVersions.computeIfAbsent(key, k -> new HashSet<>()).add(version);
    }

    private void detectConflicts(DependencyNode node) {
        String key = node.groupId + ":" + node.artifactId;
        if (artifactVersions.containsKey(key) && artifactVersions.get(key).size() > 1) {
            node.hasConflict = true;
        }
        for (DependencyNode child : node.children) {
            detectConflicts(child);
        }
    }

    public DependencyNode filterTree(DependencyNode root, String query) {
        if (query == null || query.trim().isEmpty()) return root;
        
        DependencyNode filtered = new DependencyNode(root.groupId, root.artifactId, root.version, root.scope);
        filtered.hasConflict = root.hasConflict;
        filtered.license = root.license;
        
        for (DependencyNode child : root.children) {
            DependencyNode filteredChild = filterTree(child, query);
            if (filteredChild != null) {
                filtered.children.add(filteredChild);
            }
        }
        
        if (root.groupId.contains(query) || root.artifactId.contains(query) || !filtered.children.isEmpty()) {
            return filtered;
        }
        return null;
    }

    // --- Search APIs ---

    public String fetchLicense(String groupId, String artifactId) throws Exception {
        String urlStr = String.format("https://search.maven.org/solrsearch/select?q=g:\"%s\"+AND+a:\"%s\"&rows=1&wt=json", groupId, artifactId);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        if (conn.getResponseCode() != 200) {
            return "Unknown";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        
        Matcher m = Pattern.compile("\"l\"\\s*:\\s*\\[\"(.*?)\"\\]").matcher(response.toString());
        if (m.find()) {
            return m.group(1);
        }
        
        return "Unknown";
    }
}
