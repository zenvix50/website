package com.zenvix.tools.projects;

import com.zenvix.core.services.GradleManager;
import com.zenvix.core.services.MavenManager;
import com.zenvix.tools.SpringBootRunner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectManagerTest {

    @TempDir
    Path tempDir;

    @Mock private MavenManager mockMaven;
    @Mock private GradleManager mockGradle;
    @Mock private SpringBootRunner mockRunner;

    private ProjectManager manager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new ProjectManager(tempDir.toString(), mockMaven, mockGradle, mockRunner);
    }

    @Test
    public void testImport_detectsMavenProject() throws Exception {
        Path projectDir = tempDir.resolve("my-maven-app");
        Files.createDirectories(projectDir);
        Files.write(projectDir.resolve("pom.xml"), "<project><artifactId>maven-app</artifactId></project>".getBytes());

        ProjectConfig config = manager.importProject(projectDir.toString());
        assertEquals("MAVEN", config.buildTool);
        assertEquals("maven-app", config.name);
        
        assertNotNull(manager.getProject(config.id));
    }

    @Test
    public void testImport_detectsGradleProject() throws Exception {
        Path projectDir = tempDir.resolve("my-gradle-app");
        Files.createDirectories(projectDir);
        Files.write(projectDir.resolve("build.gradle"), "plugins { id 'java' }".getBytes());
        Files.write(projectDir.resolve("settings.gradle"), "rootProject.name = 'gradle-app'".getBytes());

        ProjectConfig config = manager.importProject(projectDir.toString());
        assertEquals("GRADLE", config.buildTool);
        assertEquals("gradle-app", config.name);
    }

    @Test
    public void testRegistry_addsAndRemovesProjects() throws Exception {
        Path projectDir = tempDir.resolve("app");
        Files.createDirectories(projectDir);
        ProjectConfig config = manager.importProject(projectDir.toString());
        
        assertEquals(1, manager.getProjects().size());
        
        manager.removeProject(config.id);
        assertEquals(0, manager.getProjects().size());
        assertNull(manager.getProject(config.id));
    }

    @Test
    public void testAssociateServices_linksServicesToProject() throws Exception {
        Path projectDir = tempDir.resolve("app");
        Files.createDirectories(projectDir);
        ProjectConfig config = manager.importProject(projectDir.toString());
        
        manager.associateService(config.id, "MySQL");
        manager.associateService(config.id, "Redis");
        
        ProjectConfig updated = manager.getProject(config.id);
        assertTrue(updated.associatedServices.contains("MySQL"));
        assertTrue(updated.associatedServices.contains("Redis"));
    }

    @Test
    public void testBuildAndRun_buildsThenRunsJar() throws Exception {
        Path projectDir = tempDir.resolve("app");
        Files.createDirectories(projectDir);
        Files.write(projectDir.resolve("pom.xml"), "<project><artifactId>app</artifactId></project>".getBytes());
        
        ProjectConfig config = manager.importProject(projectDir.toString());
        
        doAnswer(inv -> {
            Path target = projectDir.resolve("target");
            Files.createDirectories(target);
            Files.write(target.resolve("app-1.0.jar"), new byte[0]);
            return null;
        }).when(mockMaven).runBuild(anyString(), anyString(), isNull(), isNull());

        doNothing().when(mockRunner).addOrUpdateApp(any(SpringBootRunner.AppConfig.class));
        doNothing().when(mockRunner).startApp(anyString(), isNull(), isNull());

        manager.buildAndRun(config.id);
        
        verify(mockMaven).runBuild(projectDir.toString(), "clean package -DskipTests", null, null);
        verify(mockRunner).startApp("app", null, null);
    }
}
