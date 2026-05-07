package com.zenvix.core.services;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * PostgreSQLManager handles the lifecycle, configuration, and administration of PostgreSQL servers.
 * Supports PostgreSQL 15 and 16. Implements role management and pg_hba.conf editing.
 */
public class PostgreSQLManager extends ServiceManager {

    private String baseDir = "zenvix";
    private int port = 5432;
    private boolean isRunning = false;

    public PostgreSQLManager() {
        this("zenvix");
    }

    public PostgreSQLManager(String baseDir) {
        this.baseDir = baseDir;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) stop();
            } catch (Exception e) {
                System.err.println("Failed to stop PostgreSQL on shutdown: " + e.getMessage());
            }
        }));
    }

    // --- Path Management ---

    public String getPgHome() {
        return Paths.get(baseDir, "postgresql").toAbsolutePath().toString();
    }

    public String getBinPath(String binName) {
        String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        return Paths.get(getPgHome(), "bin", binName + exeExt).toString();
    }

    public String getDataDir() {
        return Paths.get(getPgHome(), "data").toAbsolutePath().toString();
    }

    public String getPidFile() {
        return Paths.get(getDataDir(), "postmaster.pid").toAbsolutePath().toString();
    }

    public String getHbaConfPath() {
        return Paths.get(getDataDir(), "pg_hba.conf").toAbsolutePath().toString();
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGDATA", getDataDir());
        pb.environment().put("PGPORT", String.valueOf(port));
        return pb;
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // --- Data Directory Initialization ---

    public void initDbIfNecessary() throws Exception {
        Path dataPath = Paths.get(getDataDir());
        if (!Files.exists(dataPath) || !Files.isDirectory(dataPath) || !Files.exists(dataPath.resolve("PG_VERSION"))) {
            Files.createDirectories(dataPath);
            
            String defaultPassword = UUID.randomUUID().toString().replace("-", "");
            SecretsVault.set("postgresql.postgres.password", defaultPassword);
            
            Path pwFile = Paths.get(getPgHome(), "pgpass_init.txt");
            Files.write(pwFile, defaultPassword.getBytes());

            ProcessBuilder pb = createProcessBuilder(getBinPath("initdb"), "-U", "postgres", "--pwfile=" + pwFile.toAbsolutePath().toString(), "--auth=scram-sha-256");
            Process p = pb.start();
            p.waitFor(30, TimeUnit.SECONDS);

            Files.deleteIfExists(pwFile);
            
            if (p.exitValue() != 0) {
                throw new Exception("initdb failed with exit code " + p.exitValue());
            }

            Path confFile = dataPath.resolve("postgresql.conf");
            if (Files.exists(confFile)) {
                String conf = new String(Files.readAllBytes(confFile));
                conf += "\nlisten_addresses = '*'\n";
                conf += "port = " + port + "\n";
                Files.write(confFile, conf.getBytes());
            }
        }
    }

    // --- Lifecycle Management ---

    @Override
    public void start() throws Exception {
        if (!isPortAvailable(port)) {
            throw new Exception("Port " + port + " is already in use.");
        }

        initDbIfNecessary();

        Path logDir = Paths.get(getDataDir(), "log");
        if (!Files.exists(logDir)) Files.createDirectories(logDir);

        ProcessBuilder pb = createProcessBuilder(getBinPath("pg_ctl"), "start", "-w", "-t", "30", "-l", logDir.resolve("postgresql.log").toString());
        
        Process p = pb.start();
        p.waitFor(35, TimeUnit.SECONDS);

        if (p.exitValue() != 0) {
            throw new Exception("pg_ctl start failed. Check logs.");
        }

        this.isRunning = true;
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        ProcessBuilder pb = createProcessBuilder(getBinPath("pg_ctl"), "stop", "-m", "fast");
        Process p = pb.start();
        p.waitFor(30, TimeUnit.SECONDS);
        
        if (p.exitValue() != 0) {
            throw new Exception("pg_ctl stop failed.");
        }
        
        this.isRunning = false;
    }

    @Override
    public void restart() throws Exception {
        stop();
        start();
    }

    @Override
    public String getStatus() throws Exception {
        ProcessBuilder pb = createProcessBuilder(getBinPath("pg_ctl"), "status");
        Process p = pb.start();
        p.waitFor(5, TimeUnit.SECONDS);
        return p.exitValue() == 0 ? "RUNNING" : "STOPPED";
    }

    public boolean isRunning() {
        try {
            return "RUNNING".equals(getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getLogs() throws Exception {
        Path logPath = Paths.get(getDataDir(), "log", "postgresql.log");
        if (!Files.exists(logPath)) return "";
        List<String> lines = Files.readAllLines(logPath);
        int tail = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
    }

    @Override
    public String getMetrics() throws Exception {
        return "Metrics retrieval via pg_stat_activity not implemented yet.";
    }

    // --- pg_hba.conf Editor ---

    public static class HbaRule {
        public String type;
        public String database;
        public String user;
        public String address;
        public String method;

        public HbaRule(String type, String database, String user, String address, String method) {
            this.type = type; 
            this.database = database; 
            this.user = user; 
            this.address = address; 
            this.method = method;
        }

        @Override
        public String toString() {
            if (type.equals("local")) {
                return String.join("\t", type, database, user, method);
            }
            return String.join("\t", type, database, user, address, method);
        }
        
        public boolean matches(HbaRule other) {
            return this.type.equals(other.type) && this.database.equals(other.database) && 
                   this.user.equals(other.user) && 
                   (this.type.equals("local") || this.address.equals(other.address));
        }
    }

    public List<HbaRule> getHbaRules() throws IOException {
        List<HbaRule> rules = new ArrayList<>();
        Path hbaPath = Paths.get(getHbaConfPath());
        if (!Files.exists(hbaPath)) return rules;

        for (String line : Files.readAllLines(hbaPath)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            if (parts.length >= 4) {
                if (parts[0].equals("local")) {
                    rules.add(new HbaRule(parts[0], parts[1], parts[2], null, parts[3]));
                } else if (parts.length >= 5) {
                    rules.add(new HbaRule(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        }
        return rules;
    }

    public void addHbaRule(HbaRule newRule) throws Exception {
        Path hbaPath = Paths.get(getHbaConfPath());
        Path tempPath = Paths.get(getHbaConfPath() + ".tmp");
        
        List<String> lines = Files.exists(hbaPath) ? Files.readAllLines(hbaPath) : new ArrayList<>();
        lines.add(newRule.toString());
        
        Files.write(tempPath, lines);
        Files.move(tempPath, hbaPath, StandardCopyOption.REPLACE_EXISTING);
        
        if (isRunning()) {
            ProcessBuilder pb = createProcessBuilder(getBinPath("pg_ctl"), "reload");
            pb.start().waitFor();
        }
    }

    public void removeHbaRule(HbaRule ruleToRemove) throws Exception {
        Path hbaPath = Paths.get(getHbaConfPath());
        Path tempPath = Paths.get(getHbaConfPath() + ".tmp");
        if (!Files.exists(hbaPath)) return;
        
        List<String> lines = Files.readAllLines(hbaPath);
        List<String> newLines = new ArrayList<>();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                newLines.add(line);
                continue;
            }
            
            String[] parts = trimmed.split("\\s+");
            HbaRule parsed = null;
            if (parts[0].equals("local") && parts.length >= 4) {
                parsed = new HbaRule(parts[0], parts[1], parts[2], null, parts[3]);
            } else if (parts.length >= 5) {
                parsed = new HbaRule(parts[0], parts[1], parts[2], parts[3], parts[4]);
            }
            
            if (parsed == null || !parsed.matches(ruleToRemove)) {
                newLines.add(line);
            }
        }
        
        Files.write(tempPath, newLines);
        Files.move(tempPath, hbaPath, StandardCopyOption.REPLACE_EXISTING);
        
        if (isRunning()) {
            ProcessBuilder pb = createProcessBuilder(getBinPath("pg_ctl"), "reload");
            pb.start().waitFor();
        }
    }

    // --- Backup & Restore ---

    public void backupDatabase(String dbName, String backupFilePath) throws Exception {
        String password = SecretsVault.get("postgresql.postgres.password");
        ProcessBuilder pb = createProcessBuilder(getBinPath("pg_dump"), "-U", "postgres", "-d", dbName, "-F", "c", "-f", backupFilePath);
        pb.environment().put("PGPASSWORD", password != null ? password : "");
        Process p = pb.start();
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new Exception("pg_dump failed with exit code " + p.exitValue());
        }
    }

    public void restoreDatabase(String dbName, String backupFilePath) throws Exception {
        String password = SecretsVault.get("postgresql.postgres.password");
        ProcessBuilder pb = createProcessBuilder(getBinPath("pg_restore"), "-U", "postgres", "-d", dbName, "-1", backupFilePath);
        pb.environment().put("PGPASSWORD", password != null ? password : "");
        Process p = pb.start();
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new Exception("pg_restore failed with exit code " + p.exitValue());
        }
    }

    // --- Role Management via JDBC ---
    
    protected Connection getConnection() throws SQLException {
        String password = SecretsVault.get("postgresql.postgres.password");
        String url = "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
        return DriverManager.getConnection(url, "postgres", password != null ? password : "");
    }

    public void createRole(String roleName, String password, boolean isSuperuser) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String sql = "CREATE ROLE " + roleName + " WITH LOGIN PASSWORD '" + password + "'";
            if (isSuperuser) sql += " SUPERUSER";
            stmt.execute(sql);
        }
    }

    public void dropRole(String roleName) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ROLE " + roleName);
        }
    }

    public void grantPrivileges(String roleName, String database, String privileges) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("GRANT " + privileges + " ON DATABASE " + database + " TO " + roleName);
        }
    }

    public List<String> listRoles() throws Exception {
        List<String> roles = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT rolname FROM pg_roles")) {
            while (rs.next()) {
                roles.add(rs.getString(1));
            }
        }
        return roles;
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
