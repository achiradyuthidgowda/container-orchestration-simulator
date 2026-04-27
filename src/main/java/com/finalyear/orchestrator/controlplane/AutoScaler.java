package com.finalyear.orchestrator.controlplane;

import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.Cluster;
import com.finalyear.orchestrator.model.ContainerInstance;
import com.finalyear.orchestrator.model.ContainerStatus;
import com.finalyear.orchestrator.model.Deployment;

import java.util.List;

/**
 * Autoscaler that adjusts the desired replica count for each
 * {@link Deployment} based on average container load.
 *
 * <p>Rules (applied every reconciliation tick):
 * <ul>
 *   <li>If average load ≥ 80 % → scale out (add 1 replica, up to maxReplicas)</li>
 *   <li>If average load ≤ 30 % and running &gt; minReplicas → scale in (remove 1 replica)</li>
 * </ul>
 */
public class AutoScaler {

    /** Load threshold above which we scale out. */
    private static final double SCALE_OUT_THRESHOLD = 0.80;

    /** Load threshold below which we scale in. */
    private static final double SCALE_IN_THRESHOLD  = 0.30;

    private final Cluster     cluster;
    private final Scheduler   scheduler;
    private final EventLogger logger = EventLogger.getInstance();

    public AutoScaler(Cluster cluster, Scheduler scheduler) {
        this.cluster   = cluster;
        this.scheduler = scheduler;
    }

    /**
     * Evaluates every deployment and adjusts desired replicas accordingly.
     * The actual creation/deletion of containers is handled here to keep
     * the reconcile loop simple.
     */
    public void reconcile() {
        for (Deployment dep : cluster.getDeployments()) {
            double avgLoad     = dep.getAverageLoad();
            int    running     = dep.getRunningCount();
            int    desired     = dep.getDesiredReplicas();

            // ── Scale Out ──────────────────────────────────────────────────
            if (avgLoad >= SCALE_OUT_THRESHOLD && desired < dep.getMaxReplicas()) {
                int newDesired = Math.min(desired + 1, dep.getMaxReplicas());
                dep.setDesiredReplicas(newDesired);
                dep.incrementScaleOut();
                logger.warning(String.format(
                        "[AutoScaler] SCALE-OUT  %-20s | load=%.0f%% > 80%% → desired %d → %d",
                        dep.getName(), avgLoad * 100, desired, newDesired));
                spawnReplica(dep);
            }

            // ── Scale In ───────────────────────────────────────────────────
            else if (avgLoad <= SCALE_IN_THRESHOLD
                    && running > dep.getMinReplicas()
                    && desired > dep.getMinReplicas()) {

                int newDesired = Math.max(desired - 1, dep.getMinReplicas());
                dep.setDesiredReplicas(newDesired);
                dep.incrementScaleIn();
                logger.info(String.format(
                        "[AutoScaler] SCALE-IN   %-20s | load=%.0f%% < 30%% → desired %d → %d",
                        dep.getName(), avgLoad * 100, desired, newDesired));
                removeOneReplica(dep);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates one additional container instance for a deployment and schedules it. */
    private void spawnReplica(Deployment dep) {
        long idx = dep.getInstances().size() + 1;
        ContainerInstance c = new ContainerInstance(
                dep.getName() + "-pod-" + idx,
                dep.getName(),
                dep.getRequestedCpu(),
                dep.getRequestedMemory());
        dep.addInstance(c);

        boolean placed = scheduler.schedule(c, cluster.getNodes());
        if (!placed) {
            c.setStatus(ContainerStatus.PENDING);
            logger.warning("[AutoScaler] Replica created but could not be scheduled: " + c.getName());
        }
    }

    /**
     * Terminates one running container for a deployment (scale-in).
     * Picks the container with the lowest current load.
     */
    private void removeOneReplica(Deployment dep) {
        List<ContainerInstance> running = dep.getInstances().stream()
                .filter(c -> c.getStatus() == ContainerStatus.RUNNING)
                .sorted((a, b) -> Double.compare(a.getSimulatedLoad(), b.getSimulatedLoad()))
                .toList();

        if (!running.isEmpty()) {
            ContainerInstance victim = running.get(0);
            if (victim.getAssignedNode() != null) {
                victim.getAssignedNode().removeContainer(victim);
            }
            victim.setStatus(ContainerStatus.TERMINATED);
            dep.removeInstance(victim);
            logger.info("[AutoScaler] Terminated replica: " + victim.getName());
        }
    }
}
