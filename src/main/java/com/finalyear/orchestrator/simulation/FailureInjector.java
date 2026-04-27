package com.finalyear.orchestrator.simulation;

import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Provides manual failure injection capabilities for demo and testing.
 *
 * <p>Callable from the Console UI or JavaFX dashboard:
 * <ul>
 *   <li>{@link #crashNode(String)} – marks a node as FAILED</li>
 *   <li>{@link #killContainer(String)} – marks a container as FAILED</li>
 *   <li>{@link #crashRandomNode()} – crashes a random healthy node</li>
 *   <li>{@link #killRandomContainer()} – kills a random running container</li>
 * </ul>
 */
public class FailureInjector {

    private final Cluster     cluster;
    private final EventLogger logger = EventLogger.getInstance();
    private final Random      random = new Random();

    public FailureInjector(Cluster cluster) {
        this.cluster = cluster;
    }

    // ── Node failures ─────────────────────────────────────────────────────────

    /**
     * Marks the named node as {@link NodeStatus#FAILED}.
     * The {@link com.finalyear.orchestrator.controlplane.NodeRecoveryController}
     * will evacuate its containers on the next tick.
     *
     * @param nodeName name of the node to crash
     * @return {@code true} if the node was found and crashed
     */
    public boolean crashNode(String nodeName) {
        Optional<Node> found = cluster.getNodes().stream()
                .filter(n -> n.getName().equalsIgnoreCase(nodeName)
                          && n.getStatus() == NodeStatus.HEALTHY)
                .findFirst();

        if (found.isPresent()) {
            found.get().setStatus(NodeStatus.FAILED);
            logger.error("[FailureInjector] *** NODE CRASHED: " + nodeName + " ***");
            return true;
        } else {
            logger.warning("[FailureInjector] Node not found or already offline: " + nodeName);
            return false;
        }
    }

    /**
     * Crashes a randomly selected healthy node.
     *
     * @return the name of the crashed node, or {@code null} if none available
     */
    public String crashRandomNode() {
        List<Node> healthy = cluster.getHealthyNodes();
        if (healthy.isEmpty()) {
            logger.warning("[FailureInjector] No healthy nodes to crash.");
            return null;
        }
        Node victim = healthy.get(random.nextInt(healthy.size()));
        crashNode(victim.getName());
        return victim.getName();
    }

    /**
     * Restores a previously failed node back to {@link NodeStatus#HEALTHY}.
     *
     * @param nodeName name of the node to restore
     * @return {@code true} if the node was found and restored
     */
    public boolean restoreNode(String nodeName) {
        Optional<Node> found = cluster.getNodes().stream()
                .filter(n -> n.getName().equalsIgnoreCase(nodeName)
                          && n.getStatus() == NodeStatus.FAILED)
                .findFirst();

        if (found.isPresent()) {
            found.get().setStatus(NodeStatus.HEALTHY);
            logger.success("[FailureInjector] Node RESTORED: " + nodeName);
            return true;
        } else {
            logger.warning("[FailureInjector] Node not found or already healthy: " + nodeName);
            return false;
        }
    }

    // ── Container failures ────────────────────────────────────────────────────

    /**
     * Marks the named container as {@link ContainerStatus#FAILED}.
     * The {@link com.finalyear.orchestrator.controlplane.SelfHealingController}
     * will restart or migrate it on the next tick.
     *
     * @param containerName name of the container to kill
     * @return {@code true} if the container was found and killed
     */
    public boolean killContainer(String containerName) {
        Optional<ContainerInstance> found = cluster.getAllContainers().stream()
                .filter(c -> c.getName().equalsIgnoreCase(containerName)
                          && c.getStatus() == ContainerStatus.RUNNING)
                .findFirst();

        if (found.isPresent()) {
            ContainerInstance c = found.get();
            if (c.getAssignedNode() != null) {
                c.getAssignedNode().removeContainer(c);
            }
            c.setStatus(ContainerStatus.FAILED);
            logger.error("[FailureInjector] *** CONTAINER KILLED: " + containerName + " ***");
            return true;
        } else {
            logger.warning("[FailureInjector] Container not found or not running: " + containerName);
            return false;
        }
    }

    /**
     * Kills a randomly selected running container.
     *
     * @return the name of the killed container, or {@code null} if none available
     */
    public String killRandomContainer() {
        List<ContainerInstance> running = cluster.getAllContainers().stream()
                .filter(c -> c.getStatus() == ContainerStatus.RUNNING)
                .toList();

        if (running.isEmpty()) {
            logger.warning("[FailureInjector] No running containers to kill.");
            return null;
        }
        ContainerInstance victim = running.get(random.nextInt(running.size()));
        killContainer(victim.getName());
        return victim.getName();
    }
}
