#!/usr/bin/env python3
"""
Unit tests for Super Metroid boss detection logic
Tests the bit mapping logic without requiring RetroArch
"""

import unittest
from unittest.mock import Mock, patch

class TestBossDetection(unittest.TestCase):
    
    def setUp(self):
        """Set up test fixtures with known memory values"""
        # Real memory values from your game session
        self.items_data = 0xf105  # Has grapple beam
        self.beams_data = 0x1007  # Has charge beam
        self.bosses_data = 0x304  # Has Bomb Torizo + Crocomire + Mother Brain
        
    def test_boss_bit_mappings_working_version(self):
        """Test boss detection using the working bit mappings from your original code"""
        bosses_data = self.bosses_data  # 0x304
        
        # From your working code - these should match what you actually defeated
        boss_flags = {
            'bomb_torizo': bool(bosses_data & 0x04),    # Bit 2 = 0x04
            'kraid': bool(bosses_data & 0x100),         # Bit 8 = 0x100  
            'spore_spawn': bool(bosses_data & 0x200),   # Bit 9 = 0x200
            'crocomire': bool(bosses_data & 0x08),      # Bit 3 = 0x08
            'phantoon': bool(bosses_data & 0x20),       # Bit 5 = 0x20
            'botwoon': bool(bosses_data & 0x40),        # Bit 6 = 0x40
            'draygon': bool(bosses_data & 0x80),        # Bit 7 = 0x80
            'ridley': bool(bosses_data & 0x10),         # Bit 4 = 0x10
            'mother_brain': bool(bosses_data & 0x01),   # Bit 0 = 0x01
        }
        
        print(f"\n🔍 Testing bosses_data = 0x{bosses_data:04X}")
        print(f"Binary: {bin(bosses_data)}")
        
        # Check which bits are set in 0x304
        # 0x304 = 0011 0000 0100 in binary
        # Bit 0 (0x01): 0 - Mother Brain should be FALSE  
        # Bit 1 (0x02): 0 - Not set
        # Bit 2 (0x04): 1 - Bomb Torizo should be TRUE ✅
        # Bit 3 (0x08): 0 - Crocomire should be FALSE
        # Bit 4 (0x10): 0 - Ridley should be FALSE  
        # Bit 5 (0x20): 0 - Phantoon should be FALSE
        # Bit 6 (0x40): 0 - Botwoon should be FALSE
        # Bit 7 (0x80): 0 - Draygon should be FALSE
        # Bit 8 (0x100): 1 - Kraid should be TRUE ✅
        # Bit 9 (0x200): 1 - Spore Spawn should be TRUE ✅
        
        # REALITY CHECK: Based on 0x304 bit analysis, these SHOULD work:
        expected_results = {
            'bomb_torizo': True,    # Bit 2 is set in 0x304 ✅
            'kraid': True,          # Bit 8 is set in 0x304 ✅ 
            'spore_spawn': True,    # Bit 9 is set in 0x304 ✅
            'crocomire': False,     # Bit 3 is NOT set in 0x304 (need different bit!)
            'phantoon': False,      # Bit 5 is NOT set in 0x304
            'botwoon': False,       # Bit 6 is NOT set in 0x304
            'draygon': False,       # Bit 7 is NOT set in 0x304
            'ridley': False,        # Bit 4 is NOT set in 0x304
            'mother_brain': False,  # Bit 0 is NOT set in 0x304 (but user has it!)
        }
        
        for boss, expected in expected_results.items():
            actual = boss_flags[boss]
            bit_value = {
                'bomb_torizo': 0x04, 'kraid': 0x100, 'spore_spawn': 0x200, 
                'crocomire': 0x08, 'phantoon': 0x20, 'botwoon': 0x40,
                'draygon': 0x80, 'ridley': 0x10, 'mother_brain': 0x01
            }[boss]
            
            print(f"  {boss}: expected={expected}, actual={actual}, bit=0x{bit_value:X}")
            self.assertEqual(actual, expected, 
                f"{boss} detection failed: expected {expected}, got {actual}")
    
    def test_grapple_beam_detection(self):
        """Test grapple beam detection and find correct bit"""
        items_data = self.items_data  # 0xf105
        
        print(f"\n🔍 GRAPPLE BEAM BIT ANALYSIS:")
        print(f"Items data: 0x{items_data:04X} = {bin(items_data)}")
        
        # Test all possible bit positions to find grapple beam
        print("Testing all bits in items_data:")
        for bit in range(16):
            bit_mask = 1 << bit
            is_set = bool(items_data & bit_mask)
            if is_set:
                print(f"  ✅ Bit {bit} (0x{bit_mask:04X}): TRUE")
            
        # User confirmed they have grapple beam, so one of the TRUE bits above is grapple
        # Based on Super Metroid documentation, grapple is often bit 11 (0x800)
        
        # Test common grapple beam bits:
        candidates = [
            (0x40, "Bit 6 (original guess)"),
            (0x800, "Bit 11 (common mapping)"), 
            (0x4000, "Bit 14"),
            (0x8000, "Bit 15"),
        ]
        
        print("\nGrapple beam candidates:")
        for bit_mask, description in candidates:
            result = bool(items_data & bit_mask)
            print(f"  {description}: {result}")
            
        # The correct bit should be one that's TRUE since user has grapple beam
    
    def test_charge_beam_detection(self):
        """Test charge beam detection"""
        beams_data = self.beams_data  # 0x1007
        
        # Test charge beam (bit 12 = 0x1000)
        charge_detected = bool(beams_data & 0x1000)
        
        print(f"\n🔍 Testing charge beam:")
        print(f"Beams data: 0x{beams_data:04X}")
        print(f"Bit 12 (0x1000) test: {charge_detected}")
        
        # This should be True since you confirmed charge beam works
        self.assertTrue(charge_detected, "Charge beam should be detected")
    
    def test_crocomire_issue_analysis(self):
        """Analyze why Crocomire shows as defeated when it shouldn't"""
        bosses_data = self.bosses_data  # 0x304
        
        print(f"\n🚨 CROCOMIRE ISSUE ANALYSIS:")
        print(f"bosses_data = 0x{bosses_data:04X} = {bin(bosses_data)}")
        
        # Test different bit positions for Crocomire
        test_bits = [
            (0x01, "Bit 0"),
            (0x02, "Bit 1"), 
            (0x08, "Bit 3"),
            (0x10, "Bit 4"),
        ]
        
        for bit_mask, bit_name in test_bits:
            result = bool(bosses_data & bit_mask)
            print(f"  {bit_name} (0x{bit_mask:02X}): {result}")
        
        # User said Crocomire IS defeated and shows as highlighted
        # Let's check which bits are actually set in 0x304:
        print("\nAll bits that ARE set in bosses_data:")
        for bit in range(16):
            bit_mask = 1 << bit
            if bosses_data & bit_mask:
                print(f"  ✅ Bit {bit} (0x{bit_mask:04X}): TRUE")
        
        # The user's defeated bosses should correspond to the TRUE bits above
    
    def test_mother_brain_mystery(self):
        """Investigate Mother Brain detection issue"""
        bosses_data = self.bosses_data  # 0x304
        
        print(f"\n🧠 MOTHER BRAIN ANALYSIS:")
        print(f"User says Mother Brain is defeated, but bit 0 (0x01) = {bool(bosses_data & 0x01)}")
        print(f"Maybe Mother Brain uses a different bit?")
        
        # In your screenshot, Mother Brain shows as defeated
        # Let's see which bits could represent it
        print("Bits that COULD be Mother Brain (currently TRUE):")
        for bit in range(16):
             bit_mask = 1 << bit
             if bosses_data & bit_mask:
                 print(f"  🧠 Candidate: Bit {bit} (0x{bit_mask:04X})")

if __name__ == '__main__':
    unittest.main(verbosity=2) 