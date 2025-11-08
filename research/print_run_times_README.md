# Print Run Times Scripts

Two simple scripts to display split times from run JSON files in a readable format.

## Python Version (Recommended)

```bash
# Make it executable (already done)
chmod +x print_run_times.py

# Run it
./print_run_times.py ~/.smtracker/runs/2025-11-06_05-44-53_kpdr-any.json

# Or use python directly
python3 print_run_times.py ~/.smtracker/runs/2025-11-06_05-44-53_kpdr-any.json
```

## Bash + jq Version

```bash
# Make it executable (already done)
chmod +x print_run_times.sh

# Run it
./print_run_times.sh ~/.smtracker/runs/2025-11-06_05-44-53_kpdr-any.json
```

## Simple jq One-Liner (no formatting)

If you just want the raw data without name mapping or time formatting:

```bash
jq -r '.completedSplits[] | "\(.splitId): \(.time.segmentTime)ms"' ~/.smtracker/runs/2025-11-06_05-44-53_kpdr-any.json
```

Or with some basic formatting:

```bash
jq -r '.completedSplits[] | 
  .splitId + " " + 
  ((.time.segmentTime / 1000 / 60) | floor | tostring) + ":" + 
  ((.time.segmentTime / 1000 % 60) | tostring)' \
  ~/.smtracker/runs/2025-11-06_05-44-53_kpdr-any.json
```

## Output Format

Both scripts produce nicely formatted output:

```
Split Name        Segment Time  Total Time
==============================================
Ceres Station        01:40.299     01:40.299
Morph Ball           01:46.349     03:26.648
First Missiles       00:23.313     03:49.961
...
Ship                 02:42.412  01:05:09.877
==============================================
Final Time                      01:05:09.877
```

## Notes

- Times are in milliseconds in the JSON files
- Formatted as `MM:SS.sss` or `HH:MM:SS.sss` depending on duration
- Both scripts include all 24 splits from the KPDR Any% route

