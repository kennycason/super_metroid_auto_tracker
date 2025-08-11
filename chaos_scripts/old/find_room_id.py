#!/usr/bin/env python3
"""
Super Metroid Room ID Finder

This script helps find the correct memory address for room ID detection
by monitoring multiple potential addresses and showing their values.

Run this script and then change rooms in Super Metroid to see which
address changes consistently with room transitions.
"""

import socket
import struct
import time

class RoomIDFinder:
    def __init__(self, host="localhost", port=55355):
        self.host = host
        self.port = port
        self.sock = None
        
    def connect(self):
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
        try:
            if not self.sock:
                if not self.connect():
                    return ""
            
            self.sock.sendto(command.encode(), (self.host, self.port))
            response, _ = self.sock.recvfrom(1024)
            return response.decode().strip()
        except Exception:
            return ""

    def read_memory(self, address: int, length: int) -> bytes:
        try:
            command = f"READ_CORE_MEMORY 0x{address:X} {length}"
            response = self.send_command(command)
            if response.startswith("READ_CORE_MEMORY"):
                hex_data = response.split()[-1]
                return bytes.fromhex(hex_data)
        except Exception:
            pass
        return b""

    def check_game_status(self) -> bool:
        response = self.send_command("GET_STATUS")
        if response and "PLAYING" in response:
            return "super metroid" in response.lower()
        return False

    def monitor_addresses(self):
        print("🔍 Super Metroid Room ID Finder")
        print("=" * 50)
        print("Monitoring potential room ID addresses...")
        print("Change rooms in Super Metroid and watch for consistent changes!")
        print("Press Ctrl+C to stop")
        print()
        
        # Potential room ID addresses to monitor
        addresses = [
            ("Room ID Main", 0x7E079B),
            ("Room ID Alt", 0x7E079D), 
            ("Area+Room", 0x7E0799),
            ("DMA Room", 0x7F00DE),
            ("Area ID", 0x7E079F),
            ("Map Station", 0x7ED91C),
            ("Current Area", 0x7E079F),
            ("Room Width", 0x7E07A5),
            ("Room Height", 0x7E07A7),
            ("Game State", 0x7E0998),
        ]
        
        previous_values = {}
        change_counts = {}
        
        while True:
            try:
                if not self.check_game_status():
                    print("⚠️  Waiting for Super Metroid...")
                    time.sleep(2)
                    continue
                
                print("\r" + " " * 100 + "\r", end="")  # Clear line
                line = "📊 "
                
                current_values = {}
                for name, addr in addresses:
                    data = self.read_memory(addr, 2)
                    if len(data) == 2:
                        value = struct.unpack('<H', data)[0]
                        current_values[addr] = value
                        
                        # Check for changes
                        if addr in previous_values:
                            if previous_values[addr] != value:
                                change_counts[addr] = change_counts.get(addr, 0) + 1
                                print(f"\n🚪 {name} CHANGED: 0x{previous_values[addr]:04X} → 0x{value:04X} (#{change_counts[addr]})")
                        
                        # Add to display line
                        changes = change_counts.get(addr, 0)
                        color = "🔴" if changes > 0 else "⚪"
                        line += f"{color}{name}:0x{value:04X}({changes}) "
                
                previous_values = current_values
                print(f"\r{line}", end="", flush=True)
                time.sleep(0.5)
                
            except KeyboardInterrupt:
                print("\n\n🛑 Monitoring stopped")
                break
            except Exception as e:
                print(f"\n❌ Error: {e}")
                time.sleep(1)
        
        print("\n📈 Change Summary:")
        for name, addr in addresses:
            changes = change_counts.get(addr, 0)
            print(f"  {name} (0x{addr:X}): {changes} changes")
        
        if change_counts:
            best_addr = max(change_counts.items(), key=lambda x: x[1])
            print(f"\n🎯 Best room ID candidate: 0x{best_addr[0]:X} ({best_addr[1]} changes)")

def main():
    finder = RoomIDFinder()
    finder.monitor_addresses()

if __name__ == "__main__":
    main()