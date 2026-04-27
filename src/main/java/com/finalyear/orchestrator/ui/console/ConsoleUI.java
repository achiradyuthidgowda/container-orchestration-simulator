package com.finalyear.orchestrator.ui.console;

import com.finalyear.orchestrator.controlplane.OrchestratorEngine;
import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.*;
import com.finalyear.orchestrator.monitoring.MonitoringService;
import com.finalyear.orchestrator.report.FinalReport;
import com.finalyear.orchestrator.simulation.FailureInjector;
import com.finalyear.orchestrator.simulation.WorkloadGenerator;
import com.finalyear.orchestrator.util.ColorUtil;

import java.util.List;
import java.util.Scanner;

/**
 * Professional interactive console UI for the Container Orchestration Simulator.
 *
 * <p>Provides:
 * <ul>
 *   <li>Attractive ASCII art title banner</li>
 *   <li>Coloured INFO / SUCCESS / WARNING / ERROR log output</li>
 *   <li>Interactive numbered menu</li>
 *   <li>Real-time cluster monitoring view</li>
 *   <li>Failure simulation controls</li>
 *   <li>Final report screen</li>
 * </ul>
 */
public class ConsoleUI {

    // ── Title banner ──────────────────────────────────────────────────────────
    private static final String BANNER =
            ColorUtil.BOLD + ColorUtil.CYAN + "\n" +
            "  ╔═══════════════════════════════════════════════════════════════╗\n" +
            "  ║   ██████╗ ██████╗ ███████╗     ██████╗ ██████╗ ██╗  ██╗     ║\n" +
            "  ║  ██╔════╝██╔═══██╗██╔════╝    ██╔═══██╗██╔══██╗██║ ██╔╝     ║\n" +
            "  ║  ██║     ██║   ██║███████╗    ██║   ██║██████╔╝█████╔╝      ║\n" +
            "  ║  ██║     ██║   ██║╚════██║    ██║   ██║██╔══██╗██╔═██╗      ║\n" +
            "  ║  ╚██████╗╚██████╔╝███████║    ╚██████╔╝██║  ██║██║  ██╗     ║\n" +
            "  ║   ╚═════╝ ╚═════╝ ╚══════╝     ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝    ║\n" +
            "  ║                                                               ║\n" +
            "  ║        CONTAINER ORCHESTRATION SIMULATOR  v1.0               ║\n" +
            "  ║        Inspired by Kubernetes | Final-Year Project           ║\n" +
            "  ╚═══════════════════════════════════════════════════════════════╝\n"
            + ColorUtil.RESET;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final OrchestratorEngine engine;
    private final FailureInjector    injector;
    private final WorkloadGenerator  workloadGen;
    private final MonitoringService  monitoring;
    private final FinalReport        report;
    private final EventLogger        logger = EventLogger.getInstance();
    private final Scanner            scanner = new Scanner(System.in);

    public ConsoleUI(OrchestratorEngine engine,
                     FailureInjector injector,
                     WorkloadGenerator workloadGen) {
        this.engine      = engine;
        this.injector    = injector;
        this.workloadGen = workloadGen;
        this.monitoring  = engine.getMonitoring();
        this.report      = new FinalReport(engine.getCluster());
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    /**
     * Starts the interactive console loop. Blocks until the user exits.
     */
    public void start() {
        System.out.println(BANNER);
        logger.success("Simulator started. Orchestrator engine is running in the background.");
        System.out.println();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1"  -> showClusterStatus();
                case "2"  -> showContainerList();
                case "3"  -> deployNewWorkload();
                case "4"  -> crashNodeMenu();
                case "5"  -> restoreNodeMenu();
                case "6"  -> killContainerMenu();
                case "7"  -> toggleTrafficSurge();
                case "8"  -> showRealTimeMonitor();
                case "9"  -> { report.print(); }
                case "0"  -> { running = false; }
                default   -> logger.warning("Unknown option. Please enter a number from the menu.");
            }
        }

        System.out.println("\n" + ColorUtil.YELLOW + "  Shutting down..." + ColorUtil.RESET);
        engine.stop();
        report.print();
        logger.info("Goodbye!");
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println(ColorUtil.BOLD + ColorUtil.BRIGHT_BLUE
                + "\n  ┌─────────────────────────────────────────┐"
                + "\n  │          MAIN MENU                      │"
                + "\n  ├─────────────────────────────────────────┤"
                + "\n  │  1. Cluster Status (nodes + resources)  │"
                + "\n  │  2. Container List                      │"
                + "\n  │  3. Deploy New Workload                 │"
                + "\n  │  4. Crash a Node (simulate failure)     │"
                + "\n  │  5. Restore a Node                      │"
                + "\n  │  6. Kill a Container (simulate failure) │"
                + "\n  │  7. Toggle Traffic Surge (autoscaler)   │"
                + "\n  │  8. Real-Time Monitor (live refresh)    │"
                + "\n  │  9. Final Report                        │"
                + "\n  │  0. Exit                                │"
                + "\n  └─────────────────────────────────────────┘"
                + ColorUtil.RESET);
        System.out.print(ColorUtil.BRIGHT_WHITE + "  Enter choice: " + ColorUtil.RESET);
    }

    // ── Option 1: Cluster Status ──────────────────────────────────────────────

    private void showClusterStatus() {
        Cluster cluster = engine.getCluster();
        section("CLUSTER STATUS");

        System.out.printf("  %-12s : %s%n", "Cluster", cluster.getName());
        System.out.printf("  %-12s : %d%n", "Total Nodes", cluster.getNodes().size());
        System.out.printf("  %-12s : %s%d%s%n", "Healthy",
                ColorUtil.GREEN, cluster.getActiveNodeCount(), ColorUtil.RESET);
        System.out.printf("  %-12s : %s%d%s%n", "Offline",
                ColorUtil.RED, cluster.getOfflineNodeCount(), ColorUtil.RESET);
        System.out.println();

        System.out.println(ColorUtil.BOLD + "  Nodes:" + ColorUtil.RESET);
        for (Node n : cluster.getNodes()) {
            String statusStr = (n.getStatus() == NodeStatus.HEALTHY)
                    ? ColorUtil.GREEN + "● HEALTHY" + ColorUtil.RESET
                    : ColorUtil.RED   + "✖ FAILED " + ColorUtil.RESET;

            System.out.printf("  %-12s %s  CPU %s  MEM %s  containers=%d%n",
                    n.getName(), statusStr,
                    ColorUtil.progressBar(n.getCpuUsagePercent(), 12),
                    ColorUtil.progressBar(n.getMemUsagePercent(), 12),
                    n.getContainerCount());
        }
    }

    // ── Option 2: Container List ──────────────────────────────────────────────

    private void showContainerList() {
        section("CONTAINER LIST");
        List<ContainerInstance> all = engine.getCluster().getAllContainers();

        if (all.isEmpty()) {
            System.out.println("  (no containers deployed)");
            return;
        }

        System.out.printf("  %-28s %-12s %-14s %-10s %s%n",
                "Name", "Status", "Node", "Restarts", "Load");
        System.out.println("  " + "─".repeat(75));

        for (ContainerInstance c : all) {
            String statusStr = switch (c.getStatus()) {
                case RUNNING    -> ColorUtil.GREEN  + "RUNNING    " + ColorUtil.RESET;
                case FAILED     -> ColorUtil.RED    + "FAILED     " + ColorUtil.RESET;
                case PENDING    -> ColorUtil.YELLOW + "PENDING    " + ColorUtil.RESET;
                case TERMINATED -> ColorUtil.CYAN   + "TERMINATED " + ColorUtil.RESET;
            };
            String node = (c.getAssignedNode() != null) ? c.getAssignedNode().getName() : "—";
            System.out.printf("  %-28s %s %-14s %-10d %s%n",
                    c.getName(), statusStr, node, c.getRestartCount(),
                    ColorUtil.progressBar(c.getSimulatedLoad() * 100, 10));
        }
    }

    // ── Option 3: Deploy Workload ─────────────────────────────────────────────

    private void deployNewWorkload() {
        section("DEPLOY NEW WORKLOAD");

        System.out.print("  Deployment name (e.g. myapp): ");
        String depName = scanner.nextLine().trim();
        if (depName.isEmpty()) { logger.warning("Name cannot be empty."); return; }

        System.out.print("  Image (e.g. nginx:latest):    ");
        String image = scanner.nextLine().trim();
        if (image.isEmpty()) image = depName + ":latest";

        int replicas = readInt("  Desired replicas [1-10]:     ", 1, 10);
        double cpu   = readDouble("  CPU cores per replica [0.1-4.0]: ", 0.1, 4.0);
        double mem   = readDouble("  Memory MB per replica [64-4096]: ", 64, 4096);

        int min = Math.max(1, replicas / 2);
        int max = Math.min(10, replicas * 3);

        Deployment dep = new Deployment(depName, image, replicas, min, max, cpu, mem);
        engine.deploy(dep);
        logger.success("Deployment '" + depName + "' created with " + replicas + " replica(s).");
    }

    // ── Option 4: Crash Node ──────────────────────────────────────────────────

    private void crashNodeMenu() {
        section("CRASH A NODE");
        listNodeNames();
        System.out.print("  Enter node name (or RANDOM): ");
        String name = scanner.nextLine().trim();

        if (name.equalsIgnoreCase("RANDOM")) {
            String crashed = injector.crashRandomNode();
            if (crashed != null) logger.error("Random node crashed: " + crashed);
        } else {
            injector.crashNode(name);
        }
    }

    // ── Option 5: Restore Node ────────────────────────────────────────────────

    private void restoreNodeMenu() {
        section("RESTORE A NODE");
        listNodeNames();
        System.out.print("  Enter node name to restore: ");
        String name = scanner.nextLine().trim();
        injector.restoreNode(name);
    }

    // ── Option 6: Kill Container ──────────────────────────────────────────────

    private void killContainerMenu() {
        section("KILL A CONTAINER");
        showContainerList();
        System.out.print("\n  Enter container name (or RANDOM): ");
        String name = scanner.nextLine().trim();

        if (name.equalsIgnoreCase("RANDOM")) {
            String killed = injector.killRandomContainer();
            if (killed != null) logger.error("Random container killed: " + killed);
        } else {
            injector.killContainer(name);
        }
    }

    // ── Option 7: Traffic Surge ───────────────────────────────────────────────

    private void toggleTrafficSurge() {
        if (workloadGen.isSurgeMode()) {
            workloadGen.deactivateSurge();
            logger.success("Traffic surge DEACTIVATED – loads will normalise.");
        } else {
            workloadGen.activateSurge();
            logger.warning("Traffic surge ACTIVATED – loads climbing! Autoscaler will react.");
        }
    }

    // ── Option 8: Real-Time Monitor ───────────────────────────────────────────

    /**
     * Refreshes the display every 3 seconds until the user presses Enter.
     */
    private void showRealTimeMonitor() {
        System.out.println(ColorUtil.YELLOW
                + "  [Real-Time Monitor] Press ENTER to stop...\n" + ColorUtil.RESET);

        Thread monitorThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    clearScreen();
                    MetricSnapshot snap = monitoring.getLatestSnapshot();
                    if (snap != null) {
                        printMonitorView(snap);
                    } else {
                        System.out.println("  Waiting for first metrics snapshot...");
                    }
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "monitor-refresh");
        monitorThread.setDaemon(true);
        monitorThread.start();

        scanner.nextLine(); // block until user presses Enter
        monitorThread.interrupt();
        System.out.println(ColorUtil.GREEN + "  Monitor stopped." + ColorUtil.RESET);
    }

    // ── Monitor view renderer ─────────────────────────────────────────────────

    private void printMonitorView(MetricSnapshot snap) {
        System.out.println(BANNER);
        System.out.println(ColorUtil.BOLD + ColorUtil.CYAN
                + "  REAL-TIME CLUSTER MONITOR   " + snap.getTimestampString()
                + ColorUtil.RESET);
        System.out.println("  " + "─".repeat(64));

        System.out.printf("  Nodes: %s%d ACTIVE%s  /  %s%d OFFLINE%s  /  %d TOTAL%n",
                ColorUtil.GREEN, snap.getActiveNodes(),  ColorUtil.RESET,
                ColorUtil.RED,   snap.getOfflineNodes(), ColorUtil.RESET,
                snap.getTotalNodes());

        System.out.printf("  Containers: %s%d RUNNING%s  /  %s%d FAILED%s%n",
                ColorUtil.GREEN, snap.getRunningContainers(), ColorUtil.RESET,
                ColorUtil.RED,   snap.getFailedContainers(),  ColorUtil.RESET);

        System.out.printf("  Cluster CPU: %s%n",
                ColorUtil.progressBar(snap.getClusterCpuPercent(), 20));
        System.out.printf("  Cluster MEM: %s%n",
                ColorUtil.progressBar(snap.getClusterMemPercent(), 20));

        System.out.println("\n  " + ColorUtil.BOLD + "Node Details:" + ColorUtil.RESET);
        for (Node n : snap.getNodes()) {
            String dot = (n.getStatus() == NodeStatus.HEALTHY)
                    ? ColorUtil.GREEN + "●" + ColorUtil.RESET
                    : ColorUtil.RED   + "✖" + ColorUtil.RESET;
            System.out.printf("  %s %-12s  CPU %s  MEM %s  [%d containers]%n",
                    dot, n.getName(),
                    ColorUtil.progressBar(n.getCpuUsagePercent(), 12),
                    ColorUtil.progressBar(n.getMemUsagePercent(), 12),
                    n.getContainerCount());
        }

        System.out.println("\n  Recovery Events: " + snap.getRecoveryEvents());
        System.out.println(ColorUtil.YELLOW
                + "  [Press ENTER to stop monitor]" + ColorUtil.RESET);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void section(String title) {
        System.out.println("\n" + ColorUtil.BOLD + ColorUtil.BRIGHT_CYAN
                + "  ══ " + title + " ══" + ColorUtil.RESET + "\n");
    }

    private void listNodeNames() {
        System.out.println("  Available nodes:");
        for (Node n : engine.getCluster().getNodes()) {
            String col = (n.getStatus() == NodeStatus.HEALTHY) ? ColorUtil.GREEN : ColorUtil.RED;
            System.out.printf("    %s%-12s%s [%s]%n",
                    col, n.getName(), ColorUtil.RESET, n.getStatus());
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= min && v <= max) return v;
                System.out.printf("  Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
            }
        }
    }

    private double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(scanner.nextLine().trim());
                if (v >= min && v <= max) return v;
                System.out.printf("  Please enter a number between %.1f and %.1f.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
            }
        }
    }

    /** Attempts to clear the terminal (best-effort; works on most platforms). */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
