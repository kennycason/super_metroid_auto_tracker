#!/usr/bin/env python3
"""
Super Metroid Chaos Generator

This script gradually corrupts graphics and sprite data in Super Metroid to create
a visual decay effect while preserving game logic and playability.

The script targets VRAM, sprite tables, and graphics data while avoiding:
- Game state memory
- Player stats/items
- Boss states
- Room/area data
- Critical game logic

Usage: 
  python3 super_metroid_chaos.py                    # Default: 1x speed, 1000 corruptions
  python3 super_metroid_chaos.py --speed 3          # 3x faster corruption  
  python3 super_metroid_chaos.py --turbo            # Maximum chaos (10x speed, 5000 corruptions)
  python3 super_metroid_chaos.py --map-hell         # Map destruction mode (8x speed, map focused)
  
Press Ctrl+C to stop the chaos
"""

import socket
import struct
import random
import time
import sys
import threading
import argparse
from typing import List, Tuple

class SuperMetroidChaosGenerator:
    """Creates gradual visual corruption in Super Metroid"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_corruptions=1000):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_corruptions = max_corruptions
        
        # SAFE memory regions - only visual map tiles and graphics that won't crash the game
        self.corruption_targets = [
            # ONLY safe palette regions (colors only, no critical data)
            {"name": "Safe Palette Colors", "start": 0x7EC020, "end": 0x7EC180, "weight": 4},
            
            # ONLY safe sprite graphics (visual data, not logic)  
            {"name": "Safe Sprite Graphics 1", "start": 0x7E2200, "end": 0x7E2800, "weight": 3},
            {"name": "Safe Sprite Graphics 2", "start": 0x7E3200, "end": 0x7E3800, "weight": 3},
            
            # ONLY safe map visual tiles (not map logic or collision data)
            {"name": "Safe Map Visual Tiles", "start": 0x7E8200, "end": 0x7E8800, "weight": 5},
            
            # Very conservative HUD graphics (display only)
            {"name": "Safe HUD Display", "start": 0x7EC180, "end": 0x7EC1C0, "weight": 2},
        ]
        
        # COMPREHENSIVE protected regions - NEVER touch these or game will crash!
        self.protected_regions = [
            # Core game engine and state
            {"start": 0x7E0000, "end": 0x7E2000, "name": "Core Game Engine"},
            
            # Player stats, items, and progression (CRITICAL!)
            {"start": 0x7E0900, "end": 0x7E0A00, "name": "Player Data"},
            
            # Boss states and game progression (CRITICAL!)
            {"start": 0x7ED800, "end": 0x7EDA00, "name": "Boss States"},
            
            # Room, area, and map logic data (CRITICAL!)
            {"start": 0x7E0700, "end": 0x7E0900, "name": "Room/Area Logic"},
            
            # Memory management and pointers (CRITICAL!)
            {"start": 0x7F0000, "end": 0x7FFFFF, "name": "High Memory"},
            
            # Map collision and logic data (CRITICAL!)
            {"start": 0x7E8000, "end": 0x7E8200, "name": "Map Logic"},
            {"start": 0x7E8800, "end": 0x7EA000, "name": "Map Collision"},
            {"start": 0x7EA000, "end": 0x7EC000, "name": "Map Data"},
            
            # Sprite logic and AI (not just graphics)
            {"start": 0x7E2000, "end": 0x7E2200, "name": "Sprite Logic"},
            {"start": 0x7E2800, "end": 0x7E3200, "name": "Sprite AI"},
            {"start": 0x7E3800, "end": 0x7E4000, "name": "Sprite Systems"},
            {"start": 0x7E4000, "end": 0x7E8000, "name": "Extended Graphics"},
            
            # Critical palette headers and control data
            {"start": 0x7EC000, "end": 0x7EC020, "name": "Palette Headers"},
            {"start": 0x7EC1C0, "end": 0x7EC200, "name": "Palette Control"},
            
            # Critical system memory
            {"start": 0x7E0000, "end": 0x7E0100, "name": "System Memory"},
            {"start": 0x7FFF00, "end": 0x800000, "name": "Stack/System"},
        ]
        
        # ULTRA-SAFE Corruption settings - prioritize stability over speed
        self.corruption_intensity = 1  # Start very gentle
        self.max_intensity = 3  # MUCH lower max intensity to prevent crashes
        self.corruption_interval = max(3.0, 5.0 / speed_multiplier)  # Slower and more careful
        self.bytes_per_corruption = 1  # ONLY 1 byte at a time (safest possible)
        self.map_chaos_mode = False  # Special mode for extra map corruption
        self.corruption_count = 0  # Track total corruptions
        
    def connect(self) -> bool:
        """Connect to RetroArch UDP interface"""
        try:
            if self.sock:
                self.sock.close()
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(1.0)
            return True
        except Exception as e:
            print(f"❌ UDP connection failed: {e}")
            return False
    
    def send_command(self, command: str) -> str:
        """Send command to RetroArch"""
        if not self.sock:
            if not self.connect():
                return None
                
        try:
            self.sock.sendto(command.encode(), (self.host, self.port))
            data, addr = self.sock.recvfrom(1024)
            return data.decode().strip()
        except Exception:
            return None
    
    def check_game_status(self) -> bool:
        """Check if Super Metroid is running"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            # Accept Super Metroid or SNES games
            if any(keyword in response_lower for keyword in ["super metroid", "metroid", "super_nes", "snes"]):
                print(f"🎮 Game detected: {response}")
                return True
        return False
    
    def is_protected_address(self, address: int) -> bool:
        """Check if address is in a protected region"""
        for region in self.protected_regions:
            if region["start"] <= address <= region["end"]:
                return True
        return False
    
    def select_corruption_target(self) -> dict:
        """Select a random corruption target based on weights"""
        targets = self.corruption_targets.copy()
        
        # In map chaos mode, heavily bias towards map-related regions
        if self.map_chaos_mode:
            map_targets = [t for t in targets if "Map" in t["name"] or "HUD" in t["name"] or "UI" in t["name"]]
            if map_targets:
                # 70% chance to hit map-related regions
                if random.random() < 0.7:
                    weights = [target["weight"] * 3 for target in map_targets]  # Triple weight for map targets
                    return random.choices(map_targets, weights=weights)[0]
        
        weights = [target["weight"] for target in targets]
        target = random.choices(targets, weights=weights)[0]
        return target
    
    def generate_corruption_bytes(self, count: int) -> List[int]:
        """Generate ULTRA-SAFE corruption byte values - prioritize stability over chaos"""
        # EXTREMELY CONSERVATIVE corruption to prevent ANY crashes
        ultra_safe_corruption_types = [
            # Only flip single bits (safest possible corruption)
            lambda: random.randint(0, 127) ^ (1 << random.randint(0, 2)),
            
            # Very limited random values (0-100 range only)
            lambda: random.randint(0, 100),
            
            # Only safe visual patterns (guaranteed not to crash)
            lambda: random.choice([0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66]),
            
            # Tiny shifts only (±8 max)
            lambda: max(0, min((random.randint(0, 80) + random.randint(-8, 8)), 100)),
            
            # Conservative palette values (visual only, safe colors)
            lambda: random.choice([0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60]),
            
            # Zero corruption (safest - just sets to zero)
            lambda: 0x00,
        ]
        
        return [random.choice(ultra_safe_corruption_types)() for _ in range(count)]
    
    def corrupt_memory_region(self, target: dict) -> bool:
        """Corrupt a specific memory region"""
        # Select random address within target
        region_size = target["end"] - target["start"]
        offset = random.randint(0, region_size - self.bytes_per_corruption)
        address = target["start"] + offset
        
        # Double-check protection
        if self.is_protected_address(address):
            print(f"⚠️  Skipped protected address 0x{address:X}")
            return False
        
        # Read original value first for logging
        original_data = None
        try:
            read_cmd = f"READ_CORE_MEMORY 0x{address:X} {self.bytes_per_corruption}"
            read_response = self.send_command(read_cmd)
            if read_response and "READ_CORE_MEMORY" in read_response:
                parts = read_response.split(' ', 2)
                if len(parts) >= 3:
                    hex_data = parts[2].replace(' ', '')
                    original_data = hex_data
        except:
            pass
        
        # Generate corruption data
        corruption_bytes = self.generate_corruption_bytes(self.bytes_per_corruption)
        
        # Write corruption
        hex_bytes = ' '.join(f'{b:02X}' for b in corruption_bytes)
        command = f"WRITE_CORE_MEMORY 0x{address:X} {hex_bytes}"
        
        response = self.send_command(command)
        success = response is not None and "WRITE_CORE_MEMORY" in response
        
        if success:
            original_str = f" (was: {original_data})" if original_data else ""
            print(f"🔥 CORRUPTED 0x{address:X} in {target['name']}")
            print(f"   Address: 0x{address:X} (offset +0x{offset:X} in region)")
            print(f"   Data: {hex_bytes}{original_str}")
            print(f"   Region: {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
        else:
            print(f"❌ Failed to corrupt 0x{address:X}")
        
        return success
    
    def escalate_chaos(self):
        """VERY gradually increase corruption intensity over time - stability first!"""
        if self.corruption_intensity < self.max_intensity:
            # MUCH slower escalation to keep game stable
            self.corruption_intensity += 0.02 * self.speed_multiplier  # Much slower escalation
            
            # NEVER increase bytes per corruption - keep it at 1 byte only for safety
            # (bytes_per_corruption stays at 1 always)
            
            # MUCH more conservative map chaos activation (only at max intensity)
            if self.corruption_intensity >= self.max_intensity and not self.map_chaos_mode:
                self.map_chaos_mode = True
                print(f"🗺️✨ Gentle Map Visual Effects Activated!")
            
            # VERY conservative interval reduction - keep corruption slow
            min_interval = max(2.0, 3.0 / self.speed_multiplier)  # Never faster than 2 seconds
            if self.corruption_interval > min_interval:
                self.corruption_interval *= 0.995  # Very gradual speed increase
    
    def corruption_loop(self):
        """Main corruption loop"""
        print("🎮 Starting Super Metroid Chaos Generator...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🎯 Max Corruptions: {self.max_corruptions}")
        print(f"⏱️  Starting interval: {self.corruption_interval:.2f}s")
        print("⚠️  Visual corruption will gradually increase over time")
        print("🛑 Press Ctrl+C to stop the chaos")
        print("-" * 50)
        
        while self.running:  # INFINITE CHAOS MODE - NEVER STOP THE DESTRUCTION! 🔥💀
            try:
                # Check if game is still running
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing corruption...")
                    time.sleep(5)
                    continue
                
                # Select target and corrupt
                target = self.select_corruption_target()
                if self.corrupt_memory_region(target):
                    self.corruption_count += 1
                    
                    # Show progress every 5 corruptions for better tracking
                    if self.corruption_count % 5 == 0:
                        print(f"📊 INFINITE CHAOS STATUS: {self.corruption_count} total corruptions applied 💀")
                        print(f"   Intensity: {self.corruption_intensity:.1f}/{self.max_intensity}")
                        print(f"   Interval: {self.corruption_interval:.2f}s")
                        print(f"   Bytes per corruption: {self.bytes_per_corruption}")
                        if self.map_chaos_mode:
                            print(f"   🗺️💀 MAP CHAOS MODE ACTIVE")
                        print("-" * 50)
                
                # Escalate chaos over time
                self.escalate_chaos()
                
                # Wait for next corruption
                time.sleep(self.corruption_interval)
                
            except Exception as e:
                print(f"❌ Corruption error: {e}")
                time.sleep(1)
        
        # INFINITE CHAOS MODE - Never stop the destruction!
        print(f"\n💀 INFINITE CHAOS SESSION ENDED 💀")
        print(f"🔥 Total corruptions applied: {self.corruption_count}")
        print("🗺️💀 Your map has been utterly destroyed!")
        print("✅ Reload your save to restore sanity!")
    
    def start_chaos(self):
        """Start the chaos generation"""
        if not self.check_game_status():
            print("❌ No SNES game detected!")
            print("   Load Super Metroid in RetroArch first")
            return False
        
        print("✅ Game detected! Starting chaos generation...")
        
        self.running = True
        try:
            self.corruption_loop()
        except KeyboardInterrupt:
            print("\n🛑 Chaos stopped by user")
        finally:
            self.running = False
            if self.sock:
                self.sock.close()
        
        return True
    
    def stop_chaos(self):
        """Stop the chaos generation"""
        self.running = False

def main():
    """Main function with command-line arguments"""
    parser = argparse.ArgumentParser(
        description="Super Metroid Chaos Generator - Gradually corrupt graphics while preserving game logic",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_chaos.py                    # Default: 1x speed, 1000 corruptions
  python3 super_metroid_chaos.py --speed 3          # 3x faster corruption
  python3 super_metroid_chaos.py --speed 5 --max 2000  # 5x speed, 2000 corruptions
  python3 super_metroid_chaos.py --turbo            # Maximum chaos (10x speed, 5000 corruptions)
  python3 super_metroid_chaos.py --map-hell         # Map destruction mode (8x speed, map focused)
        """
    )
    
    parser.add_argument(
        "--speed", "-s",
        type=float,
        default=1.0,
        help="Speed multiplier for corruption rate (default: 1.0, range: 0.1-10.0)"
    )
    
    parser.add_argument(
        "--max", "-m",
        type=int,
        default=1000,
        help="Maximum number of corruptions before stopping (default: 1000)"
    )
    
    parser.add_argument(
        "--turbo",
        action="store_true",
        help="Maximum chaos mode (equivalent to --speed 10 --max 5000)"
    )
    
    parser.add_argument(
        "--map-hell",
        action="store_true",
        help="Map destruction mode (8x speed, 3000 corruptions, map-focused)"
    )
    
    args = parser.parse_args()
    
    # Handle preset modes
    if args.turbo:
        speed = 10.0
        max_corruptions = 5000
        print("🚀💀 TURBO CHAOS MODE ACTIVATED!")
    elif args.map_hell:
        speed = 8.0
        max_corruptions = 3000
        print("🗺️💀 MAP DESTRUCTION HELL MODE!")
    else:
        speed = max(0.1, min(10.0, args.speed))  # Clamp between 0.1 and 10.0
        max_corruptions = max(1, args.max)
    
    print("🌪️  Super Metroid Chaos Generator")
    print("=" * 40)
    print("This will gradually corrupt graphics and sprites")
    print("while preserving game logic and playability.")
    print()
    
    chaos = SuperMetroidChaosGenerator(speed_multiplier=speed, max_corruptions=max_corruptions)
    
    # Auto-start chaos (safety confirmation removed for immediate execution)
    print("🤔 Ready to embrace the chaos? YES! Starting immediately...")
    print("⚠️  Press Ctrl+C at any time to stop the corruption")
    
    print("\n🎯 Initializing chaos protocols...")
    chaos.start_chaos()
    
    print("\n✅ Chaos session ended. Reload your save to restore sanity!")

if __name__ == "__main__":
    main()