package com.zenvix.ui.accessibility;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Provides static utilities enforcing high-contrast WCAG 2.1 AA focus rings
 * universally across JavaFX application environments securely.
 */
public class AccessibilityManager {

    /**
     * Injects the global high-contrast focus CSS into the provided Scene natively
     * guaranteeing interactive boundaries meet 3px offset requirements accurately natively.
     */
    public static void applyFocusVisible(Scene scene) {
        if (scene == null) return;
        
        String focusCss = 
            ".root {" +
            "  -fx-focus-color: #0288D1;" +
            "  -fx-faint-focus-color: transparent;" +
            "}" +
            "*:focused {" +
            "  -fx-border-color: #0288D1;" +
            "  -fx-border-width: 3px;" +
            "  -fx-border-style: solid;" +
            "  -fx-border-radius: 3px;" +
            "  -fx-background-insets: 0, 4;" + 
            "}";
        
        String dataUri = "data:text/css;charset=utf-8," + focusCss.replace(" ", "%20").replace("#", "%23");
        scene.getStylesheets().add(dataUri);
    }
    
    /**
     * Force-fires dynamic updates directly alerting Screen Readers sequentially.
     */
    public static void announceStateChange(Node node, String announcement) {
        node.setAccessibleText(announcement);
        node.notifyAccessibleAttributeChanged(javafx.scene.AccessibleAttribute.TEXT);
    }
}
