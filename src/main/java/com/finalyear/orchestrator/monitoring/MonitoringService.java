package com.finalyear.orchestrator.monitoring;

import com.finalyear.orchestrator.model.Cluster;
import com.finalyear.orchestrator.model.MetricSnapshot;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Monitoring service that periodically collects cluster metrics and
 * distributes them to registered listeners (console UI and/or JavaFX dashboard).
 *
 * <p>The {@link com.finalyear.orchestrator.controlplane.OrchestratorEngine}
 * calls {@link #captureSnapshot()} on every reconciliation tick.
 */
public class MonitoringService {

    private final Cluster cluster;

    /** Most recently captured snapshot – always available for UI polling. */
    private volatile MetricSnapshot latestSnapshot;

    /** Registered listeners receive every new snapshot. */
    private final CopyOnWriteArrayList<Consumer<MetricSnapshot>> listeners =
            new CopyOnWriteArrayList<>();

    public MonitoringService(Cluster cluster) {
        this.cluster = cluster;
    }

    // ── Snapshot lifecycle ────────────────────────────────────────────────────

    /**
     * Captures a fresh {@link MetricSnapshot} from the cluster and
     * notifies all registered listeners.
     */
    public void captureSnapshot() {
        MetricSnapshot snap = new MetricSnapshot(cluster);
        latestSnapshot = snap;
        for (Consumer<MetricSnapshot> l : listeners) {
            try {
                l.accept(snap);
            } catch (Exception e) {
                // never let a listener crash the monitoring loop
            }
        }
    }

    /** Returns the most recent snapshot, or {@code null} if none yet. */
    public MetricSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    // ── Listener management ───────────────────────────────────────────────────

    /** Registers a listener to receive each new snapshot. */
    public void addListener(Consumer<MetricSnapshot> listener) {
        listeners.add(listener);
    }

    /** Removes a previously registered listener. */
    public void removeListener(Consumer<MetricSnapshot> listener) {
        listeners.remove(listener);
    }
}
