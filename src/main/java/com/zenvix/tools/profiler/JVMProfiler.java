package com.zenvix.tools.profiler;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * JVMProfiler connects to remote or local Java processes via JMX
 * to gather memory, thread, and garbage collection statistics, and trigger heap dumps.
 */
public class JVMProfiler implements AutoCloseable {

    private JMXConnector jmxConnector;
    private MBeanServerConnection mbsc;
    private String baseDir = "zenvix";

    public JVMProfiler() {
        this.mbsc = ManagementFactory.getPlatformMBeanServer();
    }

    public JVMProfiler(String baseDir) {
        this.baseDir = baseDir;
        this.mbsc = ManagementFactory.getPlatformMBeanServer();
    }

    // --- Connections ---

    public void connect(String host, int port) throws IOException {
        String urlString = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
        JMXServiceURL url = new JMXServiceURL(urlString);
        this.jmxConnector = JMXConnectorFactory.connect(url);
        this.mbsc = jmxConnector.getMBeanServerConnection();
    }

    public void connectByPid(long pid) throws Exception {
        throw new UnsupportedOperationException("PID connection requires jdk.attach API. Use host:port JMX connection instead.");
    }

    // --- Memory Stats ---

    public Map<String, Long> getHeapData() throws Exception {
        ObjectName memoryMBean = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
        javax.management.openmbean.CompositeData heapUsage = (javax.management.openmbean.CompositeData) mbsc.getAttribute(memoryMBean, "HeapMemoryUsage");
        
        Map<String, Long> data = new HashMap<>();
        data.put("used", (Long) heapUsage.get("used"));
        data.put("max", (Long) heapUsage.get("max"));
        data.put("committed", (Long) heapUsage.get("committed"));
        return data;
    }

    // --- Thread Stats ---

    public Map<String, Integer> getThreadStats() throws Exception {
        ObjectName threadMBean = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        
        Map<String, Integer> stats = new HashMap<>();
        stats.put("threadCount", (Integer) mbsc.getAttribute(threadMBean, "ThreadCount"));
        stats.put("daemonThreadCount", (Integer) mbsc.getAttribute(threadMBean, "DaemonThreadCount"));
        stats.put("peakThreadCount", (Integer) mbsc.getAttribute(threadMBean, "PeakThreadCount"));
        return stats;
    }

    // --- GC Stats ---

    public List<Map<String, Object>> getGCStats() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Set<ObjectName> gcMBeans = mbsc.queryNames(new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*"), null);
        
        for (ObjectName gcName : gcMBeans) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", mbsc.getAttribute(gcName, "Name"));
            stats.put("collectionCount", mbsc.getAttribute(gcName, "CollectionCount"));
            stats.put("collectionTime", mbsc.getAttribute(gcName, "CollectionTime"));
            list.add(stats);
        }
        return list;
    }

    // --- Heap Dump ---

    public String dumpHeap(boolean live) throws Exception {
        Path logDir = Paths.get(baseDir, "logs");
        if (!Files.exists(logDir)) Files.createDirectories(logDir);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path dumpFile = logDir.resolve("heapdump-" + timestamp + ".hprof");
        
        ObjectName hotspotMBean = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
        mbsc.invoke(hotspotMBean, "dumpHeap", 
            new Object[]{dumpFile.toAbsolutePath().toString(), live}, 
            new String[]{String.class.getName(), boolean.class.getName()});
            
        return dumpFile.toAbsolutePath().toString();
    }

    @Override
    public void close() throws Exception {
        if (jmxConnector != null) {
            jmxConnector.close();
        }
    }
    
    // Testing boundary
    void setMBeanServerConnection(MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }
}
