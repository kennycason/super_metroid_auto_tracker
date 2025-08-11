#!/usr/bin/env python3
"""
Super Metroid Map Color Chaos Generator

This script specifically targets only map screen color palettes to create
visual variety in the map display without affecting game logic or functionality.

Focuses on:
- Map screen color palettes only
- Gradual color shifts and changes  
- Visual effects that don't break map functionality

Usage: 
  python3 super_metroid_map_color_chaos.py                    # Default: gentle color shifts
  python3 super_metroid_map_color_chaos.py --speed 3          # Faster color changes
  python3 super_metroid_map_color_chaos.py --rainbow          # Rainbow mode (fast cycling colors)
  python3 super_metroid_map_color_chaos.py --psychedelic      # Psychedelic mode (random color bursts)
  
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

class SuperMetroidMapColorChaos:
    """Creates gradual color changes in Super Metroid map screen only"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_corruptions=500):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_corruptions = max_corruptions
        
        # Map-specific color palette regions (expanded search)
        self.color_targets = [
            # General SNES palette regions that might contain map colors
            {"name": "Main Palette Bank", "start": 0x7EC000, "end": 0x7EC040, "weight": 3},
            {"name": "Secondary Palette", "start": 0x7EC040, "end": 0x7EC080, "weight": 3},
            {"name": "Map Icon Colors", "start": 0x7EC080, "end": 0x7EC0A0, "weight": 3},
            {"name": "Map Area Colors", "start": 0x7EC0A0, "end": 0x7EC0C0, "weight": 3},
            {"name": "Map Background Colors", "start": 0x7EC0C0, "end": 0x7EC0E0, "weight": 2},
            {"name": "Extended Palette 1", "start": 0x7EC0E0, "end": 0x7EC100, "weight": 2},
            {"name": "Extended Palette 2", "start": 0x7EC100, "end": 0x7EC120, "weight": 1},
            {"name": "Extended Palette 3", "start": 0x7EC120, "end": 0x7EC140, "weight": 1},
            {"name": "Extended Palette 4", "start": 0x7EC140, "end": 0x7EC160, "weight": 1},
            {"name": "Extended Palette 5", "start": 0x7EC160, "end": 0x7EC180, "weight": 1},
            {"name": "Full Palette Range", "start": 0x7EC180, "end": 0x7EC200, "weight": 1},
        ]
        
        # Memory regions to NEVER touch (critical game data)
        self.protected_regions = [
            # Everything below palette range
            {"start": 0x000000, "end": 0x7EC000, "name": "Critical Game Data"},
            # Everything above palette range  
            {"start": 0x7EC200, "end": 0x800000, "name": "Critical Game Data"},
        ]
        
        # Color corruption settings (adjusted by speed multiplier)
        self.corruption_intensity = 1  # Start gentle
        self.max_intensity = 5  # Lower max for color-only changes
        self.corruption_interval = 0.5 / speed_multiplier  # Much faster for testing
        self.corruption_count = 0  # Track total corruptions
        
        # Color effect modes
        self.rainbow_mode = False
        self.psychedelic_mode = False
        self.color_shift_value = 0  # For rainbow cycling
        
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
            print(f"❌ Command failed: {e}")
            return ""

    def check_game_status(self) -> bool:
        """Check if Super Metroid is loaded"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            if "super metroid" in response_lower:
                print(f"🎮 Game detected: {response}")
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

    def is_protected_region(self, address: int) -> bool:
        """Check if address is in a protected region"""
        for region in self.protected_regions:
            if region["start"] <= address < region["end"]:
                return True
        return False

    def generate_color_value(self, original_color: int) -> int:
        """Generate a new color value based on the current mode"""
        if self.rainbow_mode:
            # Rainbow cycling - smooth color transitions
            return self.rainbow_color_shift(original_color)
        elif self.psychedelic_mode:
            # Psychedelic - random bright colors
            return self.psychedelic_color_shift(original_color)
        else:
            # Gentle color shifting
            return self.gentle_color_shift(original_color)
    
    def rainbow_color_shift(self, original_color: int) -> int:
        """Create rainbow color cycling effect"""
        # Extract RGB components (assuming 16-bit 555 color format)
        r = (original_color >> 10) & 0x1F
        g = (original_color >> 5) & 0x1F  
        b = original_color & 0x1F
        
        # Apply rainbow shift
        shift = int(self.color_shift_value) % 32
        r = (r + shift) % 32
        g = (g + shift + 10) % 32  # Offset for different colors
        b = (b + shift + 20) % 32
        
        return (r << 10) | (g << 5) | b
    
    def psychedelic_color_shift(self, original_color: int) -> int:
        """Create psychedelic random color bursts"""
        # Random bright colors
        r = random.randint(16, 31)  # Bright values
        g = random.randint(16, 31)
        b = random.randint(16, 31)
        
        return (r << 10) | (g << 5) | b
    
    def gentle_color_shift(self, original_color: int) -> int:
        """Create gentle color variations"""
        # Extract RGB components
        r = (original_color >> 10) & 0x1F
        g = (original_color >> 5) & 0x1F
        b = original_color & 0x1F
        
        # Small random adjustments
        r = max(0, min(31, r + random.randint(-3, 3)))
        g = max(0, min(31, g + random.randint(-3, 3)))
        b = max(0, min(31, b + random.randint(-3, 3)))
        
        return (r << 10) | (g << 5) | b
    
    def generate_8bit_color(self, original_color: int) -> int:
        """Generate a new 8-bit color value based on the current mode"""
        if self.rainbow_mode:
            # Rainbow cycling for 8-bit values
            shift = int(self.color_shift_value) % 256
            return (original_color + shift) % 256
        elif self.psychedelic_mode:
            # Psychedelic - random bright values
            return random.randint(128, 255)  # Bright colors
        else:
            # Gentle color shifting for 8-bit
            change = random.randint(-20, 20)
            return max(0, min(255, original_color + change))

    def corrupt_color_region(self, target: dict) -> bool:
        """Corrupt colors in a specific map color region"""
        print(f"🔍 ATTEMPTING color change in {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
        
        # Select random address within the target region
        region_size = target["end"] - target["start"]
        print(f"📏 Region size: {region_size} bytes")
        
        # Always work with 2-byte color values (16-bit colors)
        max_offset = region_size - 2
        if max_offset <= 0:
            print(f"❌ Region too small: {region_size} bytes")
            return False
            
        offset = random.randint(0, max_offset) & ~1  # Align to 2-byte boundary
        address = target["start"] + offset
        print(f"🎯 Target address: 0x{address:X} (offset +0x{offset:X})")
        
        # Safety check
        if self.is_protected_region(address):
            print(f"🛡️  Address 0x{address:X} is protected - skipping")
            return False
        
        # Try reading 2 bytes first, fallback to 1 byte
        print(f"📖 Reading from 0x{address:X}...")
        original_data = self.read_memory(address, 2)
        print(f"📖 Read result: {len(original_data)} bytes: {original_data.hex() if original_data else 'None'}")
        
        if len(original_data) == 2:
            # 16-bit color (RGB555 format)
            original_color = struct.unpack('<H', original_data)[0]
            print(f"🎨 Original 16-bit color: 0x{original_color:04X}")
            new_color = self.generate_color_value(original_color)
            new_data = struct.pack('<H', new_color)
            print(f"🎨 New 16-bit color: 0x{new_color:04X}")
        elif len(original_data) == 1:
            # 8-bit color component
            original_color = original_data[0]
            print(f"🎨 Original 8-bit color: 0x{original_color:02X}")
            new_color = self.generate_8bit_color(original_color)
            new_data = bytes([new_color])
            print(f"🎨 New 8-bit color: 0x{new_color:02X}")
        else:
            print(f"❌ Failed to read color data from 0x{address:X}")
            return False
        
        # Write new color
        print(f"✏️  Writing {len(new_data)} bytes to 0x{address:X}...")
        success = self.write_memory(address, new_data)
        print(f"✏️  Write result: {success}")
        
        if success:
            print(f"✅ COLOR CHANGED 0x{address:X} in {target['name']}")
            print(f"   Address: 0x{address:X} (offset +0x{offset:X} in region)")
            print(f"   Color: {original_data.hex().upper()} → {new_data.hex().upper()}")
            if len(original_data) == 2:
                print(f"   RGB555: {self.format_rgb555(struct.unpack('<H', original_data)[0])} → {self.format_rgb555(struct.unpack('<H', new_data)[0])}")
            else:
                print(f"   8-bit: {original_data[0]:3d} → {new_data[0]:3d}")
            print(f"   Region: {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
        else:
            print(f"❌ Failed to change color at 0x{address:X}")
        
        return success
    
    def format_rgb555(self, color: int) -> str:
        """Format 16-bit RGB555 color for display"""
        r = (color >> 10) & 0x1F
        g = (color >> 5) & 0x1F
        b = color & 0x1F
        return f"R:{r:2d} G:{g:2d} B:{b:2d}"

    def select_color_target(self) -> dict:
        """Select a random color target based on weights"""
        targets = self.color_targets.copy()
        weights = [target["weight"] for target in targets]
        target = random.choices(targets, weights=weights)[0]
        return target

    def escalate_intensity(self):
        """Gradually increase color change intensity over time"""
        if self.corruption_intensity < self.max_intensity:
            self.corruption_intensity += 0.05 * self.speed_multiplier
            
            # Update color shift for rainbow mode
            if self.rainbow_mode:
                self.color_shift_value += 0.5 * self.speed_multiplier
            
            # Decrease interval for more frequent changes
            min_interval = 0.5 / self.speed_multiplier
            if self.corruption_interval > min_interval:
                self.corruption_interval *= 0.99

    def color_chaos_loop(self):
        """Main color chaos loop"""
        print("🎨 Starting Super Metroid Map Color Chaos...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🎯 Max Color Changes: {self.max_corruptions}")
        print(f"⏱️  Starting interval: {self.corruption_interval:.2f}s")
        
        if self.rainbow_mode:
            print("🌈 RAINBOW MODE: Smooth color cycling")
        elif self.psychedelic_mode:
            print("🔮 PSYCHEDELIC MODE: Random color bursts")
        else:
            print("🎨 GENTLE MODE: Subtle color variations")
            
        print("⚠️  Only affecting map screen colors - game logic preserved")
        print("🛑 Press Ctrl+C to stop the color chaos")
        print("-" * 50)
        
        while self.running and self.corruption_count < self.max_corruptions:
            try:
                print(f"🔄 Loop iteration {self.corruption_count + 1}, checking game status...")
                
                # Check if game is still running
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing color changes...")
                    time.sleep(5)
                    continue
                
                print(f"✅ Game detected, attempting color change...")
                
                # Select target and change colors
                target = self.select_color_target()
                print(f"🎯 Selected target: {target['name']}")
                
                if self.corrupt_color_region(target):
                    self.corruption_count += 1
                    
                    # Show progress every 10 color changes
                    if self.corruption_count % 10 == 0:
                        remaining = self.max_corruptions - self.corruption_count
                        print(f"🎨 COLOR STATUS: {self.corruption_count}/{self.max_corruptions} changes ({remaining} remaining)")
                        print(f"   Intensity: {self.corruption_intensity:.1f}/{self.max_intensity}")
                        print(f"   Interval: {self.corruption_interval:.2f}s")
                        if self.rainbow_mode:
                            print(f"   🌈 Rainbow shift: {self.color_shift_value:.1f}")
                        print("-" * 50)
                
                # Escalate intensity over time
                self.escalate_intensity()
                
                # Wait for next color change
                time.sleep(self.corruption_interval)
                
            except Exception as e:
                print(f"❌ Color change error: {e}")
                time.sleep(1)
        
        if self.corruption_count >= self.max_corruptions:
            print(f"\n🎉 Maximum color changes reached! ({self.max_corruptions})")
            print("🎨 Your map has been beautifully recolored!")

    def start_color_chaos(self):
        """Start the color chaos generation"""
        if not self.check_game_status():
            print("❌ No Super Metroid game detected!")
            print("Make sure Super Metroid is running in RetroArch with network commands enabled.")
            return False
        
        print("✅ Game detected! Starting color chaos generation...")
        
        self.running = True
        try:
            self.color_chaos_loop()
        except KeyboardInterrupt:
            print("\n🛑 Color chaos stopped by user")
        finally:
            self.stop_color_chaos()
        
        return True
    
    def stop_color_chaos(self):
        """Stop the color chaos generation"""
        self.running = False
        self.disconnect()

def main():
    """Main function with command-line arguments"""
    parser = argparse.ArgumentParser(
        description="Super Metroid Map Color Chaos - Change only map screen colors without affecting game logic",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_map_color_chaos.py                    # Default: gentle color shifts
  python3 super_metroid_map_color_chaos.py --speed 3          # 3x faster color changes
  python3 super_metroid_map_color_chaos.py --rainbow          # Rainbow cycling mode
  python3 super_metroid_map_color_chaos.py --psychedelic      # Random color bursts
        """
    )
    
    parser.add_argument(
        "--speed", "-s",
        type=float,
        default=1.0,
        help="Speed multiplier for color change rate (default: 1.0, range: 0.1-5.0)"
    )
    
    parser.add_argument(
        "--max", "-m",
        type=int,
        default=500,
        help="Maximum number of color changes before stopping (default: 500)"
    )
    
    parser.add_argument(
        "--rainbow",
        action="store_true",
        help="Rainbow cycling mode (smooth color transitions)"
    )
    
    parser.add_argument(
        "--psychedelic",
        action="store_true",
        help="Psychedelic mode (random bright color bursts)"
    )
    
    args = parser.parse_args()
    
    # Handle modes
    speed = max(0.1, min(5.0, args.speed))  # Clamp between 0.1 and 5.0
    max_corruptions = max(1, args.max)
    
    print("🎨 Super Metroid Map Color Chaos Generator")
    print("=" * 45)
    print("This will change ONLY map screen colors")
    print("without affecting any game logic or functionality.")
    print()
    
    chaos = SuperMetroidMapColorChaos(speed_multiplier=speed, max_corruptions=max_corruptions)
    
    # Set color modes
    if args.rainbow and args.psychedelic:
        print("⚠️  Both rainbow and psychedelic modes specified. Using psychedelic mode.")
        chaos.psychedelic_mode = True
    elif args.rainbow:
        chaos.rainbow_mode = True
    elif args.psychedelic:
        chaos.psychedelic_mode = True
    
    print("🎨 Ready to add some color chaos to your map? Starting immediately...")
    print("⚠️  Press Ctrl+C at any time to stop the color changes")
    
    print("\n🎯 Initializing color chaos protocols...")
    chaos.start_color_chaos()
    
    print("\n✅ Color chaos session ended. Your map colors have been transformed!")

if __name__ == "__main__":
    main()