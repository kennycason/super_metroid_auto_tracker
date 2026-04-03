# Testing with Custom Data Directory

The Super Metroid Auto Tracker now supports loading runs from a custom data directory, making it safe to test with different datasets **without touching your real `~/.smtracker/` data**.

## Command-Line Usage

### Run with test data

```bash
# Using Gradle
./gradlew run --args="--data-dir=test_data"

# Or with absolute path
./gradlew run --args="--data-dir=/Users/kenny/code/super_metroid_auto_tracker/test_data"

# Using the JAR file
java -jar build/libs/super_metroid_auto_tracker-1.0.0.jar --data-dir=test_data
```

### Run with test resources directly

```bash
./gradlew run --args="--data-dir=src/test/resources/runs"
```

### Show help

```bash
./gradlew run --args="--help"
```

## What Gets Loaded

**IMPORTANT**: The `--data-dir` argument should point to the **parent directory** that contains the `runs/` subdirectory, **NOT** the `runs/` directory itself!

When you specify `--data-dir=<path>`, the tracker will:
- Load runs from `<path>/runs/*.json`
- Save config to `<path>/smtracker.json`
- Save run summaries to `<path>/run-summaries.json`

Expected directory structure:
```
<data-dir>/
  ├── runs/              ← Run files go in this subdirectory
  │   └── *.json
  ├── smtracker.json     ← Config file
  └── run-summaries.json ← Summaries cache
```

**Examples:**
```bash
# ✅ CORRECT - points to parent directory
./gradlew run --args="--data-dir=/Users/kenny/.smtracker"
./gradlew run --args="--data-dir=test_data"

# ❌ WRONG - points to runs/ itself (will look for runs/runs/)
./gradlew run --args="--data-dir=/Users/kenny/.smtracker/runs"
./gradlew run --args="--data-dir=test_data/runs"
```

**Your real `~/.smtracker/` directory is NEVER touched when using `--data-dir`!**

## Test Data Directory

The `test_data/` directory contains all runs from the test resources:
- `kenny/` runs (2 complete runs: 1:06:50 and 1:07:14)
- `mrfoxdemon/` runs (1 complete run: **1:03:55** - fastest PB)
- `mrfoxdemon2/` runs (1 complete run: 1:03:55)

### Expected Personal Best

When loading `test_data/`, the Personal Best should be **1:03:55** from the mrfoxdemon complete run.

## Verifying Run Data

To check which runs are complete:

```bash
cd test_data/runs
for f in *.json; do 
  python3 -c "import json; d=json.load(open('$f')); splits=len(d.get('completedSplits',[])); has_end=d.get('endTime') is not None; total=d.get('totalTime',0)/1000; print(f'{total:7.1f}s ({total/60:5.2f}min) {splits:2d} splits\t$f') if has_end else None" 2>/dev/null
done
```

## How It Works

1. **FileStorageService** now accepts an optional `dataDir` parameter
2. If `dataDir` is provided, it uses that directory directly as the tracker directory
3. If `dataDir` is `null` (default), it uses `~/.smtracker/` as before
4. The main function parses `--data-dir=<path>` from command-line arguments
5. All services are initialized with the custom FileStorageService

## Use Cases

- **Testing with different datasets** without affecting your real runs
- **Debugging PB calculation issues** by loading specific run combinations
- **Demo/presentation mode** with curated run data
- **Unit testing** with controlled test data

