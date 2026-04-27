package com.finalyear.orchestrator.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * An immutable snapshot of cluster metrics captured at a point in time.
 *
 * <p>Produced by {@link com.finalyear.orchestrator.monitoring.MonitoringService}
 * and consumed by both the Console UI and the JavaFX Dashboard.
 */
public class MetricSnapshot {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final List<Node>    nodes;
    private final long          totalNodes;
    private final long          activeNodes;
    private final long          offlineNodes;
    private final long          runningContainers;
    private final long          failedContainers;
    private final double        clusterCpuPercent;
    private final double        clusterMemPercent;
    private final int           recoveryEvents;

    /**
     * Constructs a snapshot. All node and container data is derived from
     * the {@link Cluster} at the moment of capture.
     */
    public MetricSnapshot(Cluster cluster) {
        this.timestamp         = LocalDateTime.now();
        this.nodes             = cluster.getNodes();
        this.totalNodes        = nodes.size();
        this.activeNodes       = cluster.getActiveNodeCount();
        this.offlineNodes      = cluster.getOfflineNodeCount();
        this.runningContainers = cluster.getRunningContainerCount();
        this.failedContainers  = cluster.getFailedContainerCount();
        this.recoveryEvents    = cluster.getRecoveryEventCount();

        // Cluster-wide CPU and memory averages (healthy nodes only)
        List<Node> healthy = cluster.getHealthyNodes();
        if (!healthy.isEmpty()) {
            this.clusterCpuPercent = healthy.stream()
                    .mapToDouble(Node::getCpuUsagePercent).average().orElse(0);
            this.clusterMemPercent = healthy.stream()
                    .mapToDouble(Node::getMemUsagePercent).average().orElse(0);
        } else {
            this.clusterCpuPercent = 0;
            this.clusterMemPercent = 0;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public LocalDateTime getTimestamp()          { return timestamp; }
    public String        getTimestampString()    { return timestamp.format(FMT); }
    public List<Node>    getNodes()              { return nodes; }
    public long          getTotalNodes()         { return totalNodes; }
    public long          getActiveNodes()        { return activeNodes; }
    public long          getOfflineNodes()       { return offlineNodes; }
    public long          getRunningContainers()  { return runningContainers; }
    public long          getFailedContainers()   { return failedContainers; }
    public double        getClusterCpuPercent()  { return clusterCpuPercent; }
    public double        getClusterMemPercent()  { return clusterMemPercent; }
    public int           getRecoveryEvents()     { return recoveryEvents; }
}
