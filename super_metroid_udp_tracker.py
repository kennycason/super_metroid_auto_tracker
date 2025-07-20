#!/usr/bin/env python3
"""
Super Metroid UDP Tracker
Reads game stats directly from RetroArch via UDP
"""

import socket
import struct
import time
import json
from typing import Dict, Optional

class SuperMetroidUDPTracker:
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        
        # Super Metroid memory addresses (SNES format)
        self.memory_map = {
            'health': 0x7E09C2,
            'max_health': 0x7E09C4,
            'missiles': 0x7E09C6,
            'max_missiles': 0x7E09C8,
            'supers': 0x7E09CA,
            'max_supers': 0x7E09CC,
            'power_bombs': 0x7E09CE,
            'max_power_bombs': 0x7E09D0,
            'reserve_energy': 0x7E09D6,  # FIXED: Correct address for current reserve energy  
            'max_reserve_energy': 0x7E09D4,
            'game_state': 0x7E0998,
            'room_id': 0x7E079B,
            'area_id': 0x7E079F,
            'player_x': 0x7E0AF6,
            'player_y': 0x7E0AFA,
            # Item and boss tracking (correct Super Metroid addresses)
            'items_collected': 0x7E09A4,    # Collected items bitfield
            'beams_collected': 0x7E09A8,    # Beam weapons bitfield  
            'bosses_defeated': 0x7ED828,    # Bosses defeated bitfield
            'events_flags': 0x7ED870,       # Event flags
        }
        
        # Area names
        self.areas = {
            0: "Crateria",
            1: "Brinstar", 
            2: "Norfair",
            3: "Wrecked Ship",
            4: "Maridia",
            5: "Tourian"
        }
        
        # Item bit mappings (corrected based on actual Super Metroid RAM)
        self.item_bits = {
            'morph': 0x04,      # Morph Ball
            'bombs': 0x1000,    # Bombs  
            'varia': 0x01,      # Varia Suit
            'gravity': 0x20,    # Gravity Suit
            'hijump': 0x100,    # Hi-Jump Boots
            'speed': 0x2000,    # Speed Booster - FIXED: was 0x200, now 0x2000 (bit 13)
            'space': 0x200,     # Space Jump - FIXED: was 0x02, now 0x200 (bit 9)
            'screw': 0x08,      # Screw Attack
            'spring': 0x02,     # Spring Ball
            'grapple': 0x40,    # Grappling Beam
            'xray': 0x8000      # X-Ray Scope
        }
        
        # Beam bit mappings
        self.beam_bits = {
            'charge': 0x1000,
            'ice': 0x02,
            'wave': 0x01,
            'spazer': 0x04,
            'plasma': 0x08
        }
        
        # Boss bit mappings (simplified for now)
        self.boss_bits = {
            'bomb_torizo': 0x04,
            'spore_spawn': 0x02,
            'kraid': 0x01,
            'crocomire': 0x02,
            'phantoon': 0x01,
            'botwoon': 0x04,
            'draygon': 0x04,
            'ridley': 0x01,
            'gold_torizo': 0x04,
            'mother_brain_1': 0x02,
            'mother_brain_2': 0x08
        }
        
    def connect(self) -> bool:
        """Create UDP socket connection"""
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(1.0)  # 1 second timeout
            return True
        except Exception as e:
            print(f"❌ Failed to create socket: {e}")
            return False
            
    def send_command(self, command: str) -> Optional[str]:
        """Send command to RetroArch and get response"""
        if not self.sock:
            return None
            
        try:
            # Send command
            self.sock.sendto(command.encode(), (self.host, self.port))
            
            # Receive response
            data, addr = self.sock.recvfrom(1024)
            return data.decode().strip()
        except socket.timeout:
            return None
        except Exception as e:
            print(f"❌ Command error: {e}")
            return None
            
    def is_game_loaded(self) -> bool:
        """Check if Super Metroid (including ROM hacks) is loaded and playing"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            # Convert to lowercase for case-insensitive matching
            response_lower = response.lower()
            
            # Primary check: Any game containing "super metroid" in the name
            if "super metroid" in response_lower:
                return True
                
            # Secondary checks for ROM hacks that might not include "super metroid"
            rom_hack_keywords = [
                "map rando", "rando", "randomizer", "random",
                "sm ", "super_metroid", "metroid", "samus",
                "zebes", "crateria", "norfair", "maridia"  # Super Metroid location names
            ]
            
            if any(keyword in response_lower for keyword in rom_hack_keywords):
                return True
        return False
        
    def read_memory_range(self, start_address: int, size: int) -> Optional[bytes]:
        """Read a range of memory and return as bytes"""
        command = f"READ_CORE_MEMORY 0x{start_address:X} {size}"
        response = self.send_command(command)
        
        if not response or not response.startswith("READ_CORE_MEMORY"):
            return None
            
        try:
            # Parse response: "READ_CORE_MEMORY address hex_bytes"
            parts = response.split(' ', 2)
            if len(parts) < 3:
                return None
                
            hex_data = parts[2].replace(' ', '')
            return bytes.fromhex(hex_data)
        except ValueError:
            return None
            
    def read_word(self, address: int) -> Optional[int]:
        """Read a 16-bit word (little-endian) from memory"""
        data = self.read_memory_range(address, 2)
        if data and len(data) >= 2:
            return struct.unpack('<H', data[:2])[0]  # Little-endian 16-bit
        return None
        
    def read_byte(self, address: int) -> Optional[int]:
        """Read a single byte from memory"""
        data = self.read_memory_range(address, 1)
        if data and len(data) >= 1:
            return data[0]
        return None
        
    def get_all_stats(self) -> Dict:
        """Read all Super Metroid stats at once"""
        # Read a large chunk that contains all our stats (including reserve energy)
        base_address = self.memory_map['health']
        data = self.read_memory_range(base_address, 24)  # Read 24 bytes for all basic stats
        
        if not data or len(data) < 20:
            return {}
            
        # Parse the data (all 16-bit little-endian values)
        stats = {}
        try:
            stats['health'] = struct.unpack('<H', data[0:2])[0]
            stats['max_health'] = struct.unpack('<H', data[2:4])[0]
            stats['missiles'] = struct.unpack('<H', data[4:6])[0]
            stats['max_missiles'] = struct.unpack('<H', data[6:8])[0]
            stats['supers'] = struct.unpack('<H', data[8:10])[0]
            stats['max_supers'] = struct.unpack('<H', data[10:12])[0]
            stats['power_bombs'] = struct.unpack('<H', data[12:14])[0]
            stats['max_power_bombs'] = struct.unpack('<H', data[14:16])[0]
            
            # Read reserve energy from the correct addresses
            stats['max_reserve_energy'] = struct.unpack('<H', data[18:20])[0]  # 0x7E09D4
            stats['reserve_energy'] = self.read_word(self.memory_map['reserve_energy'])  # 0x7E09D6
            
            # Read additional data separately
            stats['area_id'] = self.read_byte(self.memory_map['area_id'])
            stats['area_name'] = self.areas.get(stats['area_id'], "Unknown")
            stats['room_id'] = self.read_word(self.memory_map['room_id'])
            stats['game_state'] = self.read_word(self.memory_map['game_state'])
            
            # Read item and boss data
            items_data = self.read_word(self.memory_map['items_collected'])
            beams_data = self.read_word(self.memory_map['beams_collected']) 
            bosses_data = self.read_word(self.memory_map['bosses_defeated'])
            
            # Parse item bitflags
            stats['items'] = {}
            if items_data is not None:
                stats['items']['morph'] = bool(items_data & 0x04)
                stats['items']['bombs'] = bool(items_data & 0x1000)
                stats['items']['varia'] = bool(items_data & 0x01)
                stats['items']['gravity'] = bool(items_data & 0x20)
                stats['items']['hijump'] = bool(items_data & 0x100)
                stats['items']['speed'] = bool(items_data & 0x2000)
                stats['items']['space'] = bool(items_data & 0x200)
                stats['items']['screw'] = bool(items_data & 0x08)
                stats['items']['spring'] = bool(items_data & 0x02)
                stats['items']['grapple'] = bool(items_data & 0x4000)
                stats['items']['xray'] = bool(items_data & 0x8000)
                
            # Parse beam bitflags
            if beams_data is not None:
                stats['items']['charge'] = bool(beams_data & 0x1000)
                stats['items']['ice'] = bool(beams_data & 0x02)
                stats['items']['wave'] = bool(beams_data & 0x01)
                stats['items']['spazer'] = bool(beams_data & 0x04)
                stats['items']['plasma'] = bool(beams_data & 0x08)
                
            # Parse bosses defeated
            if bosses_data is not None:
                crocomire_data = self.read_word(0x7ED829)
                
                # BOSS SCANNING - Check multiple addresses for special bosses like Phantoon and Draygon
                boss_scan_candidates = {
                    'boss_plus_1': 0x7ED829,    # Same as Crocomire
                    'boss_plus_2': 0x7ED82A,    # Adjacent to main boss area  
                    'boss_plus_3': 0x7ED82B,    # Further adjacent
                    'boss_plus_4': 0x7ED82C,    # Even further adjacent
                    'boss_plus_5': 0x7ED82D,    # More adjacent
                    'boss_minus_1': 0x7ED827,   # Before main boss area
                }
                
                boss_scan_results = {}
                for scan_name, addr in boss_scan_candidates.items():
                    scan_data = self.read_word(addr)
                    if scan_data is not None:
                        boss_scan_results[scan_name] = scan_data
                
                # PHANTOON DETECTION
                phantoon_detected = False
                boss_plus_3_data = boss_scan_results.get('boss_plus_3', 0)
                if boss_plus_3_data & 0x01:
                    phantoon_detected = True
                
                # DRAYGON DETECTION
                draygon_detected = False
                draygon_candidates = [
                    ('boss_plus_4', 0x01),  
                    ('boss_plus_4', 0x02),   
                    ('boss_plus_5', 0x100), 
                    ('boss_minus_1', 0x400), 
                ]
                
                for scan_name, bit_mask in draygon_candidates:
                    candidate_data = boss_scan_results.get(scan_name, 0)
                    if candidate_data & bit_mask:
                        draygon_detected = True
                        break
                
                # RIDLEY DETECTION
                ridley_detected = False
                ridley_candidates = [
                    ('boss_plus_1', 0x400),
                    ('boss_plus_1', 0x200), 
                    ('boss_plus_1', 0x100), 
                    ('boss_plus_2', 0x100), 
                ]
                
                for scan_name, bit_mask in ridley_candidates:
                    candidate_data = boss_scan_results.get(scan_name, 0)
                    if candidate_data & bit_mask:
                        ridley_detected = True
                        break
                
                # BOTWOON DETECTION
                botwoon_detected = False
                botwoon_candidates = [
                    ('boss_plus_2', 0x04),  
                    ('boss_plus_2', 0x02),  
                    ('boss_plus_4', 0x02),  
                    ('boss_plus_1', 0x02),  
                ]
                
                for scan_name, bit_mask in botwoon_candidates:
                    candidate_data = boss_scan_results.get(scan_name, 0)
                    if candidate_data & bit_mask:
                        botwoon_detected = True
                        break
                
                # GOLDEN TORIZO DETECTION
                golden_torizo_detected = False
                boss_plus_1_val = boss_scan_results.get('boss_plus_1', 0)
                boss_plus_2_val = boss_scan_results.get('boss_plus_2', 0)
                boss_plus_3_val = boss_scan_results.get('boss_plus_3', 0)
                
                if ((boss_plus_1_val & 0x0700) and (boss_plus_1_val & 0x0003)) or \
                   (boss_plus_2_val & 0x0100) or \
                   (boss_plus_3_val & 0x0300):
                    golden_torizo_detected = True
                
                # MOTHER BRAIN DETECTION
                mother_brain_detected = bool(bosses_data & 0x01)
                
                stats['bosses'] = {
                    'bomb_torizo': bool(bosses_data & 0x04),
                    'kraid': bool(bosses_data & 0x100),
                    'spore_spawn': bool(bosses_data & 0x200),
                    'crocomire': bool(crocomire_data & 0x02) if crocomire_data is not None else False,
                    'phantoon': phantoon_detected,
                    'botwoon': botwoon_detected,
                    'draygon': draygon_detected,
                    'ridley': ridley_detected,
                    'golden_torizo': golden_torizo_detected,
                    'mother_brain': mother_brain_detected,
                }
            else:
                stats['bosses'] = {}
            
        except struct.error:
            return {}
            
        return stats
        
    def get_status(self) -> Dict:
        """Get full game status including RetroArch info"""
        status = {
            'connected': False,
            'game_loaded': False,
            'retroarch_version': None,
            'game_info': None,
            'stats': {}
        }
        
        if not self.sock:
            return status
            
        # Check RetroArch version
        version_response = self.send_command("VERSION")
        if version_response:
            status['connected'] = True
            status['retroarch_version'] = version_response
            
        # Check game status
        if self.is_game_loaded():
            status['game_loaded'] = True
            game_response = self.send_command("GET_STATUS")
            status['game_info'] = game_response
            
            # Get game stats
            stats = self.get_all_stats()
            if stats:
                status['stats'] = stats
                
        return status
        
    def disconnect(self):
        """Close the UDP socket"""
        if self.sock:
            self.sock.close()
            self.sock = None
            
    def __enter__(self):
        """Context manager entry"""
        self.connect()
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit"""
        self.disconnect()

def main():
    """Test the tracker"""
    with SuperMetroidUDPTracker() as tracker:
        print("🎮 Super Metroid UDP Tracker Test")
        print("=" * 40)
        
        status = tracker.get_status()
        
        if not status['connected']:
            print("❌ Could not connect to RetroArch")
            return
            
        print(f"✅ RetroArch version: {status['retroarch_version']}")
        
        if not status['game_loaded']:
            print("❌ Super Metroid not loaded or not playing")
            return
            
        print(f"🎯 Game: {status['game_info']}")
        
        if status['stats']:
            stats = status['stats']
            print(f"\n📊 Current Stats:")
            print(f"   🟢 Energy: {stats['health']} / {stats['max_health']}")
            if stats.get('reserve_energy', 0) > 0:
                print(f"   🔋 Reserve: {stats['reserve_energy']} / {stats['max_reserve_energy']}")
            print(f"   🚀 Missiles: {stats['missiles']} / {stats['max_missiles']}")
            print(f"   ⭐ Super Missiles: {stats['supers']} / {stats['max_supers']}")
            print(f"   💥 Power Bombs: {stats['power_bombs']} / {stats['max_power_bombs']}")
            print(f"   📍 Location: {stats['area_name']} (Room: {stats['room_id']})")
            print(f"   🎮 Game State: {stats['game_state']}")
        else:
            print("❌ Could not read game stats")

if __name__ == "__main__":
    main() 