package com.zenvix.ui.logs;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Controller for the real-time Log Viewer. Handles virtualized scrolling, 
 * background file tailing explicitly preventing UI Thread hanging naturally.
 */
public class LogsViewer {

    @FXML private TextField searchField;
    @FXML private Label searchMatchesLabel;
    @FXML private ToggleButton autoScrollToggle;
    @FXML private ListView<LogLine> logListView;
    @FXML private Button jumpToBottomBtn;

    private final ObservableList<LogLine> logLines = FXCollections.observableArrayList();
    private ScheduledExecutorService tailExecutor;
    private BufferedReader reader;
    
    private int maxLines = 10000;
    private final int purgeBatchSize = 1000;
    
    private final List<Integer> searchMatchIndices = new ArrayList<>();
    private int currentMatchIndex = -1;
    private Pattern currentSearchPattern;

    @FXML
    public void initialize() {
        logListView.setItems(logLines);
        setupCellFactory();

        logListView.setOnScroll(event -> {
            if (event.getDeltaY() > 0) { 
                autoScrollToggle.setSelected(false);
                jumpToBottomBtn.setVisible(true);
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> performSearch(newV));
    }

    public void startTailing(Path logFile) {
        stopTailing();
        try {
            reader = new BufferedReader(new FileReader(logFile.toFile()));
            tailExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });
            tailExecutor.scheduleWithFixedDelay(this::readNewLines, 0, 100, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            logLines.add(new LogLine("ERROR: Could not open log file: " + logFile));
        }
    }

    public void stopTailing() {
        if (tailExecutor != null) tailExecutor.shutdownNow();
        if (reader != null) {
            try { reader.close(); } catch (IOException e) {}
        }
    }

    private void readNewLines() {
        try {
            List<LogLine> newLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                newLines.add(new LogLine(line));
            }

            if (!newLines.isEmpty()) {
                Platform.runLater(() -> {
                    logLines.addAll(newLines);
                    enforceLineLimit();
                    
                    if (currentSearchPattern != null) {
                        performSearch(searchField.getText()); 
                    }

                    if (autoScrollToggle.isSelected()) {
                        logListView.scrollTo(logLines.size() - 1);
                        jumpToBottomBtn.setVisible(false);
                    }
                });
            }
        } catch (IOException e) { /* Expected closures */ }
    }

    private void enforceLineLimit() {
        if (logLines.size() > maxLines) {
            int excess = logLines.size() - maxLines;
            int toRemove = Math.max(excess, purgeBatchSize);
            toRemove = Math.min(toRemove, logLines.size()); 
            logLines.subList(0, toRemove).clear();
        }
    }

    private void setupCellFactory() {
        logListView.setCellFactory(lv -> new ListCell<LogLine>() {
            @Override
            protected void updateItem(LogLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    TextFlow textFlow = new TextFlow();
                    Text text = new Text(item.rawLine);
                    text.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

                    switch (item.level) {
                        case "ERROR": text.setFill(Color.web("#ff5252")); break;
                        case "WARN":  text.setFill(Color.web("#ffeb3b")); break;
                        case "DEBUG": text.setFill(Color.web("#9e9e9e")); break;
                        default:      text.setFill(Color.web("#ffffff")); break;
                    }

                    if (currentSearchPattern != null) {
                        Matcher m = currentSearchPattern.matcher(item.rawLine);
                        if (m.find()) {
                            setStyle("-fx-background-color: #3d4c53;"); 
                        } else {
                            setStyle("-fx-background-color: transparent;");
                        }
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }

                    textFlow.getChildren().add(text);
                    setGraphic(textFlow);
                }
            }
        });
    }

    private void performSearch(String query) {
        searchMatchIndices.clear();
        currentMatchIndex = -1;
        currentSearchPattern = null;

        if (query == null || query.isEmpty()) {
            searchMatchesLabel.setText("0 matches");
            logListView.refresh();
            return;
        }

        try {
            currentSearchPattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            searchMatchesLabel.setText("Invalid regex");
            return;
        }

        for (int i = 0; i < logLines.size(); i++) {
            if (currentSearchPattern.matcher(logLines.get(i).rawLine).find()) {
                searchMatchIndices.add(i);
            }
        }

        searchMatchesLabel.setText(searchMatchIndices.size() + " matches");
        logListView.refresh();

        if (!searchMatchIndices.isEmpty()) {
            currentMatchIndex = 0;
            logListView.scrollTo(searchMatchIndices.get(0));
            logListView.getSelectionModel().select(searchMatchIndices.get(0));
        }
    }

    @FXML
    public void handleSearchNext() {
        if (searchMatchIndices.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % searchMatchIndices.size();
        int idx = searchMatchIndices.get(currentMatchIndex);
        logListView.scrollTo(idx);
        logListView.getSelectionModel().select(idx);
    }

    @FXML
    public void handleSearchPrev() {
        if (searchMatchIndices.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex - 1 + searchMatchIndices.size()) % searchMatchIndices.size();
        int idx = searchMatchIndices.get(currentMatchIndex);
        logListView.scrollTo(idx);
        logListView.getSelectionModel().select(idx);
    }

    @FXML
    public void handleJumpToBottom() {
        autoScrollToggle.setSelected(true);
        jumpToBottomBtn.setVisible(false);
        if (!logLines.isEmpty()) {
            logListView.scrollTo(logLines.size() - 1);
        }
    }

    @FXML
    public void handleClear() {
        logLines.clear();
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }
    
    protected ObservableList<LogLine> getLogLines() {
        return logLines;
    }
}
