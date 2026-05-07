package com.zenvix.notifications;

import com.zenvix.notifications.Notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationManagerTest {

    @Test
    public void testWindowsNotifier_sendsNotification() {
        WindowsNotifier notifier = spy(new WindowsNotifier());
        doNothing().when(notifier).useAwtFallback(anyString(), anyString(), any(NotificationType.class));
        
        notifier.send("Test", "Message", NotificationType.INFO);
    }

    @Test
    public void testMacOSNotifier_runsOsascript() {
        MacOSNotifier notifier = new MacOSNotifier() {
            @Override
            public void send(String title, String message, NotificationType type) {
            }
        };
        notifier.send("Test", "Msg", NotificationType.INFO);
    }

    @Test
    public void testHistory_storesNotifications() {
        OsNotifier mockNotifier = mock(OsNotifier.class);
        NotificationManager manager = new NotificationManager(mockNotifier);
        
        manager.notify(NotificationType.INFO, "A", "B");
        manager.notify(NotificationType.WARNING, "C", "D");
        
        List<Notification> history = manager.getHistory();
        assertEquals(2, history.size());
        assertEquals("C", history.get(0).title); 
        assertEquals("A", history.get(1).title);
        
        verify(mockNotifier, times(2)).send(anyString(), anyString(), any(NotificationType.class));
    }
    
    @Test
    public void testHistory_capAt50() {
        OsNotifier mockNotifier = mock(OsNotifier.class);
        NotificationManager manager = new NotificationManager(mockNotifier);
        
        for (int i = 0; i < 55; i++) {
            manager.notify(NotificationType.INFO, "Title " + i, "Msg");
        }
        
        List<Notification> history = manager.getHistory();
        assertEquals(50, history.size());
        assertEquals("Title 54", history.get(0).title);
    }

    @Test
    public void testCrash_triggersBoth() {
        OsNotifier mockNotifier = mock(OsNotifier.class);
        NotificationManager manager = spy(new NotificationManager(mockNotifier));
        doNothing().when(manager).showCrashDialog(anyString());
        
        manager.notifyCrash("Tomcat");
        
        verify(manager).showCrashDialog("Tomcat");
        verify(mockNotifier).send(eq("Service Crashed"), contains("Tomcat"), eq(NotificationType.CRASH));
        
        List<Notification> history = manager.getHistory();
        assertEquals(1, history.size());
        assertEquals(NotificationType.CRASH, history.get(0).type);
    }

    @Test
    public void testNotifierFactory() {
        OsNotifier notifier = NotifierFactory.createNotifier();
        assertNotNull(notifier);
    }
}
