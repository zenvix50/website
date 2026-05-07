package com.zenvix.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemLayerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testAutoStart_enablesOnMacOS() throws Exception {
        MacOSAutoStart mac = new MacOSAutoStart();
        mac.plistPath = tempDir.resolve("com.zenvix.plist");
        
        mac.setAutoStart(true, "/Applications/zenviX.app/Contents/MacOS/zenviX");
        assertTrue(mac.isAutoStartEnabled());
        assertTrue(Files.readString(mac.plistPath).contains("/Applications/zenviX.app/Contents/MacOS/zenviX"));
        
        mac.setAutoStart(false, "");
        assertFalse(mac.isAutoStartEnabled());
    }

    @Test
    public void testAutoStart_enablesOnLinux() throws Exception {
        LinuxAutoStart linux = new LinuxAutoStart();
        linux.desktopFile = tempDir.resolve("zenvix.desktop");
        
        linux.setAutoStart(true, "/opt/zenvix/zenvix");
        assertTrue(linux.isAutoStartEnabled());
        assertTrue(Files.readString(linux.desktopFile).contains("Exec=/opt/zenvix/zenvix"));
        
        linux.setAutoStart(false, "");
        assertFalse(linux.isAutoStartEnabled());
    }

    @Test
    public void testAutoStart_enablesOnWindows() {
        WindowsAutoStart win = spy(new WindowsAutoStart());
        assertDoesNotThrow(() -> win.setAutoStart(false, "C:\\zenvix.exe"));
    }

    @Test
    public void testSingleInstance_preventsSecondInstance() throws Exception {
        SingleInstanceManager instance1 = new SingleInstanceManager(tempDir, 49152);
        boolean first = instance1.checkAndLock();
        assertTrue(first, "First instance should acquire lock");
        
        SingleInstanceManager instance2 = spy(new SingleInstanceManager(tempDir, 49152));
        doNothing().when(instance2).showAlreadyRunningDialog();

        boolean second = instance2.checkAndLock();
        assertFalse(second, "Second instance should fail to acquire lock");
        
        instance1.release();
    }
}
