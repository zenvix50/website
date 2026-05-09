package com.zenvix.docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DockerManagerTest {

    private ComposeGenerator composeGenerator;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        composeGenerator = new ComposeGenerator();
    }

    @Test
    public void testDetection_detectsDockerRunning() {
        DockerManager dm = new DockerManager() {
            @Override
            protected boolean checkDockerAvailability() {
                return true; 
            }
        };
        assertTrue(dm.isAvailable());
    }

    @Test
    public void testDetection_handlesDockerNotInstalled() {
        DockerManager dm = new DockerManager() {
            @Override
            protected boolean checkDockerAvailability() {
                return false; 
            }
        };
        assertFalse(dm.isAvailable());
    }

    @Test
    public void testContainerStart_startsContainer() {
        DockerManager dm = new DockerManager() {
            @Override
            protected boolean checkDockerAvailability() { return true; }
        };
        DockerManager spyDm = spy(dm);
        doReturn(true).when(spyDm).executeCommand("docker", "start", "zenvix-mysql");
        
        boolean success = spyDm.startContainer("zenvix-mysql");
        assertTrue(success);
        verify(spyDm).executeCommand("docker", "start", "zenvix-mysql");
    }

    @Test
    public void testContainerStop_stopsContainer() {
        DockerManager dm = new DockerManager() {
            @Override
            protected boolean checkDockerAvailability() { return true; }
        };
        DockerManager spyDm = spy(dm);
        doReturn(true).when(spyDm).executeCommand("docker", "stop", "zenvix-mysql");
        
        boolean success = spyDm.stopContainer("zenvix-mysql");
        assertTrue(success);
        verify(spyDm).executeCommand("docker", "stop", "zenvix-mysql");
    }

    @Test
    public void testComposeGenerate_generatesValidYaml() throws Exception {
        Path composeFile = tempDir.resolve("docker-compose.yml");
        
        List<ComposeGenerator.ServiceConfig> services = Arrays.asList(
            new ComposeGenerator.ServiceConfig(ComposeGenerator.ServiceType.MYSQL, 3306, "secret"),
            new ComposeGenerator.ServiceConfig(ComposeGenerator.ServiceType.REDIS, 6379, null)
        );
        
        boolean success = composeGenerator.generateComposeFile(services, composeFile);
        assertTrue(success);
        assertTrue(Files.exists(composeFile));
        
        String yaml = new String(Files.readAllBytes(composeFile));
        assertTrue(yaml.contains("version: '3.8'"));
        assertTrue(yaml.contains("image: mysql:8.0"));
        assertTrue(yaml.contains("MYSQL_ROOT_PASSWORD: secret"));
        assertTrue(yaml.contains("image: redis:7"));
        assertTrue(yaml.contains("\"6379:6379\""));
    }
}
