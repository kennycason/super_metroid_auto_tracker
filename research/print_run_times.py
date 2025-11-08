#!/usr/bin/env python3
"""
Simple script to print split times from a run JSON file in a readable format.

Usage:
    python print_run_times.py <run_file.json>
    python print_run_times.py ~/.smtracker/runs/2025-11-06_05-44-53_kpdr-any.json
"""

import json
import sys
from pathlib import Path

# Map split IDs to display names
SPLIT_NAMES = {
    "ceres_station": "Ceres Station",
    "morph_ball": "Morph Ball",
    "first_missile": "First Missiles",
    "bomb": "Bomb",
    "first_super": "First Super",
    "charge_beam": "Charge Beam",
    "spazer": "Spazer",
    "kraid": "Kraid",
    "varia_suit": "Varia Suit",
    "hi_jump": "Hi-Jump Boots",
    "speed_booster": "Speed Booster",
    "wave_beam": "Wave Beam",
    "ice_beam": "Ice Beam",
    "first_power_bomb": "First Power Bomb",
    "phantoon": "Phantoon",
    "gravity_suit": "Gravity Suit",
    "draygon": "Draygon",
    "space_jump": "Space Jump",
    "plasma_beam": "Plasma Beam",
    "ridley": "Ridley",
    "golden_four": "Golden Four (G4)",
    "mother_brain_1": "Mother Brain 1",
    "mother_brain_2": "Mother Brain 2",
    "ship": "Ship"
}


def format_time(milliseconds: int) -> str:
    """Convert milliseconds to HH:MM:SS.ss format."""
    total_seconds = milliseconds / 1000
    hours = int(total_seconds // 3600)
    minutes = int((total_seconds % 3600) // 60)
    seconds = total_seconds % 60
    
    if hours > 0:
        return f"{hours:02d}:{minutes:02d}:{seconds:06.3f}"
    else:
        return f"{minutes:02d}:{seconds:06.3f}"


def print_run_times(run_file: Path):
    """Print split times from a run JSON file."""
    with open(run_file, 'r') as f:
        run_data = json.load(f)
    
    completed_splits = run_data.get("completedSplits", [])
    total_time = run_data.get("totalTime", 0)
    
    if not completed_splits:
        print("No completed splits found in this run.")
        return
    
    # Find longest split name for alignment
    max_name_length = max(len(SPLIT_NAMES.get(split["splitId"], split["splitId"])) 
                          for split in completed_splits)
    
    print(f"\n{'Split Name':<{max_name_length}}  Segment Time  Total Time")
    print("=" * (max_name_length + 30))
    
    for split in completed_splits:
        split_id = split["splitId"]
        split_name = SPLIT_NAMES.get(split_id, split_id)
        segment_time = split["time"]["segmentTime"]
        total = split["time"]["totalTime"]
        
        segment_str = format_time(segment_time)
        total_str = format_time(total)
        
        print(f"{split_name:<{max_name_length}}  {segment_str:>12}  {total_str:>12}")
    
    print("=" * (max_name_length + 30))
    print(f"{'Final Time':<{max_name_length}}  {'':<12}  {format_time(total_time):>12}")
    print()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    
    run_file = Path(sys.argv[1])
    
    if not run_file.exists():
        print(f"Error: File not found: {run_file}")
        sys.exit(1)
    
    print_run_times(run_file)

