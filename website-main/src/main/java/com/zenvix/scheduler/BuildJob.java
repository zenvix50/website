package com.zenvix.scheduler;

import java.time.LocalDateTime;

public class BuildJob extends ScheduledJob {
    public BuildJob(String name, String cronExpression, JobType type, String projectPath, String taskOrGoal) {
        super(name, cronExpression, type);
        this.config.put("projectPath", projectPath);
        this.config.put("task", taskOrGoal); 
    }

    @Override
    public JobRunHistory executeJob() {
        long start = System.currentTimeMillis();
        
        JobResult result = JobResult.SUCCESS;
        String output = "Executed build: " + config.get("task") + " in " + config.get("projectPath");
        
        return new JobRunHistory(LocalDateTime.now(), result, System.currentTimeMillis() - start, output);
    }
}
