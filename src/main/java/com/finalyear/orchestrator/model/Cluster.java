package com.finalyear.orchestrator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The top-level model object that holds all nodes and deployments.
 *
 * <p>All controller classes work against this shared {@code Cluster} instance.
 * Methods are synchronized where necessary to support concurrent access from
 * the reconciliation loop and the UI threads.
 */
public class Cluster {

    /** Human-readable cluster name. */
    private final String name;

    private final List<Node>       nodes       = new ArrayList<>();
    private final List<Deployment> deployments = new ArrayList<>();

    /** Running count of recovery events (node or container). */
    private volatile int recoveryEventCount;

    public Cluster(String name) {
        this.name = name;
    }

    // ── Node management ───────────────────────────────────────────────────────

    public synchronized void addNode(Node node) {
        nodes.add(node);
    }

    public synchronized List<Node> getNodes() {
        return Collections.unmodifiableList(new ArrayList<>(nodes));
    }

    /** Returns all nodes that are currently HEALTHY. */
    public synchronized List<Node> getHealthyNodes() {
        return nodes.stream()
                .filter(n -> n.getStatus() == NodeStatus.HEALTHY)
                .toList();
    }

    /** Returns count of HEALTHY nodes. */
    public synchronized long getActiveNodeCount() {
        return nodes.stream().filter(n -> n.getStatus() == NodeStatus.HEALTHY).count();
    }

    /** Returns count of FAILED nodes. */
    public synchronized long getOfflineNodeCount() {
        return nodes.stream().filter(n -> n.getStatus() == NodeStatus.FAILED).count();
    }

    // ── Deployment management ─────────────────────────────────────────────────

    public synchronized void addDeployment(Deployment deployment) {
        deployments.add(deployment);
    }

    public synchronized List<Deployment> getDeployments() {
        return Collections.unmodifiableList(new ArrayList<>(deployments));
    }

    // ── Aggregate metrics ─────────────────────────────────────────────────────

    /** Returns total number of containers in RUNNING state across all nodes. */
    public synchronized long getRunningContainerCount() {
        return nodes.stream()
                .flatMap(n -> n.getContainers().stream())
                .filter(c -> c.getStatus() == ContainerStatus.RUNNING)
                .count();
    }

    /** Returns total number of containers in FAILED state across all deployments. */
    public synchronized long getFailedContainerCount() {
        return deployments.stream()
                .flatMap(d -> d.getInstances().stream())
                .filter(c -> c.getStatus() == ContainerStatus.FAILED)
                .count();
    }

    /** Returns a flat list of every container instance across all deployments. */
    public synchronized List<ContainerInstance> getAllContainers() {
        List<ContainerInstance> all = new ArrayList<>();
        for (Deployment d : deployments) {
            all.addAll(d.getInstances());
        }
        return Collections.unmodifiableList(all);
    }

    // ── Recovery counter ──────────────────────────────────────────────────────

    public void incrementRecoveryEvents()  { recoveryEventCount++; }
    public int  getRecoveryEventCount()    { return recoveryEventCount; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getName() { return name; }

    @Override
    public String toString() {
        return String.format("Cluster[%s | nodes=%d | deployments=%d]",
                name, nodes.size(), deployments.size());
    }
}
