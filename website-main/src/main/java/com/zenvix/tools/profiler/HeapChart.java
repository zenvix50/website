package com.zenvix.tools.profiler;

import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HeapChart visualizes JVM memory usage (Used vs Max Heap) over time.
 * Automatically synchronizes multithreaded Executor background events accurately directly onto the JavaFX render thread.
 */
public class HeapChart {

    private final JVMProfiler profiler;
    private final LineChart<Number, Number> lineChart;
    private final XYChart.Series<Number, Number> usedSeries;
    private final XYChart.Series<Number, Number> maxSeries;
    
    private final ScheduledExecutorService scheduler;
    private int timePoint = 0;

    public HeapChart(JVMProfiler profiler) {
        this.profiler = profiler;

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setTickUnit(10);
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Memory (MB)");
        
        this.lineChart = new LineChart<>(xAxis, yAxis);
        this.lineChart.setTitle("JVM Heap Usage");
        this.lineChart.setCreateSymbols(false);
        this.lineChart.setAnimated(false);

        usedSeries = new XYChart.Series<>();
        usedSeries.setName("Used Heap");

        maxSeries = new XYChart.Series<>();
        maxSeries.setName("Max Heap");

        lineChart.getData().add(usedSeries);
        lineChart.getData().add(maxSeries);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public void startPolling() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Long> heapData = profiler.getHeapData();
                long usedMb = heapData.get("used") / (1024 * 1024);
                long maxMb = heapData.get("max") / (1024 * 1024);
                
                int currentX = timePoint;
                timePoint += 2;

                runOnUIThread(() -> {
                    usedSeries.getData().add(new XYChart.Data<>(currentX, usedMb));
                    maxSeries.getData().add(new XYChart.Data<>(currentX, maxMb));
                    
                    if (usedSeries.getData().size() > 30) { 
                        usedSeries.getData().remove(0);
                        maxSeries.getData().remove(0);
                    }
                    
                    NumberAxis xAxis = (NumberAxis) lineChart.getXAxis();
                    xAxis.setLowerBound(Math.max(0, currentX - 60));
                    xAxis.setUpperBound(Math.max(60, currentX));
                });
            } catch (Exception e) { /* ignore disconnections cleanly */ }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        scheduler.shutdownNow();
    }

    public LineChart<Number, Number> getChart() {
        return lineChart;
    }

    protected void runOnUIThread(Runnable runnable) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            java.lang.reflect.Method runLater = platformClass.getMethod("runLater", Runnable.class);
            runLater.invoke(null, runnable);
        } catch (Exception | java.lang.Error e) {
            runnable.run();
        }
    }
}
