package com.zenvix.core.services;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MySQLManager handles the lifecycle, configuration, and administration of MySQL or MariaDB instances.
 * Implements user privilege management and import/export capabilities.
 * Relies on SecretsVault for secure password retrieval.
 */
public class MySQLManager extends ServiceManager {

    private String baseDir = "zenvix";
    private int port = 3306;
    private Process mysqlProcess;
    private boolean isMariaDB = false;
    private String versionStr = "";
    private boolean isRunning = false;

    public MySQLManager() {
        this("zenvix");
    }

    public MySQLManager(String baseDir) {
        this.baseDir = baseDir;
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) stop();
            } catch (Exception e) {
                System.err.println("Failed to stop MySQL on shutdown: " + e.getMessage());
            }
        }));
    }

    // --- Path Management ---
    
    public String getMysqlHome() {
        return Paths.get(baseDir, "mysql").toAbsolutePath().toString();
    }

    public String getBinPath(String binName) {
        String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        return Paths.get(getMysqlHome(), "bin", binName + exeExt).toString();
    }

    public String getConfigPath() {
        String confFile = System.getProperty("os.name").toLowerCase().contains("win") ? "my.ini" : "my.cnf";
        return Paths.get(getMysqlHome(), confFile).toAbsolutePath().toString();
    }

    public String getDataDir() {
        return Paths.get(getMysqlHome(), "data").toAbsolutePath().toString();
    }

    public String getPidFile() {
        Path config = Paths.get(getConfigPath());
        if (Files.exists(config)) {
            try {
                List<String> lines = Files.readAllLines(config);
                for (String line : lines) {
                    if (line.trim().startsWith("pid-file")) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            return parts[1].trim();
                        }
                    }
                }
            } catch (IOException e) {
                // Ignore config read error, fallback to default
            }
        }
        return Paths.get(getDataDir(), "mysql.pid").toAbsolutePath().toString();
    }

    protected ProcessBuilder createProcessBuilder(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("MYSQL_HOME", getMysqlHome());
        return pb;
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // --- Version Detection ---

    public void detectVersion() throws Exception {
        ProcessBuilder pb = createProcessBuilder(getBinPath("mysqld"), "--version");
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }
        p.waitFor(5, TimeUnit.SECONDS);
        versionStr = output.toString();
        isMariaDB = versionStr.toLowerCase().contains("mariadb");
    }

    // --- Lifecycle Management ---

    @Override
    public void start() throws Exception {
        if (!isPortAvailable(port)) {
            throw new Exception("Port " + port + " is already in use.");
        }

        Files.createDirectories(Paths.get(getDataDir()));
        
        String conf = getConfigPath();
        String mysqld = getBinPath("mysqld");
        
        ProcessBuilder pb = createProcessBuilder(mysqld, "--defaults-file=" + conf, "--port=" + port);
        pb.redirectErrorStream(true);
        mysqlProcess = pb.start();
        
        long startTime = System.currentTimeMillis();
        boolean started = false;
        while (System.currentTimeMillis() - startTime < 30000) {
            if (isRunningInternal()) {
                started = true;
                this.isRunning = true;
                break;
            }
            if (!mysqlProcess.isAlive()) {
                throw new Exception("MySQL process terminated prematurely.");
            }
            Thread.sleep(500);
        }

        if (!started) {
            mysqlProcess.destroyForcibly();
            throw new Exception("MySQL failed to start in time.");
        }
    }

    @Override
    public void stop() throws Exception {
        if (!isRunning()) return;

        String rootPassword = SecretsVault.get("mysql.root.password");
        if (rootPassword == null) rootPassword = "";

        ProcessBuilder pb = createProcessBuilder(getBinPath("mysqladmin"), 
            "-u", "root", 
            "-p" + rootPassword, 
            "-P", String.valueOf(port),
            "-h", "127.0.0.1",
            "shutdown");
        
        Process p = pb.start();
        p.waitFor(15, TimeUnit.SECONDS);

        if (mysqlProcess != null && mysqlProcess.isAlive()) {
            mysqlProcess.destroy();
            mysqlProcess.waitFor(5, TimeUnit.SECONDS);
            if (mysqlProcess.isAlive()) {
                mysqlProcess.destroyForcibly();
            }
        }
        
        Files.deleteIfExists(Paths.get(getPidFile()));
        this.isRunning = false;
    }

    @Override
    public void restart() throws Exception {
        stop();
        start();
    }

    @Override
    public String getStatus() throws Exception {
        return isRunning() ? "RUNNING" : "STOPPED";
    }

    public boolean isRunning() {
        return isRunningInternal();
    }
    
    private boolean isRunningInternal() {
        if (mysqlProcess != null && mysqlProcess.isAlive()) return true;
        File pidFile = new File(getPidFile());
        if (pidFile.exists()) {
            try {
                String pidStr = new String(Files.readAllBytes(pidFile.toPath())).trim();
                return !pidStr.isEmpty();
            } catch (IOException e) {
                // Ignore
            }
        }
        return false;
    }

    @Override
    public String getLogs() throws Exception {
        Path errLog = Paths.get(getDataDir(), "error.log"); 
        if (!Files.exists(errLog)) return "";
        List<String> lines = Files.readAllLines(errLog);
        int tail = Math.min(lines.size(), 100);
        return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
    }

    @Override
    public String getMetrics() throws Exception {
        return "Metrics retrieval via JDBC SHOW STATUS not implemented yet.";
    }

    // --- Import / Export ---

    public void importDatabase(String dbName, String sqlFilePath) throws Exception {
        String rootPassword = SecretsVault.get("mysql.root.password");
        if (rootPassword == null) rootPassword = "";

        ProcessBuilder pb = createProcessBuilder(getBinPath("mysql"), 
            "-u", "root", 
            "-p" + rootPassword, 
            "-P", String.valueOf(port),
            "-h", "127.0.0.1",
            dbName);
            
        pb.redirectInput(new File(sqlFilePath));
        pb.redirectErrorStream(true);
        
        Process p = pb.start();
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new Exception("Database import failed with exit code " + p.exitValue());
        }
    }

    public void exportDatabase(String dbName, String outputSqlPath) throws Exception {
        String rootPassword = SecretsVault.get("mysql.root.password");
        if (rootPassword == null) rootPassword = "";

        ProcessBuilder pb = createProcessBuilder(getBinPath("mysqldump"), 
            "-u", "root", 
            "-p" + rootPassword, 
            "-P", String.valueOf(port),
            "-h", "127.0.0.1",
            "--routines", "--triggers",
            dbName);
            
        pb.redirectOutput(new File(outputSqlPath));
        pb.redirectErrorStream(false); // Do not redirect error to output for clean dumps
        
        Process p = pb.start();
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new Exception("Database export failed with exit code " + p.exitValue());
        }
    }

    // --- JDBC Connection Wrapper ---
    
    protected Connection getConnection() throws SQLException {
        String rootPassword = SecretsVault.get("mysql.root.password");
        String url = "jdbc:mysql://127.0.0.1:" + port + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, "root", rootPassword != null ? rootPassword : "");
    }

    // --- User Privilege Management ---

    public void createUser(String username, String password) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE USER '" + username + "'@'%' IDENTIFIED BY '" + password + "'");
        }
    }

    public void dropUser(String username) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP USER '" + username + "'@'%'");
        }
    }

    public void grantPrivileges(String username, String database, String privileges) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("GRANT " + privileges + " ON " + database + ".* TO '" + username + "'@'%'");
            stmt.execute("FLUSH PRIVILEGES");
        }
    }

    public void revokePrivileges(String username, String database, String privileges) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("REVOKE " + privileges + " ON " + database + ".* FROM '" + username + "'@'%'");
            stmt.execute("FLUSH PRIVILEGES");
        }
    }

    // --- Root Password Management ---

    public void changeRootPassword(String newPassword) throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            if (isMariaDB) {
                stmt.execute("ALTER USER 'root'@'localhost' IDENTIFIED BY '" + newPassword + "'");
                try {
                    stmt.execute("ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY '" + newPassword + "'");
                } catch (SQLException e) { /* ignore */ }
            } else {
                // MySQL 8.x logic
                stmt.execute("ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '" + newPassword + "'");
                try {
                    stmt.execute("ALTER USER 'root'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY '" + newPassword + "'");
                } catch (SQLException e) { /* ignore */ }
            }
            stmt.execute("FLUSH PRIVILEGES");
        }
        SecretsVault.set("mysql.root.password", newPassword);
    }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public void setIsMariaDB(boolean isMariaDB) { this.isMariaDB = isMariaDB; }
}
