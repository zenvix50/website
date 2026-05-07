package com.zenvix.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages dynamically scheduled Cron bounds explicitly intercepting 5-field inputs 
 * evaluating raw execution strings directly mapping physical executor closures seamlessly.
 */
public class JobScheduler {

    private final List<ScheduledJob> jobs = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public JobScheduler() {
        this.executor = Executors.newScheduledThreadPool(4);
    }

    public void addJob(ScheduledJob job) {
        jobs.add(job);
        scheduleNext(job);
    }

    public void enableJob(String jobId, boolean enable) {
        for (ScheduledJob job : jobs) {
            if (job.id.equals(jobId)) {
                job.enabled = enable;
                if (enable) {
                    scheduleNext(job);
                } else {
                    ScheduledFuture<?> future = futures.remove(job.id);
                    if (future != null) {
                        future.cancel(false);
                    }
                }
                break;
            }
        }
    }

    public void triggerManual(String jobId) {
        for (ScheduledJob job : jobs) {
            if (job.id.equals(jobId)) {
                executor.submit(() -> runJobInternal(job));
                break;
            }
        }
    }

    protected void scheduleNext(ScheduledJob job) {
        if (!job.enabled) return;

        long delaySeconds = calculateNextExecutionDelaySeconds(job.cronExpression);
        if (delaySeconds < 0) return; 

        ScheduledFuture<?> future = executor.schedule(() -> {
            runJobInternal(job);
            scheduleNext(job); 
        }, delaySeconds, TimeUnit.SECONDS);

        futures.put(job.id, future);
    }

    protected void runJobInternal(ScheduledJob job) {
        ScheduledJob.JobRunHistory history = job.executeJob();
        job.addHistory(history);
        saveJobs();
    }

    private void saveJobs() {
    }

    public long calculateNextExecutionDelaySeconds(String cron) {
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 5) return -1; 

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);

        for (int i = 0; i < 5 * 365 * 24 * 60; i++) {
            if (matchesCron(next, parts)) {
                return ChronoUnit.SECONDS.between(now, next);
            }
            next = next.plusMinutes(1);
        }
        return -1;
    }

    protected boolean matchesCron(LocalDateTime time, String[] parts) {
        return matchField(time.getMinute(), parts[0]) &&
               matchField(time.getHour(), parts[1]) &&
               matchField(time.getDayOfMonth(), parts[2]) &&
               matchField(time.getMonthValue(), parts[3]) &&
               matchField(time.getDayOfWeek().getValue() % 7, convertWeekday(parts[4]));
    }

    private String convertWeekday(String part) {
        if (part.equals("7")) return "0"; 
        return part;
    }

    private boolean matchField(int value, String expression) {
        if (expression.equals("*")) return true;
        if (expression.contains(",")) {
            for (String sub : expression.split(",")) {
                if (matchField(value, sub)) return true;
            }
            return false;
        }
        if (expression.contains("/")) {
            String[] split = expression.split("/");
            if (split[0].equals("*")) {
                int step = Integer.parseInt(split[1]);
                return value % step == 0;
            }
        }
        if (expression.contains("-")) {
            String[] split = expression.split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            return value >= min && value <= max;
        }
        return value == Integer.parseInt(expression);
    }

    public List<ScheduledJob> getJobs() {
        return jobs;
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
