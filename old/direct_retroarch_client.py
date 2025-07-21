#!/usr/bin/env python3
"""
Direct RetroArch Network Client
Bypasses QUsb2Snes and connects directly to RetroArch
"""

import socket
import struct
import time

class DirectRetroArchClient:
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        
    def connect(self):
        """Connect to RetroArch network interface"""
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.connect((self.host, self.port))
            print(f"✅ Connected to RetroArch at {self.host}:{self.port}")
            return True
        except Exception as e:
            print(f"❌ Failed to connect: {e}")
            return False
            
    def send_command(self, command):
        """Send a command to RetroArch"""
        if not self.sock:
            return None
            
        try:
            # Send command + newline
            self.sock.send(f"{command}\n".encode())
            
            # Read response
            response = b""
            while True:
                chunk = self.sock.recv(1024)
                if not chunk:
                    break
                response += chunk
                if b'\n' in chunk:
                    break
                    
            return response.decode().strip()
        except Exception as e:
            print(f"❌ Command error: {e}")
            return None
            
    def read_memory(self, address, size):
        """Read memory from SNES"""
        # Convert SNES address to actual address
        # This is more complex - we'd need to understand RetroArch's memory mapping
        command = f"READ_CORE_MEMORY {address:X} {size}"
        return self.send_command(command)
        
    def get_core_info(self):
        """Get current core information"""
        return self.send_command("GET_STATUS")
        
    def disconnect(self):
        """Disconnect from RetroArch"""
        if self.sock:
            self.sock.close()
            self.sock = None
            print("🔌 Disconnected from RetroArch")

def main():
    """Test the direct connection"""
    client = DirectRetroArchClient()
    
    if not client.connect():
        print("❌ Could not connect to RetroArch")
        print("Make sure RetroArch is running with network_cmd_enable = true")
        return
        
    try:
        # Test basic commands
        print("📋 Getting core info...")
        info = client.get_core_info()
        if info:
            print(f"📨 Response: {info}")
        else:
            print("❌ No response from RetroArch")
            
        # Try memory read (this might not work directly)
        print("🧠 Attempting memory read...")
        memory = client.read_memory(0x7E09C2, 16)
        if memory:
            print(f"📨 Memory: {memory}")
        else:
            print("❌ Memory read failed")
            
    except KeyboardInterrupt:
        print("\n⚠️ Interrupted by user")
    finally:
        client.disconnect()

if __name__ == "__main__":
    main() 