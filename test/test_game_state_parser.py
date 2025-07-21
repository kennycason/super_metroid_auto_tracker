#!/usr/bin/env python3
"""
Comprehensive unit tests for Super Metroid game state parsing
Tests all boss detection logic, false positive fixes, and parsing functionality
Updated 2024 - Tests current game_state_parser.py implementation
"""

import unittest
import sys
import os

# Add parent directory to path to import game_state_parser
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from game_state_parser import SuperMetroidGameStateParser


class TestGameStateParser(unittest.TestCase):
    
    def setUp(self):
        """Set up test fixtures with parser instance and test data"""
        self.parser = SuperMetroidGameStateParser()
        
        # Real memory patterns from user sessions (validated working cases)
        self.working_memory_data = {
            'basic_stats': b'\x23\x00\xE7\x03\x00\x00\x87\x00\x00\x00\x1E\x00\x00\x00\x14\x00\x00\x00\xC8\x00\xC8\x00',
            'items': b'\x05\x31',  # 0x3105 - confirmed working items
            'beams': b'\x06\x10',  # 0x1006 - confirmed working beams  
            'room_id': b'\x68\xDD',  # Room ID
            'area_id': b'\x05',      # Area ID
            'game_state': b'\x0F',   # Game state
            'player_x': b'\xCD\x00', # Player X
            'player_y': b'\xC3\x00', # Player Y
            'main_bosses': b'\x04\x03',  # 0x0304 - confirmed boss data
            'crocomire': b'\x03\x02',    # 0x0203 - confirmed crocomire
            'boss_plus_1': b'\x03\x02',  # 0x0203 - boss scan data
            'boss_plus_2': b'\x00\x00',  # 0x0000
            'boss_plus_3': b'\x01\x03',  # 0x0301 - confirmed Draygon pattern
            'boss_plus_4': b'\x03\x00',  # 0x0003
        }
    
    def test_basic_stats_parsing(self):
        """Test basic stats parsing (health, missiles, etc.)"""
        print("\n🔍 Testing basic stats parsing...")
        
        stats_data = b'\x23\x00\xE7\x03\x00\x00\x87\x00\x00\x00\x1E\x00\x00\x00\x14\x00\x00\x00\xC8\x00\xC8\x00'
        result = self.parser.parse_basic_stats(stats_data)
        
        expected = {
            'health': 35,
            'max_health': 999,
            'missiles': 0, 
            'max_missiles': 135,
            'supers': 0,
            'max_supers': 30,
            'power_bombs': 0,
            'max_power_bombs': 20,
        }
        
        for key, expected_val in expected.items():
            self.assertEqual(result[key], expected_val, 
                f"Basic stats {key}: expected {expected_val}, got {result.get(key)}")
            print(f"  ✅ {key}: {result[key]}")
    
    def test_items_parsing(self):
        """Test items parsing with confirmed working patterns"""
        print("\n🎒 Testing items parsing...")
        
        items_data = b'\x05\x31'  # 0x3105
        result = self.parser.parse_items(items_data)
        
        # Expected based on 0x3105 bit analysis
        expected = {
            'varia': True,      # Bit 0 set
            'spring': True,     # Bit 1 set  
            'morph': True,      # Bit 2 set
            'screw': False,     # Bit 3 not set
            'gravity': False,   # Bit 5 not set
            'hijump': True,     # Bit 8 set
            'speed': True,      # Bit 13 set
            'bombs': True,      # Bit 12 set
            'space': False,     # Bit 9 not set
            'grapple': False,   # Bit 14 not set
            'xray': False,      # Bit 15 not set
        }
        
        for item, expected_val in expected.items():
            self.assertEqual(result[item], expected_val,
                f"Item {item}: expected {expected_val}, got {result.get(item)}")
            status = "✅" if result[item] == expected_val else "❌"
            print(f"  {status} {item}: {result[item]}")
    
    def test_beams_parsing(self):
        """Test beams parsing with confirmed working patterns"""
        print("\n⚡ Testing beams parsing...")
        
        beams_data = b'\x06\x10'  # 0x1006
        result = self.parser.parse_beams(beams_data)
        
        # Expected based on 0x1006 bit analysis
        expected = {
            'wave': False,      # Bit 0 not set
            'ice': True,        # Bit 1 set
            'spazer': True,     # Bit 2 set  
            'plasma': False,    # Bit 3 not set
            'charge': True,     # Bit 12 set
        }
        
        for beam, expected_val in expected.items():
            self.assertEqual(result[beam], expected_val,
                f"Beam {beam}: expected {expected_val}, got {result.get(beam)}")
            status = "✅" if result[beam] == expected_val else "❌"
            print(f"  {status} {beam}: {result[beam]}")
    
    def test_boss_detection_working_cases(self):
        """Test boss detection for confirmed working cases"""
        print("\n👹 Testing boss detection - working cases...")
        
        result = self.parser.parse_bosses(self.working_memory_data)
        
        # Expected based on confirmed memory patterns
        expected_working = {
            'bomb_torizo': True,   # Bit 2 set in 0x0304
            'kraid': True,         # Bit 8 set in 0x0304
            'spore_spawn': True,   # Bit 9 set in 0x0304
            'crocomire': True,     # 0x0203 & 0x02 
            'draygon': True,       # 0x0301 pattern detected
            'mother_brain': False, # Bit 0 not set in 0x0304
        }
        
        for boss, expected_val in expected_working.items():
            self.assertEqual(result[boss], expected_val,
                f"Boss {boss}: expected {expected_val}, got {result.get(boss)}")
            status = "✅" if result[boss] == expected_val else "❌"
            print(f"  {status} {boss}: {result[boss]}")
    
    def test_false_positive_prevention(self):
        """Test that false positives are prevented with our fixes"""
        print("\n🚫 Testing false positive prevention...")
        
        # Test data that previously caused false positives
        false_positive_data = {
            'main_bosses': b'\x04\x03',      # 0x0304 - should not trigger MB
            'crocomire': b'\x03\x02',        # 0x0203 - should not trigger Ridley/GT
            'boss_plus_1': b'\x03\x02',      # 0x0203 - should not trigger Ridley/GT  
            'boss_plus_2': b'\x00\x00',      # 0x0000
            'boss_plus_3': b'\x01\x03',      # 0x0301 - should trigger Draygon only
            'boss_plus_4': b'\x03\x00',      # 0x0003
        }
        
        result = self.parser.parse_bosses(false_positive_data)
        
        # These should be FALSE to prevent false positives
        expected_false = {
            'ridley': False,         # Fixed: 0x0203 should not trigger Ridley
            'golden_torizo': False,  # Fixed: 0x0203 should not trigger GT
            'mother_brain': False,   # Fixed: bit 0 not set in 0x0304
        }
        
        # These should still work correctly
        expected_true = {
            'bomb_torizo': True,     # Bit 2 set in 0x0304
            'kraid': True,           # Bit 8 set in 0x0304  
            'spore_spawn': True,     # Bit 9 set in 0x0304
            'crocomire': True,       # 0x0203 & 0x02
            'draygon': True,         # 0x0301 pattern
        }
        
        print("  False positive prevention:")
        for boss, expected_val in expected_false.items():
            self.assertEqual(result[boss], expected_val,
                f"FALSE POSITIVE: {boss} should be {expected_val}, got {result.get(boss)}")
            status = "✅" if result[boss] == expected_val else "❌"
            print(f"    {status} {boss}: {result[boss]} (prevented false positive)")
        
        print("  Legitimate detections still working:")
        for boss, expected_val in expected_true.items():
            self.assertEqual(result[boss], expected_val,
                f"LEGITIMATE: {boss} should be {expected_val}, got {result.get(boss)}")
            status = "✅" if result[boss] == expected_val else "❌"
            print(f"    {status} {boss}: {result[boss]}")
    
    def test_mother_brain_intermediate_states(self):
        """Test Mother Brain intermediate state detection during fight"""
        print("\n🧠 Testing Mother Brain intermediate states...")
        
        # Test scenario 1: Fighting MB2 in Mother Brain room
        mb_fighting_data = {
            'main_bosses': b'\x04\x03',      # 0x0304 - main MB bit not set
            'boss_plus_1': b'\x03\x06',      # 0x0603 - indicates MB progress
            'crocomire': b'\x03\x02',
            'boss_plus_2': b'\x00\x00',
            'boss_plus_3': b'\x01\x03', 
            'boss_plus_4': b'\x03\x00',
        }
        
        # Location data for Mother Brain room
        mb_room_location = {
            'area_id': 5,      # Tourian
            'room_id': 56664,  # Mother Brain room
            'missiles': 0,     # Typical during MB fight
        }
        
        result = self.parser.parse_bosses(mb_fighting_data, mb_room_location)
        
        expected_mb_fighting = {
            'mother_brain': False,    # Complete sequence not done
            'mother_brain_1': True,   # MB1 defeated (can't reach MB2 without it)
            'mother_brain_2': False,  # MB2 not defeated yet (still fighting)
        }
        
        for mb_phase, expected_val in expected_mb_fighting.items():
            self.assertEqual(result[mb_phase], expected_val,
                f"MB fighting state {mb_phase}: expected {expected_val}, got {result.get(mb_phase)}")
            status = "✅" if result[mb_phase] == expected_val else "❌"
            print(f"  {status} {mb_phase}: {result[mb_phase]} (fighting MB2)")
        
        # Test scenario 2: Complete Mother Brain sequence done
        mb_complete_data = {
            'main_bosses': b'\x05\x03',      # 0x0305 - main MB bit set (bit 0)
            'boss_plus_1': b'\x03\x08',      # Higher pattern
            'crocomire': b'\x03\x02',
            'boss_plus_2': b'\x00\x00',
            'boss_plus_3': b'\x01\x03',
            'boss_plus_4': b'\x03\x00',
        }
        
        result_complete = self.parser.parse_bosses(mb_complete_data, None)
        
        expected_mb_complete = {
            'mother_brain': True,     # Complete sequence done
            'mother_brain_1': True,   # MB1 completed
            'mother_brain_2': True,   # MB2 completed  
        }
        
        print("  Complete sequence:")
        for mb_phase, expected_val in expected_mb_complete.items():
            self.assertEqual(result_complete[mb_phase], expected_val,
                f"MB complete state {mb_phase}: expected {expected_val}, got {result_complete.get(mb_phase)}")
            status = "✅" if result_complete[mb_phase] == expected_val else "❌"
            print(f"  {status} {mb_phase}: {result_complete[mb_phase]} (sequence complete)")
    
    def test_ridley_detection_thresholds(self):
        """Test Ridley detection with proper thresholds to prevent false positives"""
        print("\n🐉 Testing Ridley detection thresholds...")
        
        # Test cases that should NOT trigger Ridley (false positive prevention)
        ridley_false_cases = [
            (b'\x03\x02', b'\x00\x00', "Current false positive pattern"),
            (b'\x00\x02', b'\x00\x00', "Low value with bit 1"),  
            (b'\x00\x01', b'\x00\x00', "Low value with bit 0"),
        ]
        
        print("  False positive prevention:")
        for boss_plus_2, boss_plus_4, description in ridley_false_cases:
            test_data = {
                'main_bosses': b'\x04\x03',
                'boss_plus_1': b'\x03\x02',
                'boss_plus_2': boss_plus_2,
                'boss_plus_3': b'\x01\x03',
                'boss_plus_4': boss_plus_4,
                'crocomire': b'\x03\x02',
            }
            
            result = self.parser.parse_bosses(test_data)
            self.assertFalse(result['ridley'], 
                f"Ridley should NOT be detected for {description}")
            print(f"    ✅ {description}: {result['ridley']} (prevented)")
        
        # Test cases that SHOULD trigger Ridley (legitimate detection)
        ridley_valid_cases = [
            (b'\x01\x00', b'\x01\x00', "Valid Ridley defeat pattern"),
            (b'\x03\x00', b'\x01\x00', "Higher pattern with bit 0"),
        ]
        
        print("  Legitimate detection:")
        for boss_plus_2, boss_plus_4, description in ridley_valid_cases:
            test_data = {
                'main_bosses': b'\x04\x03',
                'boss_plus_1': b'\x03\x02',
                'boss_plus_2': boss_plus_2,
                'boss_plus_3': b'\x01\x03',
                'boss_plus_4': boss_plus_4,
                'crocomire': b'\x03\x02',
            }
            
            result = self.parser.parse_bosses(test_data)
            self.assertTrue(result['ridley'],
                f"Ridley SHOULD be detected for {description}")
            print(f"    ✅ {description}: {result['ridley']} (detected)")
    
    def test_golden_torizo_detection_thresholds(self):
        """Test Golden Torizo detection with proper thresholds"""
        print("\n🥇 Testing Golden Torizo detection thresholds...")
        
        # Test cases that should NOT trigger Golden Torizo
        gt_false_cases = [
            (b'\x03\x02', "Current false positive pattern (below 0x0603)"),
            (b'\x00\x02', "Low value with some bits"),
            (b'\x02\x06', "Has 0x0603 bits but wrong pattern"),
        ]
        
        print("  False positive prevention:")  
        for boss_plus_1, description in gt_false_cases:
            test_data = {
                'main_bosses': b'\x04\x03',
                'boss_plus_1': boss_plus_1,
                'boss_plus_2': b'\x00\x00',
                'boss_plus_3': b'\x01\x03',
                'boss_plus_4': b'\x03\x00',
                'crocomire': b'\x03\x02',
            }
            
            result = self.parser.parse_bosses(test_data)
            self.assertFalse(result['golden_torizo'],
                f"Golden Torizo should NOT be detected for {description}")
            print(f"    ✅ {description}: {result['golden_torizo']} (prevented)")
        
        # Test cases that SHOULD trigger Golden Torizo
        gt_valid_cases = [
            (b'\x03\x06', b'\x00\x00', "Valid GT pattern (>=0x0603)"),
            (b'\x03\x07', b'\x00\x00', "Higher GT pattern"), 
            (b'\x00\x00', b'\x00\x05', "Alternative detection via boss_plus_2"),
        ]
        
        print("  Legitimate detection:")
        for boss_plus_1, boss_plus_2, description in gt_valid_cases:
            test_data = {
                'main_bosses': b'\x04\x03',
                'boss_plus_1': boss_plus_1,
                'boss_plus_2': boss_plus_2,
                'boss_plus_3': b'\x01\x03',
                'boss_plus_4': b'\x03\x00',
                'crocomire': b'\x03\x02',
            }
            
            result = self.parser.parse_bosses(test_data)
            self.assertTrue(result['golden_torizo'],
                f"Golden Torizo SHOULD be detected for {description}")
            print(f"    ✅ {description}: {result['golden_torizo']} (detected)")
    
    def test_complete_game_state_parsing(self):
        """Test complete game state parsing integration"""
        print("\n🎮 Testing complete game state parsing...")
        
        result = self.parser.parse_complete_game_state(self.working_memory_data)
        
        # Verify all major components are present
        required_keys = ['health', 'max_health', 'items', 'beams', 'bosses', 
                        'area_name', 'room_id', 'area_id']
        
        for key in required_keys:
            self.assertIn(key, result, f"Missing key in complete game state: {key}")
            print(f"  ✅ {key}: present")
        
        # Verify data types and basic sanity checks
        self.assertIsInstance(result['health'], int)
        self.assertIsInstance(result['items'], dict)
        self.assertIsInstance(result['beams'], dict)
        self.assertIsInstance(result['bosses'], dict)
        self.assertGreaterEqual(result['health'], 0)
        self.assertGreaterEqual(result['max_health'], result['health'])
        
        print(f"  ✅ Complete game state parsed successfully")
        print(f"    - Health: {result['health']}/{result['max_health']}")
        print(f"    - Area: {result.get('area_name', 'Unknown')}")
        print(f"    - Bosses defeated: {sum(1 for v in result['bosses'].values() if v == True)}")
    
    def test_edge_cases_and_error_handling(self):
        """Test edge cases and error handling"""
        print("\n⚠️ Testing edge cases and error handling...")
        
        # Test with None/empty data
        result_empty = self.parser.parse_bosses({})
        self.assertIsInstance(result_empty, dict)
        print("  ✅ Empty data handled gracefully")
        
        # Test with malformed data
        malformed_data = {
            'main_bosses': b'\x04',  # Too short
            'items': b'',            # Empty
        }
        
        result_malformed = self.parser.parse_complete_game_state(malformed_data)
        self.assertIsInstance(result_malformed, dict)
        print("  ✅ Malformed data handled gracefully")
        
        # Test game state validation
        valid_state = {'health': 100, 'max_health': 999}
        invalid_state = {'health': -1}
        
        self.assertTrue(self.parser.is_valid_game_state(valid_state))
        self.assertFalse(self.parser.is_valid_game_state(invalid_state))
        print("  ✅ Game state validation working")


def run_tests():
    """Run all tests with detailed output"""
    print("🎯 SUPER METROID GAME STATE PARSER TESTS")
    print("=" * 60)
    print("Comprehensive test suite for all parsing logic")
    print("Includes false positive prevention and edge cases")
    print("=" * 60)
    
    # Create test suite
    suite = unittest.TestLoader().loadTestsFromTestCase(TestGameStateParser)
    
    # Run tests with detailed output
    runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
    result = runner.run(suite)
    
    # Summary
    print("\n" + "=" * 60)
    print("🏆 TEST SUMMARY")
    print("=" * 60)
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    
    if result.failures:
        print("\n❌ FAILURES:")
        for test, traceback in result.failures:
            print(f"  - {test}: {traceback}")
    
    if result.errors:
        print("\n💥 ERRORS:")
        for test, traceback in result.errors:
            print(f"  - {test}: {traceback}")
    
    success_rate = ((result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun) * 100
    print(f"\n📊 Success Rate: {success_rate:.1f}%")
    
    if success_rate >= 95:
        print("🎉 EXCELLENT! All core functionality verified.")
    elif success_rate >= 85:
        print("✅ GOOD! Most functionality working correctly.")
    else:
        print("⚠️ NEEDS ATTENTION! Multiple issues detected.")
    
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1) 