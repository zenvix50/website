package com.zenvix.tools.profiler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JVMProfilerTest {

    @TempDir
    Path tempDir;

    private JVMProfiler profiler;

    @Mock
    private MBeanServerConnection mockMbsc;

    @Mock
    private CompositeData mockHeapUsage;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        profiler = new JVMProfiler(tempDir.toString());
        profiler.setMBeanServerConnection(mockMbsc);
    }

    @AfterEach
    public void tearDown() throws Exception {
        profiler.close();
        closeable.close();
    }

    @Test
    public void testCollectStats_returnsHeapData() throws Exception {
        when(mockMbsc.getAttribute(any(ObjectName.class), eq("HeapMemoryUsage"))).thenReturn(mockHeapUsage);
        when(mockHeapUsage.get("used")).thenReturn(1024L);
        when(mockHeapUsage.get("max")).thenReturn(4096L);
        when(mockHeapUsage.get("committed")).thenReturn(2048L);

        Map<String, Long> data = profiler.getHeapData();
        assertEquals(1024L, data.get("used"));
        assertEquals(4096L, data.get("max"));
        assertEquals(2048L, data.get("committed"));
    }

    @Test
    public void testThreadCount_returnsAccurateCount() throws Exception {
        when(mockMbsc.getAttribute(any(ObjectName.class), eq("ThreadCount"))).thenReturn(15);
        when(mockMbsc.getAttribute(any(ObjectName.class), eq("DaemonThreadCount"))).thenReturn(10);
        when(mockMbsc.getAttribute(any(ObjectName.class), eq("PeakThreadCount"))).thenReturn(20);

        Map<String, Integer> stats = profiler.getThreadStats();
        assertEquals(15, stats.get("threadCount"));
        assertEquals(10, stats.get("daemonThreadCount"));
        assertEquals(20, stats.get("peakThreadCount"));
    }

    @Test
    public void testGCStats_returnsGCActivity() throws Exception {
        Set<ObjectName> gcBeans = new HashSet<>();
        ObjectName g1 = new ObjectName("java.lang:type=GarbageCollector,name=G1 Young Generation");
        gcBeans.add(g1);

        when(mockMbsc.queryNames(any(ObjectName.class), isNull())).thenReturn(gcBeans);
        when(mockMbsc.getAttribute(g1, "Name")).thenReturn("G1 Young Generation");
        when(mockMbsc.getAttribute(g1, "CollectionCount")).thenReturn(5L);
        when(mockMbsc.getAttribute(g1, "CollectionTime")).thenReturn(100L);

        List<Map<String, Object>> stats = profiler.getGCStats();
        assertEquals(1, stats.size());
        assertEquals("G1 Young Generation", stats.get(0).get("name"));
        assertEquals(5L, stats.get(0).get("collectionCount"));
        assertEquals(100L, stats.get(0).get("collectionTime"));
    }

    @Test
    public void testHeapDump_generatesDumpFile() throws Exception {
        String path = profiler.dumpHeap(true);
        verify(mockMbsc).invoke(
            any(ObjectName.class), 
            eq("dumpHeap"), 
            any(Object[].class), 
            any(String[].class)
        );
        assertTrue(path.contains("heapdump-"));
        assertTrue(path.endsWith(".hprof"));
    }

    @Test
    public void testJMXConnect_connectsToRemoteProcess() {
        JVMProfiler localProfiler = new JVMProfiler();
        assertNotNull(localProfiler);
        
        Exception e = assertThrows(UnsupportedOperationException.class, () -> {
            localProfiler.connectByPid(1234);
        });
        assertTrue(e.getMessage().contains("PID connection requires"));
    }
}
