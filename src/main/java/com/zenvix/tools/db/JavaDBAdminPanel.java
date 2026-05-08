package com.zenvix.tools.db;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * JavaDBAdminPanel coordinates the JavaFX WebEngine UI and backend database execution.
 * Serializes Java ResultSets directly to JSON strings bridging Web environments.
 */
public class JavaDBAdminPanel {

    private final Consumer<String> jsExecutor;
    private DatabaseConnection activeConnection;

    public JavaDBAdminPanel(Consumer<String> jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    public void setConnection(DatabaseConnection connection) {
        this.activeConnection = connection;
    }

    public void connect(String dbType, String host, int port, String database, String username) {
        new Thread(() -> {
            try {
                if (activeConnection != null) activeConnection.close();
                activeConnection = new DatabaseConnection(dbType, host, port, database, username);
                activeConnection.connect();
                runOnUIThread(() -> jsExecutor.accept("onConnected('SUCCESS')"));
            } catch (Exception e) {
                runOnUIThread(() -> jsExecutor.accept("onConnected('ERROR: " + e.getMessage().replace("'", "\\'") + "')"));
            }
        }).start();
    }

    public void executeQuery(String sql) {
        if (activeConnection == null) return;
        new Thread(() -> {
            try {
                List<Map<String, Object>> results = activeConnection.executeQuery(sql);
                String json = exportToJson(results);
                runOnUIThread(() -> jsExecutor.accept("onQueryResult(" + json + ")"));
            } catch (Exception e) {
                runOnUIThread(() -> jsExecutor.accept("onQueryError('" + e.getMessage().replace("'", "\\'") + "')"));
            }
        }).start();
    }

    public void generateERDiagram() {
        if (activeConnection == null) return;
        new Thread(() -> {
            try {
                List<Map<String, String>> rels = activeConnection.getTableRelationships();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < rels.size(); i++) {
                    Map<String, String> r = rels.get(i);
                    sb.append(String.format("{\"child_table\":\"%s\",\"child_column\":\"%s\",\"parent_table\":\"%s\",\"parent_column\":\"%s\"}",
                            r.get("child_table"), r.get("child_column"), r.get("parent_table"), r.get("parent_column")));
                    if (i < rels.size() - 1) sb.append(",");
                }
                sb.append("]");
                runOnUIThread(() -> jsExecutor.accept("onERDiagramGenerated(" + sb.toString() + ")"));
            } catch (Exception e) {
                runOnUIThread(() -> jsExecutor.accept("onQueryError('" + e.getMessage().replace("'", "\\'") + "')"));
            }
        }).start();
    }

    public String exportToCSV(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        
        Set<String> keys = data.get(0).keySet();
        sb.append(String.join(",", keys)).append("\n");
        
        for (Map<String, Object> row : data) {
            List<String> values = new java.util.ArrayList<>();
            for (String key : keys) {
                Object val = row.get(key);
                String strVal = (val == null) ? "" : val.toString().replace("\"", "\"\"");
                values.add("\"" + strVal + "\"");
            }
            sb.append(String.join(",", values)).append("\n");
        }
        return sb.toString();
    }

    public String exportToJson(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            sb.append("{");
            int j = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String val = entry.getValue() == null ? "null" : "\"" + entry.getValue().toString().replace("\"", "\\\"").replace("\n", "\\n") + "\"";
                sb.append("\"").append(entry.getKey()).append("\":").append(val);
                if (j < row.size() - 1) sb.append(",");
                j++;
            }
            sb.append("}");
            if (i < data.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    protected void runOnUIThread(Runnable runnable) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            java.lang.reflect.Method runLater = platformClass.getMethod("runLater", Runnable.class);
            runLater.invoke(null, runnable);
        } catch (Exception e) {
            runnable.run();
        }
    }
}
