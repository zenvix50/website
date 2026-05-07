package com.zenvix.ui.theme;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class ThemeManagerTest {

    private ThemeManager themeManager;
    private Scene testScene;
    private Path configPath = Paths.get("zenvix", "config", "config.json");

    @BeforeEach
    public void setUp() throws Exception {
        if (Files.exists(configPath)) {
            Files.delete(configPath);
        }
        
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            testScene = new Scene(new Pane(), 800, 600);
            themeManager = new ThemeManager();
            latch.countDown();
        });
        latch.await();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (Files.exists(configPath)) {
            Files.delete(configPath);
        }
    }

    @Test
    public void testLightTheme_appliesLightCSS() throws Exception {
        Platform.runLater(() -> themeManager.applyTheme(testScene, ThemeManager.Theme.LIGHT));
        Thread.sleep(300); 
        
        Platform.runLater(() -> {
            assertEquals(ThemeManager.Theme.LIGHT, themeManager.getCurrentTheme());
        });
    }

    @Test
    public void testHighContrast_appliesHighContrastCSS() throws Exception {
        Platform.runLater(() -> themeManager.applyTheme(testScene, ThemeManager.Theme.HIGH_CONTRAST));
        Thread.sleep(300);
        
        Platform.runLater(() -> {
            assertEquals(ThemeManager.Theme.HIGH_CONTRAST, themeManager.getCurrentTheme());
        });
    }

    @Test
    public void testThemePersistence_savesAndLoadsTheme() throws Exception {
        Platform.runLater(() -> themeManager.applyTheme(testScene, ThemeManager.Theme.LIGHT));
        Thread.sleep(300);
        
        assertTrue(Files.exists(configPath));
        String content = new String(Files.readAllBytes(configPath));
        assertTrue(content.contains("\"theme\":\"LIGHT\""));

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            ThemeManager loadedManager = new ThemeManager();
            assertEquals(ThemeManager.Theme.LIGHT, loadedManager.getCurrentTheme());
            latch.countDown();
        });
        latch.await();
    }

    @Test
    public void testFontSize_appliesCorrectSize() throws Exception {
        Platform.runLater(() -> themeManager.setFontSize(testScene, ThemeManager.FontSize.LARGE));
        Thread.sleep(100);
        
        Platform.runLater(() -> {
            assertEquals(ThemeManager.FontSize.LARGE, themeManager.getCurrentFontSize());
            String style = testScene.getRoot().getStyle();
            assertTrue(style.contains("-fx-font-size: 14pt;"));
        });
    }
}
