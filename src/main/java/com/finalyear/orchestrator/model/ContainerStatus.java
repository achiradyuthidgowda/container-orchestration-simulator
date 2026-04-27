package com.finalyear.orchestrator.model;

/**
 * Possible lifecycle states for a {@link ContainerInstance}.
 */
public enum ContainerStatus {
    /** Container is pending placement by the scheduler. */
    PENDING,
    /** Container is running normally on a node. */
    RUNNING,
    /** Container has crashed / been killed. */
    FAILED,
    /** Container was cleanly terminated (e.g. scale-in). */
    TERMINATED
}
