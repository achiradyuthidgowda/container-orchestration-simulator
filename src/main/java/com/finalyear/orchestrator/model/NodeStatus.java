package com.finalyear.orchestrator.model;

/**
 * Possible lifecycle states for a cluster {@link Node}.
 */
public enum NodeStatus {
    /** Node is reachable and accepting containers. */
    HEALTHY,
    /** Node has been marked as failed / unreachable. */
    FAILED
}
