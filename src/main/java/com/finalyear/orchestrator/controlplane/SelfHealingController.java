package com.finalyear.orchestrator.controlplane;

import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.*;

import java.util.List;

/**
 * Self-healing controller that monitors all containers and takes corrective action
 * when a container transitions to {@link ContainerStatus#FAILED}.
 *
 * <p>Recovery strategy:
 * <ol>
 *   <li>If {@code restartCount < MAX_RESTARTS}: restart the container in-place
 *       (increment restart counter, set status back to RUNNING if node is healthy).</li>
 *   <li>If {@code restartCount >= MAX_RESTARTS}: migrate the container to another
 *       healthy node.</li>
 * </ol>
 */
public class SelfHealingController {

    /** Number of in-place restart attempts before migrating. */
    private static final int MAX_RESTARTS = 3;

    private final Cluster     cluster;
    private final Scheduler   scheduler;
    private final EventLogger logger = EventLogger.getInstance();

    public SelfHealingController(Cluster cluster, Scheduler scheduler) {
        this.cluster   = cluster;
        this.scheduler = scheduler;
    }

    /**
     * Scans all containers across all deployments and heals failed ones.
     */
    public void reconcile() {
        for (Deployment dep : cluster.getDeployments()) {
            for (ContainerInstance c : dep.getInstances()) {
                if (c.getStatus() == ContainerStatus.FAILED) {
                    heal(c, dep);
                }
            }
        }
    }

    // ── Internal healing logic ────────────────────────────────────────────────

    private void heal(ContainerInstance c, Deployment dep) {
        cluster.incrementRecoveryEvents();

        if (c.getRestartCount() < MAX_RESTARTS) {
            attemptRestart(c);
        } else {
            attemptMigration(c);
        }
    }

    /**
     * Attempts to restart a container on its current node (if still healthy).
     * Falls back to migration if the node is unhealthy.
     */
    private void attemptRestart(ContainerInstance c) {
        Node node = c.getAssignedNode();

        if (node != null && node.getStatus() == NodeStatus.HEALTHY) {
            c.incrementRestartCount();
            c.setStatus(ContainerStatus.RUNNING);
            logger.warning(String.format(
                    "[SelfHealing] Restarted %-25s on %-10s (attempt %d/%d)",
                    c.getName(), node.getName(), c.getRestartCount(), MAX_RESTARTS));
        } else {
            // Node is gone – migrate immediately
            logger.warning("[SelfHealing] Node " + (node != null ? node.getName() : "null")
                    + " unhealthy; forcing migration of " + c.getName());
            attemptMigration(c);
        }
    }

    /**
     * Migrates a container to a different healthy node.
     */
    private void attemptMigration(ContainerInstance c) {
        // Remove from old node (if any)
        Node oldNode = c.getAssignedNode();
        if (oldNode != null) {
            oldNode.removeContainer(c);
        }

        c.resetRestartCount();
        c.setStatus(ContainerStatus.PENDING);

        List<Node> candidates = cluster.getNodes().stream()
                .filter(n -> n != oldNode)
                .toList();

        boolean placed = scheduler.schedule(c, candidates);
        if (placed) {
            logger.success(String.format(
                    "[SelfHealing] Migrated %-25s %s → %s",
                    c.getName(),
                    (oldNode != null ? oldNode.getName() : "none"),
                    c.getAssignedNode().getName()));
        } else {
            logger.error("[SelfHealing] Migration FAILED for " + c.getName()
                    + " – no healthy node with free resources.");
        }
    }
}
