# Container Orchestration Simulator – Architecture & Presentation Guide

## 1. Project Overview

The **Container Orchestration Simulator** is a Java-based desktop application that
models the core concepts of Kubernetes and modern cloud platforms. It demonstrates:

- How containers are **scheduled** onto cluster nodes
- How workloads are **auto-scaled** based on real-time load
- How **failed containers** are automatically restarted or migrated
- How **failed nodes** are detected and their workloads evacuated
- A professional **console UI** with coloured logs and an interactive menu
- A **JavaFX dashboard** showing live node cards, resource bars, and a container table

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       Main.java (Entry Point)                   │
│              Choose: Console UI  ─OR─  JavaFX Dashboard        │
└────────────────────────┬────────────────────────────────────────┘
                         │
          ┌──────────────▼──────────────┐
          │     OrchestratorEngine       │  (ScheduledExecutorService)
          │     3-second tick loop       │
          └──┬──────┬──────┬──────┬─────┘
             │      │      │      │
    ┌────────▼─┐ ┌──▼──┐ ┌▼────┐ ┌▼──────────────┐
    │Workload  │ │Node │ │Self │ │  AutoScaler    │
    │Generator │ │Recov│ │Heal │ │  (scale ±1)   │
    └────────┬─┘ └──┬──┘ └┬────┘ └┬──────────────┘
             │      │     │       │
             └──────┴─────┴───────┘
                         │
                  ┌──────▼──────┐
                  │  Scheduler  │  (least-utilisation placement)
                  └──────┬──────┘
                         │
              ┌──────────▼────────────┐
              │       Cluster         │
              │  ┌────┐ ┌────┐ ┌────┐│
              │  │Node│ │Node│ │Node││
              │  │ 1  │ │ 2  │ │ 3  ││
              │  └──┬─┘ └──┬─┘ └──┬─┘│
              └─────┼───────┼───────┼──┘
                    │       │       │
               [pods]  [pods]  [pods]
                         │
                 ┌───────▼────────┐
                 │ MonitoringService│  (publishes MetricSnapshot)
                 └───┬─────────┬──┘
                     │         │
              ┌──────▼─┐  ┌────▼────────┐
              │Console  │  │JavaFX       │
              │UI       │  │Dashboard    │
              └─────────┘  └─────────────┘
```

### Package Map

| Package | Purpose |
|---------|---------|
| `model` | Plain data objects: `Cluster`, `Node`, `ContainerInstance`, `Deployment`, `MetricSnapshot` |
| `controlplane` | Reconciliation engine + controllers: `OrchestratorEngine`, `Scheduler`, `AutoScaler`, `SelfHealingController`, `NodeRecoveryController` |
| `monitoring` | `MonitoringService` – captures snapshots, distributes to listeners |
| `logging` | `EventLogger` – coloured console + persistent `logs/logs.txt` |
| `simulation` | `WorkloadGenerator` (load drift) + `FailureInjector` (crash node / kill container) |
| `ui.console` | `ConsoleUI` – interactive terminal menu |
| `ui.fx` | `FxDashboardApp` – JavaFX live dashboard |
| `report` | `FinalReport` – session summary |
| `util` | `ColorUtil` – ANSI colour helpers + progress bar renderer |

---

## 3. Feature Deep-Dive

### 3.1 Smart Scheduler
- Filters nodes: **HEALTHY** and with **sufficient free CPU + memory**.
- Sorts by utilisation score `(cpuUsed/cpuTotal + memUsed/memTotal) / 2`.
- Tie-breaker: node with fewest containers.
- Code: `controlplane/Scheduler.java`

### 3.2 Auto Scaling
- **Scale-out**: if average load across running replicas ≥ 80 % → `desiredReplicas + 1`.
- **Scale-in**: if average load ≤ 30 % AND `running > minReplicas` → `desiredReplicas - 1`.
- Runs every reconciliation tick (every 3 seconds).
- Code: `controlplane/AutoScaler.java`

### 3.3 Self-Healing
- Detects containers in `FAILED` state.
- Up to 3 in-place restarts on the same node.
- After 3 failures: **migrate** to a different node.
- Code: `controlplane/SelfHealingController.java`

### 3.4 Node Failure Recovery
- Detects nodes in `FAILED` state.
- Removes all containers from the dead node.
- Re-schedules each container to remaining healthy nodes.
- Code: `controlplane/NodeRecoveryController.java`

### 3.5 Monitoring
- `MonitoringService` captures a `MetricSnapshot` every tick.
- Snapshot includes: active/offline nodes, running/failed containers, CPU/MEM %.
- Both UIs read from the latest snapshot (no direct coupling to model).

### 3.6 Logging
- `EventLogger` (singleton) writes to console (with ANSI colours) and `logs/logs.txt`.
- Levels: DEBUG, INFO, SUCCESS, WARNING, ERROR.

---

## 4. Demo Script (for live presentation)

1. **Start the simulator** – show the banner and cluster bootstrap logs.
2. **Option 1 – Cluster Status** – 3 healthy nodes, balanced CPU/MEM.
3. **Option 3 – Deploy Workload** – deploy `myapp` with 3 replicas; watch scheduler place pods.
4. **Option 7 – Traffic Surge** – activate; wait a few ticks; show autoscaler scale-out to 4+ replicas.
5. **Option 6 – Kill Container** – kill one pod; show self-healing restart it automatically.
6. **Option 4 – Crash Node** – crash `node-2`; watch node-recovery evacuate its pods to `node-1` / `node-3`.
7. **Option 8 – Real-Time Monitor** – live view with progress bars updating every 3 s.
8. **Option 5 – Restore Node** – bring `node-2` back online.
9. **Option 9 – Final Report** – show totals, recovery events, scaled containers.
10. **Option 0 – Exit** – final report printed automatically.

---

## 5. Run Instructions

```bash
# 1. Compile and package
mvn clean package

# 2. Console UI
mvn exec:java

# 3. JavaFX Dashboard (requires display)
mvn javafx:run
```
