package com.finalyear.orchestrator.controlplane;

import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Node-failure recovery controller.
 *
 * <p>On each reconciliation tick this controller checks for FAILED nodes.
 * All containers that were running on a failed node are evacuated to remaining
 * healthy nodes using the {@link Scheduler}.
 */
public class NodeRecoveryController {

    private final Cluster     cluster;
    private final Scheduler   scheduler;
    private final EventLogger logger = EventLogger.getInstance();

    public NodeRecoveryController(Cluster cluster, Scheduler scheduler) {
        this.cluster   = cluster;
        this.scheduler = scheduler;
    }

    /**
     * Scans all FAILED nodes, evacuates their containers to healthy nodes,
     * and increments the cluster recovery counter.
     */
    public void reconcile() {
        for (Node node : cluster.getNodes()) {
            if (node.getStatus() == NodeStatus.FAILED) {
                evacuate(node);
            }
        }
    }

    // ── Internal evacuation logic ─────────────────────────────────────────────

    /**
     * Moves every RUNNING container off the failed node to a healthy node.
     * Containers that cannot be rescheduled are left in PENDING state.
     */
    private void evacuate(Node failedNode) {
        // Take a snapshot to avoid ConcurrentModificationException
        List<ContainerInstance> stranded = new ArrayList<>(failedNode.getContainers());
        if (stranded.isEmpty()) return;

        logger.error("[NodeRecovery] Node FAILED: " + failedNode.getName()
                + " – evacuating " + stranded.size() + " container(s)");

        List<Node> healthy = cluster.getNodes().stream()
                .filter(n -> n != failedNode && n.getStatus() == NodeStatus.HEALTHY)
                .toList();

        for (ContainerInstance c : stranded) {
            // Remove from failed node
            failedNode.removeContainer(c);
            c.setStatus(ContainerStatus.PENDING);
            c.resetRestartCount();

            boolean placed = scheduler.schedule(c, healthy);
            if (placed) {
                cluster.incrementRecoveryEvents();
                logger.success("[NodeRecovery] Rescheduled " + c.getName()
                        + " → " + c.getAssignedNode().getName());
            } else {
                logger.error("[NodeRecovery] Could not reschedule " + c.getName()
                        + " – insufficient cluster resources.");
            }
        }
    }
}
