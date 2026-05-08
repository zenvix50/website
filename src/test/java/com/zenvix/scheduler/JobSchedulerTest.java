package com.zenvix.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JobSchedulerTest {

    private JobScheduler scheduler;

    @BeforeEach
    public void setUp() {
        scheduler = new JobScheduler();
    }

    @Test
    public void testCronParse_parsesExpression() {
        long delay = scheduler.calculateNextExecutionDelaySeconds("* * * * *");
        assertTrue(delay > 0 && delay <= 60);

        String[] parts = "0 * * * *".split(" ");
        LocalDateTime nextHour = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
        assertTrue(scheduler.matchesCron(nextHour, parts));
    }

    @Test
    public void testSchedule_executesAtCorrectTime() throws Exception {
        ScheduledJob job = new BuildJob("Test", "* * * * *", ScheduledJob.JobType.BUILD_MAVEN, "/path", "clean");
        
        JobScheduler mockScheduler = new JobScheduler() {
            @Override
            public long calculateNextExecutionDelaySeconds(String cron) {
                return 1; 
            }
        };
        
        mockScheduler.addJob(job);
        
        Thread.sleep(1500); 
        
        assertEquals(1, job.history.size());
        assertEquals(ScheduledJob.JobResult.SUCCESS, job.lastResult);
        
        mockScheduler.shutdown();
    }

    @Test
    public void testJobHistory_recordsResult() {
        ScheduledJob job = new BackupJob("DB", "0 0 * * *", "mysql", "/backup");
        job.executeJob();
        job.addHistory(new ScheduledJob.JobRunHistory(LocalDateTime.now(), ScheduledJob.JobResult.SUCCESS, 100, "Done"));
        
        assertEquals(1, job.history.size());
        assertEquals("Done", job.history.get(0).output);
        assertEquals(ScheduledJob.JobResult.SUCCESS, job.lastResult);
    }

    @Test
    public void testManualTrigger_runsJobImmediately() throws Exception {
        ScheduledJob job = new HealthReportJob("Report", "0 0 * * *", "/report", "JSON");
        scheduler.addJob(job);
        
        scheduler.triggerManual(job.id);
        
        Thread.sleep(500);
        
        assertEquals(1, job.history.size());
        assertTrue(job.history.get(0).output.contains("Generated JSON"));
    }

    @Test
    public void testDisable_skipsJobExecution() throws Exception {
        ScheduledJob job = new BackupJob("DB", "* * * * *", "mysql", "/backup");
        
        JobScheduler mockScheduler = new JobScheduler() {
            @Override
            public long calculateNextExecutionDelaySeconds(String cron) {
                return 1;
            }
        };
        
        mockScheduler.addJob(job);
        mockScheduler.enableJob(job.id, false);
        
        Thread.sleep(1500);
        
        assertEquals(0, job.history.size(), "Job should not execute when disabled");
        mockScheduler.shutdown();
    }
}
