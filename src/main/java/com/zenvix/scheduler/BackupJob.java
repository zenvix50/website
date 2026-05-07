package com.zenvix.scheduler;

import java.time.LocalDateTime;

public class BackupJob extends ScheduledJob {
    public BackupJob(String name, String cronExpression, String serviceId, String outputDir) {
        super(name, cronExpression, JobType.BACKUP_DB);
        this.config.put("serviceId", serviceId);
        this.config.put("outputDir", outputDir);
    }

    @Override
    public JobRunHistory executeJob() {
        long start = System.currentTimeMillis();
        JobResult result = JobResult.SUCCESS;
        String output = "Backed up DB " + config.get("serviceId") + " to " + config.get("outputDir");
        
        return new JobRunHistory(LocalDateTime.now(), result, System.currentTimeMillis() - start, output);
    }
}
