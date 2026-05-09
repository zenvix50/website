package com.zenvix.monitoring;

import com.zenvix.core.services.ServiceManager;
import com.zenvix.ui.notifications.NotificationManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;

/**
 * HealthMonitor performs real-time execution bounds evaluating physical sockets
 * identifying active states safely bridging background circuit breakers natively.
 */
public class HealthMonitor {

    private final Map<String, ServiceContext> services = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private final NotificationManager notificationManager;
    private int intervalSeconds = 30;

    public static class ServiceContext {
        public String name;
        public ServiceManager manager;
        public String type; 
        public int port;
        public boolean isExpectedRunning;
        public boolean autoRestart;
        
        public HealthState currentState = HealthState.HEALTHY;
        public int consecutiveFailures = 0;
        public int restartAttempts = 0;

        public ServiceContext(String name, ServiceManager manager, String type, int port, boolean autoRestart) {
            this.name = name;
            this.manager = manager;
            this.type = type;
            this.port = port;
            this.autoRestart = autoRestart;
            this.isExpectedRunning = false;
        }
    }

    public HealthMonitor(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void registerService(String name, ServiceManager manager, String type, int port, boolean autoRestart) {
        services.put(name, new ServiceContext(name, manager, type, port, autoRestart));
    }

    public void setExpectedRunning(String name, boolean running) {
        ServiceContext ctx = services.get(name);
        if (ctx != null) {
            ctx.isExpectedRunning = running;
            if (!running) {
                ctx.consecutiveFailures = 0;
                ctx.restartAttempts = 0;
                ctx.currentState = HealthState.HEALTHY;
            }
        }
    }

    public void startMonitoring(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::performHealthChecks, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    protected void performHealthChecks() {
        for (ServiceContext ctx : services.values()) {
            if (!ctx.isExpectedRunning) continue;
            if (ctx.currentState == HealthState.CIRCUIT_OPEN) continue;

            HealthCheckResult result = checkService(ctx);

            if (result.isSuccess) {
                if (ctx.currentState != HealthState.HEALTHY) {
                    notificationManager.notifyInfo("Service Recovered", ctx.name + " is back online.");
                }
                ctx.currentState = HealthState.HEALTHY;
                ctx.consecutiveFailures = 0;
                ctx.restartAttempts = 0;
            } else {
                handleFailure(ctx, result.message);
            }
        }
    }

    protected HealthCheckResult checkService(ServiceContext ctx) {
        switch (ctx.type.toUpperCase()) {
            case "TOMCAT":
            case "NGINX":
                return checkHttp(ctx.port);
            case "REDIS":
                return checkRedis(ctx.port);
            case "MYSQL":
            case "POSTGRESQL":
            case "H2":
            case "MEMCACHED":
            default:
                return checkTcpPort(ctx.port);
        }
    }

    private HealthCheckResult checkTcpPort(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 2000);
            return new HealthCheckResult(true, "OK");
        } catch (Exception e) {
            return new HealthCheckResult(false, "Connection refused");
        }
    }

    private HealthCheckResult checkHttp(int port) {
        try {
            URL url = new URL("http://localhost:" + port + "/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 500) { 
                return new HealthCheckResult(true, "HTTP " + code);
            } else {
                return new HealthCheckResult(false, "HTTP " + code);
            }
        } catch (Exception e) {
            return new HealthCheckResult(false, "HTTP Request failed");
        }
    }

    private HealthCheckResult checkRedis(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 2000);
            socket.setSoTimeout(2000);
            OutputStream out = socket.getOutputStream();
            out.write("PING\r\n".getBytes());
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            if (response != null && response.contains("PONG")) {
                return new HealthCheckResult(true, "PONG");
            }
            return new HealthCheckResult(false, "Invalid Redis response");
        } catch (Exception e) {
            return new HealthCheckResult(false, "Redis PING failed");
        }
    }

    private void handleFailure(ServiceContext ctx, String reason) {
        ctx.consecutiveFailures++;

        if (ctx.consecutiveFailures >= 5) {
            ctx.currentState = HealthState.CIRCUIT_OPEN;
            notificationManager.notifyWarning("Circuit Breaker Open", 
                "zenviX stopped monitoring " + ctx.name + " after 5 consecutive failures.");
            return;
        }

        if (ctx.currentState == HealthState.HEALTHY) {
            ctx.currentState = HealthState.CRASHED;
            notificationManager.notifyCrash(ctx.name);
        }

        if (ctx.autoRestart && ctx.restartAttempts < 3) {
            ctx.restartAttempts++;
            notificationManager.notifyInfo("Auto-Restarting", "Attempting to restart " + ctx.name + " (Attempt " + ctx.restartAttempts + ")");
            if (ctx.manager != null) {
                new Thread(() -> {
                    try {
                        ctx.manager.stop();
                        Thread.sleep(1000);
                        ctx.manager.start();
                    } catch (Exception e) {}
                }).start();
            }
        }
    }

    public HealthState getServiceState(String name) {
        ServiceContext ctx = services.get(name);
        return ctx != null ? ctx.currentState : HealthState.HEALTHY;
    }
    
    protected void triggerChecksNow() {
        performHealthChecks();
    }
}
