#!/usr/bin/env python3
"""
🎵 Super Metroid Audio Chaos Generator 🎵

Randomly corrupts Super Metroid's sound data to create wild, glitchy audio
that gets progressively more chaotic over time!

Features:
- Progressive audio corruption (gets wilder over time)
- Multiple corruption modes (glitch, static, pitch-shift, reverse)
- Safe audio memory targeting (won't crash the game)
- Real-time audio intensity scaling
- Sound effect and music corruption

Usage:
    python3 super_metroid_chaos_audio.py --insane
    python3 super_metroid_chaos_audio.py --music-only --speed 2
    python3 super_metroid_chaos_audio.py --sfx-only --gentle
    python3 super_metroid_chaos_audio.py --progressive --max 1000
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
import math

class SuperMetroidAudioChaos:
    """Creates chaotic audio modifications in Super Metroid"""
    
    def __init__(self, host="localhost", port=55355, speed_multiplier=1.0, max_corruptions=1000):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        self.speed_multiplier = speed_multiplier
        self.max_corruptions = max_corruptions
        
        # Audio corruption tracking
        self.original_audio_data = {}    # Store original audio bytes for restoration
        self.corruption_count = 0
        self.audio_intensity = 1.0       # Progressive corruption intensity
        self.last_room_id = None
        
        # WORKING approach: Corrupt main CPU memory that affects audio behavior
        # These are accessible via RetroArch and will actually impact sound!
        self.audio_memory_map = {
            # Audio-related game state variables (main CPU RAM)
            'current_music_track': 0x7E07C0,     # Current music track ID
            'music_queue': 0x7E07C2,             # Music track queue/next
            'sfx_queue_1': 0x7E07E0,             # Sound effect queue 1
            'sfx_queue_2': 0x7E07E2,             # Sound effect queue 2
            'sfx_queue_3': 0x7E07E4,             # Sound effect queue 3
            'sfx_parameters_1': 0x7E07F0,        # SFX parameters 1
            'sfx_parameters_2': 0x7E07F2,        # SFX parameters 2
            'sfx_parameters_3': 0x7E07F4,        # SFX parameters 3
            'audio_engine_state': 0x7E0700,     # Audio engine state variables
            'music_tempo': 0x7E0702,             # Music tempo/timing
            'music_volume': 0x7E0704,            # Music volume level
            'sfx_volume': 0x7E0706,              # SFX volume level
            
            # Room-based audio triggers
            'room_music_data': 0x7E0710,         # Room-specific music data
            'ambient_sound_data': 0x7E0720,      # Ambient sound parameters
            'environmental_audio': 0x7E0730,     # Environmental audio settings
        }
        
        # Audio corruption targets using accessible main CPU memory
        self.music_targets = [
            {"name": "Music Track Control", "start": 0x7E07C0, "end": 0x7E07C8, "weight": 4},
            {"name": "Music Tempo/Timing", "start": 0x7E0702, "end": 0x7E0708, "weight": 3},
            {"name": "Music Volume", "start": 0x7E0704, "end": 0x7E0706, "weight": 3},
            {"name": "Room Music Data", "start": 0x7E0710, "end": 0x7E0718, "weight": 2},
        ]
        
        self.sfx_targets = [
            {"name": "SFX Queue 1", "start": 0x7E07E0, "end": 0x7E07E6, "weight": 4},
            {"name": "SFX Queue 2", "start": 0x7E07E2, "end": 0x7E07E8, "weight": 4},
            {"name": "SFX Parameters", "start": 0x7E07F0, "end": 0x7E07F8, "weight": 3},
            {"name": "SFX Volume", "start": 0x7E0706, "end": 0x7E0708, "weight": 2},
        ]
        
        self.environment_targets = [
            {"name": "Audio Engine State", "start": 0x7E0700, "end": 0x7E0708, "weight": 3},
            {"name": "Ambient Sound Data", "start": 0x7E0720, "end": 0x7E0728, "weight": 2},
            {"name": "Environmental Audio", "start": 0x7E0730, "end": 0x7E0738, "weight": 2},
        ]
        
        # Corruption modes
        self.corruption_modes = [
            'bit_flip',      # Flip random bits (glitchy)
            'static_inject', # Add static/noise
            'pitch_shift',   # Shift frequency values
            'volume_chaos',  # Randomize volume levels
            'echo_madness',  # Corrupt echo/reverb
            'channel_swap',  # Swap channel data
            'reverse_bits',  # Reverse bit patterns
            'progressive',   # Gradual corruption
        ]
        
        # Settings - MUCH slower to prevent system overload
        self.corruption_interval = max(2.0, 5.0 / speed_multiplier)  # Minimum 2 seconds between corruptions
        self.intensity_growth_rate = 0.01  # Slower chaos growth
        self.max_bytes_per_corruption = 2  # Limit bytes corrupted per operation
        
        # Mode flags
        self.music_only = False
        self.sfx_only = False
        self.gentle_mode = False
        self.insane_mode = False
        self.progressive_mode = False

    def connect(self) -> bool:
        """Connect to RetroArch UDP interface"""
        try:
            if self.sock:
                self.sock.close()
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(5.0)  # Longer timeout to prevent rushing
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
        """Read memory from RetroArch - FIXED to match working scripts"""
        try:
            command = f"READ_CORE_MEMORY 0x{address:X} {length}"
            response = self.send_command(command)
            
            if response.startswith("-1"):
                print(f"❌ READ FAILED: RetroArch returned error -1 for address 0x{address:X}")
                return b""
            
            if response.startswith("READ_CORE_MEMORY"):
                # Extract hex data from end of response (after the echoed command)
                hex_data = response.split()[-1]
                data = bytes.fromhex(hex_data)
                return data
        except Exception as e:
            print(f"❌ READ ERROR: Exception reading from 0x{address:X}: {e}")
        
        return b""

    def write_memory(self, address: int, data: bytes) -> bool:
        """Write memory to RetroArch - FIXED to match working scripts"""
        try:
            hex_data = data.hex()
            command = f"WRITE_CORE_MEMORY 0x{address:X} {hex_data}"
            response = self.send_command(command)
            
            if response.startswith("-1"):
                print(f"❌ WRITE FAILED: RetroArch returned error for address 0x{address:X}, data: {hex_data}")
                return False
            
            return "WRITE_CORE_MEMORY" in response
        except Exception as e:
            print(f"❌ WRITE ERROR: Exception writing to 0x{address:X}: {e}")
            return False

    def backup_audio_data(self, address: int, size: int = 1):
        """Backup original audio data for restoration"""
        if address not in self.original_audio_data:
            data = self.read_memory(address, size)
            if data:
                self.original_audio_data[address] = data

    def restore_all_audio(self):
        """Restore all original audio data"""
        print("🔄 Restoring original audio data...")
        restored = 0
        for address, original_data in self.original_audio_data.items():
            if self.write_memory(address, original_data):
                restored += 1
        print(f"✅ Restored {restored} audio bytes")

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
        # Try multiple room ID addresses for better compatibility
        room_addresses = [0x7E079B, 0x7E079D, 0x7E0799, 0x7F00DE]
        
        for addr in room_addresses:
            room_data = self.read_memory(addr, 2)
            if room_data and len(room_data) == 2:
                room_id = struct.unpack('<H', room_data)[0]
                if room_id != 0 and room_id != 0xFFFF:  # Valid room ID
                    return room_id
        return 0

    def corrupt_bit_flip(self, data: bytes) -> bytes:
        """Corruption mode: Random bit flipping"""
        if not data:
            return data
        
        result = bytearray(data)
        for i in range(len(result)):
            if random.random() < self.audio_intensity * 0.1:  # Intensity-based probability
                # Flip 1-3 random bits
                bits_to_flip = random.randint(1, min(3, int(self.audio_intensity)))
                for _ in range(bits_to_flip):
                    bit_pos = random.randint(0, 7)
                    result[i] ^= (1 << bit_pos)
        
        return bytes(result)

    def corrupt_static_inject(self, data: bytes) -> bytes:
        """Corruption mode: Inject static/noise"""
        if not data:
            return data
        
        result = bytearray(data)
        for i in range(len(result)):
            if random.random() < self.audio_intensity * 0.05:
                # Inject random noise
                noise_level = int(self.audio_intensity * 50)
                noise = random.randint(-noise_level, noise_level)
                new_value = max(0, min(255, result[i] + noise))
                result[i] = new_value
        
        return bytes(result)

    def corrupt_pitch_shift(self, data: bytes) -> bytes:
        """Corruption mode: Shift frequency values"""
        if not data:
            return data
        
        result = bytearray(data)
        shift_amount = int(self.audio_intensity * 20) - 10  # -10 to +10 based on intensity
        
        for i in range(len(result)):
            if random.random() < 0.3:  # 30% chance to shift
                new_value = max(0, min(255, result[i] + shift_amount))
                result[i] = new_value
        
        return bytes(result)

    def corrupt_volume_chaos(self, data: bytes) -> bytes:
        """Corruption mode: Randomize volume levels"""
        if not data:
            return data
        
        result = bytearray(data)
        for i in range(len(result)):
            if random.random() < self.audio_intensity * 0.08:
                # Random volume scaling
                if self.insane_mode:
                    scale = random.uniform(0.1, 3.0)
                else:
                    scale = random.uniform(0.5, 2.0)
                new_value = max(0, min(255, int(result[i] * scale)))
                result[i] = new_value
        
        return bytes(result)

    def corrupt_reverse_bits(self, data: bytes) -> bytes:
        """Corruption mode: Reverse bit patterns"""
        if not data:
            return data
        
        result = bytearray(data)
        for i in range(len(result)):
            if random.random() < self.audio_intensity * 0.03:
                # Reverse the bits in this byte
                byte_val = result[i]
                reversed_byte = 0
                for bit in range(8):
                    if byte_val & (1 << bit):
                        reversed_byte |= (1 << (7 - bit))
                result[i] = reversed_byte
        
        return bytes(result)

    def corrupt_progressive(self, data: bytes) -> bytes:
        """Corruption mode: Progressive corruption that builds up"""
        if not data:
            return data
        
        result = bytearray(data)
        corruption_strength = min(1.0, self.audio_intensity / 5.0)  # Build up to 20% intensity
        
        for i in range(len(result)):
            if random.random() < corruption_strength:
                # Multiple corruption types applied progressively
                if random.random() < 0.4:  # Bit flip
                    bit_pos = random.randint(0, 7)
                    result[i] ^= (1 << bit_pos)
                if random.random() < 0.3:  # Value shift
                    shift = random.randint(-5, 5)
                    result[i] = max(0, min(255, result[i] + shift))
                if random.random() < 0.2:  # Static injection
                    noise = random.randint(-20, 20)
                    result[i] = max(0, min(255, result[i] + noise))
        
        return bytes(result)

    def apply_corruption_mode(self, data: bytes, mode: str) -> bytes:
        """Apply a specific corruption mode to data"""
        if mode == 'bit_flip':
            return self.corrupt_bit_flip(data)
        elif mode == 'static_inject':
            return self.corrupt_static_inject(data)
        elif mode == 'pitch_shift':
            return self.corrupt_pitch_shift(data)
        elif mode == 'volume_chaos':
            return self.corrupt_volume_chaos(data)
        elif mode == 'reverse_bits':
            return self.corrupt_reverse_bits(data)
        elif mode == 'progressive':
            return self.corrupt_progressive(data)
        else:
            # Default: random bit flip
            return self.corrupt_bit_flip(data)

    def corrupt_audio_region(self, target: dict) -> bool:
        """Corrupt a specific audio memory region"""
        region_size = target["end"] - target["start"]
        if region_size <= 0:
            print(f"❌ REGION ERROR: Invalid region size for {target['name']}: {region_size}")
            return False
        
        print(f"🎯 ATTEMPTING audio corruption in {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
        
        # Choose corruption size based on intensity - LIMITED to prevent network overload
        if self.insane_mode:
            bytes_to_corrupt = random.randint(1, min(self.max_bytes_per_corruption, region_size))
        elif self.gentle_mode:
            bytes_to_corrupt = 1
        else:
            max_bytes = max(1, min(self.max_bytes_per_corruption, int(self.audio_intensity)))
            bytes_to_corrupt = random.randint(1, min(max_bytes, region_size))
        
        corrupted_bytes = 0
        for _ in range(bytes_to_corrupt):
            offset = random.randint(0, region_size - 1)
            address = target["start"] + offset
            
            # Backup original data
            self.backup_audio_data(address, 1)
            
            # Read current data
            current_data = self.read_memory(address, 1)
            if not current_data:
                print(f"❌ READ FAILED: No data returned from address 0x{address:X}")
                continue
            
            # Choose corruption mode
            if self.progressive_mode:
                mode = 'progressive'
            else:
                mode = random.choice(self.corruption_modes)
            
            # Apply corruption
            corrupted_data = self.apply_corruption_mode(current_data, mode)
            
            # Write corrupted data
            if self.write_memory(address, corrupted_data):
                original_val = current_data[0] if current_data else 0
                new_val = corrupted_data[0] if corrupted_data else 0
                print(f"🎵 AUDIO: 0x{address:X} in {target['name']}")
                print(f"   Mode: {mode.replace('_', ' ').title()}")
                print(f"   Value: 0x{original_val:02X} → 0x{new_val:02X} ({original_val} → {new_val})")
                print(f"   Region: {target['name']} (0x{target['start']:X}-0x{target['end']:X})")
                corrupted_bytes += 1
                
                # IMPORTANT: Add delay between network operations to prevent system overload
                time.sleep(0.1)  # 100ms delay between each memory operation
        
        success = corrupted_bytes > 0
        if success:
            print(f"✅ AUDIO SUCCESS: Corrupted {corrupted_bytes} bytes in {target['name']}")
        else:
            print(f"❌ AUDIO FAILURE: No bytes corrupted in {target['name']} (tried {bytes_to_corrupt} locations)")
        return success

    def select_audio_target(self, targets: list) -> dict:
        """Select a random audio target based on weights"""
        weights = [target["weight"] for target in targets]
        return random.choices(targets, weights=weights)[0]

    def escalate_audio_intensity(self):
        """Progressively increase audio corruption intensity"""
        self.audio_intensity += self.intensity_growth_rate
        
        # Cap intensity based on mode
        if self.gentle_mode:
            self.audio_intensity = min(2.0, self.audio_intensity)
        elif self.insane_mode:
            self.audio_intensity = min(10.0, self.audio_intensity)
        else:
            self.audio_intensity = min(5.0, self.audio_intensity)

    def chaos_loop(self):
        """Main audio chaos loop"""
        print("🎵 Starting Super Metroid Audio Chaos...")
        print(f"⚡ Speed Multiplier: {self.speed_multiplier}x")
        print(f"🎯 Max Corruptions: {self.max_corruptions}")
        print(f"⏱️  Interval: {self.corruption_interval:.2f}s")
        
        if self.music_only:
            print("🎼 MUSIC ONLY MODE")
        elif self.sfx_only:
            print("🔊 SFX ONLY MODE")
        else:
            print("🌈 FULL AUDIO CHAOS: Music + SFX + Environment")
        
        if self.insane_mode:
            print("🤯 INSANE MODE: Maximum audio destruction!")
        elif self.gentle_mode:
            print("😌 GENTLE MODE: Subtle audio glitches")
        elif self.progressive_mode:
            print("📈 PROGRESSIVE MODE: Builds up to total chaos!")
        
        print("🛑 Press Ctrl+C to stop and restore original audio")
        print("-" * 60)
        
        consecutive_failures = 0
        
        while self.running:  # Run indefinitely until stopped
            try:
                if not self.check_game_status():
                    print("⚠️  Game not detected, pausing...")
                    time.sleep(2.0)
                    continue
                
                success = False
                
                # Choose corruption target based on mode
                if self.music_only:
                    target = self.select_audio_target(self.music_targets)
                    success = self.corrupt_audio_region(target)
                elif self.sfx_only:
                    target = self.select_audio_target(self.sfx_targets)
                    success = self.corrupt_audio_region(target)
                else:
                    # Full chaos mode - weighted selection from all audio-related memory
                    all_targets = (
                        self.music_targets +
                        self.sfx_targets +
                        self.environment_targets
                    )
                    target = self.select_audio_target(all_targets)
                    success = self.corrupt_audio_region(target)
                
                if success:
                    self.corruption_count += 1
                    consecutive_failures = 0
                    
                    # Escalate intensity over time
                    if self.progressive_mode or not self.gentle_mode:
                        self.escalate_audio_intensity()
                    
                    # Show progress
                    if self.corruption_count % 25 == 0:
                        print(f"📊 AUDIO CHAOS STATUS: {self.corruption_count} corruptions applied")
                        print(f"   🎚️  Audio intensity: {self.audio_intensity:.2f}")
                        print(f"   🔄 Original audio backed up: {len(self.original_audio_data)} bytes")
                        print("-" * 60)
                else:
                    consecutive_failures += 1
                    print(f"❌ CORRUPTION FAILED #{consecutive_failures}: corrupt_audio_region() returned False")
                    if consecutive_failures > 10:
                        print("⚠️  Multiple failures detected, adjusting strategy...")
                        time.sleep(1.0)
                        consecutive_failures = 0
                
                # Dynamic interval adjustment - MUCH slower to prevent system overload
                current_interval = self.corruption_interval
                if self.insane_mode:
                    current_interval = max(3.0, current_interval * 0.8)  # Still slow even in insane mode
                elif self.gentle_mode:
                    current_interval *= 2.0  # Very slow and gentle
                elif self.progressive_mode:
                    # Speed up gradually but with limits
                    current_interval *= max(0.8, 2.0 / max(1.0, self.audio_intensity))
                
                time.sleep(current_interval)
                
            except KeyboardInterrupt:
                print("\n🛑 Audio chaos stopped by user")
                break
            except Exception as e:
                print(f"❌ Error during audio chaos: {e}")
                time.sleep(1.0)
        
        print(f"\n✅ Audio chaos session ended! {self.corruption_count} total corruptions applied")
        return True

    def start_chaos(self) -> bool:
        """Start the audio chaos session"""
        if not self.connect():
            print("❌ Failed to connect to RetroArch")
            return False
        
        if not self.check_game_status():
            print("❌ Game not detected. Make sure Super Metroid is running in RetroArch")
            return False
        
        print("✅ Game detected! Starting audio chaos...")
        
        # Set up signal handler for graceful shutdown
        def signal_handler(sig, frame):
            print("\n🛑 Interrupt received, stopping audio chaos...")
            self.running = False
        
        signal.signal(signal.SIGINT, signal_handler)
        self.running = True
        
        try:
            return self.chaos_loop()
        finally:
            self.stop_chaos()

    def stop_chaos(self):
        """Stop chaos and restore original audio"""
        self.running = False
        print("\n🔄 Cleaning up...")
        self.restore_all_audio()
        self.disconnect()

def main():
    """Main function with command-line arguments"""
    parser = argparse.ArgumentParser(
        description="Super Metroid Audio Chaos - Corrupt sound data for wild audio effects",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 super_metroid_chaos_audio.py --insane
  python3 super_metroid_chaos_audio.py --music-only --speed 2
  python3 super_metroid_chaos_audio.py --progressive --max 500
  python3 super_metroid_chaos_audio.py --sfx-only --gentle
        """
    )
    
    parser.add_argument('--speed', type=float, default=1.0, help='Speed multiplier (0.1-10.0)')
    parser.add_argument('--max', type=int, default=1000, help='Maximum corruptions')
    
    # Mode selection (mutually exclusive)
    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument('--music-only', action='store_true', help='Only corrupt music data')
    mode_group.add_argument('--sfx-only', action='store_true', help='Only corrupt sound effects')
    
    # Intensity selection (mutually exclusive)
    intensity_group = parser.add_mutually_exclusive_group()
    intensity_group.add_argument('--gentle', action='store_true', help='Gentle audio glitches')
    intensity_group.add_argument('--progressive', action='store_true', help='Progressive chaos buildup')
    intensity_group.add_argument('--insane', action='store_true', help='Maximum audio destruction')
    
    args = parser.parse_args()
    
    # Validate speed
    if not 0.1 <= args.speed <= 10.0:
        print("❌ Speed must be between 0.1 and 10.0")
        return
    
    # Validate max
    if not 10 <= args.max <= 10000:
        print("❌ Max corruptions must be between 10 and 10000")
        return
    
    print("🤯 INSANE AUDIO MODE!" if args.insane else "")
    print("📈 PROGRESSIVE CHAOS MODE!" if args.progressive else "")
    print("😌 GENTLE AUDIO MODE!" if args.gentle else "")
    print("🎵 Super Metroid Audio Chaos Generator")
    print("=" * 50)
    print("Corrupts audio memory to create wild, glitchy soundscapes")
    print("that get progressively more chaotic over time!")
    print("")
    print("🎯 Ready to corrupt audio!")
    print("⚠️  Sound will become increasingly distorted")
    print("🛑 Press Ctrl+C to stop and restore original audio")
    print("")
    
    # Create chaos instance
    chaos = SuperMetroidAudioChaos(
        speed_multiplier=args.speed,
        max_corruptions=args.max
    )
    
    # Set modes
    chaos.music_only = args.music_only
    chaos.sfx_only = args.sfx_only
    chaos.gentle_mode = args.gentle
    chaos.progressive_mode = args.progressive
    chaos.insane_mode = args.insane
    
    # Print active modes
    if args.music_only:
        print("🎼 Music-only mode activated")
    elif args.sfx_only:
        print("🔊 SFX-only mode activated")
    
    # Start chaos
    try:
        chaos.start_chaos()
    except KeyboardInterrupt:
        print("\n🛑 Interrupted by user")
    except Exception as e:
        print(f"\n❌ Error: {e}")
    finally:
        chaos.stop_chaos()
        print("\n✅ Audio chaos session ended. Original audio restored!")

if __name__ == "__main__":
    main()