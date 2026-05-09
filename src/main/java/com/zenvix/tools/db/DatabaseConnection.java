package com.zenvix.tools.db;

import com.zenvix.core.services.SecretsVault;

import java.sql.*;
import java.util.*;

/**
 * Handles multi-database JDBC connections, query execution, and metadata extraction.
 * Supports MySQL, PostgreSQL, and H2 mapping standard ResultSets to UI JSON outputs.
 */
public class DatabaseConnection implements AutoCloseable {

    private Connection connection;
    private final String dbType;
    private final String host;
    private final int port;
    private final String database;
    private final String username;

    public DatabaseConnection(String dbType, String host, int port, String database, String username) {
        this.dbType = dbType.toLowerCase();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
    }

    public void connect() throws SQLException {
        String url;
        String password = "";
        
        switch (dbType) {
            case "mysql":
                url = "jdbc:mysql://" + host + ":" + port + "/" + database;
                password = SecretsVault.get("mysql.root.password");
                break;
            case "postgresql":
                url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                password = SecretsVault.get("postgresql.postgres.password");
                break;
            case "h2":
                url = "jdbc:h2:tcp://" + host + ":" + port + "/~/" + database;
                password = ""; 
                break;
            default:
                throw new SQLException("Unsupported database type: " + dbType);
        }
        
        this.connection = DriverManager.getConnection(url, username, password == null ? "" : password);
    }

    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) {
            boolean isResultSet = stmt.execute(sql);
            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                }
            } else {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("UpdateCount", stmt.getUpdateCount());
                results.add(row);
            }
        }
        return results;
    }

    public List<Map<String, String>> getTableRelationships() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        
        List<Map<String, String>> relationships = new ArrayList<>();
        String query;
        if ("mysql".equals(dbType)) {
            query = "SELECT TABLE_NAME as child_table, COLUMN_NAME as child_column, " +
                    "REFERENCED_TABLE_NAME as parent_table, REFERENCED_COLUMN_NAME as parent_column " +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE REFERENCED_TABLE_NAME IS NOT NULL AND TABLE_SCHEMA = DATABASE()";
        } else if ("postgresql".equals(dbType)) {
            query = "SELECT tc.table_name as child_table, kcu.column_name as child_column, " +
                    "ccu.table_name AS parent_table, ccu.column_name AS parent_column " +
                    "FROM information_schema.table_constraints AS tc " +
                    "JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name " +
                    "JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name " +
                    "WHERE constraint_type = 'FOREIGN KEY'";
        } else {
            query = "SELECT FKTABLE_NAME as child_table, FKCOLUMN_NAME as child_column, " +
                    "PKTABLE_NAME as parent_table, PKCOLUMN_NAME as parent_column " +
                    "FROM INFORMATION_SCHEMA.CROSS_REFERENCES";
        }

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, String> rel = new HashMap<>();
                rel.put("child_table", rs.getString("child_table"));
                rel.put("child_column", rs.getString("child_column"));
                rel.put("parent_table", rs.getString("parent_table"));
                rel.put("parent_column", rs.getString("parent_column"));
                relationships.add(rel);
            }
        }
        return relationships;
    }

    public List<String> getTables() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(database, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    void setConnection(Connection conn) {
        this.connection = conn;
    }
}
