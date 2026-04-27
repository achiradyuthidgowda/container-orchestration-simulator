package com.finalyear.orchestrator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a Deployment – the user-facing specification of a workload.
 *
 * <p>A deployment declares:
 * <ul>
 *   <li>The container image / app name</li>
 *   <li>Desired replica count</li>
 *   <li>Per-container resource requests (CPU + memory)</li>
 * </ul>
 *
 * <p>The {@link com.finalyear.orchestrator.controlplane.OrchestratorEngine}
 * reconciles actual vs desired replicas by creating or removing
 * {@link ContainerInstance}s.
 */
public class Deployment {

    // ── Static ID counter ─────────────────────────────────────────────────────
    private static final AtomicLong ID_GEN = new AtomicLong(1);

    // ── Fields ────────────────────────────────────────────────────────────────
    private final long   id;
    private final String name;

    /** Docker-image-like label (e.g. "nginx:latest"). */
    private final String image;

    /** How many replicas the user (or autoscaler) wants. */
    private volatile int desiredReplicas;

    /** Minimum replicas – autoscaler will not go below this. */
    private final int minReplicas;

    /** Maximum replicas – autoscaler will not exceed this. */
    private final int maxReplicas;

    /** CPU cores each replica requests. */
    private final double requestedCpu;

    /** Memory MB each replica requests. */
    private final double requestedMemory;

    /** Live container instances managed by this deployment. */
    private final List<ContainerInstance> instances = new ArrayList<>();

    /** Running count of scale-out events (used in final report). */
    private volatile int scaleOutCount;

    /** Running count of scale-in events (used in final report). */
    private volatile int scaleInCount;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param name            deployment name (e.g. "nginx-deployment")
     * @param image           container image label
     * @param desiredReplicas initial replica count
     * @param minReplicas     autoscaler lower bound
     * @param maxReplicas     autoscaler upper bound
     * @param requestedCpu    CPU cores per replica
     * @param requestedMemory memory MB per replica
     */
    public Deployment(String name, String image,
                      int desiredReplicas, int minReplicas, int maxReplicas,
                      double requestedCpu, double requestedMemory) {
        this.id              = ID_GEN.getAndIncrement();
        this.name            = name;
        this.image           = image;
        this.desiredReplicas = desiredReplicas;
        this.minReplicas     = minReplicas;
        this.maxReplicas     = maxReplicas;
        this.requestedCpu    = requestedCpu;
        this.requestedMemory = requestedMemory;
    }

    // ── Instance management ───────────────────────────────────────────────────

    /** Adds a container instance to this deployment's managed list. */
    public synchronized void addInstance(ContainerInstance c) {
        instances.add(c);
    }

    /** Removes a container instance from this deployment's managed list. */
    public synchronized void removeInstance(ContainerInstance c) {
        instances.remove(c);
    }

    /** Returns an unmodifiable snapshot of live instances. */
    public synchronized List<ContainerInstance> getInstances() {
        return Collections.unmodifiableList(new ArrayList<>(instances));
    }

    /** Returns count of instances whose status is RUNNING. */
    public synchronized int getRunningCount() {
        return (int) instances.stream()
                .filter(c -> c.getStatus() == ContainerStatus.RUNNING)
                .count();
    }

    /**
     * Calculates the average simulated load across all RUNNING instances.
     *
     * @return average load in [0.0, 1.0], or 0 if no running instances
     */
    public synchronized double getAverageLoad() {
        List<ContainerInstance> running = instances.stream()
                .filter(c -> c.getStatus() == ContainerStatus.RUNNING)
                .toList();
        if (running.isEmpty()) return 0;
        return running.stream().mapToDouble(ContainerInstance::getSimulatedLoad).average().orElse(0);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public long   getId()              { return id; }
    public String getName()            { return name; }
    public String getImage()           { return image; }
    public int    getDesiredReplicas() { return desiredReplicas; }
    public void   setDesiredReplicas(int d) { this.desiredReplicas = d; }
    public int    getMinReplicas()     { return minReplicas; }
    public int    getMaxReplicas()     { return maxReplicas; }
    public double getRequestedCpu()    { return requestedCpu; }
    public double getRequestedMemory() { return requestedMemory; }
    public int    getScaleOutCount()   { return scaleOutCount; }
    public int    getScaleInCount()    { return scaleInCount; }
    public void   incrementScaleOut()  { scaleOutCount++; }
    public void   incrementScaleIn()   { scaleInCount++; }

    @Override
    public String toString() {
        return String.format("Deployment[%s | image=%s | desired=%d | running=%d]",
                name, image, desiredReplicas, getRunningCount());
    }
}
