package com.zenvix.ui.tray;

import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemTrayManagerTest {

    private SystemTrayManager trayManager;

    @Mock private Stage mockStage;
    @Mock private Runnable mockStartAll;
    @Mock private Runnable mockStopAll;
    @Mock private Runnable mockExit;

    @BeforeEach
    public void setUp() {
        trayManager = new SystemTrayManager();
        trayManager.testingMode = true; 
    }

    @Test
    public void testTrayAvailable_setsUpTrayIcon() {
        trayManager.initialize(mockStage, mockStartAll, mockStopAll, mockExit, null);
        assertNotNull(trayManager.getTrayIcon());
        assertNotNull(trayManager.getTrayIcon().getPopupMenu());
    }

    @Test
    public void testMinimizeToTray_hidesWindow() {
        trayManager.initialize(mockStage, mockStartAll, mockStopAll, mockExit, null);
        
        trayManager.minimizeToTray();
        
        verify(mockStage).hide();
    }

    @Test
    public void testRestoreFromTray_showsWindow() {
        trayManager.initialize(mockStage, mockStartAll, mockStopAll, mockExit, null);
        
        trayManager.restoreFromTray();
        
        verify(mockStage).show();
        verify(mockStage).toFront();
    }

    @Test
    public void testContextMenu_listsRunningServices() {
        trayManager.initialize(mockStage, mockStartAll, mockStopAll, mockExit, () -> Arrays.asList(
            new SystemTrayManager.TrayService("Tomcat", () -> {}),
            new SystemTrayManager.TrayService("MySQL", () -> {})
        ));
        
        trayManager.updateContextMenu();
        
        PopupMenu popup = trayManager.getTrayIcon().getPopupMenu();
        assertNotNull(popup);
        
        Menu servicesMenu = (Menu) popup.getItem(0);
        assertEquals("Running Services", servicesMenu.getLabel());
        assertEquals(2, servicesMenu.getItemCount());
        assertEquals("Stop Tomcat", servicesMenu.getItem(0).getLabel());
        assertEquals("Stop MySQL", servicesMenu.getItem(1).getLabel());
    }

    @Test
    public void testExit_stopsServicesAndExits() {
        trayManager.initialize(mockStage, mockStartAll, mockStopAll, mockExit, null);
        
        trayManager.performExit();
        
        verify(mockExit).run();
    }
}
