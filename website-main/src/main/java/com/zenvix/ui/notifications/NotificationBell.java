package com.zenvix.ui.notifications;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A custom JavaFX component that displays a bell icon dynamically calculating unread count badges.
 * Clicking it explicitly opens dropdown Popup tracking history states cleanly seamlessly.
 */
public class NotificationBell extends StackPane {

    private NotificationManager manager;
    private final Label iconLabel;
    private final Label badgeLabel;
    private final Circle badgeBackground;
    private int unreadCount = 0;
    
    private Popup historyPopup;
    private ListView<Notification> historyListView;

    public NotificationBell() {
        iconLabel = new Label("🔔");
        iconLabel.setFont(Font.font(20));

        badgeBackground = new Circle(8, Color.RED);
        badgeBackground.setVisible(false);

        badgeLabel = new Label("0");
        badgeLabel.setTextFill(Color.WHITE);
        badgeLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        badgeLabel.setVisible(false);

        StackPane badgePane = new StackPane(badgeBackground, badgeLabel);
        badgePane.setAlignment(Pos.TOP_RIGHT);
        badgePane.setTranslateX(5);
        badgePane.setTranslateY(-5);

        this.getChildren().addAll(iconLabel, badgePane);
        this.setPickOnBounds(true);
        this.setStyle("-fx-cursor: hand;");
        this.setOnMouseClicked(this::handleBellClick);

        buildPopup();
    }

    public void setManager(NotificationManager manager) {
        this.manager = manager;
        historyListView.setItems(manager.getHistory());
    }

    public void incrementUnread() {
        unreadCount++;
        updateBadge();
    }

    public void resetUnread() {
        unreadCount = 0;
        updateBadge();
    }

    private void updateBadge() {
        if (unreadCount > 0) {
            badgeLabel.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            badgeBackground.setVisible(true);
            badgeLabel.setVisible(true);
        } else {
            badgeBackground.setVisible(false);
            badgeLabel.setVisible(false);
        }
    }

    private void buildPopup() {
        historyPopup = new Popup();
        historyPopup.setAutoHide(true);

        VBox container = new VBox();
        container.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");
        container.setPrefSize(300, 400);

        Label title = new Label("Notifications");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-background-color: #f5f5f5;");
        title.setMaxWidth(Double.MAX_VALUE);

        historyListView = new ListView<>();
        historyListView.setCellFactory(lv -> new ListCell<Notification>() {
            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox box = new VBox(2);
                    Label msgTitle = new Label(item.title);
                    msgTitle.setStyle("-fx-font-weight: bold;");
                    Label msgBody = new Label(item.message);
                    msgBody.setWrapText(true);
                    
                    String color = item.read ? "#ffffff" : "#e3f2fd";
                    setStyle("-fx-background-color: " + color + "; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-padding: 5;");
                    
                    box.getChildren().addAll(msgTitle, msgBody);
                    setGraphic(box);
                }
            }
        });

        container.getChildren().addAll(title, historyListView);
        historyPopup.getContent().add(container);
    }

    private void handleBellClick(MouseEvent event) {
        if (historyPopup.isShowing()) {
            historyPopup.hide();
        } else {
            if (manager != null) {
                manager.markAllAsRead();
                historyListView.refresh();
            }
            historyPopup.show(this, event.getScreenX() - 150, event.getScreenY() + 20);
        }
    }
}
