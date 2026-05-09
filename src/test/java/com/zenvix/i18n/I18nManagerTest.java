package com.zenvix.i18n;

import javafx.geometry.NodeOrientation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.PropertyChangeListener;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class I18nManagerTest {

    private I18nManager manager;

    @BeforeEach
    public void setUp() {
        I18nManager.resetInstance(new Locale("en", "US"));
        manager = I18nManager.getInstance();
    }

    @Test
    public void testEnglish_returnsCorrectStrings() {
        assertEquals("zenviX — Java Development Environment Manager", manager.get("main.title"));
        assertEquals("Running", manager.get("service.status.running"));
    }

    @Test
    public void testHindi_returnsCorrectStrings() {
        manager.setLocale(new Locale("hi", "IN"));
        assertEquals("zenviX — जावा विकास पर्यावरण प्रबंधक", manager.get("main.title"));
        assertEquals("चल रहा है", manager.get("service.status.running"));
    }

    @Test
    public void testRuntimeSwitch_updatesLocale() {
        boolean[] eventFired = {false};
        PropertyChangeListener listener = evt -> {
            assertEquals("locale", evt.getPropertyName());
            assertEquals(new Locale("hi", "IN"), evt.getNewValue());
            eventFired[0] = true;
        };
        
        manager.addPropertyChangeListener(listener);
        manager.setLocale(new Locale("hi", "IN"));
        
        assertTrue(eventFired[0]);
        assertEquals(new Locale("hi", "IN"), manager.getCurrentLocale());
        
        manager.removePropertyChangeListener(listener);
    }

    @Test
    public void testMessageFormat_handlesParameters() {
        String formatted = manager.format("error.portConflict", "8080", "Tomcat", "1234");
        assertEquals("Port 8080 is already in use by Tomcat (PID: 1234)", formatted);
        
        String plural0 = manager.format("status.servicesRunning", 0);
        assertEquals("No services running", plural0);
        
        String plural1 = manager.format("status.servicesRunning", 1);
        assertEquals("1 service running", plural1);
        
        String plural5 = manager.format("status.servicesRunning", 5);
        assertEquals("5 services running", plural5);
    }

    @Test
    public void testRTL_setsCorrectNodeOrientation() {
        javafx.scene.Node mockNode = org.mockito.Mockito.mock(javafx.scene.Node.class);
        
        manager.applyRTL(mockNode);
        org.mockito.Mockito.verify(mockNode).setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        
        manager.setLocale(new Locale("ar", "SA"));
        manager.applyRTL(mockNode);
        org.mockito.Mockito.verify(mockNode).setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
    }
}
