package com.zenvix.ui.dashboard;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Custom JavaFX component representing a single unified Dashboard Metric Card cleanly mapping 
 * dropshadow values natively executing robust styles.
 */
public class DashboardCard extends VBox {

    private final Label titleLabel;
    private final Label valueLabel;
    private final Label subtitleLabel;

    public DashboardCard(String icon, String title) {
        this.getStyleClass().add("dashboard-card");
        this.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 3); " +
                      "-fx-padding: 20; -fx-min-width: 200; -fx-min-height: 120;");

        titleLabel = new Label(icon + " " + title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");

        valueLabel = new Label("-");
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #333333; -fx-padding: 10 0 0 0;");

        subtitleLabel = new Label("Initializing...");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");

        this.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
