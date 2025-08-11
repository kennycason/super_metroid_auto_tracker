#!/usr/bin/env python3
"""
🧪 Super Metroid Simple Tile Test 🧪

This is a MINIMAL test script to figure out the correct memory addresses
for live map tile modification. We'll try ONE known address at a time.

Based on actual Super Metroid memory documentation, let's test these addresses:
- 0x7F0002: Room state layer 1 (known to work)
- 0x7F6400-0x7F8000: Room collision/block data
- 0x7E2000-0x7E4000: VRAM tile graphics

Let's start VERY simple and see what actually works!
"""

import socket
import struct
import random
import time
import sys
import argparse

class SuperMetroidSimpleTileTest:
    """Simple test to find working tile memory addresses"""
    
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        self.running = False
        
        # TEST ADDRESSES - these are from actual Super Metroid documentation
        self.test_addresses = [
            # Known working addresses from your game state parser
            {'addr': 0x7E079B, 'name': 'Room ID', 'size': 2, 'read_only': True},
            {'addr': 0x7E0AF6, 'name': 'Player X', 'size': 2, 'read_only': True},
            {'addr': 0x7E0AFA, 'name': 'Player Y', 'size': 2, 'read_only': True},
            
            # Room collision/block data (potential tile addresses)
            {'addr': 0x7F0002, 'name': 'Room State Layer 1', 'size': 1, 'read_only': False},
            {'addr': 0x7F6402, 'name': 'Room Block Data Start', 'size': 1, 'read_only': False},
            {'addr': 0x7F6500, 'name': 'Room Block Data Mid', 'size': 1, 'read_only': False},
            {'addr': 0x7F7000, 'name': 'Room Block Data End', 'size': 1, 'read_only': False},
            
            # VRAM/Graphics (potential visual tile data)
            {'addr': 0x7E2000, 'name': 'VRAM BG1 Start', 'size': 1, 'read_only': False},
            {'addr': 0x7E2100, 'name': 'VRAM BG1 Mid', 'size': 1, 'read_only': False},
            {'addr': 0x7E3000, 'name': 'VRAM BG2 Start', 'size': 1, 'read_only': False},
            {'addr': 0x7E3100, 'name': 'VRAM BG2 Mid', 'size': 1, 'read_only': False},
        ]

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

    def check_game_status(self) -> bool:
        """Check if Super Metroid is loaded"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            if "super metroid" in response_lower:
                return True
        return False

    def test_address_read(self, test_addr: dict) -> bool:
        """Test if we can read from an address"""
        data = self.read_memory(test_addr['addr'], test_addr['size'])
        if data and len(data) >= test_addr['size']:
            if test_addr['size'] == 1:
                value = data[0]
                print(f"✅ READ: {test_addr['name']} (0x{test_addr['addr']:X}) = 0x{value:02X} ({value})")
            elif test_addr['size'] == 2:
                value = struct.unpack('<H', data)[0]
                print(f"✅ READ: {test_addr['name']} (0x{test_addr['addr']:X}) = 0x{value:04X} ({value})")
            return True
        else:
            print(f"❌ READ FAILED: {test_addr['name']} (0x{test_addr['addr']:X}) - got {len(data) if data else 0} bytes, needed {test_addr['size']}")
            return False

    def test_address_write(self, test_addr: dict) -> bool:
        """Test if we can write to an address (and see the change)"""
        if test_addr['read_only']:
            print(f"⚠️  SKIPPING WRITE: {test_addr['name']} (read-only)")
            return False
        
        # Read original value
        original_data = self.read_memory(test_addr['addr'], test_addr['size'])
        if not original_data or len(original_data) < test_addr['size']:
            print(f"❌ Cannot read original value from {test_addr['name']} - got {len(original_data) if original_data else 0} bytes, needed {test_addr['size']}")
            return False
        
        if test_addr['size'] == 1:
            original_value = original_data[0]
            # Try writing a different value (but safe)
            new_value = (original_value + 1) % 256
            test_data = bytes([new_value])
        else:
            original_value = struct.unpack('<H', original_data)[0]
            new_value = (original_value + 1) % 65536
            test_data = struct.pack('<H', new_value)
        
        # Write new value
        print(f"🔄 TESTING WRITE: {test_addr['name']} 0x{original_value:02X} → 0x{new_value:02X}")
        if self.write_memory(test_addr['addr'], test_data):
            # Read back to verify
            verify_data = self.read_memory(test_addr['addr'], test_addr['size'])
            if verify_data and len(verify_data) >= test_addr['size']:
                if test_addr['size'] == 1:
                    verify_value = verify_data[0]
                else:
                    verify_value = struct.unpack('<H', verify_data)[0]
                
                if verify_value == new_value:
                    print(f"✅ WRITE SUCCESS: {test_addr['name']} changed to 0x{verify_value:02X}")
                    
                    # Restore original value
                    self.write_memory(test_addr['addr'], original_data)
                    print(f"🔄 RESTORED: {test_addr['name']} back to 0x{original_value:02X}")
                    return True
                else:
                    print(f"❌ WRITE FAILED: {test_addr['name']} read back 0x{verify_value:02X}, expected 0x{new_value:02X}")
            else:
                print(f"❌ VERIFY FAILED: Cannot read back {test_addr['name']}")
        else:
            print(f"❌ WRITE COMMAND FAILED: {test_addr['name']}")
        
        return False

    def run_address_tests(self):
        """Test all addresses to see which ones work"""
        print("🧪 SUPER METROID MEMORY ADDRESS TEST")
        print("=" * 50)
        print("Testing known addresses to find working tile memory...")
        print()
        
        if not self.check_game_status():
            print("❌ Game not detected. Make sure Super Metroid is running in RetroArch")
            return
        
        print("✅ Game detected! Starting address tests...")
        print()
        
        working_reads = []
        working_writes = []
        
        for test_addr in self.test_addresses:
            print(f"🔍 TESTING: {test_addr['name']} (0x{test_addr['addr']:X})")
            
            # Test read
            if self.test_address_read(test_addr):
                working_reads.append(test_addr)
                
                # Test write (if not read-only)
                if self.test_address_write(test_addr):
                    working_writes.append(test_addr)
            
            print()
            time.sleep(0.5)  # Brief pause between tests
        
        # Summary
        print("📊 TEST RESULTS SUMMARY")
        print("=" * 30)
        print(f"✅ Working reads: {len(working_reads)}")
        for addr in working_reads:
            print(f"   - {addr['name']} (0x{addr['addr']:X})")
        
        print(f"\n✅ Working writes: {len(working_writes)}")
        for addr in working_writes:
            print(f"   - {addr['name']} (0x{addr['addr']:X})")
        
        if working_writes:
            print(f"\n🎯 RECOMMENDATION: Use these addresses for tile modification:")
            for addr in working_writes:
                print(f"   - 0x{addr['addr']:X} ({addr['name']})")
        else:
            print(f"\n❌ NO WORKING WRITE ADDRESSES FOUND!")
            print("   We need to find the correct memory addresses for live tile data.")

def main():
    """Main function"""
    parser = argparse.ArgumentParser(description="Super Metroid Memory Address Test")
    args = parser.parse_args()
    
    print("🧪 Super Metroid Simple Tile Test")
    print("=" * 40)
    print("This script tests memory addresses to find working tile data.")
    print("We'll try reading and writing to various addresses safely.")
    print()
    
    tester = SuperMetroidSimpleTileTest()
    tester.run_address_tests()

if __name__ == "__main__":
    main()