import re
import json

# Read the file
with open('supermetroid.asl', 'r') as f:
    lines = f.readlines()

# Extract room ID mappings - find the section manually
in_room_enum = False
room_ids = {}
room_comments = {}

for line in lines:
    if 'vars.roomIDEnum = new Dictionary<string, int>' in line:
        in_room_enum = True
        continue
    
    if in_room_enum:
        if '};' in line and '{' not in line:
            break
        
        # Match pattern like: { "landingSite", 0x91F8 },
        match = re.search(r'\{\s*"([^"]+)"\s*,\s*(0x[0-9A-Fa-f]+)\s*\}', line)
        if match:
            handle = match.group(1)
            room_id = match.group(2).upper()
            room_ids[handle] = room_id
            
            # Check for comment
            comment_match = re.search(r'//\s*(.+)$', line)
            if comment_match:
                room_comments[handle] = comment_match.group(1).strip()

print(f"Found {len(room_ids)} room IDs\n")

# Now extract canonical names from the settings
# Some rooms are referenced in multiple ways, we need to map them

# Create comprehensive room data
room_data = {}

for handle, room_id in room_ids.items():
    # Convert handle to human-readable name
    # Start with the handle and try to improve it
    words = re.findall(r'[A-Z]?[a-z]+|[A-Z]+(?=[A-Z][a-z]|\b)', handle)
    default_name = ' '.join(word.capitalize() for word in words)
    
    room_data[handle] = {
        'id': room_id,
        'handle': handle,
        'name': default_name,
        'comment': room_comments.get(handle),
        'aliases': []
    }

# Now let's add the well-known names from context
well_known_names = {
    'landingSite': 'Landing Site',
    'crateriaPowerBombRoom': 'Crateria Power Bomb Room',
    'westOcean': 'West Ocean',
    'crateriaMoat': 'The Moat',
    'gauntletETankRoom': 'Gauntlet Energy Tank Room',
    'climb': 'The Climb',
    'pitRoom': 'Pit Room',
    'bombTorizo': 'Bomb Torizo Room',
    'terminator': 'Terminator Room',
    'greenPirateShaft': 'Green Pirates Shaft',
    'crateriaSupersRoom': 'Crateria Super Room',
    'theFinalMissile': 'The Final Missile',
    'greenBrinstarMainShaft': 'Green Brinstar Main Shaft',
    'sporeSpawnSuper': 'Spore Spawn Super Room',
    'earlySupers': 'Early Supers Room',
    'brinstarReserveRoom': 'Brinstar Reserve Tank Room',
    'bigPink': 'Big Pink',
    'sporeSpawnKeyhunter': 'Spore Spawn Keyhunter Room',
    'sporeSpawn': 'Spore Spawn Room',
    'pinkBrinstarPowerBombRoom': 'Pink Brinstar Power Bomb Room',
    'greenHills': 'Green Hill Zone',
    'noobBridge': 'Noob Bridge',
    'morphBall': 'Morph Ball Room',
    'blueBrinstarETankRoom': 'Blue Brinstar Energy Tank Room',
    'etacoonETankRoom': 'Etacoon Energy Tank Room',
    'etacoonSuperRoom': 'Etacoon Super Room',
    'waterway': 'Waterway',
    'alphaMissileRoom': 'First Missile Room',
    'hopperETankRoom': 'Hopper Energy Tank Room',
    'billyMays': "Billy Mays' Room",
    'redTower': 'Red Tower',
    'xRay': 'X-Ray Room',
    'caterpillar': 'Caterpillar Room',
    'betaPowerBombRoom': 'Beta Power Bomb Room',
    'alphaPowerBombsRoom': 'Alpha Power Bomb Room',
    'bat': 'Bat Room',
    'spazer': 'Spazer Room',
    'warehouseETankRoom': 'Warehouse Energy Tank Room',
    'warehouseZeela': 'Warehouse Zeela Room',
    'warehouseKiHunters': 'Warehouse Kihunter Room',
    'kraidEyeDoor': "Kraid's Eye Door",
    'kraid': "Kraid's Room",
    'statuesHallway': 'Statues Hallway',
    'statues': 'Statues Room',
    'warehouseEntrance': 'Warehouse Entrance',
    'varia': 'Varia Suit Room',
    'cathedral': 'Cathedral',
    'businessCenter': 'Business Center',
    'iceBeam': 'Ice Beam Room',
    'crumbleShaft': 'Crumble Shaft',
    'crocomireSpeedway': 'Crocomire Speedway',
    'crocomire': "Crocomire's Room",
    'hiJump': 'Hi-Jump Room',
    'crocomireEscape': 'Crocomire Escape',
    'hiJumpShaft': 'Hi-Jump Shaft',
    'postCrocomirePowerBombRoom': 'Post Crocomire Power Bomb Room',
    'cosineRoom': 'Cosine Room',
    'preGrapple': 'Post Crocomire Jump Room',
    'grapple': 'Grapple Beam Room',
    'norfairReserveRoom': 'Norfair Reserve Tank Room',
    'greenBubblesRoom': 'Green Bubbles Missile Room',
    'bubbleMountain': 'Bubble Mountain',
    'speedBoostHall': 'Speed Booster Hall',
    'speedBooster': 'Speed Booster Room',
    'singleChamber': 'Single Chamber',
    'doubleChamber': 'Double Chamber',
    'waveBeam': 'Wave Beam Room',
    'volcano': 'Volcano Room',
    'kronicBoost': 'Kronic Boost Room',
    'magdolliteTunnel': 'Magdollite Tunnel',
    'lowerNorfairElevator': 'Lower Norfair Elevator',
    'risingTide': 'Rising Tide',
    'spikyAcidSnakes': 'Spiky Acid Snakes Room',
    'acidStatue': 'Acid Statue Room',
    'mainHall': 'Main Hall',
    'goldenTorizo': "Golden Torizo's Room",
    'ridley': "Ridley's Room",
    'lowerNorfairFarming': 'Lower Norfair Farming Room',
    'mickeyMouse': 'Mickey Mouse Room',
    'pillars': 'Pillar Room',
    'writg': 'Worst Room in the Game',
    'amphitheatre': 'Amphitheatre',
    'lowerNorfairSpringMaze': 'Lower Norfair Springball Maze Room',
    'lowerNorfairEscapePowerBombRoom': 'Lower Norfair Escape Power Bomb Room',
    'redKiShaft': 'Red Kihunter Shaft',
    'wasteland': 'Wasteland',
    'metalPirates': 'Metal Pirates Room',
    'threeMusketeers': "The Musketeers' Room",
    'ridleyETankRoom': 'Ridley Tank Room',
    'screwAttack': 'Screw Attack Room',
    'lowerNorfairFireflea': 'Lower Norfair Fireflea Room',
    'bowling': 'Bowling Alley',
    'wreckedShipEntrance': 'Wrecked Ship Entrance',
    'attic': 'Attic',
    'atticWorkerRobotRoom': 'Wrecked Ship East Missile Room',
    'wreckedShipMainShaft': 'Wrecked Ship Main Shaft',
    'wreckedShipETankRoom': 'Wrecked Ship Energy Tank Room',
    'basement': 'Wrecked Ship Basement',
    'phantoon': "Phantoon's Room",
    'wreckedShipLeftSuperRoom': 'Wrecked Ship West Super Room',
    'wreckedShipRightSuperRoom': 'Wrecked Ship East Super Room',
    'gravity': 'Gravity Suit Room',
    'glassTunnel': 'Glass Tunnel',
    'mainStreet': 'Main Street',
    'mamaTurtle': 'Mama Turtle Room',
    'wateringHole': 'Watering Hole',
    'beach': 'Pseudo Plasma Spark Room',
    'plasmaBeam': 'Plasma Beam Room',
    'maridiaElevator': 'Maridia Elevator',
    'plasmaSpark': 'Plasma Spark Room',
    'toiletBowl': 'Toilet Bowl',
    'oasis': 'Oasis',
    'leftSandPit': 'West Sand Hole',
    'rightSandPit': 'East Sand Hole',
    'aqueduct': 'Aqueduct',
    'butterflyRoom': 'Butterfly Room',
    'botwoonHallway': 'Botwoon Hallway',
    'springBall': 'Spring Ball Room',
    'precious': 'The Precious Room',
    'botwoonETankRoom': 'Botwoon Energy Tank Room',
    'botwoon': "Botwoon's Room",
    'spaceJump': 'Space Jump Room',
    'westCactusAlley': 'West Cacattack Alley',
    'draygon': "Draygon's Room",
    'tourianElevator': 'Tourian Elevator',
    'metroidOne': 'Metroid Room 1',
    'metroidTwo': 'Metroid Room 2',
    'metroidThree': 'Metroid Room 3',
    'metroidFour': 'Metroid Room 4',
    'dustTorizo': 'Dust Torizo Room',
    'tourianHopper': 'Tourian Hopper Room',
    'tourianEyeDoor': 'Tourian Eye Door Room',
    'bigBoy': 'Big Boy Room',
    'motherBrain': "Mother Brain's Room",
    'rinkaShaft': 'Rinka Shaft',
    'tourianEscape4': 'Tourian Escape Room 4',
    'ceresElevator': 'Ceres Elevator',
    'flatRoom': 'Ceres Flat Room',
    'ceresRidley': 'Ceres Ridley Room',
}

# Update with well-known names
for handle, name in well_known_names.items():
    if handle in room_data:
        room_data[handle]['name'] = name

# Print sorted by room ID
print("Complete Room Mapping (sorted by Room ID):")
print("=" * 120)
print(f"{'Room ID':10} | {'Handle':35} | {'Name':60}")
print("=" * 120)

for handle in sorted(room_ids.keys(), key=lambda x: int(room_ids[x], 16)):
    data = room_data[handle]
    comment_str = f" // {data['comment']}" if data['comment'] else ""
    print(f"{data['id']:10} | {handle:35} | {data['name']:60}{comment_str}")

# Save to JSON
output = {'rooms': {}}
for handle, data in room_data.items():
    output['rooms'][handle] = {
        'id': data['id'],
        'handle': handle,
        'name': data['name'],
        'comment': data['comment']
    }

with open('room_mapping_complete.json', 'w') as f:
    json.dump(output, f, indent=2)

print("\n" + "=" * 120)
print(f"\nTotal rooms: {len(room_data)}")
print("Saved complete room mapping to room_mapping_complete.json")
