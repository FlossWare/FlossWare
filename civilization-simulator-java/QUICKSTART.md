# Quick Start Guide

## Get Running in 60 Seconds

### 1. Build

```bash
cd civilization-simulator-java
mvn clean package
```

### 2. Run a Single Simulation

```bash
mvn -q exec:java -Dexec.mainClass="org.flossware.civilization.CivilizationSimulator" -Dexec.args="single"
```

**Output:**
```
================================================================================
Alternate History Civilization Simulator v1.0.0
================================================================================

Scenario: Rome Survives to Modern Era
Time range: -27 to 2026
Duration: 2053 years

Running single simulation...

================================================================================
SIMULATION COMPLETE
================================================================================
Duration: 65 ms

FINAL STATE (2026)
--------------------------------------------------------------------------------
Civilization: Roman Empire
Population: 15,423,891
Wealth: 145,876,234
Technologies unlocked: 18
Army size: 145,876
Political stability: 0.67

Performance: 31.6 years/ms
```

### 3. Run Monte Carlo Analysis

```bash
mvn -q exec:java -Dexec.mainClass="org.flossware.civilization.CivilizationSimulator" -Dexec.args="monte"
```

**Output:**
```
Running Monte Carlo analysis (50 runs)...
Using 8 parallel threads

================================================================================
MONTE CARLO ANALYSIS COMPLETE
================================================================================
Total duration: 2500 ms
Average per run: 50 ms

Monte Carlo Analysis (50 runs):
  Average Population: 14,234,567
  Average Wealth: 123,456,789
  Average Techs Unlocked: 17.8
  Survival Rate: 96.0%
```

### 4. Run Tests

```bash
mvn test
```

**Expected:**
```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## What Just Happened?

You simulated the Roman Empire from 27 BCE to 2026 CE:

- **2,053 years** of history in **65 milliseconds**
- **8 interconnected systems**: Climate → Migration → Population → Economy → Technology → Religion → Politics → Military
- **20 technologies** researched through prerequisite chains
- **1,000+ events**: wars, plagues, technological breakthroughs, rebellions
- **Reproducible**: Same seed = same outcome every time

---

## Key Files

| File | Purpose |
|------|---------|
| `CivilizationSimulator.java` | Main entry point |
| `RomeEnuresScenario.java` | Example scenario configuration |
| `SimulationEngine.java` | Core simulation loop |
| `MonteCarloRunner.java` | Parallel execution |
| `*Module.java` | Individual simulation modules |

---

## Next Steps

### Create Your Own Scenario

```java
import org.flossware.civilization.model.*;
import org.flossware.civilization.util.ScenarioBuilder;

Scenario myScenario = new ScenarioBuilder()
    .withName("My Alternate History")
    .withTimeRange(-500, 2000)
    .withInitialState(/* your initial state */)
    .withTechTree(/* your technologies */)
    .withWorldConstraints(new WorldConstraints(0.6, 0.3, 0.2, 0.15, 0.7))
    .withSimulationRules(new SimulationRules("adaptive", true, 42L, 100, 8))
    .build();
```

### Run Your Scenario

```java
SimulationEngine engine = new SimulationEngine(myScenario, 12345L);
SimulationResult result = engine.run(0);

System.out.println("Final population: " + result.finalState().population().population());
```

### Explore the Tech Tree

The Rome scenario includes:
- Agriculture → Mining → Iron Working → Metallurgy
- Steam Engine → Combustion → Electricity
- Semiconductor → Computing → Internet

Each unlocks based on prerequisites, research points, and random diffusion.

---

## Performance Tips

### Faster Simulations
- Use **decade** time steps for stable periods
- Reduce Monte Carlo runs for quick prototyping
- Increase parallel threads (up to CPU cores)

### Deeper Analysis
- Increase Monte Carlo runs (100+)
- Enable event filtering by severity
- Collect statistics across runs

---

## Troubleshooting

### Build Fails
```bash
# Check Java version (need 21+)
java -version

# Clean build
mvn clean package -U
```

### Tests Fail
```bash
# Run specific test
mvn test -Dtest=ReproducibilityTest

# Verbose output
mvn test -X
```

### Out of Memory
```bash
# Increase heap size
mvn exec:java -Dexec.mainClass="..." -Dexec.args="..." -Xmx4g
```

---

## Example Outputs

### Event Types You'll See

- `TECHNOLOGY_UNLOCKED`: New tech researched
- `PLAGUE`: Disease outbreak (population impact)
- `WAR_DECLARED` / `WAR_ENDED`: Military conflicts
- `REBELLION`: Political instability
- `SUCCESSION_CRISIS`: Leadership transition
- `ECONOMIC_BOOM` / `ECONOMIC_COLLAPSE`: Wealth fluctuations
- `CLIMATE_DISASTER`: Environmental catastrophes
- `RELIGIOUS_SCHISM`: Religious splits

### Typical Final States

**Successful Run:**
```
Population: 20,000,000+
Wealth: 500,000,000+
Technologies: 18-20
Stability: 0.6-0.8
```

**Collapse:**
```
Population: < 100,000
Wealth: < 1,000,000
Technologies: 10-15
Stability: < 0.3
```

---

## Deep Dive

Want to understand the internals? See:

- `README.md` - Full documentation
- `IMPLEMENTATION_SUMMARY.md` - Architecture details
- JavaDoc in source code
- Test cases for examples

---

## Community

- Issues: GitHub Issues
- Discussions: GitHub Discussions
- Contributing: See CONTRIBUTING.md

---

**Ready to explore alternate histories? Run the simulation now!**

```bash
mvn -q exec:java -Dexec.mainClass="org.flossware.civilization.CivilizationSimulator" -Dexec.args="single"
```
