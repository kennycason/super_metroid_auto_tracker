#!/usr/bin/env python3
"""
Simple QUsb2Snes Test Client in Python
Requires: pip install websocket-client
"""

import json
import time
import threading
from websocket import WebSocketApp, enableTrace

class QUsb2SnesClient:
    def __init__(self, url="ws://localhost:23074"):
        self.url = url
        self.ws = None
        self.connected = False
        self.device_attached = False
        
    def on_message(self, ws, message):
        """Handle incoming messages"""
        try:
            data = json.loads(message)
            print(f"📨 Received: {json.dumps(data, indent=2)}")
            
            if "Results" in data:
                results = data["Results"]
                
                # Check if this is a device list response
                if isinstance(results, list) and len(results) > 0:
                    if any("RetroArch" in str(device) for device in results):
                        print(f"🎮 Found RetroArch device!")
                        return results
                        
                    # Check if this is device info
                    if len(results) >= 3 and results[1] == "RetroArch":
                        print(f"📋 Device Info:")
                        print(f"   Version: {results[0]}")
                        print(f"   Type: {results[1]}")
                        print(f"   ROM: {results[2]}")
                        if len(results) > 3:
                            print(f"   Flags: {', '.join(results[3:])}")
                        
        except json.JSONDecodeError as e:
            print(f"❌ Failed to parse JSON: {e}")
            
    def on_error(self, ws, error):
        """Handle errors"""
        print(f"❌ WebSocket error: {error}")
        
    def on_close(self, ws, close_status_code, close_msg):
        """Handle connection close"""
        self.connected = False
        print(f"🔌 Connection closed. Code: {close_status_code}, Message: {close_msg}")
        
    def on_open(self, ws):
        """Handle connection open"""
        self.connected = True
        print("✅ Connected to QUsb2Snes!")
        
    def connect(self):
        """Connect to QUsb2Snes"""
        print(f"🔗 Connecting to {self.url}...")
        self.ws = WebSocketApp(self.url,
                              on_open=self.on_open,
                              on_message=self.on_message,
                              on_error=self.on_error,
                              on_close=self.on_close)
        
        # Start connection in a separate thread
        wst = threading.Thread(target=self.ws.run_forever)
        wst.daemon = True
        wst.start()
        
        # Wait for connection
        timeout = 5
        while not self.connected and timeout > 0:
            time.sleep(0.1)
            timeout -= 0.1
            
        return self.connected
        
    def send_command(self, opcode, operands=None, space="SNES", flags=None):
        """Send a command to QUsb2Snes"""
        if not self.connected:
            print("❌ Not connected!")
            return False
            
        command = {
            "Opcode": opcode,
            "Space": space
        }
        
        if operands:
            command["Operands"] = operands
        if flags:
            command["Flags"] = flags
            
        message = json.dumps(command)
        print(f"📤 Sending: {message}")
        self.ws.send(message)
        return True
        
    def get_device_list(self):
        """Get list of available devices"""
        return self.send_command("DeviceList")
        
    def attach_to_device(self, device_name):
        """Attach to a specific device"""
        result = self.send_command("Attach", [device_name])
        if result:
            self.device_attached = True
        return result
        
    def get_device_info(self):
        """Get device information"""
        return self.send_command("Info")
        
    def read_memory(self, address, size):
        """Read memory from SNES"""
        return self.send_command("GetAddress", [address, str(size)])
        
    def disconnect(self):
        """Disconnect from QUsb2Snes"""
        if self.ws:
            self.ws.close()

def main():
    """Main test routine"""
    # Enable debug output
    enableTrace(True)
    
    client = QUsb2SnesClient()
    
    try:
        # Connect
        if not client.connect():
            print("❌ Failed to connect to QUsb2Snes")
            return
            
        time.sleep(1)  # Give time for connection to stabilize
        
        # Test sequence
        print("\n🧪 Starting test sequence...")
        
        # 1. Get device list
        print("\n1️⃣ Getting device list...")
        client.get_device_list()
        time.sleep(1)
        
        # 2. Attach to RetroArch
        print("\n2️⃣ Attaching to RetroArch...")
        client.attach_to_device("RetroArch Localhost")
        time.sleep(1)
        
        # 3. Get device info
        print("\n3️⃣ Getting device info...")
        client.get_device_info()
        time.sleep(1)
        
        # 4. Read some memory
        print("\n4️⃣ Reading memory at $7E0000...")
        client.read_memory("7E0000", 16)
        time.sleep(1)
        
        # Keep connection alive for a bit
        print("\n⏰ Keeping connection alive for 10 seconds...")
        for i in range(10):
            print(f"   {10-i} seconds remaining...")
            time.sleep(1)
            
    except KeyboardInterrupt:
        print("\n⚠️ Interrupted by user")
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        print("\n🔌 Disconnecting...")
        client.disconnect()

if __name__ == "__main__":
    main() 