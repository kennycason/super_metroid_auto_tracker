#!/usr/bin/env python3
"""
Super Metroid - Give Grappling Beam Script

This script uses RetroArch's UDP network interface to write to Super Metroid's memory
and give Samus the Grappling Beam. The script reads the current items value, sets the
grappling beam bit (0x4000), and writes it back to memory.

Based on the memory layout from the Super Metroid tracker server.

Usage: python give_grappling_beam.py
"""

import socket
import struct
import sys
import time

class RetroArchGrappleGiver:
    """Gives grappling beam to Samus via RetroArch UDP interface"""
    
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        
        # Super Metroid memory addresses (from tracker server)
        self.ITEMS_ADDRESS = 0x7E09A4  # Items memory location
        self.GRAPPLE_BIT = 0x4000      # Grappling beam bit mask
        
    def connect(self) -> bool:
        """Connect to RetroArch UDP interface"""
        try:
            if self.sock:
                self.sock.close()
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(2.0)
            return True
        except Exception as e:
            print(f"❌ UDP connection failed: {e}")
            return False
    
    def send_command(self, command: str) -> str:
        """Send command to RetroArch and get response"""
        if not self.sock:
            if not self.connect():
                return None
                
        try:
            # Clear any pending data
            self.sock.settimeout(0.1)
            try:
                while True:
                    self.sock.recv(1024)
            except socket.timeout:
                pass
                
            # Send command
            self.sock.settimeout(2.0)
            print(f"📤 Sending: {command}")
            self.sock.sendto(command.encode(), (self.host, self.port))
            data, addr = self.sock.recvfrom(1024)
            response = data.decode().strip()
            print(f"📥 Response: {response}")
            return response
            
        except socket.timeout:
            print(f"⏰ UDP timeout for command: {command}")
            return None
        except Exception as e:
            print(f"❌ UDP error for command {command}: {e}")
            return None
    
    def read_memory(self, address: int, size: int) -> bytes:
        """Read memory from RetroArch"""
        command = f"READ_CORE_MEMORY 0x{address:X} {size}"
        response = self.send_command(command)
        
        if not response or not response.startswith("READ_CORE_MEMORY"):
            return None
            
        try:
            parts = response.split(' ', 2)
            if len(parts) < 3:
                return None
            hex_data = parts[2].replace(' ', '')
            return bytes.fromhex(hex_data)
        except ValueError as e:
            print(f"❌ Failed to parse memory data: {e}")
            return None
    
    def write_memory(self, address: int, data: bytes) -> bool:
        """Write memory to RetroArch"""
        # Convert bytes to space-separated hex values
        hex_bytes = ' '.join(f'{b:02X}' for b in data)
        command = f"WRITE_CORE_MEMORY 0x{address:X} {hex_bytes}"
        
        response = self.send_command(command)
        return response is not None and "WRITE_CORE_MEMORY" in response
    
    def check_game_status(self) -> bool:
        """Check if any SNES game is loaded (assumes Super Metroid compatible)"""
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            response_lower = response.lower()
            # Accept any SNES game (Super Metroid, ROM hacks, etc.)
            if any(keyword in response_lower for keyword in ["super_nes", "snes", "metroid", "super metroid"]):
                return True
        return False
    
    def give_grappling_beam(self) -> bool:
        """Give Samus the grappling beam"""
        print("🎮 Super Metroid Grappling Beam Giver")
        print("=" * 40)
        
        # Check if game is running
        print("🔍 Checking if SNES game is running...")
        if not self.check_game_status():
            print("❌ No SNES game detected or RetroArch not responding!")
            print("   Make sure a SNES game is loaded in RetroArch")
            return False
        
        print("✅ SNES game detected! Proceeding with memory modification...")
        
        # Read current items value
        print(f"📖 Reading items memory at 0x{self.ITEMS_ADDRESS:X}...")
        items_data = self.read_memory(self.ITEMS_ADDRESS, 2)
        
        if not items_data or len(items_data) != 2:
            print("❌ Failed to read items memory!")
            return False
        
        # Parse current items
        current_items = struct.unpack('<H', items_data)[0]
        print(f"📊 Current items value: 0x{current_items:04X} ({current_items})")
        
        # Check if grappling beam is already equipped
        has_grapple = bool(current_items & self.GRAPPLE_BIT)
        print(f"🔧 Grappling beam status: {'✅ EQUIPPED' if has_grapple else '❌ NOT EQUIPPED'}")
        
        if has_grapple:
            print("🎉 Samus already has the grappling beam!")
            return True
        
        # Add grappling beam
        new_items = current_items | self.GRAPPLE_BIT
        print(f"⚡ Adding grappling beam...")
        print(f"   Old value: 0x{current_items:04X}")
        print(f"   New value: 0x{new_items:04X}")
        
        # Write new items value
        new_data = struct.pack('<H', new_items)
        if not self.write_memory(self.ITEMS_ADDRESS, new_data):
            print("❌ Failed to write grappling beam to memory!")
            return False
        
        # Verify the change
        print("🔍 Verifying grappling beam was added...")
        time.sleep(0.5)  # Give it a moment
        
        verify_data = self.read_memory(self.ITEMS_ADDRESS, 2)
        if verify_data:
            verify_items = struct.unpack('<H', verify_data)[0]
            has_grapple_now = bool(verify_items & self.GRAPPLE_BIT)
            
            if has_grapple_now:
                print("🎉 SUCCESS! Samus now has the grappling beam!")
                print(f"   Verified value: 0x{verify_items:04X}")
                return True
            else:
                print("❌ Verification failed - grappling beam not detected")
                return False
        else:
            print("⚠️  Could not verify - but command was sent")
            return True
    
    def close(self):
        """Close UDP connection"""
        if self.sock:
            self.sock.close()

def main():
    """Main function"""
    print("🚀 Starting RetroArch Grappling Beam Giver...")
    
    giver = RetroArchGrappleGiver()
    
    try:
        success = giver.give_grappling_beam()
        if success:
            print("\n✅ Operation completed successfully!")
            print("   Switch to RetroArch to see Samus with her new grappling beam!")
        else:
            print("\n❌ Operation failed!")
            print("   Make sure:")
            print("   1. RetroArch is running with a SNES game loaded")
            print("   2. Network commands are enabled in RetroArch settings")
            print("   3. RetroArch is listening on port 55355")
    
    except KeyboardInterrupt:
        print("\n🛑 Operation cancelled by user")
    
    except Exception as e:
        print(f"\n💥 Unexpected error: {e}")
    
    finally:
        giver.close()

if __name__ == "__main__":
    main()