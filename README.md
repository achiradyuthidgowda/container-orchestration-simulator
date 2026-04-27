# Container Orchestration Simulator

**Final-Year Mini Project** – A Java-based Container Orchestration Simulator
inspired by Kubernetes and modern cloud platforms.

---

## Features

| Feature | Description |
|---------|-------------|
| Smart Scheduler | Places containers on the least-loaded node; considers CPU and memory |
| Auto Scaling | Scale-out at >80% load; scale-in at <30% load (with min/max bounds) |
| Self-Healing | Restarts failed containers; migrates after 3 failed restarts |
| Node Failure Recovery | Detects node failure; evacuates all containers to healthy nodes |
| Real-Time Monitoring | Active nodes, running containers, CPU/MEM%, recovery events |
| Console UI | Banner, coloured logs, interactive menu, live monitor view |
| JavaFX Dashboard | Live node cards, progress bars, container table, action buttons |
| Logging | Coloured console + persistent `logs/logs.txt` |
| Failure Simulation | Crash node, restore node, kill container, traffic surge |
| Final Report | Session summary: nodes, containers, scaled events, recovery count |

---

## Quick Start

### Prerequisites

- **Java 17+** – https://adoptium.net/
- **Maven 3.8+** – https://maven.apache.org/download.cgi
- JavaFX dependencies are downloaded by Maven automatically

### 1. Clone and Build

```bash
git clone https://github.com/achiradyuthidgowda/container-orchestration-simulator.git
cd container-orchestration-simulator
mvn clean package
```

### 2. Run – Console UI (recommended; works everywhere)

```bash
mvn exec:java
```

Pass `--console` to skip the UI-selection prompt:

```bash
mvn exec:java -Dexec.args="--console"
```

### 3. Run – JavaFX Dashboard

```bash
mvn javafx:run
```

> **Windows note**: Windows Terminal or PowerShell 7+ supports ANSI colours.
> If you see garbled escape codes in cmd.exe, use Windows Terminal instead.

---

## Project Structure

```
src/main/java/com/finalyear/orchestrator/
├── Main.java
├── model/          Node, ContainerInstance, Deployment, Cluster, MetricSnapshot
├── controlplane/   OrchestratorEngine, Scheduler, AutoScaler,
│                   SelfHealingController, NodeRecoveryController
├── monitoring/     MonitoringService
├── logging/        EventLogger, LogLevel
├── simulation/     WorkloadGenerator, FailureInjector
├── report/         FinalReport
├── ui/console/     ConsoleUI
├── ui/fx/          FxDashboardApp
└── util/           ColorUtil
```

---

## Demo Walkthrough (Console UI)

| Step | Action | What you will see |
|------|--------|-------------------|
| 1 | Start | 3 nodes bootstrapped, 5 containers scheduled |
| 2 | Option 1 | Node CPU/MEM bars, container counts |
| 3 | Option 3 | Deploy a new workload with custom replicas |
| 4 | Option 7 | Activate traffic surge – autoscaler creates more replicas |
| 5 | Option 6 | Kill a container – self-healing restarts it on the next tick |
| 6 | Option 4 | Crash node-2 – containers moved to healthy nodes |
| 7 | Option 8 | Live monitor with refreshing bars |
| 8 | Option 5 | Restore node-2 |
| 9 | Option 9 | Final report with all statistics |

---

## Documentation

| File | Description |
|------|-------------|
| `docs/PRESENTATION.md` | Full architecture, feature explanation, demo script |
| `docs/VIVA_QA.md` | 25 viva questions with detailed answers |
| `docs/SAMPLE_OUTPUT.md` | Complete sample console session + JavaFX guidance |

---

## Tech Stack

- **Language**: Java 17
- **Build**: Maven 3.8
- **GUI**: JavaFX 17
- **Concurrency**: `ScheduledExecutorService`, `synchronized`, `volatile`
- **Logging**: Custom `EventLogger` (ANSI colours + file output)
- **Design Patterns**: Singleton, Observer, Strategy, MVC-like separation

---

## License

MIT © 2025 achiradyuthidgowda
