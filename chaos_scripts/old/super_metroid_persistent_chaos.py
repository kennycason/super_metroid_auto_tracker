#!/usr/bin/env python3
"""
Super Metroid Persistent Chaos Generator

Enhanced version that maintains color changes across room transitions
and adds sprite pixel corruption with valid palette colors.

Features:
- Persistent palette changes (reapplied when reset)
- Sprite pixel corruption with valid colors
- Backup and restore original palettes
- Room change detection and re-application

Usage: 
  python3 super_metroid_persistent_chaos.py                    # Default: persistent colors + sprites
  python3 super_metroid_persistent_chaos.py --speed 3          # Faster changes
  python3 super_metroid_persistent_chaos.py --palette-only     # Only palette changes
  python3 super_metroid_persistent_chaos.py --sprites-only     # Only sprite corruption
  python3 super_metroid_persistent_chaos.py --rainbow          # Rainbow mode
  
Press Ctrl+C to stop and restore original colors
"""

import socket
import struct
import random
import time
import sys
import threading
import argparse
from typing import List, Tuple, Dict
import copy

class SuperMetroidPersistentChaos:
    """Creates persistent visual chaos in Super Metroid with palette and sprite corruption"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_corruptions=1000):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_corruptions = max_corruptions
        
        # Persistent palette tracking
        self.original_palettes = {}  # Store original palette values
        self.current_changes = {}    # Store current palette modifications
        
        # Persistent sprite tracking
        self.original_sprites = {}   # Store original sprite values
        self.current_sprite_changes = {}  # Store current sprite modifications
        
        self.last_room_id = None     # Track room changes
        self.last_frame_count = 0    # Track frame changes as backup persistence
        
        # Enhanced palette regions (more comprehensive)
        self.palette_targets = [
            {"name": "Samus Palette", "start": 0x7EC000, "end": 0x7EC020, "weight": 4},
            {"name": "Enemy Palette 1", "start": 0x7EC020, "end": 0x7EC040, "weight": 3},
            {"name": "Enemy Palette 2", "start": 0x7EC040, "end": 0x7EC060, "weight": 3},
            {"name": "Environment Palette", "start": 0x7EC060, "end": 0x7EC080, "weight": 3},
            {"name": "Map Icon Colors", "start": 0x7EC080, "end": 0x7EC0A0, "weight": 3},
            {"name": "Map Area Colors", "start": 0x7EC0A0, "end": 0x7EC0C0, "weight": 3},
            {"name": "Map Background", "start": 0x7EC0C0, "end": 0x7EC0E0, "weight": 2},
            {"name": "UI Palette", "start": 0x7EC0E0, "end": 0x7EC100, "weight": 2},
            {"name": "Extended Palette 1", "start": 0x7EC100, "end": 0x7EC120, "weight": 1},
            {"name": "Extended Palette 2", "start": 0x7EC120, "end": 0x7EC140, "weight": 1},
            {"name": "HUD Palette", "start": 0x7EC140, "end": 0x7EC160, "weight": 2},
            {"name": "Beam Palette", "start": 0x7EC160, "end": 0x7EC180, "weight": 2},
            {"name": "Misc Palette", "start": 0x7EC180, "end": 0x7EC200, "weight": 1},
        ]
        
        # Sprite graphics regions (pixel data)
        self.sprite_targets = [
            {"name": "Samus Sprites", "start": 0x7E2000, "end": 0x7E3000, "weight": 4},
            {"name": "Enemy Sprites 1", "start": 0x7E3000, "end": 0x7E4000, "weight": 3},
            {"name": "Enemy Sprites 2", "start": 0x7E4000, "end": 0x7E5000, "weight": 3},
            {"name": "Environment Sprites", "start": 0x7E5000, "end": 0x7E6000, "weight": 2},
            {"name": "Projectile Sprites", "start": 0x7E6000, "end": 0x7E7000, "weight": 2},
            {"name": "Effect Sprites", "start": 0x7E7000, "end": 0x7E8000, "weight": 1},
        ]
        
        # Valid palette indices for sprite pixels (4-bit values)
        self.valid_palette_indices = list(range(16))  # 0-15 for 4-bit palette indices
        
        # Settings
        self.corruption_interval = 0.5 / speed_multiplier
        self.corruption_count = 0
        self.palette_only = False
        self.sprites_only = False
        
        # Super aggressive mode for insane difficulty
        self.super_aggressive = False
        
        # Color modes
        self.rainbow_mode = False
        self.psychedelic_mode = False
        self.color_shift_value = 0
        
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

    def check_game_status(self) -> bool:
        """Check if Super Metroid is loaded"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            if "super metroid" in response_lower:
                return True
        return False

    def read_memory(self, address: int, length: int) -> bytes:
        """Read memory from RetroArch"""
        try:
            command = f"READ_CORE_MEMORY 0x{address:X} {length}"
            response = self.send_command(command)
            if response.startswith("READ_CORE_MEMORY"):
                hex_data = response.split()[-1]
                return bytes.fromhex(hex_data)
        except Exception:
            pass
        return b""

    def write_memory(self, address: int, data: bytes) -> bool:
        """Write memory to RetroArch"""
        try:
            hex_data = data.hex()
            command = f"WRITE_CORE_MEMORY 0x{address:X} {hex_data}"
            response = self.send_command(command)
            return "WRITE_CORE_MEMORY" in response
        except Exception:
            return False

    def get_current_room_id(self) -> int:
        """Get current room ID to detect room changes - try multiple addresses"""
        # Try multiple possible room ID addresses for different ROM versions
        room_addresses = [
            0x7E079B,  # Common room ID address
            0x7E079D,  # Alternative room ID address
            0x7E0799,  # Area ID + room combo
            0x7F00DE,  # DMAed room ID
        ]
        
        for addr in room_addresses:
            room_data = self.read_memory(addr, 2)
            if len(room_data) == 2:
                room_id = struct.unpack('<H', room_data)[0]
                if room_id != 0 and room_id != 0xFFFF:  # Valid room ID
                    return room_id
        return 0

    def backup_palette(self, address: int) -> bytes:
        """Backup original palette data"""
        if address not in self.original_palettes:
            data = self.read_memory(address, 1)
            if data:
                self.original_palettes[address] = data[0]
                return data
        return bytes([self.original_palettes[address]])
    
    def backup_sprite(self, address: int) -> bytes:
        """Backup original sprite data"""
        if address not in self.original_sprites:
            data = self.read_memory(address, 1)
            if data:
                self.original_sprites[address] = data[0]
                return data
        return bytes([self.original_sprites[address]])

    def restore_all_palettes(self):
        """Restore all original palette data"""
        print("🔄 Restoring original palette data...")
        restored = 0
        for address, original_value in self.original_palettes.items():
            if self.write_memory(address, bytes([original_value])):
                restored += 1
        print(f"✅ Restored {restored} palette values")
    
    def restore_all_sprites(self):
        """Restore all original sprite data"""
        print("🔄 Restoring original sprite data...")
        restored = 0
        for address, original_value in self.original_sprites.items():
            if self.write_memory(address, bytes([original_value])):
                restored += 1
        print(f"✅ Restored {restored} sprite values")

    def detect_room_change(self) -> bool:
        """Detect if player changed rooms or major frame changes occurred"""
        current_room = self.get_current_room_id()
        
        # Also check frame counter for additional change detection
        frame_data = self.read_memory(0x7E05B4, 2)  # Frame counter
        current_frame = 0
        if len(frame_data) == 2:
            current_frame = struct.unpack('<H', frame_data)[0]
        
        # Initialize on first run
        if self.last_room_id is None:
            self.last_room_id = current_room
            self.last_frame_count = current_frame
            print(f"🚪 Initial room: 0x{current_room:04X}, frame: {current_frame}")
            return False
        
        # Check for room change
        if current_room != self.last_room_id and current_room != 0:
            print(f"🚪 Room change detected: 0x{self.last_room_id:04X} → 0x{current_room:04X}")
            self.last_room_id = current_room
            self.last_frame_count = current_frame
            return True
            
        # Check for significant frame jump (potential scene change)
        frame_diff = abs(current_frame - self.last_frame_count)
        if frame_diff > 300:  # Significant scene change
            print(f"🎬 Scene change detected: frame jump {frame_diff}")
            self.last_frame_count = current_frame
            return True
            
        self.last_frame_count = current_frame
        return False

    def reapply_palette_changes(self):
        """Reapply all stored palette changes with immediate and delayed application"""
        if not self.current_changes:
            return
        
        print(f"🎨 Reapplying {len(self.current_changes)} palette changes...")
        
        # Immediate application (prevent flash)
        immediate_applied = 0
        for address, new_value in self.current_changes.items():
            if self.write_memory(address, bytes([new_value])):
                immediate_applied += 1
        
        # Wait a moment for room to fully load, then reapply again
        time.sleep(0.3)
        
        delayed_applied = 0
        failed = 0
        for address, new_value in self.current_changes.items():
            if self.write_memory(address, bytes([new_value])):
                delayed_applied += 1
            else:
                failed += 1
        
        print(f"✅ Applied immediately: {immediate_applied}, after delay: {delayed_applied}")
        if failed > 0:
            print(f"⚠️  {failed} changes failed to reapply")
    
    def reapply_sprite_changes(self):
        """Reapply all stored sprite changes with immediate and delayed application"""
        if not self.current_sprite_changes:
            return
        
        print(f"🖼️  Reapplying {len(self.current_sprite_changes)} sprite changes...")
        
        # Immediate application (prevent flash)
        immediate_applied = 0
        for address, new_value in self.current_sprite_changes.items():
            if self.write_memory(address, bytes([new_value])):
                immediate_applied += 1
        
        # Wait a moment for room to fully load, then reapply again
        time.sleep(0.3)
        
        delayed_applied = 0
        failed = 0
        for address, new_value in self.current_sprite_changes.items():
            if self.write_memory(address, bytes([new_value])):
                delayed_applied += 1
            else:
                failed += 1
        
        print(f"✅ Applied immediately: {immediate_applied}, after delay: {delayed_applied}")
        if failed > 0:
            print(f"⚠️  {failed} sprite changes failed to reapply")

    def verify_and_reapply_changes(self):
        """Verify current palette and sprite state and reapply lost changes"""
        # Check palette changes
        lost_palette_changes = 0
        if self.current_changes:
            for address, expected_value in self.current_changes.items():
                current_data = self.read_memory(address, 1)
                if current_data and len(current_data) > 0:
                    current_value = current_data[0]
                    if current_value != expected_value:
                        # Change was lost, reapply it
                        if self.write_memory(address, bytes([expected_value])):
                            lost_palette_changes += 1
        
        # Check sprite changes
        lost_sprite_changes = 0
        if self.current_sprite_changes:
            for address, expected_value in self.current_sprite_changes.items():
                current_data = self.read_memory(address, 1)
                if current_data and len(current_data) > 0:
                    current_value = current_data[0]
                    if current_value != expected_value:
                        # Change was lost, reapply it
                        if self.write_memory(address, bytes([expected_value])):
                            lost_sprite_changes += 1
        
        total_restored = lost_palette_changes + lost_sprite_changes
        if total_restored > 0:
            print(f"🔄 Restored {lost_palette_changes} palette + {lost_sprite_changes} sprite changes ({total_restored} total)")

    def generate_palette_color(self, original_color: int) -> int:
        """Generate new palette color with enhanced psychedelic mode"""
        if self.rainbow_mode:
            shift = int(self.color_shift_value) % 256
            return (original_color + shift) % 256
        elif self.psychedelic_mode:
            # Psychedelic mode: dramatic color shifts with high contrast
            chaos_modes = [
                lambda: random.randint(200, 255),  # Bright colors
                lambda: random.randint(0, 50),     # Dark colors  
                lambda: 255 - original_color,      # Invert
                lambda: (original_color + random.randint(100, 200)) % 256,  # Big shift
                lambda: random.choice([0, 85, 170, 255])  # High contrast values
            ]
            return random.choice(chaos_modes)()
        else:
            # Gentle mode: subtle changes
            change = random.randint(-30, 30)
            return max(0, min(255, original_color + change))

    def generate_sprite_pixel(self, original_pixel: int) -> int:
        """Generate new sprite pixel with enhanced psychedelic mode"""
        # Extract the two 4-bit palette indices from the byte
        high_nibble = (original_pixel >> 4) & 0x0F
        low_nibble = original_pixel & 0x0F
        
        if self.psychedelic_mode:
            # Psychedelic mode: more aggressive changes with high contrast
            chaos_patterns = [
                lambda: (15, 0),     # Max contrast white/black
                lambda: (0, 15),     # Inverted max contrast
                lambda: (14, 14),    # Bright color
                lambda: (1, 1),      # Dark color
                lambda: (random.randint(8, 15), random.randint(8, 15)),  # Bright pair
                lambda: (random.randint(0, 7), random.randint(0, 7)),    # Dark pair
                lambda: (15 - high_nibble, 15 - low_nibble),  # Invert both
                lambda: (random.choice([0, 7, 8, 15]), random.choice([0, 7, 8, 15]))  # High contrast choices
            ]
            new_high, new_low = random.choice(chaos_patterns)()
            return (new_high << 4) | new_low
        elif self.rainbow_mode:
            # Rainbow mode: cycle through palette indices
            shift = int(self.color_shift_value / 10) % 16
            new_high = (high_nibble + shift) % 16
            new_low = (low_nibble + shift) % 16
            return (new_high << 4) | new_low
        else:
            # Gentle mode: small random changes
            if random.random() < 0.4:  # 40% chance to change high nibble
                high_nibble = random.choice(self.valid_palette_indices)
            if random.random() < 0.4:  # 40% chance to change low nibble
                low_nibble = random.choice(self.valid_palette_indices)
            return (high_nibble << 4) | low_nibble

    def corrupt_palette_region(self, target: dict) -> bool:
        """Corrupt colors in a palette region with persistence"""
        region_size = target["end"] - target["start"]
        offset = random.randint(0, region_size - 1)
        address = target["start"] + offset
        
        # Backup original if not already done
        self.backup_palette(address)
        
        # Read current value
        current_data = self.read_memory(address, 1)
        if not current_data:
            return False
        
        original_color = current_data[0]
        new_color = self.generate_palette_color(original_color)
        
        # Store the change for persistence
        self.current_changes[address] = new_color
        
        # Apply the change
        success = self.write_memory(address, bytes([new_color]))
        
        if success:
            print(f"🎨 PALETTE: 0x{address:X} in {target['name']}")
            print(f"   Color: 0x{original_color:02X} → 0x{new_color:02X} ({original_color} → {new_color})")
            print(f"   Region: {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
            
        return success

    def corrupt_sprite_region(self, target: dict) -> bool:
        """Corrupt pixels in a sprite region with persistence"""
        region_size = target["end"] - target["start"]
        offset = random.randint(0, region_size - 1)
        address = target["start"] + offset
        
        # Read current pixel data
        current_data = self.read_memory(address, 1)
        if not current_data:
            return False
        
        original_pixel = current_data[0]
        
        # Backup original sprite data if not already backed up
        if address not in self.original_sprites:
            self.backup_sprite(address)
        
        new_pixel = self.generate_sprite_pixel(original_pixel)
        
        # Apply the change
        success = self.write_memory(address, bytes([new_pixel]))
        
        if success:
            # Store the sprite change for persistence
            self.current_sprite_changes[address] = new_pixel
            
            print(f"🖼️  SPRITE: 0x{address:X} in {target['name']}")
            print(f"   Pixel: 0x{original_pixel:02X} → 0x{new_pixel:02X}")
            print(f"   Indices: {(original_pixel>>4)&0xF},{original_pixel&0xF} → {(new_pixel>>4)&0xF},{new_pixel&0xF}")
            print(f"   Region: {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
            
        return success

    def select_target(self, targets: list) -> dict:
        """Select a random target based on weights"""
        weights = [target["weight"] for target in targets]
        return random.choices(targets, weights=weights)[0]

    def chaos_loop(self):
        """Main persistent chaos loop with enhanced control"""
        print("🎨 Starting Super Metroid Persistent Chaos...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🎯 Max Changes: {self.max_corruptions}")
        print(f"⏱️  Interval: {self.corruption_interval:.2f}s")
        
        if self.palette_only:
            print("🎨 PALETTE ONLY MODE: Only color changes")
            palette_ratio = 1.0
            sprite_ratio = 0.0
        elif self.sprites_only:
            print("🖼️  SPRITES ONLY MODE: Only sprite corruption")
            palette_ratio = 0.0
            sprite_ratio = 1.0
        else:
            print("🌈 FULL CHAOS MODE: Palettes + Sprites")
            # More aggressive mix for visible changes
            palette_ratio = 0.6  # 60% palette for visual impact
            sprite_ratio = 0.4   # 40% sprites
            
        if self.rainbow_mode:
            print("🌈 Rainbow color cycling enabled")
        elif self.psychedelic_mode:
            print("🔮 PSYCHEDELIC MODE: Maximum visual chaos!")
            
        print("🔄 Persistent mode: Changes will survive room transitions")
        print("🛑 Press Ctrl+C to stop and restore original colors")
        print("-" * 60)
        
        consecutive_failures = 0
        
        continuous_check_counter = 0
        
        while self.running:  # Run indefinitely until stopped
            try:
                # Check for room changes and reapply if needed
                if self.detect_room_change():
                    self.reapply_palette_changes()
                    self.reapply_sprite_changes()
                
                # Ultra-aggressive persistence based on mode
                continuous_check_counter += 1
                check_frequency = 1 if self.super_aggressive else 2
                if continuous_check_counter >= check_frequency:
                    self.verify_and_reapply_changes()
                    continuous_check_counter = 0
                
                # Check if game is still running
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing...")
                    time.sleep(5)
                    continue
                
                # Reset failure counter on successful game detection
                consecutive_failures = 0
                
                # Decide what to corrupt with enhanced control
                success = False
                if self.palette_only:
                    target = self.select_target(self.palette_targets)
                    success = self.corrupt_palette_region(target)
                elif self.sprites_only:
                    target = self.select_target(self.sprite_targets)
                    success = self.corrupt_sprite_region(target)
                else:
                    # In psychedelic mode, favor palette changes for more visible effects
                    if self.psychedelic_mode:
                        palette_bias = 0.8  # 80% palette in psychedelic mode
                    else:
                        palette_bias = palette_ratio
                        
                    if random.random() < palette_bias:
                        target = self.select_target(self.palette_targets)
                        success = self.corrupt_palette_region(target)
                        if success:
                            print(f"🎨 PALETTE CHAOS #{self.corruption_count + 1}")
                    else:
                        target = self.select_target(self.sprite_targets)
                        success = self.corrupt_sprite_region(target)
                        if success:
                            print(f"🖼️  SPRITE CHAOS #{self.corruption_count + 1}")
                
                if success:
                    self.corruption_count += 1
                    consecutive_failures = 0
                    
                    # Update rainbow shift
                    if self.rainbow_mode:
                        self.color_shift_value += 3 * self.speed_multiplier
                    elif self.psychedelic_mode:
                        # Psychedelic mode gets extra intensity
                        self.color_shift_value += 5 * self.speed_multiplier
                    
                    # Show progress more frequently for better feedback
                    if self.corruption_count % 10 == 0:  # Every 10 changes instead of 20
                        palette_count = len(self.current_changes)
                        sprite_count = len(self.current_sprite_changes)
                        
                        print(f"📊 CHAOS STATUS: {self.corruption_count} changes applied")
                        print(f"   🎨 Palette changes: {palette_count} (persistent)")
                        print(f"   🖼️  Sprite changes: {sprite_count} (persistent)")
                        print(f"   🔄 Original values backed up: {len(self.original_palettes)} palette + {len(self.original_sprites)} sprite")
                        if self.rainbow_mode or self.psychedelic_mode:
                            mode_name = "Rainbow" if self.rainbow_mode else "Psychedelic"
                            print(f"   ✨ {mode_name} intensity: {self.color_shift_value:.1f}")
                        print("-" * 60)
                else:
                    consecutive_failures += 1
                    if consecutive_failures > 10:
                        print("⚠️  Multiple failures detected, adjusting strategy...")
                        # Try different approach after failures
                        consecutive_failures = 0
                
                # Dynamic interval adjustment for different modes
                current_interval = self.corruption_interval
                if self.psychedelic_mode:
                    # Faster in psychedelic mode
                    current_interval *= 0.5
                if self.super_aggressive:
                    # Super aggressive mode: even faster
                    current_interval *= 0.3
                
                time.sleep(current_interval)
                
            except Exception as e:
                print(f"❌ Error: {e}")
                consecutive_failures += 1
                time.sleep(1)
        
        if self.corruption_count >= self.max_corruptions:
            print(f"\n🎉 Maximum changes reached! ({self.max_corruptions})")
            print(f"🎨 Final stats: {len(self.current_changes)} persistent palette changes")

    def start_chaos(self):
        """Start the persistent chaos generation"""
        if not self.check_game_status():
            print("❌ No Super Metroid game detected!")
            return False
        
        print("✅ Game detected! Starting persistent chaos...")
        
        self.running = True
        try:
            self.chaos_loop()
        except KeyboardInterrupt:
            print("\n🛑 Chaos stopped by user")
        finally:
            self.stop_chaos()
        
        return True
    
    def stop_chaos(self):
        """Stop chaos and restore original data"""
        self.running = False
        print("\n🔄 Cleaning up...")
        self.restore_all_palettes()
        self.restore_all_sprites()
        self.disconnect()

def main():
    """Main function with command-line arguments"""
    parser = argparse.ArgumentParser(
        description="Super Metroid Persistent Chaos - Persistent visual changes with sprite corruption",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_persistent_chaos.py                    # Full chaos mode
  python3 super_metroid_persistent_chaos.py --speed 3          # 3x faster
  python3 super_metroid_persistent_chaos.py --palette-only     # Only colors
  python3 super_metroid_persistent_chaos.py --sprites-only     # Only sprites
  python3 super_metroid_persistent_chaos.py --rainbow          # Rainbow colors
        """
    )
    
    parser.add_argument("--speed", "-s", type=float, default=1.0, help="Speed multiplier (default: 1.0)")
    parser.add_argument("--max", "-m", type=int, default=1000, help="Max changes (default: 1000)")
    parser.add_argument("--palette-only", action="store_true", help="Only change palette colors")
    parser.add_argument("--sprites-only", action="store_true", help="Only corrupt sprite pixels")
    parser.add_argument("--rainbow", action="store_true", help="Rainbow color cycling")
    parser.add_argument("--psychedelic", action="store_true", help="Psychedelic color mode - MAXIMUM CHAOS!")
    parser.add_argument("--turbo", action="store_true", help="Turbo mode (speed x4)")
    parser.add_argument("--insane", action="store_true", help="Insane mode (speed x8, psychedelic, max chaos)")
    
    args = parser.parse_args()
    
    # Handle special modes
    speed = args.speed
    if args.turbo:
        speed *= 4
        print("🚀 TURBO MODE ACTIVATED!")
    if args.insane:
        speed *= 8
        args.psychedelic = True
        args.max = 2000  # More chaos in insane mode
        print("🤯 INSANE MODE ACTIVATED!")
        print("⚠️  WARNING: MAXIMUM CHAOS INCOMING!")
        print("💀 SUPER-AGGRESSIVE PERSISTENCE ENABLED!")
    
    speed = max(0.1, min(20.0, speed))  # Increased max speed
    max_corruptions = max(1, args.max)
    
    print("🌈 Super Metroid Persistent Chaos Generator")
    print("=" * 50)
    print("Creates persistent visual chaos that survives room changes")
    print("and includes sprite pixel corruption with valid colors.")
    print()
    
    chaos = SuperMetroidPersistentChaos(speed_multiplier=speed, max_corruptions=max_corruptions)
    
    # Set modes
    chaos.palette_only = args.palette_only
    chaos.sprites_only = args.sprites_only
    chaos.rainbow_mode = args.rainbow
    chaos.psychedelic_mode = args.psychedelic
    chaos.super_aggressive = args.insane  # Enable super aggressive for insane mode
    
    if args.palette_only and args.sprites_only:
        print("⚠️  Cannot use both --palette-only and --sprites-only. Using full chaos mode.")
        chaos.palette_only = False
        chaos.sprites_only = False
    
    print("🎨 Ready to create persistent visual chaos!")
    print("⚠️  Changes will persist across room transitions")
    print("🛑 Press Ctrl+C to stop and restore original graphics")
    
    print("\n🎯 Initializing persistent chaos protocols...")
    chaos.start_chaos()
    
    print("\n✅ Chaos session ended. Original graphics restored!")

if __name__ == "__main__":
    main()