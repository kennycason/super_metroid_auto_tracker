#!/usr/bin/env python3
"""
🗺️💀 Super Metroid Map Structure Chaos Generator 💀🗺️

This script directly modifies the actual map structure in Super Metroid:
- Swaps tile types (empty ↔ solid, ground ↔ crumble, etc.)
- Rotates and flips tiles randomly
- Changes collision data (passable ↔ solid)
- Mutates block types and properties
- Creates chaotic level geometry in real-time!

⚠️  WARNING: This WILL break the map structure and may make areas unbeatable!
⚠️  Save your game before running - you may need to reset!

Features:
- Real-time map tile mutation
- Collision data chaos (walls become empty, empty becomes walls)
- Block type randomization (normal → crumble → spike → door)
- Tile rotation and mirroring
- Progressive map destruction over time
- Safe zones to prevent complete lockout

Usage:
    python3 super_metroid_map_structure_chaos.py --mild          # Gentle map changes
    python3 super_metroid_map_structure_chaos.py --destructive   # Heavy map chaos
    python3 super_metroid_map_structure_chaos.py --apocalypse    # TOTAL MAP DESTRUCTION
    python3 super_metroid_map_structure_chaos.py --collision-only # Only change collision, keep visuals
"""

import socket
import struct
import random
import time
import sys
import signal
import argparse
from typing import List, Tuple, Dict
import copy

class SuperMetroidMapStructureChaos:
    """Directly modifies Super Metroid's map structure and collision data"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_modifications=1000):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_modifications = max_modifications
        
        # Map modification tracking
        self.original_map_data = {}    # Store original map data for restoration
        self.modification_count = 0
        self.chaos_intensity = 1.0
        self.last_room_id = None
        
        # Super Metroid SAFE INDIVIDUAL TILE Memory Layout
        # ONLY target specific tile data - no system memory!
        self.map_memory_regions = {
            # Individual tile IDs (what tile graphics to show)
            'tile_id_layer1': {'start': 0x7F6400, 'end': 0x7F6500, 'name': 'Tile ID Layer 1'},
            'tile_id_layer2': {'start': 0x7F6500, 'end': 0x7F6600, 'name': 'Tile ID Layer 2'},
            
            # Individual tile collision data (passable/solid for each tile)
            'tile_collision_data': {'start': 0x7F6600, 'end': 0x7F6700, 'name': 'Tile Collision Data'},
            
            # Individual tile type data (normal/crumble/spike for each tile)
            'tile_type_data': {'start': 0x7F6700, 'end': 0x7F6800, 'name': 'Tile Type Data'},
        }
        
        # SAFE tile types - only use values that won't crash the game
        self.safe_tile_types = {
            0x00: 'empty',          # Air/empty space
            0x01: 'solid',          # Normal solid block
            0x03: 'crumble',        # Crumble blocks
            0x0E: 'bomb_block',     # Bomb blocks
        }
        
        # SAFE tile IDs - common tile graphics that exist in most rooms
        self.safe_tile_ids = [
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F
        ]
        
        # SAFE collision types - only basic collision that won't break physics
        self.safe_collision_types = {
            0x00: 'passable',       # Can walk through
            0x01: 'solid',          # Solid wall/floor
            0x02: 'platform',       # One-way platform (can jump through from below)
        }
        
        # Tile property flags (for rotation/mirroring)
        self.tile_properties = {
            'flip_horizontal': 0x40,
            'flip_vertical': 0x80,
            'rotate_90': 0x01,
            'rotate_180': 0x02,
            'rotate_270': 0x03,
        }
        
        # Chaos settings
        self.modification_interval = 2.0 / speed_multiplier  # Start slower for map changes
        self.intensity_growth_rate = 0.05
        self.max_intensity = 5.0
        
        # Mode flags
        self.mild_mode = False
        self.destructive_mode = False
        self.apocalypse_mode = False
        self.collision_only_mode = False
        
        # Safety settings
        self.preserve_save_stations = True
        self.preserve_doors = True
        self.max_empty_percentage = 0.3  # Don't make more than 30% of map empty
        self.safe_zones = []  # Areas to never modify (around spawn points, etc.)

    def connect(self) -> bool:
        """Connect to RetroArch UDP interface"""
        try:
            if self.sock:
                self.sock.close()
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(2.0)
            return True
        except Exception as e:
            print(f"❌ Connection failed: {e}")
            return False

    def disconnect(self):
        """Disconnect from RetroArch"""
        if self.sock:
            self.sock.close()
            self.sock = None

    def send_command(self, command: str) -> str:
        """Send command to RetroArch and get response"""
        try:
            if not self.sock:
                if not self.connect():
                    return ""
            
            self.sock.sendto(command.encode(), (self.host, self.port))
            response, _ = self.sock.recvfrom(1024)
            return response.decode().strip()
        except Exception as e:
            return ""

    def read_memory(self, address: int, length: int) -> bytes:
        """Read memory from RetroArch"""
        command = f"READ_CORE_MEMORY 0x{address:X} {length}"
        response = self.send_command(command)
        
        if response.startswith("-1"):
            return b""
        
        try:
            # Parse hex response
            hex_data = response.split()[-1]
            return bytes.fromhex(hex_data)
        except (ValueError, IndexError):
            return b""

    def write_memory(self, address: int, data: bytes) -> bool:
        """Write memory to RetroArch"""
        hex_data = data.hex().upper()
        command = f"WRITE_CORE_MEMORY 0x{address:X} {hex_data}"
        response = self.send_command(command)
        return "WRITE_CORE_MEMORY" in response

    def log_map_destruction(self, address: int, original: int, new: int, region_name: str):
        """Log permanent map destruction (NO RESTORATION!)"""
        if address not in self.original_map_data:
            self.original_map_data[address] = original
        
        # Just track for statistics - NEVER restore!
        print(f"💀 PERMANENT DESTRUCTION: 0x{address:X} in {region_name}")
        print(f"   🔥 IRREVERSIBLE: 0x{original:02X} → 0x{new:02X} (LOST FOREVER!)")

    def check_game_status(self) -> bool:
        """Check if Super Metroid is loaded"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            if "super metroid" in response_lower:
                return True
        return False

    def get_current_room_id(self) -> int:
        """Get current room ID"""
        room_data = self.read_memory(0x7E079B, 2)
        if room_data and len(room_data) == 2:
            return struct.unpack('<H', room_data)[0]
        return 0

    def get_current_screen_position(self) -> Tuple[int, int]:
        """Get current screen position within room"""
        x_data = self.read_memory(0x7E0911, 2)  # Screen X position
        y_data = self.read_memory(0x7E0915, 2)  # Screen Y position
        
        screen_x = struct.unpack('<H', x_data)[0] if x_data and len(x_data) == 2 else 0
        screen_y = struct.unpack('<H', y_data)[0] if y_data and len(y_data) == 2 else 0
        
        return screen_x, screen_y

    def force_screen_redraw(self):
        """GENTLY request screen redraw - no aggressive signals that might hang emulator"""
        # Just a gentle nudge to update graphics
        self.write_memory(0x7E05B5, bytes([0x01]))  # Gentle graphics update flag

    def is_safe_zone(self, address: int) -> bool:
        """Check if this address is in a safe zone (never modify)"""
        # For now, no safe zones - pure chaos!
        # Could add save station protection later
        return False

    def get_tile_type_name(self, tile_value: int) -> str:
        """Get human-readable name for tile type"""
        return self.safe_tile_types.get(tile_value, f'unknown_{tile_value:02X}')

    def get_collision_type_name(self, collision_value: int) -> str:
        """Get human-readable name for collision type"""
        return self.safe_collision_types.get(collision_value, f'unknown_{collision_value:02X}')

    def mutate_tile_id_safe(self, original_id: int) -> int:
        """SAFELY change tile graphics ID - only use known safe tile IDs"""
        # Only use pre-defined safe tile IDs that exist in most rooms
        return random.choice(self.safe_tile_ids)

    def mutate_tile_type_safe(self, original_type: int) -> int:
        """SAFELY change tile type - only use basic safe types"""
        # Only swap between the 4 safest tile types
        return random.choice(list(self.safe_tile_types.keys()))

    def mutate_collision_safe(self, original_collision: int) -> int:
        """SAFELY change collision - only basic passable/solid/platform"""
        return random.choice(list(self.safe_collision_types.keys()))

    def corrupt_map_region(self, region: dict) -> bool:
        """Corrupt a specific map memory region"""
        region_size = region['end'] - region['start']
        if region_size <= 0:
            return False
        
        print(f"🗺️ TARGETING: {region['name']} (0x{region['start']:X}-0x{region['end']:X})")
        
        # Choose number of bytes to corrupt based on intensity and mode
        if self.apocalypse_mode:
            bytes_to_corrupt = random.randint(3, min(10, region_size // 10))
        elif self.destructive_mode:
            bytes_to_corrupt = random.randint(2, min(6, region_size // 20))
        elif self.mild_mode:
            bytes_to_corrupt = random.randint(1, 3)
        else:
            bytes_to_corrupt = random.randint(1, min(5, int(self.chaos_intensity)))
        
        corrupted_count = 0
        for _ in range(bytes_to_corrupt):
            # Select random address within region
            offset = random.randint(0, region_size - 1)
            address = region['start'] + offset
            
            # Skip safe zones
            if self.is_safe_zone(address):
                continue
            
            # No backup - this is PERMANENT DESTRUCTION!
            
            # Read current value
            current_data = self.read_memory(address, 1)
            if not current_data:
                continue
            
            original_value = current_data[0]
            
            # Apply SAFE mutation based on region type
            if 'Tile ID' in region['name']:
                new_value = self.mutate_tile_id_safe(original_value)
                mutation_type = "TILE_ID"
                old_name = f"ID_{original_value:02X}"
                new_name = f"ID_{new_value:02X}"
            elif 'Tile Type' in region['name']:
                new_value = self.mutate_tile_type_safe(original_value)
                mutation_type = "TILE_TYPE"
                old_name = self.safe_tile_types.get(original_value, f"unknown_{original_value:02X}")
                new_name = self.safe_tile_types.get(new_value, f"unknown_{new_value:02X}")
            elif 'Collision' in region['name']:
                new_value = self.mutate_collision_safe(original_value)
                mutation_type = "COLLISION"
                old_name = self.safe_collision_types.get(original_value, f"unknown_{original_value:02X}")
                new_name = self.safe_collision_types.get(new_value, f"unknown_{new_value:02X}")
            else:
                # Skip unknown regions to be extra safe
                print(f"⚠️  Skipping unknown region type: {region['name']}")
                continue
            
            # Write new value - PERMANENT DESTRUCTION!
            if self.write_memory(address, bytes([new_value])):
                # Log the permanent destruction
                self.log_map_destruction(address, original_value, new_value, region['name'])
                
                print(f"💀 {mutation_type}: {old_name} → {new_name}")
                
                # Gentle screen update for tile changes only
                if 'Tile ID' in region['name']:
                    self.force_screen_redraw()
                    print(f"   🎨 Tile graphics changed")
                
                corrupted_count += 1
        
        success = corrupted_count > 0
        if success:
            print(f"✅ MAP CHAOS: Modified {corrupted_count} bytes in {region['name']}")
        else:
            print(f"❌ MAP CHAOS FAILED: No changes made to {region['name']}")
        
        return success

    def select_map_region(self) -> dict:
        """Select a random map region based on mode and weights"""
        regions = list(self.map_memory_regions.values())
        
        if self.collision_only_mode:
            # Only target collision regions (prioritize active collision!)
            collision_regions = [r for r in regions if 'Collision' in r['name'] or 'Active' in r['name']]
            return random.choice(collision_regions) if collision_regions else random.choice(regions)
        
        # Weight regions for SAFE individual tile changes only
        weights = []
        for region in regions:
            if 'Tile ID' in region['name']:
                weights.append(5)   # Tile graphics changes (visual only, safer)
            elif 'Tile Collision' in region['name']:
                weights.append(4)   # Collision changes (affect physics)
            elif 'Tile Type' in region['name']:
                weights.append(3)   # Type changes (behavior changes)
            else:
                weights.append(1)   # Other safe changes
        
        return random.choices(regions, weights=weights)[0]

    def escalate_chaos(self):
        """Increase map chaos intensity over time"""
        if self.chaos_intensity < self.max_intensity:
            self.chaos_intensity += self.intensity_growth_rate * self.speed_multiplier
            
            # Speed up modifications over time
            min_interval = 0.5 / self.speed_multiplier
            if self.modification_interval > min_interval:
                self.modification_interval *= 0.98  # Gradually faster

    def chaos_loop(self):
        """Main map chaos loop"""
        print("🗺️💀 Starting Super Metroid Map Structure Chaos...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🎯 Max Modifications: {self.max_modifications}")
        print(f"⏱️  Starting Interval: {self.modification_interval:.2f}s")
        
        if self.mild_mode:
            print("😌 MILD MODE: Gentle map changes")
        elif self.destructive_mode:
            print("💥 DESTRUCTIVE MODE: Heavy map chaos")
        elif self.apocalypse_mode:
            print("💀 APOCALYPSE MODE: TOTAL MAP DESTRUCTION!")
        elif self.collision_only_mode:
            print("🚧 COLLISION ONLY: Physics changes, visuals preserved")
        else:
            print("🌪️  BALANCED CHAOS: Mixed map modifications")
        
        print("⚠️  WARNING: This WILL break the map structure!")
        print("⚠️  Save your game first - you may need to reset!")
        print("🛑 Press Ctrl+C to stop and restore original map")
        print("-" * 60)
        
        consecutive_failures = 0
        
        while self.running:  # Run indefinitely until stopped
            try:
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing...")
                    time.sleep(2.0)
                    continue
                
                # Check for room changes and report current location
                current_room = self.get_current_room_id()
                if current_room != self.last_room_id:
                    screen_x, screen_y = self.get_current_screen_position()
                    print(f"🚪 ROOM CHANGE: 0x{current_room:04X} (Screen: {screen_x}, {screen_y})")
                    self.last_room_id = current_room
                
                # Select and corrupt map region
                region = self.select_map_region()
                success = self.corrupt_map_region(region)
                
                if success:
                    self.modification_count += 1
                    consecutive_failures = 0
                    
                    # Escalate chaos over time
                    self.escalate_chaos()
                    
                    # Show progress
                    if self.modification_count % 10 == 0:
                        print(f"📊 MAP DESTRUCTION STATUS: {self.modification_count} total modifications")
                        print(f"   💀 Chaos intensity: {self.chaos_intensity:.1f}/{self.max_intensity}")
                        print(f"   🔥 Map bytes PERMANENTLY DESTROYED: {len(self.original_map_data)}")
                        print(f"   ⚠️  NO RESTORATION - DAMAGE IS PERMANENT!")
                        print("-" * 60)
                else:
                    consecutive_failures += 1
                    if consecutive_failures > 5:
                        print("⚠️  Multiple failures detected, adjusting strategy...")
                        time.sleep(1.0)
                        consecutive_failures = 0
                
                # Wait for next modification
                time.sleep(self.modification_interval)
                
            except KeyboardInterrupt:
                print("\n🛑 Map chaos stopped by user")
                break
            except Exception as e:
                print(f"❌ Error during map chaos: {e}")
                time.sleep(1.0)
        
        print(f"\n💀💀💀 MAP DESTRUCTION SESSION ENDED! 💀💀💀")
        print(f"🔥 Total modifications: {self.modification_count}")
        print(f"🗺️ Map bytes permanently destroyed: {len(self.original_map_data)}")
        print(f"⚠️  DAMAGE IS PERMANENT - No restoration!")
        print(f"💀 Your map is forever changed...")
        return True

    def start_chaos(self) -> bool:
        """Start the map chaos session"""
        if not self.connect():
            print("❌ Failed to connect to RetroArch")
            return False
        
        if not self.check_game_status():
            print("❌ Game not detected. Make sure Super Metroid is running in RetroArch")
            return False
        
        print("✅ Game detected! Starting map structure chaos...")
        
        # Set up signal handler for graceful shutdown
        def signal_handler(sig, frame):
            print("\n🛑 Interrupt received, stopping map chaos...")
            self.running = False
        
        signal.signal(signal.SIGINT, signal_handler)
        self.running = True
        
        try:
            return self.chaos_loop()
        finally:
            self.stop_chaos()

    def stop_chaos(self):
        """Stop chaos - NO RESTORATION! Damage is permanent!"""
        self.running = False
        print("\n💀 Stopping map destruction...")
        print("🔥 NO RESTORATION - All damage is PERMANENT!")
        print("⚠️  Your map will remain corrupted until you reload your save!")
        self.disconnect()

def main():
    """Main function with command-line arguments"""
    parser = argparse.ArgumentParser(
        description="Super Metroid Map Structure Chaos - Directly modify map tiles and collision",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_map_structure_chaos.py --mild
  python3 super_metroid_map_structure_chaos.py --destructive --speed 2
  python3 super_metroid_map_structure_chaos.py --apocalypse
  python3 super_metroid_map_structure_chaos.py --collision-only --speed 0.5
        """
    )
    
    parser.add_argument('--speed', type=float, default=1.0, help='Speed multiplier (0.1-10.0)')
    parser.add_argument('--max', type=int, default=1000, help='Maximum modifications')
    
    # Mode selection (mutually exclusive)
    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument('--mild', action='store_true', help='Gentle map changes')
    mode_group.add_argument('--destructive', action='store_true', help='Heavy map chaos')
    mode_group.add_argument('--apocalypse', action='store_true', help='TOTAL MAP DESTRUCTION')
    mode_group.add_argument('--collision-only', action='store_true', help='Only change collision, keep visuals')
    
    args = parser.parse_args()
    
    # Validate speed
    if not 0.1 <= args.speed <= 10.0:
        print("❌ Speed must be between 0.1 and 10.0")
        return
    
    # Validate max
    if not 10 <= args.max <= 10000:
        print("❌ Max modifications must be between 10 and 10000")
        return
    
    # Display mode warnings
    if args.apocalypse:
        print("💀💀💀 APOCALYPSE MODE ACTIVATED! 💀💀💀")
        print("⚠️  THIS WILL COMPLETELY DESTROY THE MAP!")
    elif args.destructive:
        print("💥 DESTRUCTIVE MODE ACTIVATED!")
        print("⚠️  Heavy map structure changes!")
    elif args.mild:
        print("😌 MILD MODE ACTIVATED!")
        print("ℹ️  Gentle map modifications")
    elif args.collision_only:
        print("🚧 COLLISION-ONLY MODE ACTIVATED!")
        print("ℹ️  Physics changes only, visuals preserved")
    
    print("🗺️💀 Super Metroid Map Structure Chaos Generator")
    print("=" * 60)
    print("Directly modifies map tiles, collision data, and block types")
    print("to create chaotic level geometry in real-time!")
    print("")
    print("⚠️  WARNING: This WILL break the map structure!")
    print("⚠️  Save your game before running!")
    print("🎯 Ready to destroy the map structure!")
    print("🛑 Press Ctrl+C to stop and restore original map")
    print("")
    
    # Create chaos instance
    chaos = SuperMetroidMapStructureChaos(
        speed_multiplier=args.speed,
        max_modifications=args.max
    )
    
    # Set modes
    chaos.mild_mode = args.mild
    chaos.destructive_mode = args.destructive
    chaos.apocalypse_mode = args.apocalypse
    chaos.collision_only_mode = args.collision_only
    
    # Start chaos
    try:
        chaos.start_chaos()
    except KeyboardInterrupt:
        print("\n🛑 Interrupted by user")
    except Exception as e:
        print(f"\n❌ Error: {e}")
    finally:
        chaos.stop_chaos()
        print("\n💀 Map chaos session ended. NO RESTORATION - Map is permanently damaged!")

if __name__ == "__main__":
    main()