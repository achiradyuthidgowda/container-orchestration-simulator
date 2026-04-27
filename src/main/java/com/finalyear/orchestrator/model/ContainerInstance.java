package com.finalyear.orchestrator.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single running container (analogous to a Kubernetes Pod).
 *
 * <p>Each container belongs to exactly one {@link Deployment} and is
 * placed on exactly one {@link Node} once scheduled.
 */
public class ContainerInstance {

    // ── Static ID counter ─────────────────────────────────────────────────────
    private static final AtomicLong ID_GEN = new AtomicLong(1);

    // ── Fields ────────────────────────────────────────────────────────────────
    private final long   id;
    private final String name;

    /** CPU cores this container requests from a node. */
    private final double requestedCpu;

    /** Memory (MB) this container requests from a node. */
    private final double requestedMemory;

    /** Current lifecycle status. */
    private volatile ContainerStatus status;

    /** The node this container is placed on (null if unscheduled). */
    private volatile Node assignedNode;

    /** How many times this container has been restarted after failure. */
    private volatile int restartCount;

    /**
     * Simulated CPU load in [0.0, 1.0]; updated by the workload generator.
     * Used by the autoscaler to decide scaling actions.
     */
    private volatile double simulatedLoad;

    /** Name of the deployment this container belongs to. */
    private final String deploymentName;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param name            human-readable name, e.g. "nginx-pod-1"
     * @param deploymentName  parent deployment name
     * @param requestedCpu    CPU cores requested
     * @param requestedMemory memory MB requested
     */
    public ContainerInstance(String name, String deploymentName,
                             double requestedCpu, double requestedMemory) {
        this.id             = ID_GEN.getAndIncrement();
        this.name           = name;
        this.deploymentName = deploymentName;
        this.requestedCpu    = requestedCpu;
        this.requestedMemory = requestedMemory;
        this.status          = ContainerStatus.PENDING;
        this.simulatedLoad   = 0.3 + Math.random() * 0.3; // start between 30-60 %
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public long            getId()             { return id; }
    public String          getName()           { return name; }
    public String          getDeploymentName() { return deploymentName; }
    public double          getRequestedCpu()   { return requestedCpu; }
    public double          getRequestedMemory(){ return requestedMemory; }
    public ContainerStatus getStatus()         { return status; }
    public void            setStatus(ContainerStatus s) { this.status = s; }
    public Node            getAssignedNode()   { return assignedNode; }
    public void            setAssignedNode(Node n) { this.assignedNode = n; }
    public int             getRestartCount()   { return restartCount; }
    public void            incrementRestartCount() { restartCount++; }
    public void            resetRestartCount() { restartCount = 0; }
    public double          getSimulatedLoad()  { return simulatedLoad; }
    public void            setSimulatedLoad(double l) { this.simulatedLoad = Math.max(0, Math.min(1, l)); }

    @Override
    public String toString() {
        String node = (assignedNode != null) ? assignedNode.getName() : "unassigned";
        return String.format("Container[%s | %s | node=%s | restarts=%d | load=%.0f%%]",
                name, status, node, restartCount, simulatedLoad * 100);
    }
}
