package com.zenvix.ui.accessibility;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Generates an accessible Dialog wrapping 2-column TableViews explicitly filtering 
 * global keyboard hooks securely mapping robust navigation paths accurately.
 */
public class KeyboardShortcutDialog extends Dialog<Void> {

    public static class Shortcut {
        private final String action;
        private final String combination;

        public Shortcut(String action, String combination) {
            this.action = action;
            this.combination = combination;
        }

        public String getAction() { return action; }
        public String getCombination() { return combination; }
    }

    public KeyboardShortcutDialog() {
        this.setTitle("Keyboard Shortcuts");
        this.setHeaderText("zenviX Shortcut Reference");

        VBox content = new VBox(10);
        content.setPrefSize(400, 300);

        TextField searchField = new TextField();
        searchField.setPromptText("Search shortcuts...");
        searchField.setAccessibleText("Search keyboard shortcuts");

        TableView<Shortcut> table = new TableView<>();
        table.setAccessibleText("Keyboard shortcuts table");

        TableColumn<Shortcut, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionCol.setPrefWidth(200);

        TableColumn<Shortcut, String> keyCol = new TableColumn<>("Shortcut");
        keyCol.setCellValueFactory(new PropertyValueFactory<>("combination"));
        keyCol.setPrefWidth(180);

        table.getColumns().add(actionCol);
        table.getColumns().add(keyCol);

        ObservableList<Shortcut> shortcuts = FXCollections.observableArrayList(
            new Shortcut("Start All Services", "Ctrl+Shift+S"),
            new Shortcut("Stop All Services", "Ctrl+Shift+X"),
            new Shortcut("Restart All Services", "Ctrl+Shift+R"),
            new Shortcut("Open Terminal", "Ctrl+T"),
            new Shortcut("View Global Logs", "Ctrl+L"),
            new Shortcut("Backup Configuration", "Ctrl+B"),
            new Shortcut("Toggle Focused Service", "Ctrl+1 through Ctrl+9"),
            new Shortcut("Keyboard Reference", "F1")
        );

        FilteredList<Shortcut> filteredData = new FilteredList<>(shortcuts, p -> true);
        table.setItems(filteredData);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(shortcut -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (shortcut.getAction().toLowerCase().contains(lowerCaseFilter)) return true;
                return shortcut.getCombination().toLowerCase().contains(lowerCaseFilter);
            });
        });

        content.getChildren().addAll(searchField, table);
        this.getDialogPane().setContent(content);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        Platform.runLater(searchField::requestFocus);
    }
}
