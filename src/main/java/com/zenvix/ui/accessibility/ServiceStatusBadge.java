package com.zenvix.ui.accessibility;

import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Custom accessible status badge guaranteeing Screen Readers securely announce 
 * dynamic text modifiers mapping "RUNNING" / "STOPPED" bypassing visual hex codes naturally!
 */
public class ServiceStatusBadge extends StackPane {

    private String statusText = "UNKNOWN";
    private final Label label;
    private final Rectangle background;

    public ServiceStatusBadge() {
        this.setAccessibleRole(AccessibleRole.TEXT);
        this.setFocusTraversable(true);
        
        background = new Rectangle(80, 25);
        background.setArcWidth(10);
        background.setArcHeight(10);
        
        label = new Label(statusText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 3 8;");

        this.getChildren().addAll(background, label);
        setStatus("STOPPED");
    }

    public void setStatus(String status) {
        this.statusText = status;
        label.setText(status.toUpperCase());
        
        switch (status.toUpperCase()) {
            case "RUNNING":
                background.setFill(Color.web("#4caf50"));
                break;
            case "STOPPED":
                background.setFill(Color.web("#f44336"));
                break;
            case "STARTING":
            case "STOPPING":
                background.setFill(Color.web("#ffeb3b"));
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-padding: 3 8;");
                break;
            case "WARNING":
                background.setFill(Color.web("#ff9800"));
                break;
            default:
                background.setFill(Color.GRAY);
                break;
        }

        this.notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        if (attribute == AccessibleAttribute.TEXT) {
            return "Service status: " + statusText;
        }
        return super.queryAccessibleAttribute(attribute, parameters);
    }
}
