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
            'game_state': 0x7E0998,
            'room_id': 0x7E079B,
            'area_id': 0x7E079F,
            'player_x': 0x7E0AF6,
            'player_y': 0x7E0AFA
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
        """Check if Super Metroid is loaded and playing"""
        response = self.send_command("GET_STATUS")
        if response and "Super Metroid" in response and "PLAYING" in response:
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
        # Read a large chunk that contains all our stats
        base_address = self.memory_map['health']
        data = self.read_memory_range(base_address, 32)  # Read 32 bytes
        
        if not data or len(data) < 16:
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
            
            # Read additional data separately
            stats['area_id'] = self.read_byte(self.memory_map['area_id'])
            stats['area_name'] = self.areas.get(stats['area_id'], "Unknown")
            stats['room_id'] = self.read_word(self.memory_map['room_id'])
            stats['game_state'] = self.read_word(self.memory_map['game_state'])
            
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
            print(f"   🚀 Missiles: {stats['missiles']} / {stats['max_missiles']}")
            print(f"   ⭐ Super Missiles: {stats['supers']} / {stats['max_supers']}")
            print(f"   💥 Power Bombs: {stats['power_bombs']} / {stats['max_power_bombs']}")
            print(f"   📍 Location: {stats['area_name']} (Room: {stats['room_id']})")
            print(f"   🎮 Game State: {stats['game_state']}")
        else:
            print("❌ Could not read game stats")

if __name__ == "__main__":
    main() 