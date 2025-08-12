#!/usr/bin/env python3
"""
🔍 Game Detection Debug Script
Check what RetroArch reports for the current game
"""

import socket

def send_command(command: str, host="localhost", port=55355) -> str:
    """Send command to RetroArch and get response"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(2)
        sock.sendto(command.encode(), (host, port))
        response, _ = sock.recvfrom(4096)
        sock.close()
        return response.decode().strip()
    except Exception as e:
        return f"Error: {e}"

def main():
    print("🔍 RetroArch Game Detection Debug")
    print("=" * 40)
    
    print("📡 Sending GET_STATUS command...")
    status = send_command("GET_STATUS")
    print(f"📋 Raw response: {repr(status)}")
    print(f"📋 Response: {status}")
    print()
    
    print("🔍 Analysis:")
    if "PLAYING" in status:
        print("✅ RetroArch is playing a game")
        status_lower = status.lower()
        print(f"🔤 Lowercase: {status_lower}")
        
        if "super metroid" in status_lower:
            print("✅ Game detected as 'super metroid'")
        else:
            print("❌ Game NOT detected as 'super metroid'")
            print("💡 This is why the chaos script fails!")
            
        # Check for common ROM hack patterns
        if "fusion" in status_lower:
            print("🎯 Detected 'fusion' in name")
        if "x-fusion" in status_lower:
            print("🎯 Detected 'x-fusion' in name")
        if "metroid" in status_lower:
            print("🎯 At least 'metroid' is in the name")
            
    else:
        print("❌ RetroArch is not playing a game")
        
    print("\n💡 Solution:")
    print("We need to modify the game detection in super_metroid_gradient_chaos.py")
    print("to accept ROM hacks like X-Fusion!")

if __name__ == "__main__":
    main()