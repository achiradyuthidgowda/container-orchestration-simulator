# Container Orchestration Simulator – Viva Q&A Guide

A comprehensive collection of questions your examiner may ask, with model answers.

---

## Section 1: Project Fundamentals

**Q1. What is a Container Orchestration Simulator?**
> It is a Java application that simulates the core functions of platforms like Kubernetes: scheduling containers onto worker nodes, auto-scaling based on load, self-healing failed containers, and recovering from node failures — all without running actual Docker containers.

**Q2. Why Java and not Python or Go?**
> Java provides strong OOP principles (encapsulation, inheritance, polymorphism), a robust concurrency API (`ScheduledExecutorService`), and excellent tooling via Maven and IntelliJ/VS Code. JavaFX integrates natively for the GUI.

**Q3. What is the difference between a container and a node in your simulator?**
> A **Node** represents a physical or virtual machine with fixed CPU (cores) and memory (MB) capacity. A **Container** (ContainerInstance) represents a single running workload unit that requests a slice of a node's CPU and memory.

**Q4. What inspired the project architecture?**
> Kubernetes: the concept of a control-plane reconciliation loop, deployments, pods, and the scheduler placing pods based on available resources.

---

## Section 2: Scheduling

**Q5. How does your Smart Scheduler work?**
> It filters HEALTHY nodes that have enough free CPU and memory to satisfy the container's request. It then sorts the candidates by a utilisation score — `(allocatedCpu/totalCpu + allocatedMem/totalMem) / 2` — and picks the node with the lowest score (least loaded). The tie-breaker is the node with the fewest containers.

**Q6. What happens if no node has enough resources?**
> The container is left in `PENDING` state. The engine retries scheduling on every tick. A log warning is generated.

**Q7. Could you improve the scheduler? How?**
> Yes: bin-packing (First-Fit Decreasing), priority classes, topology awareness (spreading pods across availability zones), or node taints/tolerations similar to Kubernetes.

---

## Section 3: Auto Scaling

**Q8. When does the autoscaler scale out?**
> When the average simulated load across running replicas of a deployment exceeds 80 %, it increments `desiredReplicas` by 1 (up to `maxReplicas`) and creates a new container instance.

**Q9. When does it scale in?**
> When average load drops below 30 % AND current running replicas exceed `minReplicas`, it decrements `desiredReplicas` by 1 and terminates the container with the lowest current load.

**Q10. How do you prevent flapping (constant scale-out / scale-in)?**
> In production systems you add a cooldown period. In our simulator the 3-second tick rate and random load drift naturally prevent rapid oscillation. A real enhancement would be a configurable cooldown counter.

---

## Section 4: Self-Healing

**Q11. What happens when a container fails?**
> The `SelfHealingController` detects it on the next tick. If `restartCount < 3`, it restarts the container in-place (increments restart counter, sets status back to RUNNING). After 3 failed restarts, it migrates the container to a different healthy node.

**Q12. What if the node hosting the failed container is itself down?**
> The restart path detects an unhealthy node and falls through to migration immediately, bypassing the restart counter.

**Q13. What is the migration process?**
> The container is removed from the failed/old node (freeing its resources), reset to PENDING, and re-scheduled via the same Scheduler onto any other healthy node.

---

## Section 5: Node Failure Recovery

**Q14. How does node failure recovery work?**
> The `NodeRecoveryController` scans all nodes on each tick. For any node in `FAILED` state it takes a snapshot of its containers, removes them all from the node, and re-schedules each one to a remaining healthy node.

**Q15. What happens to containers that cannot be rescheduled?**
> They remain in PENDING state and are retried every tick. A log error is generated.

**Q16. Can a failed node come back online?**
> Yes — the `FailureInjector.restoreNode()` sets the node back to HEALTHY. New containers can then be scheduled there.

---

## Section 6: Monitoring & Logging

**Q17. How is real-time monitoring implemented?**
> The `MonitoringService` captures a `MetricSnapshot` at the end of every reconciliation tick. The snapshot is immutable and holds all key metrics (nodes, containers, CPU%, MEM%). Registered listeners (Console UI, JavaFX) receive the snapshot via a callback pattern.

**Q18. Where are logs stored?**
> `logs/logs.txt` in the project root. The `EventLogger` writes to both the console (with ANSI colours) and this file simultaneously using a `PrintWriter` backed by a `BufferedWriter`.

---

## Section 7: Design Patterns & OOP

**Q19. Which design patterns did you use?**
> - **Singleton**: `EventLogger` — one global logger shared everywhere.
> - **Observer/Listener**: `MonitoringService` notifies UI listeners on each tick.
> - **Strategy**: `Scheduler` encapsulates the placement algorithm independently of the engine.
> - **Factory method**: `Main.buildCluster()` constructs the initial cluster.
> - **MVC-like separation**: model (data), controlplane (logic), ui (presentation).

**Q20. How does concurrency work?**
> The `OrchestratorEngine` uses a single-threaded `ScheduledExecutorService` for the reconciliation loop. Model mutations (adding/removing containers from nodes) are `synchronized` at the `Node` and `Deployment` level. The JavaFX UI uses `Platform.runLater()` for thread-safe UI updates.

---

## Section 8: Improvements & Future Work

**Q21. What would you add given more time?**
> 1. **Persistent state** – save/load cluster state to JSON (e.g., Gson/Jackson).
> 2. **Rolling updates** – update deployments with zero downtime.
> 3. **Health probes** – liveness and readiness checks per container.
> 4. **Horizontal Pod Autoscaler cooldown** – prevent oscillation.
> 5. **Network simulation** – model inter-container latency.
> 6. **REST API** – expose cluster state via a tiny HTTP server (e.g., Javalin).

**Q22. How would you make it production-ready?**
> Replace simulated loads with actual process monitoring (JMX, Prometheus), use a real container runtime (Docker SDK / containerd), add distributed coordination (Raft / etcd), and introduce role-based access control.

---

## Section 9: Tools & Build

**Q23. Why Maven?**
> Maven provides standardised project structure, dependency management, and plugin support. The `javafx-maven-plugin` lets us launch the JavaFX app with a single command (`mvn javafx:run`) without manually configuring the module path.

**Q24. How do you run the project?**
> ```bash
> mvn clean package       # compile + package
> mvn exec:java           # Console UI
> mvn javafx:run          # JavaFX Dashboard
> ```

**Q25. What Java version is required?**
> Java 17 (LTS). The project uses records, pattern matching for switch, sealed interfaces (optional), and the `Stream.toList()` shorthand introduced in Java 16+.
