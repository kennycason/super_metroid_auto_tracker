#!/usr/bin/env python3
"""
🔍 STEP-BY-STEP DIAGNOSTIC 🔍

Now that we know JSL/RTL works, let's add memory operations ONE AT A TIME
to identify exactly what causes the crashes.
"""

import struct

class StepByStepIPSPatcher:
    """Step-by-step diagnostic IPS patcher"""
    
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

class StepByStepDiagnostic:
    """Progressively add operations to find what breaks"""
    
    def __init__(self):
        self.patcher = StepByStepIPSPatcher()
    
    def create_step1_register_save_test(self, output_ips: str) -> bool:
        """STEP 1: Test register save/restore operations"""
        
        print("🔍 STEP 1: REGISTER SAVE/RESTORE TEST")
        print("=" * 50)
        print("🎯 Testing if PHA/PHX/PHY/PLA/PLX/PLY work")
        print("⚠️  NO memory writes - just register operations")
        print()
        
        # Test register save/restore (what we do before memory writes)
        step1_code = bytes([
            0x48,  # PHA (save A register)
            0xDA,  # PHX (save X register) 
            0x5A,  # PHY (save Y register)
            # Now restore them
            0x7A,  # PLY (restore Y register)
            0xFA,  # PLX (restore X register)
            0x68,  # PLA (restore A register)
            0x6B,  # RTL (return)
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, step1_code, "Step 1: Register Save/Restore Test")
        
        # Hook at safe 0x90B5
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_step2_bank_switch_test(self, output_ips: str) -> bool:
        """STEP 2: Test bank switching (PHB/PLB)"""
        
        print("🔍 STEP 2: BANK SWITCHING TEST")
        print("=" * 50)
        print("🎯 Testing if PHB/PLB bank switching works")
        print("⚠️  NO memory writes - just bank register operations")
        print()
        
        step2_code = bytes([
            0x48,        # PHA
            0xDA,        # PHX
            0x5A,        # PHY
            0x8B,        # PHB (save data bank register)
            0xA9, 0x7E,  # LDA #$7E (load bank 0x7E)
            0x48,        # PHA (push to stack)
            0xAB,        # PLB (pull from stack to data bank register)
            # Now restore everything
            0xAB,        # PLB (restore original data bank)
            0x7A,        # PLY
            0xFA,        # PLX
            0x68,        # PLA
            0x6B,        # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, step2_code, "Step 2: Bank Switching Test")
        
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_step3_safe_memory_write_test(self, output_ips: str) -> bool:
        """STEP 3: Test safe memory write to known unused area"""
        
        print("🔍 STEP 3: SAFE MEMORY WRITE TEST")
        print("=" * 50)
        print("🎯 Testing single memory write to safe unused WRAM")
        print("📍 Writing to 0x7F0000 (should be safe unused area)")
        print()
        
        step3_code = bytes([
            0x48,        # PHA
            0xDA,        # PHX
            0x5A,        # PHY
            0x8B,        # PHB
            0xA9, 0x7E,  # LDA #$7E
            0x48,        # PHA
            0xAB,        # PLB
            # Safe memory write test
            0xA9, 0xAA,  # LDA #$AA (test pattern)
            0x8F, 0x00, 0x00, 0x7F,  # STA $7F0000 (absolute long addressing to safe area)
            # Restore and return
            0xAB,        # PLB
            0x7A,        # PLY
            0xFA,        # PLX
            0x68,        # PLA
            0x6B,        # RTL
        ])
        
        code_address = 0x2F8000
        self.patcher.add_patch(code_address, step3_code, "Step 3: Safe Memory Write Test")
        
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)
    
    def create_step4_sprite_corruption_test(self, output_ips: str) -> bool:
        """STEP 4: Test sprite graphics corruption (we know this should be visible)"""
        
        print("🔍 STEP 4: SPRITE CORRUPTION TEST")
        print("=" * 50)
        print("🎯 Testing write to sprite graphics (should cause visible corruption)")
        print("📍 Writing to 0x7E2000 (sprite graphics area)")
        print()
        
        step4_code = bytes([
            0x48,        # PHA
            0xDA,        # PHX
            0x5A,        # PHY
            0x8B,        # PHB
            0xA9, 0x7E,  # LDA #$7E
            0x48,        # PHA
            0xAB,        # PLB
            # Sprite corruption (should be visible!)
            0xA9, 0xFF,  # LDA #$FF (bright pattern)
            0x8F, 0x00, 0x20, 0x7E,  # STA $7E2000 (sprite graphics)
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
        self.patcher.add_patch(code_address, step4_code, "Step 4: Sprite Corruption Test")
        
        hook_bytes = bytes([
            0x22, code_address & 0xFF, (code_address >> 8) & 0xFF, (code_address >> 16) & 0xFF,  # JSL
            0xEA,  # NOP
        ])
        
        hook_address = 0x90B5
        self.patcher.add_patch(hook_address, hook_bytes, "Hook at 0x90B5")
        
        return self.patcher.create_ips_file(output_ips)

def main():
    """Generate step-by-step diagnostic tests"""
    print("🔍 Step-by-Step Assembly Diagnostic")
    print("=" * 60)
    print("🎯 We know JSL/RTL works - now find what breaks!")
    print()
    
    diagnostic = StepByStepDiagnostic()
    
    try:
        # Generate all steps
        print("📄 Generating Step 1: Register operations...")
        step1_success = diagnostic.create_step1_register_save_test("super_metroid_step1_registers.ips")
        
        print("\n📄 Generating Step 2: Bank switching...")
        diagnostic = StepByStepDiagnostic()  # Reset for next test
        step2_success = diagnostic.create_step2_bank_switch_test("super_metroid_step2_bank.ips")
        
        print("\n📄 Generating Step 3: Safe memory write...")
        diagnostic = StepByStepDiagnostic()  # Reset for next test
        step3_success = diagnostic.create_step3_safe_memory_write_test("super_metroid_step3_safe_write.ips")
        
        print("\n📄 Generating Step 4: Sprite corruption...")
        diagnostic = StepByStepDiagnostic()  # Reset for next test
        step4_success = diagnostic.create_step4_sprite_corruption_test("super_metroid_step4_sprite.ips")
        
        if all([step1_success, step2_success, step3_success, step4_success]):
            print("\n🎉 ALL DIAGNOSTIC TESTS GENERATED!")
            print("\n🔍 TEST THESE IN ORDER:")
            print("   1️⃣ super_metroid_step1_registers.ips - Should work like minimal test")
            print("   2️⃣ super_metroid_step2_bank.ips - Tests bank switching") 
            print("   3️⃣ super_metroid_step3_safe_write.ips - Tests memory writes")
            print("   4️⃣ super_metroid_step4_sprite.ips - Should cause visible corruption")
            print("\n🎯 WHEN TO STOP:")
            print("   ❌ First test that crashes = we found the problem!")
            print("   ✅ If step 4 works but shows corruption = memory writes work!")
            return 0
        else:
            print("\n❌ Failed to generate some tests")
            return 1
            
    except Exception as e:
        print(f"\n❌ Error: {e}")
        return 1

if __name__ == "__main__":
    exit(main())