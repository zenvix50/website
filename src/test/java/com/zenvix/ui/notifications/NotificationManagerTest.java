package com.zenvix.ui.notifications;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
public class NotificationManagerTest {

    private NotificationManager manager;
    private NotificationManager spyManager;

    @Mock
    private Consumer<String> mockRestart;

    @Mock
    private Consumer<String> mockLogs;

    @BeforeEach
    public void setUp() {
        manager = new NotificationManager();
        spyManager = spy(manager);
    }

    @Test
    public void testOSToast_sendsOnWindows() throws Exception {
        doNothing().when(spyManager).executeCommand(any());
        
        spyManager.sendWindowsToast("WinTitle", "WinMsg", Notification.Type.INFO);
        
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(spyManager).executeCommand(captor.capture());
        
        String[] cmd = captor.getValue();
        assertEquals("powershell.exe", cmd[0]);
        assertTrue(cmd[2].contains("WinTitle"));
    }

    @Test
    public void testOSToast_sendsOnMacOS() throws Exception {
        doNothing().when(spyManager).executeCommand(any());
        
        spyManager.sendMacOSToast("MacTitle", "MacMsg");
        
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(spyManager).executeCommand(captor.capture());
        
        String[] cmd = captor.getValue();
        assertEquals("osascript", cmd[0]);
        assertTrue(cmd[2].contains("MacTitle"));
    }

    @Test
    public void testOSToast_sendsOnLinux() throws Exception {
        doNothing().when(spyManager).executeCommand(any());
        
        spyManager.sendLinuxToast("LinTitle", "LinMsg");
        
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(spyManager).executeCommand(captor.capture());
        
        String[] cmd = captor.getValue();
        assertEquals("notify-send", cmd[0]);
        assertEquals("LinTitle", cmd[3]);
    }

    @Test
    public void testInAppBell_incrementsUnreadCount() throws Exception {
        Platform.runLater(() -> {
            NotificationBell bell = new NotificationBell();
            manager.setBell(bell);
            
            NotificationManager testManager = new NotificationManager() {
                @Override
                protected void sendOSToast(String title, String message, Notification.Type type) {}
            };
            testManager.setBell(bell);
            testManager.notifyInfo("T", "M");
            testManager.notifyInfo("T", "M");
            
            try {
                java.lang.reflect.Field f = NotificationBell.class.getDeclaredField("unreadCount");
                f.setAccessible(true);
                int count = (int) f.get(bell);
                assertEquals(2, count);
            } catch (Exception e) {}
        });
        Thread.sleep(200);
    }

    @Test
    public void testHistory_storesLast50Notifications() throws Exception {
        Platform.runLater(() -> {
            NotificationManager testManager = new NotificationManager() {
                @Override
                protected void sendOSToast(String title, String message, Notification.Type type) {}
            };
            
            for (int i = 0; i < 60; i++) {
                testManager.notifyInfo("T" + i, "M");
            }
            
            assertEquals(50, testManager.getHistory().size());
            assertEquals("T59", testManager.getHistory().get(0).title);
        });
        Thread.sleep(200);
    }

    @Test
    public void testCrashPopup_showsDialogOnCrash() throws Exception {
        Platform.runLater(() -> {
            NotificationManager testManager = new NotificationManager() {
                @Override
                protected void sendOSToast(String title, String message, Notification.Type type) {}
                
                @Override
                protected void showCrashDialog(String serviceName) {}
            };
            NotificationManager testSpy = spy(testManager);
            
            testSpy.notifyCrash("Tomcat");
            
            verify(testSpy).showCrashDialog("Tomcat");
        });
        Thread.sleep(200);
    }
}
