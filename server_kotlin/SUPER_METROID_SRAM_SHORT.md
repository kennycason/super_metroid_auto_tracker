1. **Basic Stats (0x7E09C2 - 0x7E09D6)**:
  - Health: 0x7E09C2 (2 bytes)
  - Max Health: 0x7E09C4 (2 bytes)
  - Missiles: 0x7E09C6 (2 bytes)
  - Max Missiles: 0x7E09C8 (2 bytes)
  - Super Missiles: 0x7E09CA (2 bytes)
  - Max Super Missiles: 0x7E09CC (2 bytes)
  - Power Bombs: 0x7E09CE (2 bytes)
  - Max Power Bombs: 0x7E09D0 (2 bytes)
  - Reserve Energy: 0x7E09D6 (2 bytes)
  - Max Reserve Energy: 0x7E09D4 (2 bytes)

2. **Location Data**:
  - Room ID: 0x7E079B (2 bytes, little-endian)
  - Area ID: 0x7E079F (1 byte)
    - 0: Crateria
    - 1: Brinstar
    - 2: Norfair
    - 3: Wrecked Ship
    - 4: Maridia
    - 5: Tourian
  - Game State: 0x7E0998 (2 bytes)
  - Player X: 0x7E0AF6 (2 bytes)
  - Player Y: 0x7E0AFA (2 bytes)

3. **Items and Equipment**:
  - Items: 0x7E09A4 (2 bytes)
    - Varia Suit: 0x0001
    - Spring Ball: 0x0002
    - Morph Ball: 0x0004
    - Screw Attack: 0x0008
    - (other items with their bit masks)
  - Beams: 0x7E09A8 (2 bytes)
    - Wave Beam: 0x0001
    - Ice Beam: 0x0002
    - Spazer: 0x0004
    - Plasma: 0x0008
    - Charge Beam: 0x1000

4. **Boss Defeat Status**:
  - Main Bosses: 0x7ED828 (2 bytes)
    - Mother Brain: 0x0001
    - Bomb Torizo: 0x0004
    - Kraid: 0x0100
    - Spore Spawn: 0x0200
    - Ridley: 0x0400
    - Golden Torizo: 0x0800
  - Crocomire: 0x7ED82C (2 bytes, special detection logic)
  - Advanced Boss Detection:
    - Various memory addresses (0x7ED829-0x7ED82D) for complex boss detection
    - Escape timer addresses for Mother Brain phase detection
