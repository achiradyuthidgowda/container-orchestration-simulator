# Sample Console Output

Below is a representative console session demonstrating key features.
ANSI colour codes are shown as descriptive tags like `[GREEN]` / `[RED]`.

---

## Startup

```
  ╔═══════════════════════════════════════════════════════════════╗
  ║   ██████╗ ██████╗ ███████╗     ██████╗ ██████╗ ██╗  ██╗     ║
  ║  ██╔════╝██╔═══██╗██╔════╝    ██╔═══██╗██╔══██╗██║ ██╔╝     ║
  ║  ██║     ██║   ██║███████╗    ██║   ██║██████╔╝█████╔╝      ║
  ║  ██║     ██║   ██║╚════██║    ██║   ██║██╔══██╗██╔═██╗      ║
  ║  ╚██████╗╚██████╔╝███████║    ╚██████╔╝██║  ██║██║  ██╗     ║
  ║   ╚═════╝ ╚═════╝ ╚══════╝     ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝    ║
  ║                                                               ║
  ║        CONTAINER ORCHESTRATION SIMULATOR  v1.0               ║
  ║        Inspired by Kubernetes | Final-Year Project           ║
  ╚═══════════════════════════════════════════════════════════════╝

14:05:01 [SUCCESS] Cluster 'prod-cluster-1' initialised with 3 nodes.
14:05:01 [SUCCESS] Scheduled [nginx-frontend-pod-1] → [node-1]  (cpu=0.5, mem=256MB, util=3.1%)
14:05:01 [SUCCESS] Scheduled [nginx-frontend-pod-2] → [node-3]  (cpu=0.5, mem=256MB, util=0.8%)
14:05:01 [SUCCESS] Scheduled [nginx-frontend-pod-3] → [node-2]  (cpu=0.5, mem=256MB, util=3.1%)
14:05:01 [SUCCESS] Deployed: nginx-frontend (3 replicas)
14:05:01 [SUCCESS] Scheduled [api-backend-pod-1]    → [node-1]  (cpu=1.0, mem=512MB, util=9.4%)
14:05:01 [SUCCESS] Scheduled [api-backend-pod-2]    → [node-3]  (cpu=1.0, mem=512MB, util=3.1%)
14:05:01 [SUCCESS] Deployed: api-backend (2 replicas)
14:05:01 [SUCCESS] OrchestratorEngine started (tick every 3s)
14:05:01 [SUCCESS] Simulator started. Orchestrator engine is running in the background.
```

---

## Main Menu

```
  ┌─────────────────────────────────────────┐
  │          MAIN MENU                      │
  ├─────────────────────────────────────────┤
  │  1. Cluster Status (nodes + resources)  │
  │  2. Container List                      │
  │  3. Deploy New Workload                 │
  │  4. Crash a Node (simulate failure)     │
  │  5. Restore a Node                      │
  │  6. Kill a Container (simulate failure) │
  │  7. Toggle Traffic Surge (autoscaler)   │
  │  8. Real-Time Monitor (live refresh)    │
  │  9. Final Report                        │
  │  0. Exit                                │
  └─────────────────────────────────────────┘
  Enter choice:
```

---

## Option 1 – Cluster Status

```
  ══ CLUSTER STATUS ══

  Cluster      : prod-cluster-1
  Total Nodes  : 3
  Healthy      : [GREEN] 3 [RESET]
  Offline      : [RED]   0 [RESET]

  Nodes:
  node-1       [GREEN] ● HEALTHY [RESET]  CPU [████░░░░░░░░]  18.8%  MEM [████░░░░░░░░]  10.7%  containers=2
  node-2       [GREEN] ● HEALTHY [RESET]  CPU [██░░░░░░░░░░]  12.5%  MEM [███░░░░░░░░░]   3.1%  containers=1
  node-3       [GREEN] ● HEALTHY [RESET]  CPU [███░░░░░░░░░]  12.5%  MEM [██░░░░░░░░░░]   2.4%  containers=2
```

---

## Option 7 – Traffic Surge & Auto-Scaling

```
  Enter choice: 7
14:05:48 [WARNING] Traffic surge ACTIVATED – loads climbing! Autoscaler will react.

  [3 seconds later – autoscaler tick]

14:05:51 [WARNING] [AutoScaler] SCALE-OUT  nginx-frontend       | load=83% > 80% → desired 3 → 4
14:05:51 [SUCCESS] Scheduled [nginx-frontend-pod-4] → [node-3]  (cpu=0.5, mem=256MB, util=4.9%)

  [Another 3 seconds]

14:05:54 [WARNING] [AutoScaler] SCALE-OUT  api-backend          | load=81% > 80% → desired 2 → 3
14:05:54 [SUCCESS] Scheduled [api-backend-pod-3]    → [node-2]  (cpu=1.0, mem=512MB, util=18.8%)
```

---

## Option 4 – Crash Node + Recovery

```
  Enter choice: 4
  Available nodes:
    [GREEN] node-1      [RESET] [HEALTHY]
    [GREEN] node-2      [RESET] [HEALTHY]
    [GREEN] node-3      [RESET] [HEALTHY]
  Enter node name (or RANDOM): node-2

14:06:10 [ERROR]   [FailureInjector] *** NODE CRASHED: node-2 ***

  [Next reconciliation tick]

14:06:13 [ERROR]   [NodeRecovery] Node FAILED: node-2 – evacuating 2 container(s)
14:06:13 [SUCCESS] [NodeRecovery] Rescheduled nginx-frontend-pod-3 → node-1
14:06:13 [SUCCESS] [NodeRecovery] Rescheduled api-backend-pod-3    → node-3
14:06:13 [INFO]    Recovery events: 2
```

---

## Option 6 – Kill Container + Self-Healing

```
  Enter choice: 6
  Enter container name (or RANDOM): RANDOM

14:06:30 [ERROR]   [FailureInjector] *** CONTAINER KILLED: api-backend-pod-1 ***

  [Next tick]

14:06:33 [WARNING] [SelfHealing] Restarted api-backend-pod-1 on node-1 (attempt 1/3)

  [If killed again twice more]

14:06:45 [WARNING] [SelfHealing] Restarted api-backend-pod-1 on node-1 (attempt 3/3)

  [Killed a fourth time]

14:06:58 [SUCCESS] [SelfHealing] Migrated api-backend-pod-1 node-1 → node-3
```

---

## Final Report

```
╔══════════════════════════════════════════════════════════╗
║          CONTAINER ORCHESTRATION SIMULATOR               ║
║                  FINAL SESSION REPORT                   ║
╚══════════════════════════════════════════════════════════╝
  Generated : 2025-01-15 14:10:22
  Cluster   : prod-cluster-1

  ► NODE SUMMARY
    Total Nodes          : 3
    Active Nodes         : [GREEN]  2 [RESET]
    Offline Nodes        : [RED]    1 [RESET]

    [GREEN] HEALTHY [RESET] node-1        CPU [████████░░]  67.2%  MEM [███████░░░]  62.5%
    [RED]   FAILED  [RESET] node-2        CPU [░░░░░░░░░░]   0.0%  MEM [░░░░░░░░░░]   0.0%
    [GREEN] HEALTHY [RESET] node-3        CPU [██████░░░░]  58.3%  MEM [█████░░░░░]  50.0%

  ► CONTAINER SUMMARY
    Running Containers   : [GREEN]  6 [RESET]
    Failed Containers    : [RED]    0 [RESET]
    Scaled Events        : [CYAN]   5 [RESET]
    Recovery Events      : [YELLOW] 7 [RESET]

  ► DEPLOYMENT BREAKDOWN
    nginx-frontend         | running=[GREEN]4[RESET] failed=[RED]0[RESET] scaleOut=2 scaleIn=0
    api-backend            | running=[GREEN]2[RESET] failed=[RED]0[RESET] scaleOut=1 scaleIn=0

══════════════════════════════════════════════════════════
```

---

## JavaFX Dashboard (Screenshot Guidance)

When running `mvn javafx:run`:

1. **Header**: Title + cluster stats bar (active nodes, running containers, CPU/MEM progress bars, surge indicator).
2. **Left panel**: Node cards for each node — name, status badge, CPU bar, MEM bar, container count. Red border = FAILED.
3. **Right panel top**: Container table — name, status, deployment, node, load %, restarts.
4. **Right panel bottom**: Action log (live updates when buttons are pressed).
5. **Footer button bar**:
   - 💥 Crash Random Node
   - 🔧 Restore Node
   - ☠ Kill Random Container
   - 📈 Toggle Traffic Surge
   - 📊 Final Report (opens a scrollable dialog)

> **Tip for demo**: Start the app, wait ~10 seconds for the engine to fill in metrics,
> then click "Toggle Traffic Surge" — watch node cards' CPU bars climb and the
> container count increase as autoscaler creates replicas.
