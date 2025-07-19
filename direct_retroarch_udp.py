#!/usr/bin/env python3
"""
Direct RetroArch UDP Client
Uses UDP to communicate with RetroArch's network interface
"""

import socket
import time

class RetroArchUDPClient:
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        
    def connect(self):
        """Create UDP socket"""
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.sock.settimeout(2.0)  # 2 second timeout
            print(f"✅ UDP socket ready for {self.host}:{self.port}")
            return True
        except Exception as e:
            print(f"❌ Failed to create socket: {e}")
            return False
            
    def send_command(self, command):
        """Send a command via UDP"""
        if not self.sock:
            return None
            
        try:
            # Send command
            self.sock.sendto(command.encode(), (self.host, self.port))
            print(f"📤 Sent: {command}")
            
            # Try to receive response
            try:
                data, addr = self.sock.recvfrom(1024)
                response = data.decode().strip()
                print(f"📨 Received: {response}")
                return response
            except socket.timeout:
                print("⏰ No response (timeout)")
                return None
                
        except Exception as e:
            print(f"❌ Command error: {e}")
            return None
            
    def test_commands(self):
        """Test various RetroArch commands"""
        commands = [
            "VERSION",
            "GET_STATUS", 
            "GET_CONFIG_PARAM network_cmd_enable",
            "MENU_TOGGLE",  # This might show/hide menu
            "READ_CORE_MEMORY 0x7E09C2 16",  # Try to read Super Metroid health
        ]
        
        for cmd in commands:
            print(f"\n🧪 Testing command: {cmd}")
            response = self.send_command(cmd)
            time.sleep(0.5)  # Small delay between commands
            
    def disconnect(self):
        """Close UDP socket"""
        if self.sock:
            self.sock.close()
            self.sock = None
            print("🔌 Socket closed")

def main():
    """Test the UDP connection"""
    client = RetroArchUDPClient()
    
    if not client.connect():
        return
        
    try:
        print("📋 Testing RetroArch UDP commands...")
        client.test_commands()
        
    except KeyboardInterrupt:
        print("\n⚠️ Interrupted by user")
    finally:
        client.disconnect()

if __name__ == "__main__":
    main() 