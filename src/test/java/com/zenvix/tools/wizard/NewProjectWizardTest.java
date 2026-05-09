package com.zenvix.tools.wizard;

import com.zenvix.core.services.MavenManager;
import com.zenvix.tools.projects.ProjectManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewProjectWizardTest {

    @TempDir
    Path tempDir;

    @Mock private MavenManager mockMavenManager;
    @Mock private ProjectManager mockProjectManager;

    private NewProjectWizard wizard;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        wizard = spy(new NewProjectWizard(mockMavenManager, mockProjectManager));
    }

    @Test
    public void testInitializrIntegration_downloadsAndExtractsZip() throws Exception {
        NewProjectWizard.ProjectSpecs specs = new NewProjectWizard.ProjectSpecs();
        specs.type = NewProjectWizard.TemplateType.SPRING_BOOT;
        specs.artifactId = "spring-demo";
        specs.destination = tempDir;
        
        doAnswer(inv -> {
            Path zipPath = inv.getArgument(2);
            try (java.util.zip.ZipOutputStream zout = new java.util.zip.ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                zout.putNextEntry(new java.util.zip.ZipEntry("pom.xml"));
                zout.write("<project></project>".getBytes());
                zout.closeEntry();
            }
            return null;
        }).when(wizard).downloadSpringZip(any(), any(), any());

        when(mockProjectManager.importProject(anyString())).thenReturn(null);

        wizard.generateProject(specs, msg -> {});

        assertTrue(Files.exists(tempDir.resolve("spring-demo/pom.xml")));
        verify(mockProjectManager).importProject(tempDir.resolve("spring-demo").toAbsolutePath().toString());
    }

    @Test
    public void testMavenArchetype_generatesProject() throws Exception {
        NewProjectWizard.ProjectSpecs specs = new NewProjectWizard.ProjectSpecs();
        specs.type = NewProjectWizard.TemplateType.MAVEN_ARCHETYPE;
        specs.artifactId = "maven-demo";
        specs.destination = tempDir;
        
        doNothing().when(mockMavenManager).runBuild(anyString(), anyString(), any(), any());
        when(mockProjectManager.importProject(anyString())).thenReturn(null);

        wizard.generateProject(specs, msg -> {});

        verify(mockMavenManager).runBuild(
            eq(tempDir.toString()), 
            contains("archetype:generate"), 
            any(), 
            any()
        );
        verify(mockProjectManager).importProject(tempDir.resolve("maven-demo").toAbsolutePath().toString());
    }

    @Test
    public void testGradleTemplate_generatesProjectStructure() throws Exception {
        NewProjectWizard.ProjectSpecs specs = new NewProjectWizard.ProjectSpecs();
        specs.type = NewProjectWizard.TemplateType.GRADLE_TEMPLATE;
        specs.artifactId = "gradle-demo";
        specs.destination = tempDir;
        
        when(mockProjectManager.importProject(anyString())).thenReturn(null);

        wizard.generateProject(specs, msg -> {});

        assertTrue(Files.exists(tempDir.resolve("gradle-demo/build.gradle")));
        assertTrue(Files.exists(tempDir.resolve("gradle-demo/settings.gradle")));
        assertTrue(Files.exists(tempDir.resolve("gradle-demo/src/main/java/com/example/Main.java")));
    }

    @Test
    public void testJakartaEE_generatesCorrectPom() throws Exception {
        NewProjectWizard.ProjectSpecs specs = new NewProjectWizard.ProjectSpecs();
        specs.type = NewProjectWizard.TemplateType.JAKARTA_EE;
        specs.artifactId = "jakarta-demo";
        specs.destination = tempDir;
        
        when(mockProjectManager.importProject(anyString())).thenReturn(null);

        wizard.generateProject(specs, msg -> {});

        Path pom = tempDir.resolve("jakarta-demo/pom.xml");
        assertTrue(Files.exists(pom));
        String content = new String(Files.readAllBytes(pom));
        assertTrue(content.contains("jakarta.jakartaee-api"));
        
        assertTrue(Files.exists(tempDir.resolve("jakarta-demo/src/main/webapp/WEB-INF/web.xml")));
    }
}
