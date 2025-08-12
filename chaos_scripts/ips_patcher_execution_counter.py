#!/usr/bin/env python3
"""
🔍 EXECUTION FREQUENCY TEST 🔍

Since our code works but doesn't show visible effects, the issue is likely
that 0x90B5 doesn't execute frequently enough. Let's test execution frequency
and then try more frequent hooks.
"""

import struct

class ExecutionCounterPatcher:
    """Test execution frequency and try better hooks"""
    
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
                    f.write(struct.pack('>I', address)[1:])
                    
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

class ExecutionFrequencyTest:
    """Test execution frequency and find better hooks"""
    
    def __init__(self):
        self.patcher = ExecutionCounterPatcher()
    
    def create_accumulating_corruption_test(self, output_ips: str) -> bool:
        """Create test that accumulates visible corruption over time"""
        
        print("🔍 ACCUMULATING CORRUPTION TEST")
        print("=" * 50)
        print("🎯 Each execution writes to NEW sprite memory locations")
        print("💡 Should gradually corrupt more and more graphics")
        print("📍 Using frame counter to vary memory locations")
        print()
        
        # Code that corrupts different locations based on frame counter
        accumulating_code = bytes([
            0x48,        # PHA
            0xDA,        # PHX  
            0x5A,        # PHY
            0x8B,        # PHB
            0xA9, 0x7E,  # LDA #$7E
            0x48,        # PHA
            0xAB,        # PLB
            
            # Read frame counter
            0xAD, 0xB4, 0x05,     # LDA $05B4 (frame counter)
            0x29, 0x1F,           # AND #$1F (mask to 0-31 range)
            0x18,                 # CLC (clear carry)
            0x69, 0x00,           # ADC #$00 (add base address offset)
            0xAA,                 # TAX (transfer to X register)
            
            # Write corruption pattern based on frame counter
            0xA9, 0xFF,           # LDA #$FF (bright corruption pattern)
            0x9F, 0x00, 0x20, 0x7E,  # STA $7E2000,X (sprite graphics + frame offset)
            0x9F, 0x20, 0x20, 0x7E,  # STA $7E2020,X (second area)
            0x9F, 0x40, 0x20, 0x7E,  # STA $7E2040,X (third area)
            
            # Also write some fixed bright patterns for immediate visibility
            0xA9, 0xAA,           # LDA #$AA (alternating pattern)
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000 (always corrupt this)
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001
            0x8F, 0x02, 0x20, 0x7E,  # STA $7E2002
            
            # Restore and return
            0xAB,        # PLB
            0x7A,        # PLY
            0xFA,        # PLX
            0x68,        # PLA
            0x6B,        # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, accumulating_code, "Accumulating Corruption Test")
        
        # Hook at 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_frequent_hook_test(self, output_ips: str) -> bool:
        """Test a more frequent hook point"""
        
        print("🔍 FREQUENT HOOK TEST") 
        print("=" * 50)
        print("🎯 Trying 0x8095 - VBlank waiting loop (should be more frequent)")
        print("📍 Should execute multiple times per frame")
        print()
        
        # Simple bright corruption that should be very visible
        frequent_code = bytes([
            0x48,        # PHA
            0xA9, 0xFF,  # LDA #$FF (bright white)
            # Write to multiple sprite locations for maximum visibility
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001  
            0x8F, 0x02, 0x20, 0x7E,  # STA $7E2002
            0x8F, 0x03, 0x20, 0x7E,  # STA $7E2003
            0x8F, 0x04, 0x20, 0x7E,  # STA $7E2004
            0x8F, 0x05, 0x20, 0x7E,  # STA $7E2005
            0x8F, 0x06, 0x20, 0x7E,  # STA $7E2006
            0x8F, 0x07, 0x20, 0x7E,  # STA $7E2007
            0x68,        # PLA
            0x6B,        # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, frequent_code, "Frequent Hook Corruption Test")
        
        # Try 0x8095 instead of 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x8095  # Different VBlank-related address
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x8095 (More Frequent)")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_main_loop_safe_test(self, output_ips: str) -> bool:
        """Test a main loop hook but with minimal code"""
        
        print("🔍 MAIN LOOP SAFE TEST")
        print("=" * 50) 
        print("🎯 Trying 0x82E4 again but with MINIMAL code")
        print("⚠️  Previously crashed, but maybe our heavy code was the issue")
        print()
        
        # Ultra-minimal code for main loop
        minimal_main_code = bytes([
            0x48,        # PHA
            0xA9, 0xAA,  # LDA #$AA 
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000 (single write)
            0x68,        # PLA
            0x6B,        # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, minimal_main_code, "Minimal Main Loop Test")
        
        # Try 0x82E4 with minimal code
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x82E4  # Main loop (previously crashed)
        self.patcher.add_patch(hook_address, hook_bytes, "Minimal Hook at 0x82E4")
        
        return self.patcher.create_ips_file(output_ips)

def main():
    """Generate execution frequency tests"""
    print("🔍 Execution Frequency Diagnostic")
    print("=" * 60)
    print("🎯 Finding hooks that execute frequently enough for visible effects")
    print()
    
    try:
        # Test 1: Accumulating corruption at 0x90B5
        print("📄 Generating Test 1: Accumulating corruption...")
        test1 = ExecutionFrequencyTest()
        test1_success = test1.create_accumulating_corruption_test("super_metroid_accumulating_test.ips")
        
        # Test 2: More frequent hook  
        print("\n📄 Generating Test 2: More frequent hook...")
        test2 = ExecutionFrequencyTest()
        test2_success = test2.create_frequent_hook_test("super_metroid_frequent_hook.ips")
        
        # Test 3: Minimal main loop
        print("\n📄 Generating Test 3: Minimal main loop...")
        test3 = ExecutionFrequencyTest()
        test3_success = test3.create_main_loop_safe_test("super_metroid_main_loop_minimal.ips")
        
        if all([test1_success, test2_success, test3_success]):
            print("\n🎉 ALL FREQUENCY TESTS GENERATED!")
            print("\n🔍 TEST PRIORITY ORDER:")
            print("   1️⃣ super_metroid_accumulating_test.ips - Should show gradual corruption over time")
            print("   2️⃣ super_metroid_frequent_hook.ips - Should show immediate heavy corruption") 
            print("   3️⃣ super_metroid_main_loop_minimal.ips - MIGHT crash, but worth testing")
            print("\n🎯 WHAT TO LOOK FOR:")
            print("   ✅ Any visible sprite corruption = hook is working!")
            print("   ❌ No corruption = hook is too infrequent")
            print("   💥 Crash = hook point is unsafe")
            return 0
        else:
            print("\n❌ Failed to generate some tests")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())