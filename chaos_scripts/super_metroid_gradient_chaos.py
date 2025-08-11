#!/usr/bin/env python3
"""
Super Metroid Gradient Chaos Generator

Creates SMOOTH, FREQUENT color transitions on gradients to give Samus a panic attack!
Enhanced version with psychedelic gradient flows and seamless color morphing.

Features:
- Smooth gradient color transitions (no jarring changes)
- High frequency updates for maximum panic effect
- Persistent changes across room transitions  
- Seamless color morphing with mathematical curves
- Rainbow and psychedelic gradient modes

Usage: 
  python3 super_metroid_gradient_chaos.py                    # Default smooth gradients
  python3 super_metroid_gradient_chaos.py --speed 5          # 5x faster panic mode
  python3 super_metroid_gradient_chaos.py --panic            # MAXIMUM PANIC MODE!
  python3 super_metroid_gradient_chaos.py --rainbow          # Smooth rainbow gradients
  python3 super_metroid_gradient_chaos.py --psychedelic      # Smooth psychedelic flow
  
Press Ctrl+C to stop and restore original colors
"""

import socket
import struct
import random
import time
import sys
import threading
import argparse
import math
from typing import List, Tuple, Dict
import copy

class SuperMetroidGradientChaos:
    """Creates smooth gradient chaos in Super Metroid with seamless color transitions"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_corruptions=1000):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_corruptions = max_corruptions
        
        # Gradient state tracking
        self.original_palettes = {}  # Store original palette values
        self.current_gradients = {}  # Store gradient target states
        self.current_colors = {}     # Store current color values for persistence
        self.gradient_progress = {}  # Track progression (0.0 to 1.0)
        self.gradient_speeds = {}    # Individual gradient speeds
        
        # Persistent sprite tracking
        self.original_sprites = {}   # Store original sprite values
        self.current_sprite_changes = {}  # Store current sprite modifications
        
        self.last_room_id = None     # Track room changes
        self.last_frame_count = 0    # Track frame changes as backup persistence
        
        # ENHANCED: More palette regions for maximum panic effect
        self.palette_targets = [
            {"name": "Samus Palette", "start": 0x7EC000, "end": 0x7EC020, "weight": 5, "gradient_type": "smooth"},
            {"name": "Enemy Palette 1", "start": 0x7EC020, "end": 0x7EC040, "weight": 4, "gradient_type": "wave"},
            {"name": "Enemy Palette 2", "start": 0x7EC040, "end": 0x7EC060, "weight": 4, "gradient_type": "pulse"},
            {"name": "Environment Palette", "start": 0x7EC060, "end": 0x7EC080, "weight": 4, "gradient_type": "smooth"},
            {"name": "Map Icon Colors", "start": 0x7EC080, "end": 0x7EC0A0, "weight": 3, "gradient_type": "rainbow"},
            {"name": "Map Area Colors", "start": 0x7EC0A0, "end": 0x7EC0C0, "weight": 3, "gradient_type": "psychedelic"},
            {"name": "Map Background", "start": 0x7EC0C0, "end": 0x7EC0E0, "weight": 3, "gradient_type": "wave"},
            {"name": "UI Palette", "start": 0x7EC0E0, "end": 0x7EC100, "weight": 3, "gradient_type": "pulse"},
            {"name": "Extended Palette 1", "start": 0x7EC100, "end": 0x7EC120, "weight": 2, "gradient_type": "smooth"},
            {"name": "Extended Palette 2", "start": 0x7EC120, "end": 0x7EC140, "weight": 2, "gradient_type": "rainbow"},
            {"name": "HUD Palette", "start": 0x7EC140, "end": 0x7EC160, "weight": 3, "gradient_type": "psychedelic"},
            {"name": "Beam Palette", "start": 0x7EC160, "end": 0x7EC180, "weight": 3, "gradient_type": "wave"},
            {"name": "Misc Palette", "start": 0x7EC180, "end": 0x7EC200, "weight": 2, "gradient_type": "pulse"},
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
        
        # Settings - MUCH MORE FREQUENT for panic effect!
        self.corruption_interval = 0.1 / speed_multiplier  # 10x faster than original!
        self.corruption_count = 0
        self.palette_only = False
        self.sprites_only = False
        
        # Gradient modes
        self.panic_mode = False          # MAXIMUM FREQUENCY panic mode
        self.rainbow_mode = False        # Smooth rainbow gradients
        self.psychedelic_mode = False    # Smooth psychedelic flow
        self.time_offset = 0.0           # Global time for smooth animations
        
        # Color math constants
        self.gradient_curves = {
            "smooth": lambda t: (math.sin(t * math.pi) + 1) / 2,  # Smooth S-curve
            "wave": lambda t: (math.sin(t * math.pi * 2) + 1) / 2,  # Sine wave
            "pulse": lambda t: (math.sin(t * math.pi * 4) + 1) / 2,  # Fast pulse
            "rainbow": lambda t: t,  # Linear for rainbow cycling
            "psychedelic": lambda t: (math.sin(t * math.pi * 6) + 1) / 2,  # Chaotic wave
        }
        
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
        """Get current room ID to detect room changes"""
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
        """Detect if player changed rooms"""
        current_room = self.get_current_room_id()
        
        # Initialize on first run
        if self.last_room_id is None:
            self.last_room_id = current_room
            print(f"🚪 Initial room: 0x{current_room:04X}")
            return False
        
        # Check for room change
        if current_room != self.last_room_id and current_room != 0:
            print(f"🚪 Room change detected: 0x{self.last_room_id:04X} → 0x{current_room:04X}")
            self.last_room_id = current_room
            return True
            
        return False

    def smooth_color_interpolation(self, start_color: int, end_color: int, progress: float) -> int:
        """Smoothly interpolate between two colors using proper math"""
        # Ensure progress is between 0 and 1
        progress = max(0.0, min(1.0, progress))
        
        # Linear interpolation
        result = int(start_color + (end_color - start_color) * progress)
        return max(0, min(255, result))

    def generate_gradient_target(self, address: int, gradient_type: str) -> Tuple[int, float, float]:
        """Generate a smooth gradient target for an address"""
        # Get original color
        if address not in self.original_palettes:
            self.backup_palette(address)
        
        original_color = self.original_palettes[address]
        
        # Generate target color based on mode
        if self.rainbow_mode:
            # Smooth rainbow cycling
            hue_offset = (self.time_offset * 100) % 360
            # Convert HSV-like to RGB for smooth rainbow
            target_color = int(128 + 127 * math.sin(math.radians(hue_offset + address * 10)))
        elif self.psychedelic_mode:
            # Smooth psychedelic colors with multiple frequencies
            wave1 = math.sin(self.time_offset * 2 + address * 0.01) * 80
            wave2 = math.cos(self.time_offset * 3 + address * 0.02) * 60
            wave3 = math.sin(self.time_offset * 1.5 + address * 0.015) * 40
            target_color = int(original_color + wave1 + wave2 + wave3)
            target_color = max(0, min(255, target_color))
        else:
            # Gentle smooth gradients
            wave = math.sin(self.time_offset + address * 0.001) * 40
            target_color = int(original_color + wave)
            target_color = max(0, min(255, target_color))
        
        # Calculate gradient speed based on panic mode (INCREASED SPEEDS)
        if self.panic_mode:
            gradient_speed = random.uniform(0.25, 0.40)  # Very fast for panic
        else:
            gradient_speed = random.uniform(0.12, 0.30)  # 2-3x faster normal speed
        
        return target_color, gradient_speed, 0.0  # target, speed, initial_progress

    def update_gradient_at_address(self, address: int, target: dict) -> bool:
        """Update a single gradient at an address"""
        gradient_type = target.get("gradient_type", "smooth")
        
        # Initialize gradient if not exists
        if address not in self.current_gradients:
            target_color, speed, progress = self.generate_gradient_target(address, gradient_type)
            self.current_gradients[address] = {
                "start_color": self.original_palettes.get(address, 0),
                "target_color": target_color,
                "progress": progress,
                "speed": speed,
                "gradient_type": gradient_type
            }
        
        gradient = self.current_gradients[address]
        
        # Update progress
        gradient["progress"] += gradient["speed"]
        
        # Check if gradient is complete, generate new target
        if gradient["progress"] >= 1.0:
            # Generate new gradient target
            new_target, new_speed, _ = self.generate_gradient_target(address, gradient_type)
            gradient["start_color"] = gradient["target_color"]  # Current becomes start
            gradient["target_color"] = new_target
            gradient["progress"] = 0.0
            gradient["speed"] = new_speed
        
        # Apply gradient curve
        curve_func = self.gradient_curves.get(gradient_type, self.gradient_curves["smooth"])
        curved_progress = curve_func(gradient["progress"])
        
        # Calculate current color
        current_color = self.smooth_color_interpolation(
            gradient["start_color"], 
            gradient["target_color"], 
            curved_progress
        )
        
        # Write the color
        success = self.write_memory(address, bytes([current_color]))
        
        if success:
            # Store for persistence (FIXED: use current_colors, not sprite_changes!)
            self.current_colors[address] = current_color
            return True
        
        return False

    def corrupt_gradient_region(self, target: dict) -> bool:
        """Apply smooth gradient corruption to a region"""
        region_size = target["end"] - target["start"]
        
        # In panic mode, update more addresses at once
        num_addresses = 3 if self.panic_mode else 1
        
        success_count = 0
        for _ in range(num_addresses):
            offset = random.randint(0, region_size - 1)
            address = target["start"] + offset
            
            if self.update_gradient_at_address(address, target):
                success_count += 1
        
        # Only log occasionally to prevent spam
        if success_count > 0 and self.corruption_count % 20 == 0:
            print(f"🌊 GRADIENT: {target['name']} - {success_count} colors flowing smoothly")
        
        return success_count > 0

    def select_target(self, targets: list) -> dict:
        """Select a random target based on weights"""
        weights = [target["weight"] for target in targets]
        return random.choices(targets, weights=weights)[0]

    def reapply_gradients(self):
        """Reapply all gradient states after room change with immediate + delayed strategy"""
        if not self.current_colors:
            return
        
        print(f"🌊 Reapplying {len(self.current_colors)} gradient colors...")
        
        # IMMEDIATE APPLICATION (prevent flash)
        immediate_applied = 0
        for address, current_color in self.current_colors.items():
            if self.write_memory(address, bytes([current_color])):
                immediate_applied += 1
        
        # Wait for room to fully load, then reapply again
        time.sleep(0.3)
        
        delayed_applied = 0
        failed = 0
        for address, current_color in self.current_colors.items():
            if self.write_memory(address, bytes([current_color])):
                delayed_applied += 1
            else:
                failed += 1
        
        print(f"✅ Applied immediately: {immediate_applied}, after delay: {delayed_applied}")
        if failed > 0:
            print(f"⚠️  {failed} gradient colors failed to reapply")

    def verify_and_reapply_gradients(self):
        """Verify current gradient state and reapply lost colors"""
        if not self.current_colors:
            return
            
        lost_changes = 0
        for address, expected_color in self.current_colors.items():
            current_data = self.read_memory(address, 1)
            if current_data and len(current_data) > 0:
                current_value = current_data[0]
                if current_value != expected_color:
                    # Color was lost, reapply it
                    if self.write_memory(address, bytes([expected_color])):
                        lost_changes += 1
        
        if lost_changes > 0:
            print(f"🔄 Restored {lost_changes} lost gradient colors")

    def chaos_loop(self):
        """Main gradient chaos loop"""
        print("🌊 Starting Super Metroid Gradient Chaos...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🌊 Gradient Interval: {self.corruption_interval:.3f}s")
        
        if self.panic_mode:
            print("💀 PANIC MODE: MAXIMUM FREQUENCY GRADIENT CHAOS!")
        if self.rainbow_mode:
            print("🌈 Rainbow gradient mode: Smooth color cycling")
        elif self.psychedelic_mode:
            print("🔮 PSYCHEDELIC MODE: Smooth multi-wave chaos!")
            
        print("🌊 Smooth gradients will flow seamlessly")
        print("🛑 Press Ctrl+C to stop and restore original colors")
        print("-" * 60)
        
        consecutive_failures = 0
        continuous_check_counter = 0
        
        while self.running:  # Run indefinitely until stopped
            try:
                # Update global time for smooth animations (FASTER)
                self.time_offset += self.corruption_interval * 25
                
                # Check for room changes and reapply gradients
                if self.detect_room_change():
                    self.reapply_gradients()
                
                # AGGRESSIVE PERSISTENCE: Continuously verify and reapply
                continuous_check_counter += 1
                check_frequency = 1 if self.panic_mode else 3  # More aggressive in panic mode
                if continuous_check_counter >= check_frequency:
                    self.verify_and_reapply_gradients()
                    continuous_check_counter = 0
                
                # Check if game is still running
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing...")
                    time.sleep(5)
                    continue
                
                # Reset failure counter on successful game detection
                consecutive_failures = 0
                
                # Apply gradient chaos
                success = False
                if self.palette_only or not self.sprites_only:
                    target = self.select_target(self.palette_targets)
                    success = self.corrupt_gradient_region(target)
                
                if success:
                    self.corruption_count += 1
                    consecutive_failures = 0
                    
                    # Show progress less frequently to avoid spam
                    if self.corruption_count % 50 == 0:  # Every 50 changes
                        active_gradients = len(self.current_gradients)
                        persistent_colors = len(self.current_colors)
                        
                        print(f"📊 GRADIENT STATUS: {self.corruption_count} updates applied")
                        print(f"   🌊 Active gradients: {active_gradients}")
                        print(f"   🎨 Persistent colors: {persistent_colors}")
                        print(f"   ⚡ Update frequency: {1/self.corruption_interval:.1f} Hz")
                        print(f"   🔄 Original colors backed up: {len(self.original_palettes)}")
                        if self.rainbow_mode or self.psychedelic_mode or self.panic_mode:
                            modes = []
                            if self.rainbow_mode: modes.append("Rainbow")
                            if self.psychedelic_mode: modes.append("Psychedelic")  
                            if self.panic_mode: modes.append("Panic")
                            print(f"   ✨ Modes: {', '.join(modes)}")
                        print("-" * 60)
                else:
                    consecutive_failures += 1
                    if consecutive_failures > 10:
                        print("⚠️  Multiple failures detected, adjusting strategy...")
                        consecutive_failures = 0
                
                # Use the fast interval for smooth gradients
                time.sleep(self.corruption_interval)
                
            except Exception as e:
                print(f"❌ Error: {e}")
                consecutive_failures += 1
                time.sleep(1)

    def start_chaos(self):
        """Start the gradient chaos generation"""
        if not self.check_game_status():
            print("❌ No Super Metroid game detected!")
            return False
        
        print("✅ Game detected! Starting gradient chaos...")
        
        self.running = True
        try:
            self.chaos_loop()
        except KeyboardInterrupt:
            print("\n🛑 Gradient chaos stopped by user")
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
        description="Super Metroid Gradient Chaos - Smooth, frequent color transitions for maximum panic effect!",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_gradient_chaos.py                    # Smooth gradients
  python3 super_metroid_gradient_chaos.py --speed 5          # 5x faster 
  python3 super_metroid_gradient_chaos.py --panic            # MAXIMUM PANIC!
  python3 super_metroid_gradient_chaos.py --rainbow          # Smooth rainbow
  python3 super_metroid_gradient_chaos.py --psychedelic      # Smooth psychedelic
        """
    )
    
    parser.add_argument("--speed", "-s", type=float, default=1.0, help="Speed multiplier (default: 1.0)")
    parser.add_argument("--max", "-m", type=int, default=10000, help="Max changes (default: 10000)")
    parser.add_argument("--palette-only", action="store_true", help="Only change palette colors")
    parser.add_argument("--rainbow", action="store_true", help="Smooth rainbow gradient cycling")
    parser.add_argument("--psychedelic", action="store_true", help="Smooth psychedelic gradient flow")
    parser.add_argument("--panic", action="store_true", help="PANIC MODE - maximum frequency chaos!")
    
    args = parser.parse_args()
    
    # Handle special modes
    speed = args.speed
    if args.panic:
        speed *= 10  # 10x faster for maximum panic!
        print("💀 PANIC MODE ACTIVATED!")
        print("⚠️  WARNING: MAXIMUM FREQUENCY GRADIENT CHAOS!")
        print("🌊 SAMUS PANIC ATTACK INCOMING!")
    
    speed = max(0.1, min(50.0, speed))  # Allow very high speeds for panic mode
    max_corruptions = max(1, args.max)
    
    print("🌊 Super Metroid Gradient Chaos Generator")
    print("=" * 50)
    print("Creates smooth, frequent color gradients that flow seamlessly")
    print("for the ultimate panic attack experience!")
    print()
    
    chaos = SuperMetroidGradientChaos(speed_multiplier=speed, max_corruptions=max_corruptions)
    
    # Set modes
    chaos.palette_only = args.palette_only
    chaos.rainbow_mode = args.rainbow
    chaos.psychedelic_mode = args.psychedelic
    chaos.panic_mode = args.panic
    
    print("🌊 Ready to create smooth gradient chaos!")
    print("💀 Prepare for Samus's panic attack!")
    print("🛑 Press Ctrl+C to stop and restore original colors")
    
    print("\n🎯 Initializing gradient flow protocols...")
    chaos.start_chaos()
    
    print("\n✅ Gradient chaos session ended. Original colors restored!")

if __name__ == "__main__":
    main()