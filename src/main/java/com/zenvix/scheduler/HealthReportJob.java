package com.zenvix.scheduler;

import java.time.LocalDateTime;

public class HealthReportJob extends ScheduledJob {
    public HealthReportJob(String name, String cronExpression, String outputDir, String format) {
        super(name, cronExpression, JobType.HEALTH_REPORT);
        this.config.put("outputDir", outputDir);
        this.config.put("format", format);
    }

    @Override
    public JobRunHistory executeJob() {
        long start = System.currentTimeMillis();
        JobResult result = JobResult.SUCCESS;
        String output = "Generated " + config.get("format") + " report in " + config.get("outputDir");
        
        return new JobRunHistory(LocalDateTime.now(), result, System.currentTimeMillis() - start, output);
    }
}
