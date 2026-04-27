package com.finalyear.orchestrator.simulation;

import com.finalyear.orchestrator.model.Cluster;
import com.finalyear.orchestrator.model.ContainerInstance;
import com.finalyear.orchestrator.model.ContainerStatus;

import java.util.List;
import java.util.Random;

/**
 * Simulates realistic workload fluctuations by randomly adjusting each
 * running container's {@link ContainerInstance#getSimulatedLoad()} on
 * every reconciliation tick.
 *
 * <p>The load drifts up or down by a small random delta, keeping the
 * simulation feeling organic rather than stepping monotonically.
 */
public class WorkloadGenerator {

    private static final double DRIFT = 0.08; // max change per tick
    private final Random random = new Random();

    /** Controls whether loads are currently artificially high ("traffic surge"). */
    private volatile boolean surgeMode = false;

    /**
     * Called by the orchestrator engine every tick.
     *
     * @param cluster the cluster whose container loads to update
     */
    public void tick(Cluster cluster) {
        for (ContainerInstance c : cluster.getAllContainers()) {
            if (c.getStatus() != ContainerStatus.RUNNING) continue;

            double current = c.getSimulatedLoad();
            double delta   = (random.nextDouble() * 2 - 1) * DRIFT; // [-DRIFT, +DRIFT]

            if (surgeMode) {
                // Surge: bias strongly upward
                delta += 0.12;
            }

            double newLoad = Math.max(0.05, Math.min(0.98, current + delta));
            c.setSimulatedLoad(newLoad);
        }
    }

    /**
     * Activates "traffic surge" mode: loads will trend upward,
     * eventually triggering the autoscaler's scale-out path.
     */
    public void activateSurge() {
        surgeMode = true;
    }

    /**
     * Deactivates surge mode; loads return to normal drift.
     */
    public void deactivateSurge() {
        surgeMode = false;
    }

    public boolean isSurgeMode() { return surgeMode; }
}
