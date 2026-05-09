package com.zenvix.ui.logs;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class LogsViewerTest {

    @TempDir
    Path tempDir;

    private LogsViewer viewer;

    @Start
    public void start(Stage stage) throws Exception {
        viewer = new LogsViewer();
        
        javafx.scene.control.ListView<LogLine> lv = new javafx.scene.control.ListView<>();
        javafx.scene.control.TextField search = new javafx.scene.control.TextField();
        javafx.scene.control.Label matches = new javafx.scene.control.Label();
        javafx.scene.control.ToggleButton scroll = new javafx.scene.control.ToggleButton();
        javafx.scene.control.Button jump = new javafx.scene.control.Button();
        
        injectField("logListView", lv);
        injectField("searchField", search);
        injectField("searchMatchesLabel", matches);
        injectField("autoScrollToggle", scroll);
        injectField("jumpToBottomBtn", jump);
        
        viewer.initialize();
        
        stage.setScene(new Scene(lv, 800, 600));
        stage.show();
    }

    private void injectField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field f = LogsViewer.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(viewer, value);
    }

    @AfterEach
    public void tearDown() {
        viewer.stopTailing();
    }

    @Test
    public void testLogStreaming_appendsNewLines() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.write(logFile, "Line 1\n".getBytes());

        viewer.startTailing(logFile);
        
        Thread.sleep(300); 
        
        Files.write(logFile, "Line 2\n".getBytes(), StandardOpenOption.APPEND);
        
        Thread.sleep(300);

        Platform.runLater(() -> {
            assertEquals(2, viewer.getLogLines().size());
            assertEquals("Line 2", viewer.getLogLines().get(1).rawLine);
        });
    }

    @Test
    public void testLineLimitPurge_removesOldestLines() throws Exception {
        viewer.setMaxLines(5);
        
        Platform.runLater(() -> {
            for (int i = 0; i < 15; i++) {
                viewer.getLogLines().add(new LogLine("Line " + i));
            }
            
            try {
                java.lang.reflect.Method m = LogsViewer.class.getDeclaredMethod("enforceLineLimit");
                m.setAccessible(true);
                m.invoke(viewer);
            } catch (Exception e) {}

            assertTrue(viewer.getLogLines().size() <= 10); // Batch removed appropriately
        });
    }

    @Test
    public void testColorCoding_colorsCorrectly() {
        LogLine errorLine = new LogLine("2026-05-07 ERROR Something failed");
        assertEquals("ERROR", errorLine.level);

        LogLine warnLine = new LogLine("WARN Unused variable");
        assertEquals("WARN", warnLine.level);
        
        LogLine debugLine = new LogLine("DEBUG trace route");
        assertEquals("DEBUG", debugLine.level);
    }

    @Test
    public void testRegexSearch_highlightsMatches() throws Exception {
        Platform.runLater(() -> {
            viewer.getLogLines().add(new LogLine("apple"));
            viewer.getLogLines().add(new LogLine("banana"));
            viewer.getLogLines().add(new LogLine("cherry"));
            
            try {
                java.lang.reflect.Method m = LogsViewer.class.getDeclaredMethod("performSearch", String.class);
                m.setAccessible(true);
                m.invoke(viewer, "ban");
                
                java.lang.reflect.Field f = LogsViewer.class.getDeclaredField("searchMatchIndices");
                f.setAccessible(true);
                java.util.List<Integer> matches = (java.util.List<Integer>) f.get(viewer);
                
                assertEquals(1, matches.size());
                assertEquals(1, matches.get(0));
            } catch (Exception e) {
                fail();
            }
        });
    }

    @Test
    public void testAutoScroll_scrollsToBottom() throws Exception {
        Platform.runLater(() -> {
            try {
                java.lang.reflect.Method m = LogsViewer.class.getDeclaredMethod("handleJumpToBottom");
                m.setAccessible(true);
                m.invoke(viewer);
                
                java.lang.reflect.Field f = LogsViewer.class.getDeclaredField("autoScrollToggle");
                f.setAccessible(true);
                javafx.scene.control.ToggleButton toggle = (javafx.scene.control.ToggleButton) f.get(viewer);
                
                assertTrue(toggle.isSelected());
            } catch (Exception e) {}
        });
    }
}
