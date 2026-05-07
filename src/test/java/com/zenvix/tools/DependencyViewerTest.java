package com.zenvix.tools;

import com.zenvix.core.services.GradleManager;
import com.zenvix.core.services.MavenManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DependencyViewerTest {

    @Mock private MavenManager mockMaven;
    @Mock private GradleManager mockGradle;

    private DependencyViewer viewer;

    @BeforeEach
    public void setUp() {
        viewer = new DependencyViewer(mockMaven, mockGradle);
    }

    @Test
    public void testMavenTree_parsesDependencyTree() {
        String output = 
            "[INFO] com.example:demo:jar:1.0.0\n" +
            "[INFO] +- org.springframework.boot:spring-boot-starter:jar:3.2.3:compile\n" +
            "[INFO] |  +- org.springframework.boot:spring-boot:jar:3.2.3:compile\n" +
            "[INFO] |  \\- org.springframework:spring-core:jar:6.1.4:compile\n" +
            "[INFO] \\- org.slf4j:slf4j-api:jar:2.0.9:compile";

        DependencyNode root = viewer.parseMavenTree(output);

        assertEquals(2, root.children.size());
        assertEquals("org.springframework.boot", root.children.get(0).groupId);
        assertEquals("spring-boot-starter", root.children.get(0).artifactId);
        assertEquals("3.2.3", root.children.get(0).version);
        
        assertEquals(2, root.children.get(0).children.size());
        assertEquals("spring-boot", root.children.get(0).children.get(0).artifactId);
        
        assertEquals("slf4j-api", root.children.get(1).artifactId);
    }

    @Test
    public void testGradleTree_parsesDependencyTree() {
        String output = 
            "+--- org.springframework.boot:spring-boot-starter:3.2.3\n" +
            "|    +--- org.springframework.boot:spring-boot:3.2.3\n" +
            "|    \\--- org.springframework:spring-core:6.1.4 -> 6.1.5\n" +
            "\\--- org.slf4j:slf4j-api:2.0.9 (*)";

        DependencyNode root = viewer.parseGradleTree(output);

        assertEquals(2, root.children.size());
        assertEquals("spring-boot-starter", root.children.get(0).artifactId);
        
        assertEquals(2, root.children.get(0).children.size());
        assertEquals("6.1.5", root.children.get(0).children.get(1).version); 
        
        assertEquals("slf4j-api", root.children.get(1).artifactId);
        assertEquals("2.0.9", root.children.get(1).version); 
    }

    @Test
    public void testConflictDetection_highlightsConflicts() {
        String output = 
            "[INFO] +- org.lib:test-lib:jar:1.0:compile\n" +
            "[INFO] \\- org.other:other-lib:jar:2.0:compile\n" +
            "[INFO]    \\- org.lib:test-lib:jar:1.1:compile";

        DependencyNode root = viewer.parseMavenTree(output);

        DependencyNode child1 = root.children.get(0);
        DependencyNode nestedChild = root.children.get(1).children.get(0);

        assertTrue(child1.hasConflict);
        assertTrue(nestedChild.hasConflict);
        assertFalse(root.children.get(1).hasConflict);
    }

    @Test
    public void testFilter_filtersByArtifactId() {
        String output = 
            "[INFO] +- org.springframework.boot:spring-boot-starter:jar:3.2.3:compile\n" +
            "[INFO] |  \\- org.springframework:spring-core:jar:6.1.4:compile\n" +
            "[INFO] \\- org.slf4j:slf4j-api:jar:2.0.9:compile";

        DependencyNode root = viewer.parseMavenTree(output);
        
        DependencyNode filtered = viewer.filterTree(root, "spring");
        
        assertNotNull(filtered);
        assertEquals(1, filtered.children.size()); 
        assertEquals("spring-boot-starter", filtered.children.get(0).artifactId);
        assertEquals(1, filtered.children.get(0).children.size());
    }

    @Test
    public void testLicenseLookup_returnsLicenseForArtifact() throws Exception {
        DependencyViewer spiedViewer = org.mockito.Mockito.spy(viewer);
        org.mockito.Mockito.doReturn("Apache-2.0").when(spiedViewer).fetchLicense("org.slf4j", "slf4j-api");
        
        String license = spiedViewer.fetchLicense("org.slf4j", "slf4j-api");
        assertEquals("Apache-2.0", license);
    }
}
