import re
import json

# Read the file
with open('supermetroid.asl', 'r') as f:
    content = f.read()

# Extract room ID mappings
room_id_section = re.search(r'vars\.roomIDEnum = new Dictionary<string, int>\s*{([^}]+)}', content, re.DOTALL)
room_ids = {}
room_comments = {}

if room_id_section:
    for line in room_id_section.group(1).split('\n'):
        # Match room ID with optional comment
        match = re.search(r'\{\s*"([^"]+)"\s*,\s*(0x[0-9A-F]+)\s*\}(?:\s*,)?(?:\s*//\s*(.+))?', line, re.IGNORECASE)
        if match:
            handle = match.group(1)
            room_id = match.group(2)
            comment = match.group(3).strip() if match.group(3) else None
            room_ids[handle] = room_id
            if comment:
                room_comments[handle] = comment

print(f"Found {len(room_ids)} room IDs")
print("\nRoom IDs (sorted by address):")
print("=" * 100)

# Sort by room ID value
for handle in sorted(room_ids.keys(), key=lambda x: int(room_ids[x], 16)):
    room_id = room_ids[handle]
    comment = room_comments.get(handle, '')
    if comment:
        print(f"{room_id:8} | {handle:40} | {comment}")
    else:
        print(f"{room_id:8} | {handle:40}")

# Now let's extract full names from settings
print("\n\n" + "="*100)
print("Extracting full names from settings...")
print("="*100)

# Look for settings that reference specific rooms
room_name_mapping = {}

# Pattern to match settings that mention room names
patterns = [
    (r'settings\.Add\("([^"]+)"[^"]+?"([^"]*(?:Room|Tank|Beam|Suit|Ball|Bomb|Attack|Jump|Booster)[^"]*)"', 'setting'),
    (r'settings\.SetToolTip\("([^"]+)"[^"]+?in (?:the )?([^"]+?(?:Room|Energy Tank Room|Tank Room)[^"]*?)"', 'tooltip_room'),
    (r'settings\.SetToolTip\("([^"]+)"[^"]+?entering (?:the )?([^"]+)"', 'tooltip_entering'),
]

for pattern, ptype in patterns:
    matches = re.findall(pattern, content, re.IGNORECASE)
    for match in matches:
        setting_name = match[0]
        room_name = match[1]
        if setting_name not in room_name_mapping:
            room_name_mapping[setting_name] = room_name

# Print mapping
print("\nSettings to Room Names:")
for setting in sorted(room_name_mapping.keys()):
    print(f"{setting:40} -> {room_name_mapping[setting]}")

# Save to JSON
output = {
    'rooms': {}
}

for handle in room_ids.keys():
    output['rooms'][handle] = {
        'id': room_ids[handle],
        'handle': handle,
        'comment': room_comments.get(handle, None),
        'fullName': None  # We'll need to manually map these
    }

with open('room_mapping.json', 'w') as f:
    json.dump(output, f, indent=2)

print("\n\nSaved room mapping to room_mapping.json")
