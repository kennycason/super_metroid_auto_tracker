#!/usr/bin/env python3
"""
Super Metroid - Give Super Missiles Script

This script uses RetroArch's UDP network interface to write to Super Metroid's memory
and give Samus super missiles. The script reads the current super missiles count,
adds 5 to it, and writes it back to memory.

Based on the memory layout from the Super Metroid tracker server.

Usage: python give_super_missiles.py
"""

import socket
import struct
import sys
import time

class RetroArchSuperMissileGiver:
    """Gives super missiles to Samus via RetroArch UDP interface"""
    
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        
        # Super Metroid memory addresses (from tracker server)
        self.SUPERS_ADDRESS = 0x7E09CA      # Current super missiles count
        self.MAX_SUPERS_ADDRESS = 0x7E09CC  # Max super missiles capacity
        self.SUPERS_TO_ADD = 5              # How many to give
        
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
    
    def give_super_missiles(self) -> bool:
        """Give Samus super missiles"""
        print("🚀 Super Metroid Super Missile Giver")
        print("=" * 40)
        
        # Check if game is running
        print("🔍 Checking if SNES game is running...")
        if not self.check_game_status():
            print("❌ No SNES game detected or RetroArch not responding!")
            print("   Make sure a SNES game is loaded in RetroArch")
            return False
        
        print("✅ SNES game detected! Proceeding with super missile modification...")
        
        # Read current super missiles count
        print(f"📖 Reading super missiles at 0x{self.SUPERS_ADDRESS:X}...")
        supers_data = self.read_memory(self.SUPERS_ADDRESS, 2)
        
        if not supers_data or len(supers_data) != 2:
            print("❌ Failed to read super missiles memory!")
            return False
        
        # Parse current count (2-byte little-endian)
        current_supers = struct.unpack('<H', supers_data)[0]
        print(f"📊 Current super missiles: {current_supers}")
        
        # Read max capacity for reference
        print(f"📖 Reading max super missiles at 0x{self.MAX_SUPERS_ADDRESS:X}...")
        max_supers_data = self.read_memory(self.MAX_SUPERS_ADDRESS, 2)
        
        if max_supers_data and len(max_supers_data) == 2:
            max_supers = struct.unpack('<H', max_supers_data)[0]
            print(f"📊 Max super missiles capacity: {max_supers}")
        else:
            max_supers = 999  # Default fallback
            print(f"⚠️  Could not read max capacity, assuming {max_supers}")
        
        # Calculate new count
        new_supers = current_supers + self.SUPERS_TO_ADD
        new_max_supers = max_supers
        
        # If we exceed capacity, increase the max capacity too
        if new_supers > max_supers:
            new_max_supers = new_supers
            print(f"🔧 Increasing max capacity from {max_supers} to {new_max_supers}")
            
            # Write new max capacity first
            max_data = struct.pack('<H', new_max_supers)
            if not self.write_memory(self.MAX_SUPERS_ADDRESS, max_data):
                print("❌ Failed to write max super missiles capacity!")
                return False
            print(f"✅ Max capacity updated to {new_max_supers}")
        
        print(f"🚀 Adding {self.SUPERS_TO_ADD} super missiles...")
        print(f"   Current: {current_supers}")
        print(f"   New:     {new_supers}")
        print(f"   Max:     {new_max_supers}")
        
        # Write new super missiles count
        new_data = struct.pack('<H', new_supers)
        if not self.write_memory(self.SUPERS_ADDRESS, new_data):
            print("❌ Failed to write super missiles to memory!")
            return False
        
        # Verify the change
        print("🔍 Verifying super missiles were added...")
        time.sleep(0.5)  # Give it a moment
        
        verify_data = self.read_memory(self.SUPERS_ADDRESS, 2)
        if verify_data:
            verify_supers = struct.unpack('<H', verify_data)[0]
            
            if verify_supers == new_supers:
                print(f"🎉 SUCCESS! Samus now has {verify_supers} super missiles!")
                print(f"   Added: {verify_supers - current_supers} super missiles")
                return True
            else:
                print(f"❌ Verification failed - expected {new_supers}, got {verify_supers}")
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
    print("🚀 Starting RetroArch Super Missile Giver...")
    
    giver = RetroArchSuperMissileGiver()
    
    try:
        success = giver.give_super_missiles()
        if success:
            print("\n✅ Operation completed successfully!")
            print("   Switch to RetroArch to see Samus with her new super missiles!")
            print("   Check your inventory or HUD for the updated count.")
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