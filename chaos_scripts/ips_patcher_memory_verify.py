#!/usr/bin/env python3
"""
🔍 MEMORY VERIFICATION TEST 🔍

Our code runs without crashing but shows no visual effects.
Let's verify that our memory writes are actually happening by:
1. Writing to known critical areas that SHOULD cause visible corruption
2. Testing different memory banks
3. Testing direct CGRAM writes
"""

import struct

class MemoryVerifyPatcher:
    """Verify memory writes are actually happening"""
    
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

class MemoryVerificationTest:
    """Test different memory areas and methods"""
    
    def __init__(self):
        self.patcher = MemoryVerifyPatcher()
    
    def create_critical_area_test(self, output_ips: str) -> bool:
        """Test writing to areas that MUST show corruption if working"""
        
        print("🔍 CRITICAL AREA CORRUPTION TEST")
        print("=" * 50)
        print("🎯 Writing to areas that MUST cause visible corruption")
        print("📍 Character graphics, tilemap, critical game data")
        print()
        
        critical_code = bytes([
            # Save registers
            0x48, 0xDA, 0x5A, 0x8B,  # PHA, PHX, PHY, PHB
            0xA9, 0x7E, 0x48, 0xAB,  # LDA #$7E, PHA, PLB
            
            # Test 1: Character/sprite graphics (should break sprites)
            0xA9, 0xFF,  # LDA #$FF
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000 (sprite graphics)
            0x8F, 0x00, 0x30, 0x7E,  # STA $7E3000 (more graphics)
            0x8F, 0x00, 0x40, 0x7E,  # STA $7E4000 (more graphics)
            0x8F, 0x00, 0x50, 0x7E,  # STA $7E5000 (more graphics)
            
            # Test 2: Try different patterns that should be VERY visible
            0xA9, 0x00,  # LDA #$00 (black)
            0x8F, 0x01, 0x20, 0x7E,  # STA $7E2001
            0xA9, 0xFF,  # LDA #$FF (white)
            0x8F, 0x02, 0x20, 0x7E,  # STA $7E2002
            0xA9, 0x00,  # LDA #$00 (black)
            0x8F, 0x03, 0x20, 0x7E,  # STA $7E2003
            0xA9, 0xFF,  # LDA #$FF (white)
            0x8F, 0x04, 0x20, 0x7E,  # STA $7E2004
            
            # Test 3: Try writing to bank 0x00 (might be more direct)
            0xA9, 0x00,  # LDA #$00 (switch to bank 0)
            0x48, 0xAB,  # PHA, PLB
            0xA9, 0xAA,  # LDA #$AA
            0x8D, 0x00, 0x20,  # STA $2000 (direct bank 0 write)
            0x8D, 0x01, 0x20,  # STA $2001
            0x8D, 0x02, 0x20,  # STA $2002
            
            # Test 4: Try CGRAM registers directly (no bank needed)
            0xA9, 0x00,  # LDA #$00
            0x8D, 0x21, 0x21,  # STA $2121 (CGRAM address)
            0xA9, 0xFF,  # LDA #$FF  
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data - write twice for 16-bit)
            
            # Restore registers
            0xAB,  # PLB (restore bank)
            0xAB, 0x7A, 0xFA, 0x68,  # PLB, PLY, PLX, PLA
            0x6B,  # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, critical_code, "Critical Area Corruption Test")
        
        # Hook at 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_direct_cgram_test(self, output_ips: str) -> bool:
        """Test direct CGRAM manipulation"""
        
        print("🔍 DIRECT CGRAM TEST")
        print("=" * 50)
        print("🎯 Writing directly to CGRAM registers")
        print("📍 Should cause immediate color changes")
        print()
        
        cgram_code = bytes([
            # Save registers (minimal)
            0x48,  # PHA
            
            # Direct CGRAM writes - this SHOULD work if anything does
            0xA9, 0x00,  # LDA #$00
            0x8D, 0x21, 0x21,  # STA $2121 (CGRAM address - palette 0, color 0)
            
            # Write bright white (0x7FFF in SNES format)
            0xA9, 0xFF,  # LDA #$FF (low byte)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            0xA9, 0x7F,  # LDA #$7F (high byte)  
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            
            # Write to palette entry 1
            0xA9, 0x02,  # LDA #$02 (palette 0, color 1)
            0x8D, 0x21, 0x21,  # STA $2121 (CGRAM address)
            0xA9, 0x00,  # LDA #$00 (low byte - pure red)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)  
            0xA9, 0x7C,  # LDA #$7C (high byte - pure red)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            
            # Write to palette entry 2
            0xA9, 0x04,  # LDA #$04 (palette 0, color 2)
            0x8D, 0x21, 0x21,  # STA $2121 (CGRAM address)
            0xA9, 0xE0,  # LDA #$E0 (low byte - pure green)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            0xA9, 0x03,  # LDA #$03 (high byte - pure green)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            
            # Write to palette entry 3  
            0xA9, 0x06,  # LDA #$06 (palette 0, color 3)
            0x8D, 0x21, 0x21,  # STA $2121 (CGRAM address)
            0xA9, 0x1F,  # LDA #$1F (low byte - pure blue)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            0xA9, 0x00,  # LDA #$00 (high byte - pure blue)
            0x8D, 0x22, 0x21,  # STA $2122 (CGRAM data)
            
            # Restore and return
            0x68,  # PLA
            0x6B,  # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, cgram_code, "Direct CGRAM Manipulation")
        
        # Hook at 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)

def main():
    """Generate memory verification tests"""
    print("🔍 Memory Verification Tests")
    print("=" * 60)
    print("🎯 Verifying our memory writes are actually happening")
    print()
    
    try:
        # Test 1: Critical areas that MUST show corruption
        print("📄 Generating Test 1: Critical area corruption...")
        test1 = MemoryVerificationTest()
        test1_success = test1.create_critical_area_test("super_metroid_critical_areas.ips")
        
        # Test 2: Direct CGRAM writes
        print("\n📄 Generating Test 2: Direct CGRAM writes...")
        test2 = MemoryVerificationTest()
        test2_success = test2.create_direct_cgram_test("super_metroid_direct_cgram.ips")
        
        if all([test1_success, test2_success]):
            print("\n🎉 MEMORY VERIFICATION TESTS GENERATED!")
            print("\n🔍 TEST ORDER:")
            print("   1️⃣ super_metroid_direct_cgram.ips - Direct color register writes")
            print("   2️⃣ super_metroid_critical_areas.ips - Critical graphics corruption")
            print("\n🎯 WHAT WE'RE TESTING:")
            print("   🌈 CGRAM test: Should show IMMEDIATE bright colors")
            print("   💥 Critical test: Should break sprites/graphics badly")
            print("   📊 If NEITHER works: Our code might not be executing at all!")
            print("\n🔍 IF STILL NO EFFECTS:")
            print("   → Need to check if 0x90B5 actually executes during gameplay")
            print("   → May need to find a different hook entirely")
            return 0
        else:
            print("\n❌ Failed to generate tests")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())