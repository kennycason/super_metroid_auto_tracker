#!/usr/bin/env python3
"""
🔍 MINIMAL TEST - Empty Hook Patch 🔍

This creates an IPS patch that hooks into 0x8463 but does ABSOLUTELY NOTHING.
If we still see the same ship sprite corruption, it means 0x8463 is the wrong hook point.
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

class EmptyHookTester:
    """Test if our hook point is causing the sprite corruption"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def allocate_space(self, size: int) -> int:
        """Allocate space in ROM for our minimal code"""
        # Use same free space location as main patcher
        return 0x2F8000
    
    def create_empty_hook_test(self, output_ips: str) -> bool:
        """Create a patch that hooks but does absolutely nothing"""
        
        print("🔍 MINIMAL TEST - Empty Hook")
        print("=" * 50)
        print("🎯 Testing if 0x8463 hook point is causing sprite corruption")
        print("⚠️  This patch should do NOTHING except hook and return")
        print()
        
        # MINIMAL code that does absolutely nothing
        minimal_code = bytes([
            # Just return immediately - no registers saved, no memory touched
            0x6B,  # RTL (return long) - that's it!
        ])
        
        # Put minimal code in free space
        code_address = self.allocate_space(len(minimal_code))
        self.patcher.add_patch(code_address, minimal_code, "Minimal Empty Code (RTL only)")
        
        # Hook at 0x8463 - same as our main patch
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL to minimal code
            0xEA,  # NOP
        ])
        
        hook_address = 0x8463
        self.patcher.add_patch(hook_address, hook_bytes, "Empty Hook at 0x8463")
        
        # Create IPS patch file
        print("\n📄 Creating minimal test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ MINIMAL TEST PATCH COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n🔍 WHAT TO TEST:")
        print("   1. Apply this patch to clean Super Metroid ROM")
        print("   2. Load in emulator and check ship area")
        print("   3. If you STILL see sprite corruption, then 0x8463 is wrong hook point!")
        print("   4. If you see NO corruption, then our hook point is fine")
        print("\n🎯 This will tell us if 0x8463 is corrupting sprites by itself!")
        
        return True

def main():
    """Create minimal empty hook test"""
    print("🔍 Empty Hook Tester - Minimal IPS Generator")
    print("=" * 60)
    print("📄 Generating: super_metroid_empty_test.ips")
    print("🎯 Testing if hook point 0x8463 causes sprite corruption")
    print()
    
    # Create the empty hook tester
    tester = EmptyHookTester()
    
    try:
        # Generate the minimal test patch
        success = tester.create_empty_hook_test("super_metroid_empty_test.ips")
        
        if success:
            print("\n🎉 SUCCESS! Minimal test patch ready!")
            print("\n🔍 DEBUGGING INSTRUCTIONS:")
            print("   → Apply 'super_metroid_empty_test.ips' to clean ROM")
            print("   → If you STILL see ship sprite issues, 0x8463 is bad hook point")
            print("   → If you see NO issues, then our palette code is the problem")
            return 0
        else:
            print("\n❌ Failed to create minimal test patch")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())