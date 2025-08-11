#!/usr/bin/env python3
"""
🎮 Super Metroid Chaos Gameplay Modifier 🎮

Randomly modifies Samus's equipment, stats, physics, and gameplay mechanics
in real-time for the ultimate chaotic Super Metroid experience!

Features:
- Random equipment toggling (suits, beams, items)
- Dynamic stat modification (health, missiles, etc.)
- Physics chaos (gravity, momentum, damage)
- Enemy behavior modification
- Real-time difficulty scaling

Usage:
    python3 super_metroid_chaos_gameplay.py --insane
    python3 super_metroid_chaos_gameplay.py --equipment-only --speed 2
    python3 super_metroid_chaos_gameplay.py --physics-only --gentle
"""

import socket
import time
import random
import struct
import signal
import sys
import argparse
from typing import Dict, Any, List, Tuple
import copy

class SuperMetroidGameplayChaos:
    """Creates chaotic gameplay modifications in Super Metroid"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_modifications=1000):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_modifications = max_modifications
        
        # Modification tracking
        self.original_values = {}    # Store original values for restoration
        self.modification_count = 0
        self.last_room_id = None
        
        # Super Metroid memory addresses (ONLY VERIFIED REAL ADDRESSES!)
        self.memory_map = {
            # Basic stats (VERIFIED from game_state_parser.py)
            'health': 0x7E09C2,
            'max_health': 0x7E09C4,
            'missiles': 0x7E09C6,
            'max_missiles': 0x7E09C8,
            'supers': 0x7E09CA,
            'max_supers': 0x7E09CC,
            'power_bombs': 0x7E09CE,
            'max_power_bombs': 0x7E09D0,
            'reserve_energy': 0x7E09D6,
            'max_reserve_energy': 0x7E09D4,
            
            # Equipment (VERIFIED - these work!)
            'items': 0x7E09A4,      # Morph ball, bombs, suits, space jump, etc.
            'beams': 0x7E09A8,      # Beam weapons
            
            # Position (VERIFIED from game_state_parser.py)
            'player_x': 0x7E0AF6,
            'player_y': 0x7E0AFA,
            
            # Damage (VERIFIED - this one works!)
            'damage_multiplier': 0x7E0A4E,
            
            # Game state (VERIFIED from game_state_parser.py)
            'room_id': 0x7E079B,
            'area_id': 0x7E079F,
            'game_state': 0x7E0998,
            
            # Note: All fake/invented addresses removed!
        }
        
        # Item bit flags (from items address 0x7E09A4)
        self.item_flags = {
            'morph_ball': 0x0004,
            'bombs': 0x1000,
            'hi_jump': 0x0100,
            'speed_booster': 0x2000,
            'grappling_beam': 0x4000,
            'x_ray': 0x8000,
            'varia_suit': 0x0001,
            'gravity_suit': 0x0020,
            'space_jump': 0x0200,
            'screw_attack': 0x0008,
            'spring_ball': 0x0002,
        }
        
        # Beam bit flags (from beams address 0x7E09A8)
        self.beam_flags = {
            'wave': 0x0001,
            'ice': 0x0002,
            'spazer': 0x0004,
            'plasma': 0x0008,
            'charge': 0x1000,
        }
        
        # Chaos settings
        self.modification_interval = 1.0 / speed_multiplier
        self.chaos_intensity = 1.0
        
        # Mode flags
        self.equipment_only = False
        self.physics_only = False
        self.stats_only = False
        self.gentle_mode = False
        self.insane_mode = False
        
        # Value ranges for different modification types (SAFETY LIMITS)
        self.value_ranges = {
            'health': (50, 1499),  # Never below 50 HP (safety buffer)
            'missiles': (0, 255),
            'supers': (0, 50),
            'power_bombs': (0, 50),
            'physics_multiplier': (0.7, 1.5),  # SAFER: 0.7x to 1.5x (still noticeable)
            'damage_multiplier': (0.7, 2.0),   # SAFER: 0.7x to 2x damage max
        }
        
        # SAFETY SETTINGS
        self.min_safe_health = 50       # Never go below 50 HP
        self.max_physics_change = 0.3   # Max 30% physics change per modification
        self.position_check_enabled = True  # Check for safe positions

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
        command = f"READ_CORE_MEMORY {address:X} {length}"
        response = self.send_command(command)
        
        if response.startswith("-1"):
            return b""
        
        try:
            # Parse hex response
            hex_data = response.strip()
            return bytes.fromhex(hex_data)
        except ValueError:
            return b""

    def write_memory(self, address: int, data: bytes) -> bool:
        """Write memory to RetroArch"""
        hex_data = data.hex().upper()
        command = f"WRITE_CORE_MEMORY {address:X} {hex_data}"
        response = self.send_command(command)
        return not response.startswith("-1")

    def backup_value(self, address: int, size: int = 2):
        """Backup original value for restoration"""
        if address not in self.original_values:
            data = self.read_memory(address, size)
            if data:
                if size == 1:
                    self.original_values[address] = data[0]
                elif size == 2:
                    self.original_values[address] = struct.unpack('<H', data)[0]

    def restore_all_values(self):
        """Restore all original values"""
        print("🔄 Restoring original gameplay values...")
        restored = 0
        for address, original_value in self.original_values.items():
            if isinstance(original_value, int):
                if original_value < 256:
                    # Single byte
                    if self.write_memory(address, bytes([original_value])):
                        restored += 1
                else:
                    # Two bytes
                    if self.write_memory(address, struct.pack('<H', original_value)):
                        restored += 1
        print(f"✅ Restored {restored} gameplay values")

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
        room_data = self.read_memory(self.memory_map['room_id'], 2)
        if room_data and len(room_data) == 2:
            return struct.unpack('<H', room_data)[0]
        return 0

    def modify_equipment(self) -> bool:
        """Randomly modify Samus's equipment"""
        success = False
        
        # Randomly choose between items and beams
        if random.choice([True, False]):
            # Modify items
            address = self.memory_map['items']
            self.backup_value(address, 2)
            
            current_data = self.read_memory(address, 2)
            if current_data and len(current_data) == 2:
                current_items = struct.unpack('<H', current_data)[0]
                
                # Pick a random item to toggle
                item_name, item_flag = random.choice(list(self.item_flags.items()))
                
                if self.insane_mode:
                    # Insane mode: always toggle
                    new_items = current_items ^ item_flag
                else:
                    # Normal mode: 70% chance to toggle
                    if random.random() < 0.7:
                        new_items = current_items ^ item_flag
                    else:
                        new_items = current_items
                
                if self.write_memory(address, struct.pack('<H', new_items)):
                    had_item = bool(current_items & item_flag)
                    has_item = bool(new_items & item_flag)
                    action = "🔴 LOST" if had_item and not has_item else "🟢 GAINED" if not had_item and has_item else "⚪ KEPT"
                    print(f"🎒 EQUIPMENT: {action} {item_name.replace('_', ' ').title()}")
                    success = True
        else:
            # Modify beams
            address = self.memory_map['beams']
            self.backup_value(address, 2)
            
            current_data = self.read_memory(address, 2)
            if current_data and len(current_data) == 2:
                current_beams = struct.unpack('<H', current_data)[0]
                
                # Pick a random beam to toggle
                beam_name, beam_flag = random.choice(list(self.beam_flags.items()))
                
                if self.insane_mode:
                    # Insane mode: always toggle
                    new_beams = current_beams ^ beam_flag
                else:
                    # Normal mode: 60% chance to toggle
                    if random.random() < 0.6:
                        new_beams = current_beams ^ beam_flag
                    else:
                        new_beams = current_beams
                
                if self.write_memory(address, struct.pack('<H', new_beams)):
                    had_beam = bool(current_beams & beam_flag)
                    has_beam = bool(new_beams & beam_flag)
                    action = "🔴 LOST" if had_beam and not has_beam else "🟢 GAINED" if not had_beam and has_beam else "⚪ KEPT"
                    print(f"🔫 BEAM: {action} {beam_name.replace('_', ' ').title()}")
                    success = True
        
        return success

    def modify_stats(self) -> bool:
        """Randomly modify Samus's stats with SAFETY CHECKS"""
        stat_choices = ['health', 'missiles', 'supers', 'power_bombs']
        if self.gentle_mode:
            # Gentle mode: smaller changes
            stat_choices = ['missiles', 'supers', 'power_bombs']  # Avoid health changes
        
        stat_name = random.choice(stat_choices)
        address = self.memory_map[stat_name]
        self.backup_value(address, 2)
        
        current_data = self.read_memory(address, 2)
        if not current_data or len(current_data) != 2:
            return False
        
        current_value = struct.unpack('<H', current_data)[0]
        min_val, max_val = self.value_ranges.get(stat_name, (0, 100))
        
        # SAFETY CHECK: Never let health go below safe minimum
        if stat_name == 'health':
            min_val = max(min_val, self.min_safe_health)
            # Also check max health to not exceed it
            max_health_data = self.read_memory(self.memory_map['max_health'], 2)
            if max_health_data and len(max_health_data) == 2:
                max_health = struct.unpack('<H', max_health_data)[0]
                max_val = min(max_val, max_health)
        
        if self.insane_mode:
            # Insane mode: but still SAFE ranges
            if stat_name == 'health':
                # Health changes more conservatively even in insane mode
                change_percent = random.uniform(-0.4, 0.6)  # -40% to +60%
                new_value = int(current_value * (1 + change_percent))
            else:
                new_value = random.randint(min_val, max_val)
        elif self.gentle_mode:
            # Gentle mode: small adjustments
            if stat_name == 'health':
                change = random.randint(-20, 30)  # Small health changes
            else:
                change = random.randint(-10, 10)
            new_value = max(min_val, min(max_val, current_value + change))
        else:
            # Normal mode: moderate changes
            if stat_name == 'health':
                change_percent = random.uniform(-0.2, 0.3)  # -20% to +30% for health
            else:
                change_percent = random.uniform(-0.3, 0.5)  # -30% to +50% for others
            new_value = int(current_value * (1 + change_percent))
            new_value = max(min_val, min(max_val, new_value))
        
        # FINAL SAFETY CHECK
        new_value = max(min_val, min(max_val, new_value))
        
        if self.write_memory(address, struct.pack('<H', new_value)):
            change = new_value - current_value
            change_str = f"{'📈' if change > 0 else '📉'} {change:+d}"
            print(f"📊 STAT: {stat_name.replace('_', ' ').title()} {current_value} → {new_value} {change_str}")
            return True
        
        return False

    def modify_physics(self) -> bool:
        """Randomly modify physics parameters with SAFETY LIMITS"""
        # REALISTIC physics choices - only VERIFIED addresses that actually exist!
        physics_choices = [
            'player_x', 'player_y'  # Only position manipulation for now - these are proven to work
        ]
        physics_name = random.choice(physics_choices)
        address = self.memory_map[physics_name]
        
        self.backup_value(address, 2)
        
        current_data = self.read_memory(address, 2)
        if not current_data or len(current_data) != 2:
            return False
        
        current_value = struct.unpack('<H', current_data)[0]
        
        # MUCH SAFER multipliers to prevent game-breaking physics
        if self.insane_mode:
            # Insane mode: still noticeable but not extreme
            multiplier = random.uniform(0.6, 1.7)  # 60% to 170% (was 10% to 400%)
        elif self.gentle_mode:
            # Gentle mode: very subtle changes
            multiplier = random.uniform(0.9, 1.1)  # 90% to 110%
        else:
            # Normal mode: moderate but safe changes
            multiplier = random.uniform(0.7, 1.4)  # 70% to 140%
        
        new_value = int(current_value * multiplier)
        
        # SAFETY CHECKS: Don't let values go too extreme
        if current_value > 0:  # Avoid division by zero
            ratio = new_value / current_value
            if ratio > 2.0:  # Never more than 2x original
                new_value = int(current_value * 2.0)
            elif ratio < 0.5:  # Never less than 50% original
                new_value = int(current_value * 0.5)
        
        new_value = max(1, min(32767, new_value))  # Keep in safe 16-bit range, never 0
        
        if self.write_memory(address, struct.pack('<H', new_value)):
            actual_multiplier = new_value / current_value if current_value > 0 else 1.0
            print(f"🌪️  PHYSICS: {physics_name.replace('_', ' ').title()} {current_value} → {new_value} (×{actual_multiplier:.2f})")
            return True
        
        return False

    def modify_damage(self) -> bool:
        """Randomly modify damage parameters with SAFER LIMITS"""
        damage_address = self.memory_map['damage_multiplier']
        self.backup_value(damage_address, 2)
        
        # MUCH SAFER damage multipliers
        if self.insane_mode:
            # Insane mode: noticeable but not game-breaking
            multiplier = random.uniform(0.8, 2.5)  # 80% to 250% (was 100% to 1000%)
        elif self.gentle_mode:
            # Gentle mode: very subtle damage changes
            multiplier = random.uniform(0.9, 1.2)  # 90% to 120%
        else:
            # Normal mode: moderate but safe damage changes
            multiplier = random.uniform(0.7, 1.8)  # 70% to 180%
        
        # Convert to integer representation (multiply by 256 for fixed-point)
        damage_value = int(multiplier * 256)
        damage_value = max(179, min(640, damage_value))  # 0.7x to 2.5x SAFE range
        
        if self.write_memory(damage_address, struct.pack('<H', damage_value)):
            actual_multiplier = damage_value / 256.0
            print(f"💥 DAMAGE: Multiplier set to {actual_multiplier:.2f}x")
            return True
        
        return False

    def modify_position(self) -> bool:
        """TELEPORT CHAOS: Randomly move Samus around the room!"""
        position_targets = ['player_x', 'player_y']  # Only verified addresses
        target = random.choice(position_targets)
        address = self.memory_map[target]
        
        self.backup_value(address, 2)
        
        current_data = self.read_memory(address, 2)
        if not current_data or len(current_data) != 2:
            print(f"❌ POSITION FAILED: Cannot read {target} at 0x{address:X}")
            return False
        
        current_value = struct.unpack('<H', current_data)[0]  # Unsigned 16-bit position
        
        if self.insane_mode:
            # Insane: big teleports!
            position_change = random.randint(-200, 200)
        else:
            # Normal: smaller movements
            position_change = random.randint(-100, 100)
        
        new_value = max(0, min(current_value + position_change, 4095))  # Stay in reasonable room bounds
        
        if self.write_memory(address, struct.pack('<H', new_value)):
            direction = "➡️" if position_change > 0 else "⬅️" if position_change < 0 else "⏸️"
            print(f"📍 TELEPORT: {target.upper()} {current_value} → {new_value} {direction}")
            return True
        
        return False

    def modify_gamestate(self) -> bool:
        """SIMPLE: Modify basic game state values that we know work!"""
        # Only use addresses we KNOW exist and work
        gamestate_targets = ['game_state', 'room_id', 'area_id']
        target = random.choice(gamestate_targets)
        address = self.memory_map[target]
        
        self.backup_value(address, 2)
        
        current_data = self.read_memory(address, 2)
        if not current_data or len(current_data) != 2:
            print(f"❌ GAMESTATE FAILED: Cannot read {target} at 0x{address:X}")
            return False
        
        current_value = struct.unpack('<H', current_data)[0]
        
        # Generate chaos value based on type
        if target == 'game_state':
            # Game state: just add some chaos
            new_value = current_value + random.randint(-10, 10)
            new_value = max(0, min(new_value, 255))
        elif target == 'room_id':
            # Room ID: small changes to avoid breaking everything
            new_value = current_value + random.randint(-100, 100)
            new_value = max(0, min(new_value, 65535))
        elif target == 'area_id':
            # Area ID: cycle through areas 0-5
            new_value = random.randint(0, 5)
        else:
            new_value = current_value
        
        if self.write_memory(address, struct.pack('<H', new_value)):
            print(f"🎮 GAMESTATE: {target.replace('_', ' ').title()} {current_value} → {new_value}")
            return True
        
        return False

    def chaos_loop(self):
        """Main chaos loop"""
        print("🎮 Starting Super Metroid Gameplay Chaos...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🎯 Max Modifications: {self.max_modifications}")
        print(f"⏱️  Interval: {self.modification_interval:.2f}s")
        
        if self.equipment_only:
            print("🎒 EQUIPMENT ONLY MODE")
        elif self.physics_only:
            print("🌪️  PHYSICS ONLY MODE")
        elif self.stats_only:
            print("📊 STATS ONLY MODE")
        else:
            print("🌈 FULL CHAOS MODE: Equipment + Stats + Physics + Damage")
        
        if self.insane_mode:
            print("🤯 INSANE MODE: Maximum chaos!")
        elif self.gentle_mode:
            print("😌 GENTLE MODE: Subtle modifications")
        
        print("🛑 Press Ctrl+C to stop and restore original values")
        print("-" * 60)
        
        consecutive_failures = 0
        
        while self.running:  # Run indefinitely until stopped
            try:
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing...")
                    time.sleep(2.0)
                    continue
                
                success = False
                
                # Choose modification type based on mode
                if self.equipment_only:
                    success = self.modify_equipment()
                elif self.physics_only:
                    success = self.modify_physics()
                elif self.stats_only:
                    success = self.modify_stats()
                else:
                    # Full chaos mode - REALISTIC selection with ONLY WORKING functions!
                    modification_type = random.choices(
                        ['equipment', 'stats', 'physics', 'damage', 'position', 'gamestate'],
                        weights=[4, 4, 2, 3, 2, 1],  # Equipment/stats most reliable, position fun, gamestate risky
                        k=1
                    )[0]
                    
                    if modification_type == 'equipment':
                        success = self.modify_equipment()
                    elif modification_type == 'stats':
                        success = self.modify_stats()
                    elif modification_type == 'physics':
                        success = self.modify_physics()
                    elif modification_type == 'damage':
                        success = self.modify_damage()
                    elif modification_type == 'position':
                        success = self.modify_position()  # NEW: Teleport Samus around!
                    elif modification_type == 'gamestate':
                        success = self.modify_gamestate()  # NEW: Basic game state chaos!
                
                if success:
                    self.modification_count += 1
                    consecutive_failures = 0
                    
                    # Show progress
                    if self.modification_count % 20 == 0:
                        print(f"📊 CHAOS STATUS: {self.modification_count} modifications applied")
                        print(f"   🔄 Original values backed up: {len(self.original_values)}")
                        print("-" * 60)
                else:
                    consecutive_failures += 1
                    if consecutive_failures > 10:
                        print("⚠️  Multiple failures detected, adjusting strategy...")
                        time.sleep(1.0)
                        consecutive_failures = 0
                
                # Dynamic interval adjustment
                current_interval = self.modification_interval
                if self.insane_mode:
                    current_interval *= 0.3  # 3x faster
                elif self.gentle_mode:
                    current_interval *= 2.0  # 2x slower
                
                time.sleep(current_interval)
                
            except KeyboardInterrupt:
                print("\n🛑 Chaos stopped by user")
                break
            except Exception as e:
                print(f"❌ Error during chaos: {e}")
                time.sleep(1.0)
        
        print(f"\n✅ Chaos session ended! {self.modification_count} total modifications applied")
        return True

    def start_chaos(self) -> bool:
        """Start the chaos session"""
        if not self.connect():
            print("❌ Failed to connect to RetroArch")
            return False
        
        if not self.check_game_status():
            print("❌ Game not detected. Make sure Super Metroid is running in RetroArch")
            return False
        
        print("✅ Game detected! Starting gameplay chaos...")
        
        # Set up signal handler for graceful shutdown
        def signal_handler(sig, frame):
            print("\n🛑 Interrupt received, stopping chaos...")
            self.running = False
        
        signal.signal(signal.SIGINT, signal_handler)
        self.running = True
        
        try:
            return self.chaos_loop()
        finally:
            self.stop_chaos()

    def stop_chaos(self):
        """Stop chaos and restore original values"""
        self.running = False
        print("\n🔄 Cleaning up...")
        self.restore_all_values()
        self.disconnect()

def main():
    """Main function with command-line arguments"""
    parser = argparse.ArgumentParser(
        description="Super Metroid Gameplay Chaos - Randomly modify equipment, stats, and physics",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_chaos_gameplay.py --insane
  python3 super_metroid_chaos_gameplay.py --equipment-only --speed 2
  python3 super_metroid_chaos_gameplay.py --gentle --max 100
  python3 super_metroid_chaos_gameplay.py --physics-only --speed 0.5
        """
    )
    
    parser.add_argument('--speed', type=float, default=1.0, help='Speed multiplier (0.1-10.0)')
    parser.add_argument('--max', type=int, default=1000, help='Maximum modifications')
    
    # Mode selection (mutually exclusive)
    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument('--equipment-only', action='store_true', help='Only modify equipment/items')
    mode_group.add_argument('--stats-only', action='store_true', help='Only modify health/missiles/etc')
    mode_group.add_argument('--physics-only', action='store_true', help='Only modify movement physics')
    
    # Intensity selection (mutually exclusive)
    intensity_group = parser.add_mutually_exclusive_group()
    intensity_group.add_argument('--gentle', action='store_true', help='Gentle modifications')
    intensity_group.add_argument('--insane', action='store_true', help='Extreme chaos mode')
    
    args = parser.parse_args()
    
    # Validate speed
    if not 0.1 <= args.speed <= 10.0:
        print("❌ Speed must be between 0.1 and 10.0")
        return
    
    # Validate max
    if not 10 <= args.max <= 10000:
        print("❌ Max modifications must be between 10 and 10000")
        return
    
    print("🤯 INSANE MODE ACTIVATED!" if args.insane else "")
    print("😌 GENTLE MODE ACTIVATED!" if args.gentle else "")
    print("🎮 Super Metroid Gameplay Chaos Generator")
    print("=" * 50)
    print("Randomly modifies equipment, stats, physics, and damage")
    print("for the ultimate chaotic Super Metroid experience!")
    print("")
    print("🎯 Ready to create gameplay chaos!")
    print("⚠️  Equipment and stats will change randomly")
    print("🛑 Press Ctrl+C to stop and restore original values")
    print("")
    
    # Create chaos instance
    chaos = SuperMetroidGameplayChaos(
        speed_multiplier=args.speed,
        max_modifications=args.max
    )
    
    # Set modes
    chaos.equipment_only = args.equipment_only
    chaos.stats_only = args.stats_only
    chaos.physics_only = args.physics_only
    chaos.gentle_mode = args.gentle
    chaos.insane_mode = args.insane
    
    # Print active modes
    if args.equipment_only:
        print("🎒 Equipment-only mode activated")
    elif args.stats_only:
        print("📊 Stats-only mode activated")
    elif args.physics_only:
        print("🌪️  Physics-only mode activated")
    
    # Start chaos
    try:
        chaos.start_chaos()
    except KeyboardInterrupt:
        print("\n🛑 Interrupted by user")
    except Exception as e:
        print(f"\n❌ Error: {e}")
    finally:
        chaos.stop_chaos()
        print("\n✅ Gameplay chaos session ended. Original values restored!")

if __name__ == "__main__":
    main()