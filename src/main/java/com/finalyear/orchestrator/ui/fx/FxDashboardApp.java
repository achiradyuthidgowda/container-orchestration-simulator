package com.finalyear.orchestrator.ui.fx;

import com.finalyear.orchestrator.controlplane.OrchestratorEngine;
import com.finalyear.orchestrator.model.*;
import com.finalyear.orchestrator.monitoring.MonitoringService;
import com.finalyear.orchestrator.report.FinalReport;
import com.finalyear.orchestrator.simulation.FailureInjector;
import com.finalyear.orchestrator.simulation.WorkloadGenerator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX Dashboard GUI for the Container Orchestration Simulator.
 *
 * <p>Features:
 * <ul>
 *   <li>Node cards with live CPU/memory progress bars</li>
 *   <li>Container table with status, node, load columns</li>
 *   <li>Cluster-wide statistics bar</li>
 *   <li>Buttons: Crash Random Node, Restore Node, Kill Container, Surge Traffic, Final Report</li>
 *   <li>Auto-refresh every 2 seconds via {@link javafx.application.Platform#runLater}</li>
 * </ul>
 *
 * <p>Launch via {@code mvn javafx:run} or directly through {@link Main}.
 */
public class FxDashboardApp extends Application {

    // ── Statics set by the launcher before Application.launch() ──────────────

    /** Set before launch so the JavaFX app can share the running engine. */
    public static OrchestratorEngine ENGINE;
    public static FailureInjector    INJECTOR;
    public static WorkloadGenerator  WORKLOAD;

    // ── Instance fields ───────────────────────────────────────────────────────

    private OrchestratorEngine engine;
    private FailureInjector    injector;
    private WorkloadGenerator  workload;
    private MonitoringService  monitoring;

    // UI components refreshed on each tick
    private FlowPane  nodeCardsPane;
    private TableView<ContainerRow> containerTable;
    private Label     lblActiveNodes, lblOfflineNodes, lblRunning, lblFailed, lblRecovery;
    private ProgressBar pbCpu, pbMem;
    private Label       lblCpuPct, lblMemPct;
    private TextArea    logArea;
    private Label       lblSurge;

    private final ScheduledExecutorService refresher =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "fx-refresher");
                t.setDaemon(true);
                return t;
            });

    // ── JavaFX lifecycle ──────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        this.engine     = ENGINE;
        this.injector   = INJECTOR;
        this.workload   = WORKLOAD;
        this.monitoring = engine.getMonitoring();

        // ── Root layout ───────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");

        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildButtonBar());

        // ── Scene ─────────────────────────────────────────────────────────────
        Scene scene = new Scene(root, 1100, 720);
        stage.setTitle("Container Orchestration Simulator – Dashboard");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            refresher.shutdown();
            engine.stop();
        });
        stage.show();

        // ── Start auto-refresh ────────────────────────────────────────────────
        refresher.scheduleAtFixedRate(this::refreshUI, 0, 2, TimeUnit.SECONDS);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private VBox buildHeader() {
        Label title = new Label("🚀  Container Orchestration Simulator");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#00d4ff"));

        Label subtitle = new Label("Kubernetes-Inspired | Real-Time Dashboard");
        subtitle.setFont(Font.font("Monospace", 13));
        subtitle.setTextFill(Color.web("#888888"));

        // Stats bar
        HBox stats = buildStatsBar();

        VBox header = new VBox(6, title, subtitle, new Separator(), stats);
        header.setPadding(new Insets(14, 20, 10, 20));
        header.setStyle("-fx-background-color: #16213e;");
        return header;
    }

    private HBox buildStatsBar() {
        lblActiveNodes  = statLabel("Active Nodes",   "—", "#00e676");
        lblOfflineNodes = statLabel("Offline Nodes",  "—", "#ff5252");
        lblRunning      = statLabel("Running",         "—", "#00e676");
        lblFailed       = statLabel("Failed",          "—", "#ff5252");
        lblRecovery     = statLabel("Recovery Events", "—", "#ffab40");
        lblSurge        = statLabel("Surge Mode",      "OFF","#888888");

        VBox cpuBox = progressStat("Cluster CPU", pbCpu = new ProgressBar(0), lblCpuPct = new Label("0%"));
        VBox memBox = progressStat("Cluster MEM", pbMem = new ProgressBar(0), lblMemPct = new Label("0%"));
        styleProgressBar(pbCpu, "#00d4ff");
        styleProgressBar(pbMem, "#7c4dff");

        HBox bar = new HBox(20,
                lblActiveNodes, lblOfflineNodes, lblRunning, lblFailed, lblRecovery,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                cpuBox, memBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                lblSurge);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 0, 0));
        return bar;
    }

    // ── Centre (node cards + container table) ─────────────────────────────────

    private SplitPane buildCenter() {
        // Left: node cards
        nodeCardsPane = new FlowPane();
        nodeCardsPane.setHgap(12);
        nodeCardsPane.setVgap(12);
        nodeCardsPane.setPadding(new Insets(12));
        nodeCardsPane.setStyle("-fx-background-color: #1a1a2e;");

        ScrollPane nodeScroll = new ScrollPane(nodeCardsPane);
        nodeScroll.setFitToWidth(true);
        nodeScroll.setStyle("-fx-background-color: #1a1a2e; -fx-background: #1a1a2e;");

        Label nodesTitle = styledSectionLabel("  🖥  Nodes");
        VBox leftPane = new VBox(nodesTitle, nodeScroll);
        leftPane.setStyle("-fx-background-color: #1a1a2e;");

        // Right: container table + log
        containerTable = buildContainerTable();
        Label containerTitle = styledSectionLabel("  📦  Containers");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background: #0d0d1a; -fx-text-fill: #aaaaaa; "
                + "-fx-font-family: Monospace; -fx-font-size: 11;");
        logArea.setPrefHeight(120);
        Label logTitle = styledSectionLabel("  📋  Event Log");

        VBox rightPane = new VBox(containerTitle, containerTable, logTitle, logArea);
        rightPane.setStyle("-fx-background-color: #1a1a2e;");
        VBox.setVgrow(containerTable, Priority.ALWAYS);

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.38);
        return split;
    }

    @SuppressWarnings("unchecked")
    private TableView<ContainerRow> buildContainerTable() {
        TableView<ContainerRow> table = new TableView<>();
        table.setStyle("-fx-base: #1a1a2e; -fx-control-inner-background: #0d0d1a; "
                + "-fx-table-cell-border-color: #333366;");

        TableColumn<ContainerRow,String> colName    = col("Container Name", "name",      200);
        TableColumn<ContainerRow,String> colStatus  = col("Status",          "status",    90);
        TableColumn<ContainerRow,String> colDep     = col("Deployment",      "deployment",140);
        TableColumn<ContainerRow,String> colNode    = col("Node",            "node",      100);
        TableColumn<ContainerRow,String> colLoad    = col("Load",            "load",      70);
        TableColumn<ContainerRow,String> colRst     = col("Restarts",        "restarts",  70);

        table.getColumns().addAll(colName, colStatus, colDep, colNode, colLoad, colRst);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    // ── Bottom button bar ─────────────────────────────────────────────────────

    private HBox buildButtonBar() {
        Button btnCrashNode    = actionButton("💥 Crash Random Node",     "#c62828", this::onCrashNode);
        Button btnRestoreNode  = actionButton("🔧 Restore Node",           "#1565c0", this::onRestoreNode);
        Button btnKillContainer= actionButton("☠ Kill Random Container",  "#ad1457", this::onKillContainer);
        Button btnSurge        = actionButton("📈 Toggle Traffic Surge",   "#e65100", this::onSurge);
        Button btnReport       = actionButton("📊 Final Report",           "#2e7d32", this::onReport);

        HBox bar = new HBox(10,
                btnCrashNode, btnRestoreNode, btnKillContainer, btnSurge, btnReport);
        bar.setPadding(new Insets(10, 20, 12, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #0f3460;");
        return bar;
    }

    // ── Button actions ────────────────────────────────────────────────────────

    private void onCrashNode() {
        String name = injector.crashRandomNode();
        appendLog(name != null ? "Crashed node: " + name : "No healthy nodes to crash.");
    }

    private void onRestoreNode() {
        List<Node> failed = engine.getCluster().getNodes().stream()
                .filter(n -> n.getStatus() == NodeStatus.FAILED).toList();
        if (failed.isEmpty()) {
            appendLog("No failed nodes to restore.");
            return;
        }
        Node target = failed.get(0);
        injector.restoreNode(target.getName());
        appendLog("Restored node: " + target.getName());
    }

    private void onKillContainer() {
        String name = injector.killRandomContainer();
        appendLog(name != null ? "Killed container: " + name : "No running containers.");
    }

    private void onSurge() {
        if (workload.isSurgeMode()) {
            workload.deactivateSurge();
            appendLog("Traffic surge DEACTIVATED.");
        } else {
            workload.activateSurge();
            appendLog("Traffic surge ACTIVATED – autoscaler will react!");
        }
    }

    private void onReport() {
        FinalReport r = new FinalReport(engine.getCluster());
        // Strip ANSI codes for display in a plain dialog
        String raw = r.generate().replaceAll("\u001B\\[[;\\d]*m", "");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Final Report");
        alert.setHeaderText("Session Summary");
        TextArea ta = new TextArea(raw);
        ta.setEditable(false);
        ta.setFont(Font.font("Monospace", 12));
        ta.setPrefSize(700, 500);
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().setMinWidth(720);
        alert.showAndWait();
    }

    // ── UI refresh ────────────────────────────────────────────────────────────

    private void refreshUI() {
        MetricSnapshot snap = monitoring.getLatestSnapshot();
        if (snap == null) return;

        Platform.runLater(() -> {
            // Stats bar
            lblActiveNodes.setText("Active Nodes\n" + snap.getActiveNodes());
            lblOfflineNodes.setText("Offline Nodes\n" + snap.getOfflineNodes());
            lblRunning.setText("Running\n"  + snap.getRunningContainers());
            lblFailed.setText("Failed\n"   + snap.getFailedContainers());
            lblRecovery.setText("Recovery Events\n" + snap.getRecoveryEvents());
            lblSurge.setText("Surge Mode\n" + (workload.isSurgeMode() ? "ON 🔥" : "OFF"));

            double cpu = snap.getClusterCpuPercent() / 100.0;
            double mem = snap.getClusterMemPercent() / 100.0;
            pbCpu.setProgress(cpu);
            pbMem.setProgress(mem);
            lblCpuPct.setText(String.format("%.0f%%", cpu * 100));
            lblMemPct.setText(String.format("%.0f%%", mem * 100));

            // Node cards
            nodeCardsPane.getChildren().clear();
            for (Node n : snap.getNodes()) {
                nodeCardsPane.getChildren().add(buildNodeCard(n));
            }

            // Container table
            List<ContainerRow> rows = engine.getCluster().getAllContainers().stream()
                    .map(ContainerRow::new).toList();
            containerTable.getItems().setAll(rows);
        });
    }

    // ── Node card builder ─────────────────────────────────────────────────────

    private VBox buildNodeCard(Node n) {
        boolean healthy = n.getStatus() == NodeStatus.HEALTHY;

        Label nameLabel = new Label("🖥  " + n.getName());
        nameLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web("#e0e0e0"));

        Label statusLabel = new Label(healthy ? "● HEALTHY" : "✖  FAILED");
        statusLabel.setFont(Font.font("Monospace", 11));
        statusLabel.setTextFill(healthy ? Color.web("#00e676") : Color.web("#ff5252"));

        ProgressBar cpuBar = new ProgressBar(n.getCpuUsagePercent() / 100.0);
        ProgressBar memBar = new ProgressBar(n.getMemUsagePercent() / 100.0);
        cpuBar.setPrefWidth(160);
        memBar.setPrefWidth(160);
        styleProgressBar(cpuBar, healthy ? "#00d4ff" : "#555555");
        styleProgressBar(memBar, healthy ? "#7c4dff" : "#555555");

        Label cpuLbl = cardMetric(String.format("CPU  %.0f%%", n.getCpuUsagePercent()));
        Label memLbl = cardMetric(String.format("MEM  %.0f%%", n.getMemUsagePercent()));
        Label cntLbl = cardMetric("Containers: " + n.getContainerCount());

        VBox card = new VBox(5, nameLabel, statusLabel,
                cpuLbl, cpuBar, memLbl, memBar, cntLbl);
        card.setPadding(new Insets(10));
        card.setPrefWidth(190);
        card.setStyle("-fx-background-color: " + (healthy ? "#16213e" : "#3e1616") + ";"
                + "-fx-border-color: " + (healthy ? "#00d4ff" : "#ff5252") + ";"
                + "-fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;");
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label statLabel(String title, String value, String colour) {
        Label lbl = new Label(title + "\n" + value);
        lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(colour));
        lbl.setAlignment(Pos.CENTER);
        return lbl;
    }

    private VBox progressStat(String label, ProgressBar pb, Label pctLabel) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Monospace", 11));
        lbl.setTextFill(Color.web("#aaaaaa"));
        pctLabel.setFont(Font.font("Monospace", 10));
        pctLabel.setTextFill(Color.web("#dddddd"));
        pb.setPrefWidth(120);
        return new VBox(2, lbl, new HBox(4, pb, pctLabel));
    }

    private Label styledSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#00d4ff"));
        lbl.setPadding(new Insets(6, 0, 4, 0));
        return lbl;
    }

    private Label cardMetric(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Monospace", 11));
        lbl.setTextFill(Color.web("#bbbbbb"));
        return lbl;
    }

    private Button actionButton(String text, String colour, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + colour + "; -fx-text-fill: white; "
                + "-fx-font-size: 12; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void styleProgressBar(ProgressBar pb, String trackColour) {
        pb.setStyle("-fx-accent: " + trackColour + "; -fx-background-color: #333355;");
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String title, String field, int width) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(field));
        col.setPrefWidth(width);
        col.setStyle("-fx-text-fill: #cccccc; -fx-font-family: Monospace;");
        return col;
    }

    private void appendLog(String msg) {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText("[ACTION] " + msg + "\n");
            }
        });
    }

    // ── JavaBeans row model for the container TableView ───────────────────────

    /** Simple JavaBeans wrapper for displaying a {@link ContainerInstance} row. */
    public static class ContainerRow {
        private final String name, status, deployment, node, load, restarts;

        public ContainerRow(ContainerInstance c) {
            this.name       = c.getName();
            this.status     = c.getStatus().name();
            this.deployment = c.getDeploymentName();
            this.node       = (c.getAssignedNode() != null) ? c.getAssignedNode().getName() : "—";
            this.load       = String.format("%.0f%%", c.getSimulatedLoad() * 100);
            this.restarts   = String.valueOf(c.getRestartCount());
        }

        public String getName()       { return name; }
        public String getStatus()     { return status; }
        public String getDeployment() { return deployment; }
        public String getNode()       { return node; }
        public String getLoad()       { return load; }
        public String getRestarts()   { return restarts; }
    }
}
