package com.finalyear.orchestrator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a physical (or virtual) machine in the cluster.
 *
 * <p>Tracks:
 * <ul>
 *   <li>Total CPU (cores) and total memory (MB)</li>
 *   <li>Currently allocated CPU and memory (sum of all running containers)</li>
 *   <li>The list of {@link ContainerInstance}s placed on this node</li>
 *   <li>The node's health status ({@link NodeStatus})</li>
 * </ul>
 */
public class Node {

    // ── Static ID counter ─────────────────────────────────────────────────────
    private static final AtomicLong ID_GEN = new AtomicLong(1);

    // ── Fields ────────────────────────────────────────────────────────────────
    private final long   id;
    private final String name;

    /** Total CPU capacity in cores. */
    private final double totalCpu;

    /** Total memory capacity in megabytes. */
    private final double totalMemory;

    /** CPU currently allocated to running containers (cores). */
    private volatile double allocatedCpu;

    /** Memory currently allocated to running containers (MB). */
    private volatile double allocatedMemory;

    /** Health status of this node. */
    private volatile NodeStatus status;

    /** Containers placed on this node. */
    private final List<ContainerInstance> containers = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a new {@code Node} with the given capacity.
     *
     * @param name        human-readable name (e.g. "node-1")
     * @param totalCpu    total CPU cores available
     * @param totalMemory total memory in MB
     */
    public Node(String name, double totalCpu, double totalMemory) {
        this.id          = ID_GEN.getAndIncrement();
        this.name        = name;
        this.totalCpu    = totalCpu;
        this.totalMemory = totalMemory;
        this.status      = NodeStatus.HEALTHY;
    }

    // ── Resource helpers ──────────────────────────────────────────────────────

    /** @return available CPU cores (total minus allocated). */
    public synchronized double getAvailableCpu() {
        return totalCpu - allocatedCpu;
    }

    /** @return available memory in MB (total minus allocated). */
    public synchronized double getAvailableMemory() {
        return totalMemory - allocatedMemory;
    }

    /**
     * Calculates a utilisation score in [0.0, 1.0] combining CPU and memory.
     * Used by the scheduler to pick the least-loaded node.
     */
    public synchronized double getUtilizationScore() {
        double cpuUtil = (totalCpu    > 0) ? allocatedCpu    / totalCpu    : 0;
        double memUtil = (totalMemory > 0) ? allocatedMemory / totalMemory : 0;
        return (cpuUtil + memUtil) / 2.0;
    }

    /** CPU utilization as a percentage [0..100]. */
    public synchronized double getCpuUsagePercent() {
        return (totalCpu > 0) ? (allocatedCpu / totalCpu) * 100.0 : 0;
    }

    /** Memory utilization as a percentage [0..100]. */
    public synchronized double getMemUsagePercent() {
        return (totalMemory > 0) ? (allocatedMemory / totalMemory) * 100.0 : 0;
    }

    // ── Container management ──────────────────────────────────────────────────

    /**
     * Adds a container to this node, updating allocated resources.
     *
     * @param c the container to add
     */
    public synchronized void addContainer(ContainerInstance c) {
        containers.add(c);
        allocatedCpu    += c.getRequestedCpu();
        allocatedMemory += c.getRequestedMemory();
        c.setAssignedNode(this);
        c.setStatus(ContainerStatus.RUNNING);
    }

    /**
     * Removes a container from this node, freeing its resources.
     *
     * @param c the container to remove
     */
    public synchronized void removeContainer(ContainerInstance c) {
        if (containers.remove(c)) {
            allocatedCpu    = Math.max(0, allocatedCpu    - c.getRequestedCpu());
            allocatedMemory = Math.max(0, allocatedMemory - c.getRequestedMemory());
            c.setAssignedNode(null);
        }
    }

    /** Returns an unmodifiable snapshot of the containers on this node. */
    public synchronized List<ContainerInstance> getContainers() {
        return Collections.unmodifiableList(new ArrayList<>(containers));
    }

    /** Returns the number of containers currently on this node. */
    public synchronized int getContainerCount() {
        return containers.size();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public long         getId()            { return id; }
    public String       getName()          { return name; }
    public double       getTotalCpu()      { return totalCpu; }
    public double       getTotalMemory()   { return totalMemory; }
    public NodeStatus   getStatus()        { return status; }
    public void         setStatus(NodeStatus s) { this.status = s; }

    public synchronized double getAllocatedCpu()    { return allocatedCpu; }
    public synchronized double getAllocatedMemory() { return allocatedMemory; }

    @Override
    public String toString() {
        return String.format("Node[%s | CPU %.1f/%.1f | Mem %.0f/%.0fMB | %s | containers=%d]",
                name, allocatedCpu, totalCpu, allocatedMemory, totalMemory,
                status, containers.size());
    }
}
