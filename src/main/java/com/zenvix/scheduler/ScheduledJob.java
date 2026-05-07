package com.zenvix.scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Robust abstract footprint standardizing Cron job states mapping specific 
 * execution chains safely avoiding object drift natively dynamically.
 */
public abstract class ScheduledJob {
    public enum JobType { BUILD_MAVEN, BUILD_GRADLE, BACKUP_DB, HEALTH_REPORT }
    public enum JobResult { PENDING, SUCCESS, FAILURE }

    public static class JobRunHistory {
        public LocalDateTime runTime;
        public JobResult result;
        public long durationMs;
        public String output;

        public JobRunHistory(LocalDateTime runTime, JobResult result, long durationMs, String output) {
            this.runTime = runTime;
            this.result = result;
            this.durationMs = durationMs;
            this.output = output;
        }
    }

    public String id = UUID.randomUUID().toString();
    public String name;
    public String cronExpression;
    public JobType jobType;
    public boolean enabled = true;
    public Map<String, String> config = new ConcurrentHashMap<>();
    
    public LocalDateTime lastRun;
    public JobResult lastResult = JobResult.PENDING;
    public List<JobRunHistory> history = new ArrayList<>();

    public ScheduledJob(String name, String cronExpression, JobType jobType) {
        this.name = name;
        this.cronExpression = cronExpression;
        this.jobType = jobType;
    }

    public void addHistory(JobRunHistory record) {
        lastRun = record.runTime;
        lastResult = record.result;
        history.add(0, record);
        if (history.size() > 10) {
            history.remove(history.size() - 1);
        }
    }

    public abstract JobRunHistory executeJob();
}
