#!/usr/bin/env python3
"""
🔍 EXECUTION PROOF TEST 🔍

Since we get no color changes even with comprehensive palette testing,
let's create a test that PROVES our code is executing by corrupting
something we KNOW will be visible - sprite graphics memory.

If we see graphics corruption, our code executes.
If we see no corruption, our code never runs.
"""

import struct

class MinimalIPSPatcher:
    """Minimal IPS patcher for testing"""
    
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

class ExecutionProofTest:
    """Test that definitively proves if our code executes"""
    
    def __init__(self):
        self.patcher = MinimalIPSPatcher()
    
    def allocate_space(self, size: int) -> int:
        """Allocate space for our execution test code"""
        return 0x2F8000
    
    def create_execution_proof_test(self, output_ips: str) -> bool:
        """Create a test that PROVES execution by corrupting visible graphics"""
        
        print("🔍 EXECUTION PROOF TEST")
        print("=" * 50)
        print("🎯 This will PROVE if our code executes by corrupting graphics")
        print("⚠️  If you see graphics corruption, code runs!")
        print("⚠️  If you see NO corruption, code never executes!")
        print()
        
        # DEFINITIVE execution test - corrupt sprite graphics
        execution_test_code = bytes([
            # NO register saving - keep it minimal to avoid issues
            
            # Method 1: Direct sprite graphics corruption (should be immediately visible)
            0xA9, 0xAA,                          # LDA #$AA (alternating pattern)
            0x8F, 0x00, 0x20, 0x7E,              # STA $7E2000 (sprite graphics - long addressing)
            0x8F, 0x01, 0x20, 0x7E,              # STA $7E2001
            0x8F, 0x02, 0x20, 0x7E,              # STA $7E2002
            0x8F, 0x03, 0x20, 0x7E,              # STA $7E2003
            0x8F, 0x04, 0x20, 0x7E,              # STA $7E2004
            
            # Method 2: Different pattern for contrast  
            0xA9, 0x55,                          # LDA #$55 (different pattern)
            0x8F, 0x10, 0x20, 0x7E,              # STA $7E2010
            0x8F, 0x11, 0x20, 0x7E,              # STA $7E2011
            0x8F, 0x12, 0x20, 0x7E,              # STA $7E2012
            0x8F, 0x13, 0x20, 0x7E,              # STA $7E2013
            0x8F, 0x14, 0x20, 0x7E,              # STA $7E2014
            
            # Method 3: Hit different graphics regions
            0xA9, 0xFF,                          # LDA #$FF (solid pattern)
            0x8F, 0x00, 0x30, 0x7E,              # STA $7E3000 (different graphics area)
            0x8F, 0x01, 0x30, 0x7E,              # STA $7E3001
            0x8F, 0x02, 0x30, 0x7E,              # STA $7E3002
            
            # Return immediately
            0x6B,                                # RTL
        ])
        
        # Put execution test code in free space
        code_address = self.allocate_space(len(execution_test_code))
        self.patcher.add_patch(code_address, execution_test_code, "Execution Proof Test Code")
        
        # Hook at our working VBlank address (0x90B5)
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL to test code
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5  # Our confirmed working hook
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at VBlank 0x90B5")
        
        # Create IPS patch file
        print("\n📄 Creating execution proof test IPS...")
        if not self.patcher.create_ips_file(output_ips):
            return False
        
        print("\n✅ EXECUTION PROOF TEST COMPLETE!")
        print(f"📄 IPS Patch: {output_ips}")
        print("\n🔍 CRITICAL TEST RESULTS:")
        print("   🟢 IF YOU SEE GRAPHICS CORRUPTION:")
        print("      → Our code DOES execute")
        print("      → Problem is with palette addressing")  
        print("      → We need different palette memory approach")
        print()
        print("   🔴 IF YOU SEE NO GRAPHICS CORRUPTION:")
        print("      → Our code NEVER executes")
        print("      → 0x90B5 hook doesn't actually run our code")
        print("      → We need a different hook point entirely")
        print()
        print("🎯 This will definitively solve the mystery!")
        
        return True

def main():
    """Create execution proof test"""
    print("🔍 Execution Proof Tester")
    print("=" * 60)
    print("📄 Generating: super_metroid_execution_proof.ips")
    print("🎯 Proving if our code actually executes")
    print()
    
    # Create the execution tester
    tester = ExecutionProofTest()
    
    try:
        # Generate the execution proof test
        success = tester.create_execution_proof_test("super_metroid_execution_proof.ips")
        
        if success:
            print("\n🎉 SUCCESS! Execution proof test ready!")
            print("\n🔍 THE ULTIMATE TEST:")
            print("   → This will DEFINITELY show if our code runs")
            print("   → Graphics corruption = code executes")
            print("   → No corruption = code never runs")
            print("   → This solves the mystery once and for all!")
            return 0
        else:
            print("\n❌ Failed to create execution proof test")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())