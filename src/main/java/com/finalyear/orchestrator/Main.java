package com.finalyear.orchestrator;

import com.finalyear.orchestrator.controlplane.OrchestratorEngine;
import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.Cluster;
import com.finalyear.orchestrator.model.Deployment;
import com.finalyear.orchestrator.model.Node;
import com.finalyear.orchestrator.monitoring.MonitoringService;
import com.finalyear.orchestrator.simulation.FailureInjector;
import com.finalyear.orchestrator.simulation.WorkloadGenerator;
import com.finalyear.orchestrator.ui.console.ConsoleUI;
import com.finalyear.orchestrator.ui.fx.FxDashboardApp;
import com.finalyear.orchestrator.util.ColorUtil;

import javafx.application.Application;

import java.util.Scanner;

/**
 * Application entry point.
 *
 * <p>On startup the user chooses between:
 * <ol>
 *   <li><b>Console UI</b>   – runs in the terminal; no graphical display required.</li>
 *   <li><b>JavaFX Dashboard</b> – opens a desktop GUI window.</li>
 * </ol>
 *
 * <p><b>Run via Maven:</b>
 * <pre>
 *   mvn clean package
 *   mvn exec:java                     # Console UI
 *   mvn javafx:run                    # JavaFX UI
 * </pre>
 *
 * <p>A pre-seeded cluster (3 nodes, 2 demo deployments) is created automatically
 * so you can see the simulator working immediately without any manual setup.
 */
public class Main {

    private static final EventLogger logger = EventLogger.getInstance();

    public static void main(String[] args) {

        // ── Check for headless / CI environments ──────────────────────────────
        boolean headless = isHeadless(args);

        // ── Build shared infrastructure ───────────────────────────────────────
        Cluster           cluster     = buildCluster();
        WorkloadGenerator workloadGen = new WorkloadGenerator();
        MonitoringService monitoring  = new MonitoringService(cluster);
        OrchestratorEngine engine     = new OrchestratorEngine(cluster, monitoring, workloadGen);
        FailureInjector    injector   = new FailureInjector(cluster);

        // ── Pre-seed demo deployments ─────────────────────────────────────────
        seedDemoDeployments(engine);

        // ── Start the orchestrator loop ───────────────────────────────────────
        engine.start();

        // ── Choose UI ─────────────────────────────────────────────────────────
        if (headless) {
            launchConsole(engine, injector, workloadGen);
            return;
        }

        System.out.println(ColorUtil.BOLD + ColorUtil.CYAN
                + "\n  ╔══════════════════════════════════════════╗"
                + "\n  ║  Container Orchestration Simulator       ║"
                + "\n  ╚══════════════════════════════════════════╝"
                + ColorUtil.RESET);
        System.out.println();
        System.out.println(ColorUtil.BRIGHT_WHITE + "  Select UI mode:" + ColorUtil.RESET);
        System.out.println("   1) Console UI  (recommended – works everywhere)");
        System.out.println("   2) JavaFX Dashboard  (requires display / JDK with JavaFX)");
        System.out.println("   [default: 1]\n");
        System.out.print("  Your choice: ");

        Scanner sc = new Scanner(System.in);
        String choice = sc.nextLine().trim();

        if ("2".equals(choice)) {
            launchFx(engine, injector, workloadGen);
        } else {
            launchConsole(engine, injector, workloadGen);
        }
    }

    // ── Launchers ─────────────────────────────────────────────────────────────

    private static void launchConsole(OrchestratorEngine engine,
                                      FailureInjector injector,
                                      WorkloadGenerator workloadGen) {
        ConsoleUI ui = new ConsoleUI(engine, injector, workloadGen);
        ui.start(); // blocks until user exits
    }

    private static void launchFx(OrchestratorEngine engine,
                                 FailureInjector injector,
                                 WorkloadGenerator workloadGen) {
        // Share the running engine with the JavaFX Application class
        FxDashboardApp.ENGINE   = engine;
        FxDashboardApp.INJECTOR = injector;
        FxDashboardApp.WORKLOAD = workloadGen;

        // JavaFX Application.launch() is blocking
        Application.launch(FxDashboardApp.class);
    }

    // ── Cluster factory ───────────────────────────────────────────────────────

    /**
     * Creates a realistic 3-node cluster with varied CPU and memory capacities.
     */
    private static Cluster buildCluster() {
        Cluster cluster = new Cluster("prod-cluster-1");

        cluster.addNode(new Node("node-1", 8.0,  16384)); // 8 cores, 16 GB
        cluster.addNode(new Node("node-2", 4.0,   8192)); // 4 cores,  8 GB
        cluster.addNode(new Node("node-3", 12.0, 32768)); // 12 cores, 32 GB

        logger.success("Cluster '" + cluster.getName() + "' initialised with "
                + cluster.getNodes().size() + " nodes.");
        return cluster;
    }

    /**
     * Seeds two demo deployments so the dashboard shows live data immediately.
     */
    private static void seedDemoDeployments(OrchestratorEngine engine) {
        // Deployment 1: nginx web front-end
        engine.deploy(new Deployment(
                "nginx-frontend", "nginx:1.25",
                3,   // desiredReplicas
                1,   // minReplicas
                8,   // maxReplicas
                0.5, // CPU cores per pod
                256  // memory MB per pod
        ));

        // Deployment 2: Spring Boot API back-end
        engine.deploy(new Deployment(
                "api-backend", "openjdk:17-slim",
                2,   // desiredReplicas
                1,   // minReplicas
                6,   // maxReplicas
                1.0, // CPU cores per pod
                512  // memory MB per pod
        ));

        logger.success("Demo deployments seeded (nginx-frontend, api-backend).");
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if we should skip the UI-selection prompt (e.g. CI,
     * headless server, or the {@code --console} flag is passed).
     */
    private static boolean isHeadless(String[] args) {
        for (String arg : args) {
            if ("--console".equalsIgnoreCase(arg) || "--headless".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        // Also detect headless JVM environment
        return Boolean.getBoolean("java.awt.headless")
                && System.getenv("DISPLAY") == null
                && System.getenv("WAYLAND_DISPLAY") == null;
    }
}
