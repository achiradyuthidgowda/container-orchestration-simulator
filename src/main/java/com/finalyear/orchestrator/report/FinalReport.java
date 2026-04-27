package com.finalyear.orchestrator.report;

import com.finalyear.orchestrator.model.Cluster;
import com.finalyear.orchestrator.model.ContainerStatus;
import com.finalyear.orchestrator.model.Deployment;
import com.finalyear.orchestrator.model.Node;
import com.finalyear.orchestrator.model.NodeStatus;
import com.finalyear.orchestrator.util.ColorUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates a formatted final-report summary for the cluster session.
 *
 * <p>Printed when the user selects "Exit" from the console menu,
 * or via the dashboard "Final Report" button.
 */
public class FinalReport {

    private final Cluster cluster;

    public FinalReport(Cluster cluster) {
        this.cluster = cluster;
    }

    /**
     * Builds and returns a multi-line report string suitable for console output.
     */
    public String generate() {
        StringBuilder sb = new StringBuilder();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // ── Header ─────────────────────────────────────────────────────────────
        sb.append(ColorUtil.BOLD).append(ColorUtil.CYAN)
          .append("╔══════════════════════════════════════════════════════════╗\n")
          .append("║          CONTAINER ORCHESTRATION SIMULATOR               ║\n")
          .append("║                  FINAL SESSION REPORT                   ║\n")
          .append("╚══════════════════════════════════════════════════════════╝\n")
          .append(ColorUtil.RESET);

        sb.append(ColorUtil.BRIGHT_WHITE + "  Generated : " + ts + "\n" + ColorUtil.RESET);
        sb.append(ColorUtil.BRIGHT_WHITE + "  Cluster   : " + cluster.getName() + "\n\n" + ColorUtil.RESET);

        // ── Node summary ───────────────────────────────────────────────────────
        sb.append(ColorUtil.BOLD + ColorUtil.YELLOW + "  ► NODE SUMMARY\n" + ColorUtil.RESET);
        long total   = cluster.getNodes().size();
        long active  = cluster.getActiveNodeCount();
        long offline = cluster.getOfflineNodeCount();

        sb.append(String.format("    %-20s : %s%d%s\n", "Total Nodes",   ColorUtil.WHITE,  total,   ColorUtil.RESET));
        sb.append(String.format("    %-20s : %s%d%s\n", "Active Nodes",  ColorUtil.GREEN,  active,  ColorUtil.RESET));
        sb.append(String.format("    %-20s : %s%d%s\n", "Offline Nodes", ColorUtil.RED,    offline, ColorUtil.RESET));

        sb.append("\n");

        // Per-node detail
        for (Node n : cluster.getNodes()) {
            String statusColor = (n.getStatus() == NodeStatus.HEALTHY) ? ColorUtil.GREEN : ColorUtil.RED;
            sb.append(String.format("    [%s%-7s%s] %-12s  CPU %s  MEM %s\n",
                    statusColor, n.getStatus(), ColorUtil.RESET,
                    n.getName(),
                    ColorUtil.progressBar(n.getCpuUsagePercent(), 10),
                    ColorUtil.progressBar(n.getMemUsagePercent(), 10)));
        }

        sb.append("\n");

        // ── Container summary ──────────────────────────────────────────────────
        sb.append(ColorUtil.BOLD + ColorUtil.YELLOW + "  ► CONTAINER SUMMARY\n" + ColorUtil.RESET);
        long running    = cluster.getRunningContainerCount();
        long failed     = cluster.getFailedContainerCount();
        long recovery   = cluster.getRecoveryEventCount();

        long scaled = cluster.getDeployments().stream()
                .mapToLong(d -> d.getScaleOutCount() + d.getScaleInCount())
                .sum();

        sb.append(String.format("    %-20s : %s%d%s\n", "Running Containers",  ColorUtil.GREEN,  running,  ColorUtil.RESET));
        sb.append(String.format("    %-20s : %s%d%s\n", "Failed Containers",   ColorUtil.RED,    failed,   ColorUtil.RESET));
        sb.append(String.format("    %-20s : %s%d%s\n", "Scaled Events",       ColorUtil.CYAN,   scaled,   ColorUtil.RESET));
        sb.append(String.format("    %-20s : %s%d%s\n", "Recovery Events",     ColorUtil.YELLOW, recovery, ColorUtil.RESET));

        sb.append("\n");

        // ── Deployment breakdown ───────────────────────────────────────────────
        sb.append(ColorUtil.BOLD + ColorUtil.YELLOW + "  ► DEPLOYMENT BREAKDOWN\n" + ColorUtil.RESET);
        for (Deployment d : cluster.getDeployments()) {
            long dRunning = d.getInstances().stream()
                    .filter(c -> c.getStatus() == ContainerStatus.RUNNING).count();
            long dFailed  = d.getInstances().stream()
                    .filter(c -> c.getStatus() == ContainerStatus.FAILED).count();
            sb.append(String.format("    %-22s | running=%s%d%s failed=%s%d%s scaleOut=%d scaleIn=%d\n",
                    d.getName(),
                    ColorUtil.GREEN, dRunning, ColorUtil.RESET,
                    ColorUtil.RED,   dFailed,  ColorUtil.RESET,
                    d.getScaleOutCount(), d.getScaleInCount()));
        }

        sb.append("\n");

        // ── Footer ─────────────────────────────────────────────────────────────
        sb.append(ColorUtil.BOLD + ColorUtil.CYAN
                + "══════════════════════════════════════════════════════════\n"
                + ColorUtil.RESET);

        return sb.toString();
    }

    /** Prints the report to stdout. */
    public void print() {
        System.out.println(generate());
    }
}
