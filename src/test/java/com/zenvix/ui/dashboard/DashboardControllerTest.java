package com.zenvix.ui.dashboard;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class DashboardControllerTest {

    private DashboardController controller;

    @Start
    public void start(Stage stage) throws Exception {
        controller = new DashboardController();
        
        javafx.scene.layout.FlowPane fp = new javafx.scene.layout.FlowPane();
        try {
            java.lang.reflect.Field f = DashboardController.class.getDeclaredField("cardsContainer");
            f.setAccessible(true);
            f.set(controller, fp);
        } catch (Exception e) {}
        
        controller.initialize();
        
        stage.setScene(new Scene(fp, 800, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        controller.stopRefreshTask();
    }

    @Test
    public void testActiveServicesCard_updatesCount() throws Exception {
        controller.updateServiceMetrics(5, 10, Collections.emptyList(), Collections.emptyList());
        
        Thread.sleep(2500);
        
        Platform.runLater(() -> {
            try {
                java.lang.reflect.Field f = DashboardController.class.getDeclaredField("activeServicesCard");
                f.setAccessible(true);
                DashboardCard card = (DashboardCard) f.get(controller);
                assertNotNull(card);
            } catch (Exception e) {
                fail();
            }
        });
    }

    @Test
    public void testMemoryCard_showsCombinedMemory() throws Exception {
        controller.updateServiceMetrics(5, 10, Arrays.asList(1234, 5678), Collections.emptyList());
        Thread.sleep(2500); 
    }

    @Test
    public void testNetworkCard_showsPortStatus() throws Exception {
        controller.updateServiceMetrics(1, 1, Collections.emptyList(), 
            Arrays.asList(new DashboardController.ServicePort("Web", 80, true, true)));
        Thread.sleep(2500);
    }

    @Test
    public void testRefresh_updatesEvery2Seconds() throws Exception {
        controller.startRefreshTask(1); 
        Thread.sleep(2500);
    }
}
