package com.finalyear.orchestrator.controlplane;

import com.finalyear.orchestrator.logging.EventLogger;
import com.finalyear.orchestrator.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Smart scheduler that places a {@link ContainerInstance} on the
 * least-loaded healthy {@link Node} while respecting resource limits.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Filter nodes: {@link NodeStatus#HEALTHY} and with sufficient
 *       free CPU + memory to satisfy the container's request.</li>
 *   <li>Sort by {@link Node#getUtilizationScore()} ascending (least loaded first).</li>
 *   <li>Use the first node as the placement target.</li>
 *   <li>Tie-breaker: fewest containers (lowest {@link Node#getContainerCount()}).</li>
 * </ol>
 */
public class Scheduler {

    private final EventLogger logger = EventLogger.getInstance();

    /**
     * Attempts to schedule a container.
     *
     * @param container the container to place
     * @param nodes     the list of all cluster nodes
     * @return {@code true} if placement succeeded; {@code false} if no node
     *         has sufficient resources
     */
    public boolean schedule(ContainerInstance container, List<Node> nodes) {

        Optional<Node> target = nodes.stream()
                .filter(n -> n.getStatus() == NodeStatus.HEALTHY)
                .filter(n -> n.getAvailableCpu()    >= container.getRequestedCpu())
                .filter(n -> n.getAvailableMemory() >= container.getRequestedMemory())
                .min(Comparator
                        .comparingDouble(Node::getUtilizationScore)
                        .thenComparingInt(Node::getContainerCount));

        if (target.isPresent()) {
            Node node = target.get();
            node.addContainer(container);
            logger.success(String.format("Scheduled [%s] → [%s] (cpu=%.1f, mem=%.0fMB, util=%.1f%%)",
                    container.getName(), node.getName(),
                    container.getRequestedCpu(), container.getRequestedMemory(),
                    node.getUtilizationScore() * 100));
            return true;
        } else {
            logger.warning("No suitable node found for container: " + container.getName()
                    + " (requested cpu=" + container.getRequestedCpu()
                    + " mem=" + container.getRequestedMemory() + "MB)");
            return false;
        }
    }
}
