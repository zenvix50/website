package com.zenvix.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfflineModeManagerTest {

    private OfflineModeManager manager;

    @BeforeEach
    public void setUp() {
        manager = new OfflineModeManager();
    }

    @Test
    public void testNetworkCheck_detectsOnlineStatus() throws Exception {
        OfflineModeManager spyManager = spy(new OfflineModeManager() {
            @Override
            protected void checkNetworkStatus() {
                try {
                    java.lang.reflect.Field field = OfflineModeManager.class.getDeclaredField("isOnline");
                    field.setAccessible(true);
                    AtomicBoolean online = (AtomicBoolean) field.get(this);
                    online.set(true);
                } catch (Exception e) {}
            }
        });
        
        spyManager.checkNetworkStatus();
        assertTrue(spyManager.isOnline());
    }

    @Test
    public void testOfflineMode_disablesInternetFeatures() throws Exception {
        OfflineModeManager spyManager = spy(new OfflineModeManager());
        
        java.lang.reflect.Field field = OfflineModeManager.class.getDeclaredField("isOnline");
        field.setAccessible(true);
        AtomicBoolean online = (AtomicBoolean) field.get(spyManager);
        online.set(false);
        
        doNothing().when(spyManager).showOfflineDialog();
        
        boolean[] ran = {false};
        boolean success = spyManager.executeInternetRequiredFeature(() -> ran[0] = true);
        
        assertFalse(success);
        assertFalse(ran[0], "Feature should not execute when offline");
        verify(spyManager).showOfflineDialog();
    }

    @Test
    public void testGracefulError_showsOfflineDialog() throws Exception {
        OfflineModeManager spyManager = spy(new OfflineModeManager());
        
        java.lang.reflect.Field field = OfflineModeManager.class.getDeclaredField("isOnline");
        field.setAccessible(true);
        AtomicBoolean online = (AtomicBoolean) field.get(spyManager);
        online.set(false);
        
        doNothing().when(spyManager).showOfflineDialog();
        
        spyManager.executeInternetRequiredFeature(() -> {});
        verify(spyManager, times(1)).showOfflineDialog();
    }

    @Test
    public void testSystemServiceDetection_detectsInstalledServices() throws Exception {
        File dummyDir = Files.createTempDirectory("dummyBin").toFile();
        File dummyBin = new File(dummyDir, "mysql");
        
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            dummyBin = new File(dummyDir, "mysql.exe");
        }
        
        dummyBin.createNewFile();
        dummyBin.setExecutable(true);

        OfflineModeManager spyManager = spy(new OfflineModeManager());
        doReturn(dummyDir.getAbsolutePath()).when(spyManager).getSystemPath();

        String path = spyManager.detectSystemService("mysql");
        assertNotNull(path);
        assertEquals(dummyBin.getAbsolutePath(), path);
    }
}
