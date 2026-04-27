package com.finalyear.orchestrator.controlplane;

import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.*;
import com.finalyear.orchestrator.monitoring.MonitoringService;
import com.finalyear.orchestrator.simulation.WorkloadGenerator;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The main control-plane reconciliation loop, loosely inspired by the
 * Kubernetes controller-manager.
 *
 * <p>Every {@value #TICK_SECONDS} seconds the engine:
 * <ol>
 *   <li>Runs the {@link WorkloadGenerator} to simulate changing container loads.</li>
 *   <li>Calls {@link NodeRecoveryController#reconcile()} to evacuate failed nodes.</li>
 *   <li>Calls {@link SelfHealingController#reconcile()} to restart/migrate failed containers.</li>
 *   <li>Calls {@link AutoScaler#reconcile()} to adjust replica counts.</li>
 *   <li>Schedules any pending containers left after the above steps.</li>
 *   <li>Captures a fresh monitoring snapshot.</li>
 * </ol>
 */
public class OrchestratorEngine {

    /** Reconciliation interval in seconds. */
    private static final int TICK_SECONDS = 3;

    private final Cluster                  cluster;
    private final Scheduler                scheduler;
    private final AutoScaler               autoScaler;
    private final SelfHealingController    healingCtrl;
    private final NodeRecoveryController   recoveryCtrl;
    private final MonitoringService        monitoring;
    private final WorkloadGenerator        workloadGen;
    private final EventLogger              logger = EventLogger.getInstance();

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "orchestrator-engine");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> tickFuture;
    private volatile boolean   running = false;

    public OrchestratorEngine(Cluster cluster,
                              MonitoringService monitoring,
                              WorkloadGenerator workloadGen) {
        this.cluster      = cluster;
        this.monitoring   = monitoring;
        this.workloadGen  = workloadGen;
        this.scheduler    = new Scheduler();
        this.autoScaler   = new AutoScaler(cluster, scheduler);
        this.healingCtrl  = new SelfHealingController(cluster, scheduler);
        this.recoveryCtrl = new NodeRecoveryController(cluster, scheduler);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Starts the background reconciliation loop. */
    public synchronized void start() {
        if (running) return;
        running    = true;
        tickFuture = executor.scheduleAtFixedRate(
                this::tick, 0, TICK_SECONDS, TimeUnit.SECONDS);
        logger.success("OrchestratorEngine started (tick every " + TICK_SECONDS + "s)");
    }

    /** Stops the reconciliation loop. */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (tickFuture != null) tickFuture.cancel(false);
        executor.shutdown();
        logger.info("OrchestratorEngine stopped.");
    }

    public boolean isRunning() { return running; }

    // ── Tick ──────────────────────────────────────────────────────────────────

    private void tick() {
        try {
            // 1. Simulate load fluctuation
            workloadGen.tick(cluster);

            // 2. Node failure recovery (evacuate failed nodes first)
            recoveryCtrl.reconcile();

            // 3. Self-healing (restart / migrate failed containers)
            healingCtrl.reconcile();

            // 4. Autoscaling
            autoScaler.reconcile();

            // 5. Schedule any still-pending containers
            schedulePending();

            // 6. Capture monitoring snapshot
            monitoring.captureSnapshot();

        } catch (Exception e) {
            logger.error("Unhandled exception in reconcile tick: " + e.getMessage());
        }
    }

    /**
     * Schedules any containers that are still in {@link ContainerStatus#PENDING}
     * state (e.g., not yet placed after creation or after a node failure).
     */
    private void schedulePending() {
        List<ContainerInstance> pending = cluster.getAllContainers().stream()
                .filter(c -> c.getStatus() == ContainerStatus.PENDING)
                .toList();

        for (ContainerInstance c : pending) {
            scheduler.schedule(c, cluster.getNodes());
        }
    }

    // ── Manual deployment API (called from UI) ────────────────────────────────

    /**
     * Deploys a new {@link Deployment} and schedules its initial replicas.
     *
     * @param dep the deployment to add
     */
    public void deploy(Deployment dep) {
        cluster.addDeployment(dep);
        for (int i = 1; i <= dep.getDesiredReplicas(); i++) {
            ContainerInstance c = new ContainerInstance(
                    dep.getName() + "-pod-" + i,
                    dep.getName(),
                    dep.getRequestedCpu(),
                    dep.getRequestedMemory());
            dep.addInstance(c);
            boolean placed = scheduler.schedule(c, cluster.getNodes());
            if (!placed) {
                c.setStatus(ContainerStatus.PENDING);
            }
        }
        logger.success("Deployed: " + dep.getName()
                + " (" + dep.getDesiredReplicas() + " replicas)");
    }

    // ── Accessors for UI / simulation ─────────────────────────────────────────

    public Cluster           getCluster()     { return cluster; }
    public Scheduler         getScheduler()   { return scheduler; }
    public MonitoringService getMonitoring()  { return monitoring; }
}
