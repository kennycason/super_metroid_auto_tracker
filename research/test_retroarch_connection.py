#!/usr/bin/env python3
"""
Test RetroArch UDP connection to diagnose network command issues.
This script attempts to read memory from RetroArch like the tracker does.
"""

import socket
import time

def test_retroarch_connection(host='localhost', port=55355, timeout=2.0):
    """Test if RetroArch is responding to network commands."""
    
    print(f"🔍 Testing RetroArch connection at {host}:{port}")
    print(f"⏱️  Timeout: {timeout}s\n")
    
    try:
        # Create UDP socket
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(timeout)
        
        print("✅ UDP socket created")
        
        # Try to connect
        sock.connect((host, port))
        print(f"✅ Connected to {host}:{port}")
        
        # Try to read a simple memory address (version check at 0x7e0000)
        # RetroArch command format: "READ_CORE_MEMORY <address> <length>"
        test_address = 0x7e0000
        test_length = 2
        command = f"READ_CORE_MEMORY {test_address:x} {test_length}\n"
        
        print(f"\n📤 Sending command: {command.strip()}")
        sock.send(command.encode('utf-8'))
        print("✅ Command sent")
        
        # Try to receive response
        print(f"⏳ Waiting for response (timeout: {timeout}s)...")
        try:
            data, addr = sock.recvfrom(1024)
            print(f"✅ RECEIVED RESPONSE!")
            print(f"📦 Data: {data[:100]}...")  # Show first 100 bytes
            print(f"📍 From: {addr}")
            print("\n" + "="*60)
            print("🎉 SUCCESS! RetroArch is responding to network commands!")
            print("="*60)
            return True
            
        except socket.timeout:
            print(f"❌ TIMEOUT after {timeout}s")
            print("\n" + "="*60)
            print("⚠️  PROBLEM IDENTIFIED!")
            print("="*60)
            print("\nRetroArch is NOT responding to network commands.")
            print("\nThis means one of the following:")
            print("  1. Network Commands are NOT enabled in RetroArch")
            print("  2. RetroArch is not running")
            print("  3. No game is loaded in RetroArch")
            print("  4. Wrong port number")
            print("\n📋 SOLUTION:")
            print("  1. Open RetroArch")
            print("  2. Go to: Settings → Network")
            print("  3. Enable 'Network Commands'")
            print("  4. Set 'Network Command Port' to 55355")
            print("  5. RESTART RetroArch")
            print("  6. Load a game")
            return False
            
    except Exception as e:
        print(f"\n❌ ERROR: {e}")
        print("\n" + "="*60)
        print("⚠️  CONNECTION FAILED!")
        print("="*60)
        print("\nPossible issues:")
        print("  - RetroArch is not running")
        print("  - Port 55355 is blocked by firewall")
        print("  - RetroArch is listening on a different port")
        return False
        
    finally:
        sock.close()
        print("\n🔌 Socket closed")

def main():
    print("="*60)
    print("RetroArch Network Command Test")
    print("="*60)
    print()
    
    # Test with short timeout to match what the tracker experiences
    success = test_retroarch_connection(timeout=5.0)
    
    if not success:
        print("\n💡 TIP: Make sure RetroArch is running with a game loaded!")
        print("    The tracker cannot read memory unless a game is active.")

if __name__ == "__main__":
    main()

