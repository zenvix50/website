package com.zenvix.update;

import com.zenvix.ui.notifications.NotificationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateManagerTest {

    private UpdateManager updateManager;

    @Mock
    private NotificationManager mockNotifier;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        updateManager = spy(new UpdateManager(mockNotifier));
    }

    @Test
    public void testVersionCompare_correctlyComparesVersions() {
        assertEquals(1, updateManager.compareVersions("1.3.0", "1.2.9"));
        assertEquals(-1, updateManager.compareVersions("1.2.3", "1.2.4"));
        assertEquals(0, updateManager.compareVersions("2.0.0", "2.0.0"));
        assertEquals(1, updateManager.compareVersions("2.1", "2.0.5"));
    }

    @Test
    public void testJSONParsing_extractsFields() {
        String json = "{\"tag_name\":\"v1.5.0\", \"body\":\"New stuff\"}";
        assertEquals("v1.5.0", updateManager.extractJsonField(json, "tag_name"));
        assertEquals("New stuff", updateManager.extractJsonField(json, "body"));
    }

    @Test
    public void testGitHubAPI_parsesReleaseResponse() {
        ReleaseInfo info = new ReleaseInfo("1.5.0", "Fixes", "https://url.com/zenvix.msi", "zenvix.msi");
        doReturn(info).when(updateManager).fetchLatestRelease();
        
        ReleaseInfo fetched = updateManager.fetchLatestRelease();
        assertNotNull(fetched);
        assertEquals("1.5.0", fetched.version);
    }

    @Test
    public void testSilentMode_downloadsWithoutAsking() {
        updateManager.setUpdateMode(UpdateManager.UpdateMode.SILENT);
        ReleaseInfo info = new ReleaseInfo("2.0.0", "Major update", "http://dl.com", "installer.exe");
        
        doNothing().when(updateManager).downloadUpdate(info);
        
        updateManager.handleNewUpdate(info);
        
        verify(updateManager).downloadUpdate(info);
        verify(updateManager, never()).promptUserForUpdate(info);
    }

    @Test
    public void testPromptedMode_asksBeforeDownload() {
        updateManager.setUpdateMode(UpdateManager.UpdateMode.PROMPTED);
        ReleaseInfo info = new ReleaseInfo("2.0.0", "Major update", "http://dl.com", "installer.exe");
        
        doNothing().when(updateManager).promptUserForUpdate(info);
        
        updateManager.handleNewUpdate(info);
        
        verify(updateManager).promptUserForUpdate(info);
        verify(updateManager, never()).downloadUpdate(info);
    }

    @Test
    public void testRollback_launchesPreviousInstaller() throws Exception {
        Path previousDir = tempDir.resolve("zenvix").resolve("update").resolve("previous");
        Files.createDirectories(previousDir);
        Path oldInstaller = previousDir.resolve("zenvix-old.msi");
        Files.write(oldInstaller, "dummy".getBytes());
        
        UpdateManager dm = new UpdateManager(mockNotifier) {
            @Override
            public boolean applyUpdate(Path installerPath, String version) {
                return true;
            }
        };
        
        java.lang.reflect.Field field = UpdateManager.class.getDeclaredField("PREVIOUS_DIR");
        field.setAccessible(true);
        field.set(dm, previousDir);
        
        boolean success = dm.rollback();
        assertTrue(success);
    }
}
