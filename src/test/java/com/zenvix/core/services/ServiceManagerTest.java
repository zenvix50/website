package com.zenvix.core.services;

import org.junit.jupiter.api.Test;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceManagerTest {
    
    @Test
    public void testPortAvailability() {
        ServiceManager sm = new ServiceManager() {
            public void start() {} public void stop() {} public void restart() {}
            public ServiceStatus getStatus() { return null; }
            public String getLogs() { return null; }
            public ServiceMetrics getMetrics() { return null; }
        };
        assertTrue(sm.isPortAvailable(0));
    }

    @Test
    public void testRetryWithBackoff() throws Exception {
        ServiceManager sm = new ServiceManager() {
            public void start() {} public void stop() {} public void restart() {}
            public ServiceStatus getStatus() { return null; }
            public String getLogs() { return null; }
            public ServiceMetrics getMetrics() { return null; }
        };
        
        int[] attempts = {0};
        Callable<Boolean> action = () -> {
            attempts[0]++;
            if (attempts[0] < 3) throw new Exception("fail");
            return true;
        };
        
        boolean result = sm.retryWithBackoff(action, 3, 10);
        assertTrue(result);
        assertEquals(3, attempts[0]);
    }
}
