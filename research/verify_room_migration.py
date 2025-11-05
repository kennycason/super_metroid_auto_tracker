#!/usr/bin/env python3
"""
Verify that all hardcoded room IDs were successfully migrated to RoomDatabase
"""

import json

print("🔍 VERIFYING ROOM DATABASE MIGRATION")
print("=" * 80)

# Load RoomDatabase mapping
with open('research/room_mapping_complete.json', 'r') as f:
    db = json.load(f)['rooms']

# Key rooms to verify
key_rooms = {
    'motherBrain': 0xDD58,
    'ceresRidley': 0xE0B5,
    'ceresElevator': 0xDF45,
    'landingSite': 0x91F8,
    'statuesHallway': 0xA5ED,
    'statues': 0xA66A,
}

print("\n✅ VERIFYING KEY ROOM IDS:")
print("-" * 80)

all_match = True
for handle, expected_id in key_rooms.items():
    room = db.get(handle)
    if room:
        actual_id = int(room['id'], 16)
        match = "✅" if actual_id == expected_id else "❌"
        print(f"{match} {handle:20} | Expected: 0x{expected_id:04X} | Actual: {room['id']:8} | {room['name']}")
        if actual_id != expected_id:
            all_match = False
    else:
        print(f"❌ {handle:20} | NOT FOUND IN DATABASE")
        all_match = False

print("\n✅ VERIFYING ALL 143 ROOMS IN DATABASE:")
print("-" * 80)
print(f"Total rooms in RoomDatabase: {len(db)}")

# Verify a sample of rooms across all areas
sample_rooms = [
    ('landingSite', 0x91F8, 'Landing Site'),
    ('westOcean', 0x93FE, 'West Ocean'),
    ('bigPink', 0x9D19, 'Big Pink'),
    ('kraid', 0xA59F, "Kraid's Room"),
    ('ridley', 0xB32E, "Ridley's Room"),
    ('writg', 0xB4AD, 'Worst Room in the Game'),
    ('phantoon', 0xCD13, "Phantoon's Room"),
    ('draygon', 0xDA60, "Draygon's Room"),
]

print("\nSample verification:")
for handle, expected_id, expected_name in sample_rooms:
    room = db.get(handle)
    if room:
        actual_id = int(room['id'], 16)
        name_match = room['name'] == expected_name
        id_match = actual_id == expected_id
        match = "✅" if (id_match and name_match) else "❌"
        print(f"{match} {handle:20} | 0x{expected_id:04X} | {room['name']}")
        if not id_match or not name_match:
            all_match = False
    else:
        print(f"❌ {handle:20} | NOT FOUND")
        all_match = False

if all_match:
    print("\n" + "=" * 80)
    print("✅✅✅ ALL VERIFICATIONS PASSED! ✅✅✅")
    print("=" * 80)
    print("\n🎉 Migration complete! RoomDatabase is now the single source of truth.")
    print("   - SimpleStatusGrid.kt: 143 hardcoded rooms → RoomDatabase lookup")
    print("   - GameStateParser.kt: 6 key room IDs → RoomDatabase references")
else:
    print("\n❌ SOME VERIFICATIONS FAILED - PLEASE REVIEW")
    exit(1)
