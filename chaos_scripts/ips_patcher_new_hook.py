#!/usr/bin/env python3
"""
🎯 NEW HOOK POINT TEST 🎯

Since 0x8463 is corrupting graphics data (not executable code), 
let's try proper hook points used by ROM hackers:

- 0x82E4: Main game loop (may crash - too critical)
- 0x8289: Game state handler 
- 0x828E: Frame processing
- 0x90B5: VBlank handler
- NMI Vector at 0x7FEA

Let's test multiple safe hook points!
"""

import struct
from typing import List, Tuple, Optional

class MinimalIPSPatcher:
    """Minimal IPS patcher for testing hook points"""
    
    def __init__(self):
        self.patches = []
    
    def add_patch(self, address: int, data: bytes, description: str = ""):
        """Add a patch to the list"""
        self.patches.append({
            'address': address,
            'data': data,
            'description': description
        })
        print(f"📝 Added patch: 0x{address:06X} ({len(data)} bytes) - {description}")
    
    def create_ips_file(self, output_path: str) -> bool:
        """Create IPS patch file"""
        try:
            with open(output_path, 'wb') as f:
                # IPS header
                f.write(b'PATCH')
                
                # Write all patches
                for patch in self.patches:
                    address = patch['address']
                    data = patch['data']
                    
                    # Address (3 bytes, big-endian)
                    f.write(struct.pack('>I', address)[1:])  # Skip first byte for 3-byte address
                    
                    # Size (2 bytes, big-endian)
                    f.write(struct.pack('>H', len(data)))
                    
                    # Data
                    f.write(data)
                
                # EOF marker
                f.write(b'EOF')
            
            print(f"✅ Created IPS patch: {output_path}")
            print(f"📊 Total patches: {len(self.patches)}")
            return True
            
        except Exception as e:
            print(f"❌ Error creating IPS: {e}")
            return False

class NewHookTester:
    """Test different hook points to find one that doesn't corrupt graphics"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def allocate_space(self, size: int) -> int:
        """Allocate space in ROM for our test code"""
        return 0x2F8000
    
    def create_vblank_hook_test(self, output_ips: str) -> bool:
        """Test hooking into VBlank handler at 0x90B5"""
        
        print("🎯 NEW HOOK TEST - VBlank Handler")
        print("=" * 50)
        print("🎯 Testing VBlank handler at 0x90B5 (safer than 0x8463)")
        print("⚠️  This should not corrupt ship graphics!")
        print()
        
        # Simple palette corruption code
        chaos_code = bytes([
            # Save registers
            0x48, 0xDA, 0x5A,        # PHA, PHX, PHY
            
            # Write bright white to a few palette addresses
            0xA9, 0xFF,              # LDA #$FF (bright white)
            0x8D, 0x05, 0xC0,        # STA $7EC005 (known working address)
            0x8D, 0x25, 0xC0,        # STA $7EC025 
            0x8D, 0x65, 0xC0,        # STA $7EC065
            
            # Restore registers and return
            0x7A, 0xFA, 0x68,        # PLY, PLX, PLA
            0x6B,                    # RTL
        ])
        
        # Put code in free space
        code_address = self.allocate_space(len(chaos_code))
        self.patcher.add_patch(code_address, chaos_code, "Palette Chaos Code")
        
        # Hook at VBlank handler 0x90B5 (should be safer)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL to chaos code
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5  # VBlank handler - should be executable code
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at VBlank Handler 0x90B5")
        
        # Create IPS patch file
        print("\n📄 Creating VBlank hook test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ VBLANK HOOK TEST COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n🔍 WHAT TO TEST:")
        print("   1. Apply this patch to clean Super Metroid ROM")
        print("   2. Check if ship sprites are still corrupted")
        print("   3. Check if you see any palette color changes")
        print("\n🎯 This tests if 0x90B5 is a better hook point!")
        
        return True

def main():
    """Create VBlank hook test"""
    print("🎯 New Hook Point Tester")
    print("=" * 60)
    print("📄 Generating: super_metroid_vblank_test.ips")
    print("🎯 Testing VBlank handler hook at 0x90B5")
    print()
    
    # Create the hook tester
    tester = NewHookTester()
    
    try:
        # Generate the VBlank hook test patch
        success = tester.create_vblank_hook_test("super_metroid_vblank_test.ips")
        
        if success:
            print("\n🎉 SUCCESS! VBlank hook test ready!")
            print("\n🔍 COMPARISON TEST:")
            print("   → Old hook (0x8463): Corrupts ship sprites")
            print("   → New hook (0x90B5): Should NOT corrupt ship sprites")
            print("   → If 0x90B5 works, we found our proper hook point!")
            return 0
        else:
            print("\n❌ Failed to create VBlank hook test")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())