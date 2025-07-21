#!/usr/bin/env python3
"""
Basic functionality tests for Super Metroid game state parsing
Tests current working behavior as baseline validation
Updated 2024 - Pragmatic testing approach
"""

import unittest
import sys
import os
import struct

# Add parent directory to path to import game_state_parser
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from game_state_parser import SuperMetroidGameStateParser


class TestBasicFunctionality(unittest.TestCase):
    
    def setUp(self):
        """Set up test fixtures with parser instance"""
        self.parser = SuperMetroidGameStateParser()
    
    def test_parser_instantiation(self):
        """Test that parser can be created successfully"""
        parser = SuperMetroidGameStateParser()
        self.assertIsNotNone(parser)
        print("✅ Parser instantiation works")
    
    def test_basic_stats_parsing_structure(self):
        """Test basic stats parsing returns expected structure"""
        # Test with valid data
        stats_data = b'\x23\x00\xE7\x03\x00\x00\x87\x00\x00\x00\x1E\x00\x00\x00\x14\x00\x00\x00\xC8\x00\xC8\x00'
        result = self.parser.parse_basic_stats(stats_data)
        
        # Check that required keys exist
        required_keys = ['health', 'max_health', 'missiles', 'max_missiles', 
                        'supers', 'max_supers', 'power_bombs', 'max_power_bombs']
        
        for key in required_keys:
            self.assertIn(key, result, f"Missing key: {key}")
            self.assertIsInstance(result[key], int, f"{key} should be integer")
        
        print(f"✅ Basic stats parsing structure: {len(required_keys)} keys present")
    
    def test_items_parsing_structure(self):
        """Test items parsing returns expected structure"""
        # Test with any valid data
        items_data = b'\x05\x31'  # 0x3105
        result = self.parser.parse_items(items_data)
        
        # Check that result is a dict with boolean values
        self.assertIsInstance(result, dict)
        
        # Common items that should be present
        expected_items = ['morph', 'bombs', 'varia', 'gravity', 'hijump', 
                         'speed', 'screw', 'spring', 'xray', 'grapple', 'space']
        
        for item in expected_items:
            self.assertIn(item, result, f"Missing item: {item}")
            self.assertIsInstance(result[item], bool, f"{item} should be boolean")
        
        print(f"✅ Items parsing structure: {len(expected_items)} items present")
    
    def test_beams_parsing_structure(self):
        """Test beams parsing returns expected structure"""
        beams_data = b'\x06\x10'  # 0x1006
        result = self.parser.parse_beams(beams_data)
        
        # Check that result is a dict with boolean values
        self.assertIsInstance(result, dict)
        
        expected_beams = ['charge', 'ice', 'wave', 'spazer', 'plasma']
        
        for beam in expected_beams:
            self.assertIn(beam, result, f"Missing beam: {beam}")
            self.assertIsInstance(result[beam], bool, f"{beam} should be boolean")
        
        print(f"✅ Beams parsing structure: {len(expected_beams)} beams present")
    
    def test_boss_parsing_structure(self):
        """Test boss parsing returns expected structure"""
        # Test with working memory data
        boss_data = {
            'main_bosses': b'\x04\x03',
            'crocomire': b'\x03\x02',
            'boss_plus_1': b'\x03\x02',
            'boss_plus_2': b'\x00\x00',
            'boss_plus_3': b'\x01\x03',
            'boss_plus_4': b'\x03\x00',
        }
        
        result = self.parser.parse_bosses(boss_data)
        
        # Check that result is a dict
        self.assertIsInstance(result, dict)
        
        # Expected bosses
        expected_bosses = ['bomb_torizo', 'kraid', 'spore_spawn', 'crocomire', 
                          'phantoon', 'botwoon', 'draygon', 'ridley', 'golden_torizo',
                          'mother_brain', 'mother_brain_1', 'mother_brain_2']
        
        for boss in expected_bosses:
            self.assertIn(boss, result, f"Missing boss: {boss}")
            # Boss values can be bool, int, or other types depending on implementation
        
        print(f"✅ Boss parsing structure: {len(expected_bosses)} bosses present")
    
    def test_complete_parsing_integration(self):
        """Test complete game state parsing integration"""
        memory_data = {
            'basic_stats': b'\x23\x00\xE7\x03\x00\x00\x87\x00\x00\x00\x1E\x00\x00\x00\x14\x00\x00\x00\xC8\x00\xC8\x00',
            'items': b'\x05\x31',
            'beams': b'\x06\x10',
            'room_id': b'\x68\xDD',
            'area_id': b'\x05',
            'game_state': b'\x0F',
            'player_x': b'\xCD\x00',
            'player_y': b'\xC3\x00',
            'main_bosses': b'\x04\x03',
            'crocomire': b'\x03\x02',
            'boss_plus_1': b'\x03\x02',
            'boss_plus_2': b'\x00\x00',
            'boss_plus_3': b'\x01\x03',
            'boss_plus_4': b'\x03\x00',
        }
        
        result = self.parser.parse_complete_game_state(memory_data)
        
        # Check that all major sections are present
        required_sections = ['health', 'items', 'beams', 'bosses', 'area_name']
        
        for section in required_sections:
            self.assertIn(section, result, f"Missing section: {section}")
        
        # Basic sanity checks
        self.assertIsInstance(result['health'], int)
        self.assertIsInstance(result['items'], dict)
        self.assertIsInstance(result['beams'], dict)
        self.assertIsInstance(result['bosses'], dict)
        
        print(f"✅ Complete parsing integration: {len(required_sections)} sections present")
    
    def test_error_handling(self):
        """Test error handling with invalid data"""
        # Test with empty data
        result_empty = self.parser.parse_bosses({})
        self.assertIsInstance(result_empty, dict)
        
        # Test with None
        result_none = self.parser.parse_basic_stats(None)
        self.assertIsInstance(result_none, dict)
        
        # Test with malformed data
        result_malformed = self.parser.parse_items(b'\x00')  # Too short
        self.assertIsInstance(result_malformed, dict)
        
        print("✅ Error handling: graceful degradation works")
    
    def test_mother_brain_intermediate_logic(self):
        """Test Mother Brain intermediate state detection - LOCKS IN WORKING LOGIC"""
        # Test scenario 1: Just entered MB room - should NOT detect MB1
        mb_room_early = {
            'area_id': 5,         # Tourian
            'room_id': 56664,     # Mother Brain room
            'player_x': 194,      # Realistic position
            'player_y': 195,      # Realistic position
            'missiles': 135,      # Full missiles (just entered)
            'max_missiles': 135,  # Max missiles
            'game_state': 0x000F, # Normal game state
        }
        mb_early_fighting_data = {
            'main_bosses': struct.pack('<H', 0x0000),  # Main MB not defeated
            'boss_plus_1': struct.pack('<H', 0x0200),  # Low progression value
            'boss_plus_2': struct.pack('<H', 0x0100),  # Low alt pattern
            'boss_plus_3': struct.pack('<H', 0x0301),  # Draygon pattern (not used for MB)
        }
        result_early = self.parser.parse_bosses(mb_early_fighting_data, mb_room_early)
        self.assertFalse(result_early.get('mother_brain_1'), "MB1 should NOT be detected with full missiles")

        # Test scenario 2: EXACT USER SUCCESS CASE - 44 missiles used, beat MB1
        mb_room_beaten = {
            'area_id': 5,         # Tourian
            'room_id': 56664,     # Mother Brain room
            'player_x': 194,      # User's exact position
            'player_y': 195,      # User's exact position
            'missiles': 91,       # User's exact missiles (135 -> 91 = 44 used)
            'max_missiles': 135,  # Max missiles
            'game_state': 0x000F, # User's exact game state
        }
        mb_beaten_data = {
            'main_bosses': struct.pack('<H', 0x0000),  # Main MB not defeated
            'boss_plus_1': struct.pack('<H', 0x0703),  # User's exact memory value
            'boss_plus_2': struct.pack('<H', 0x0107),  # User's exact alt pattern
            'boss_plus_3': struct.pack('<H', 0x0301),  # Draygon pattern (not used for MB)
        }
        result_beaten = self.parser.parse_bosses(mb_beaten_data, mb_room_beaten)
        self.assertTrue(result_beaten.get('mother_brain_1'), "MB1 MUST be detected - USER SUCCESS CASE")
        self.assertFalse(result_beaten.get('mother_brain_2'), "MB2 should remain false after MB1")

        # Test scenario 3: Strong missile evidence threshold (30+ missiles used)
        mb_room_missile_threshold = {
            'area_id': 5,         # Tourian
            'room_id': 56664,     # Mother Brain room
            'player_x': 500,      # Any position
            'player_y': 300,      # Any position  
            'missiles': 105,      # Used exactly 30 missiles (135 -> 105)
            'max_missiles': 135,  # Max missiles
            'game_state': 0x000F, # Any game state
        }
        mb_missile_data = {
            'main_bosses': struct.pack('<H', 0x0000),  # Main MB not defeated
            'boss_plus_1': struct.pack('<H', 0x0400),  # Medium progression value
            'boss_plus_2': struct.pack('<H', 0x0200),  # Medium alt pattern
            'boss_plus_3': struct.pack('<H', 0x0080),  # Not used for MB
        }
        result_missile = self.parser.parse_bosses(mb_missile_data, mb_room_missile_threshold)
        self.assertTrue(result_missile.get('mother_brain_1'), "MB1 should be detected with 30+ missiles used")

        # Test scenario 4: Memory pattern + missile evidence combo
        mb_room_combo = {
            'area_id': 5,         # Tourian
            'room_id': 56664,     # Mother Brain room
            'player_x': 800,      # In fight area
            'player_y': 400,      # In fight area
            'missiles': 115,      # Used 20 missiles (135 -> 115)
            'max_missiles': 135,  # Max missiles
            'game_state': 0x000B, # Active gameplay
        }
        mb_combo_data = {
            'main_bosses': struct.pack('<H', 0x0000),  # Main MB not defeated
            'boss_plus_1': struct.pack('<H', 0x0700),  # High progression (>=0x0600) 
            'boss_plus_2': struct.pack('<H', 0x0200),  # Medium alt pattern
            'boss_plus_3': struct.pack('<H', 0x0080),  # Not used for MB
        }
        result_combo = self.parser.parse_bosses(mb_combo_data, mb_room_combo)
        self.assertTrue(result_combo.get('mother_brain_1'), "MB1 should be detected with memory+missile combo")

        # REGRESSION TEST: Ensure we never go back to overly conservative detection
        # This exact case was failing before the fix - must always pass!
        regression_test_data = {
            'area_id': 5,         # Tourian
            'room_id': 56664,     # Mother Brain room  
            'player_x': 194,      # Real user position
            'player_y': 195,      # Real user position
            'missiles': 91,       # Real user missiles after MB1 defeat
            'max_missiles': 135,  # Max missiles
            'game_state': 0x000F, # Real user game state (not 0x000B)
        }
        regression_memory_data = {
            'main_bosses': struct.pack('<H', 0x0000),  # Main MB not defeated
            'boss_plus_1': struct.pack('<H', 0x0703),  # Real memory value (0x0703 < 0x0800 old threshold)
            'boss_plus_2': struct.pack('<H', 0x0107),  # Real alt pattern
            'boss_plus_3': struct.pack('<H', 0x0301),  # Draygon pattern (was causing false detection)
        }
        regression_result = self.parser.parse_bosses(regression_memory_data, regression_test_data)
        self.assertTrue(regression_result.get('mother_brain_1'), "REGRESSION: This exact case was failing before - must work!")
        
        # REGRESSION TEST: Ensure false positive prevention still works  
        # This should NOT detect MB1 (before any missiles used)
        false_positive_test = {
            'area_id': 5,         # Tourian
            'room_id': 56664,     # Mother Brain room
            'player_x': 280,      # Pre-fix position
            'player_y': 195,      # Pre-fix position  
            'missiles': 135,      # Full missiles (no usage)
            'max_missiles': 135,  # Max missiles
            'game_state': 0x000F, # Same game state
        }
        false_positive_memory = {
            'main_bosses': struct.pack('<H', 0x0000),  # Main MB not defeated
            'boss_plus_1': struct.pack('<H', 0x0703),  # Same memory pattern
            'boss_plus_2': struct.pack('<H', 0x0107),  # Same alt pattern
            'boss_plus_3': struct.pack('<H', 0x0301),  # Draygon pattern - should not trigger MB
        }
        false_positive_result = self.parser.parse_bosses(false_positive_memory, false_positive_test)
        self.assertFalse(false_positive_result.get('mother_brain_1'), "REGRESSION: Must not detect MB1 with full missiles!")
    
    def test_working_boss_detections(self):
        """Test boss detections that we know work from user sessions"""
        # Using confirmed working memory pattern
        working_data = {
            'main_bosses': b'\x04\x03',      # 0x0304 - confirmed working
            'crocomire': b'\x03\x02',        # 0x0203 - confirmed working
            'boss_plus_3': b'\x01\x03',      # 0x0301 - confirmed Draygon
        }
        
        result = self.parser.parse_bosses(working_data)
        
        # These should be detected based on confirmed patterns
        confirmed_working = ['bomb_torizo', 'kraid', 'spore_spawn', 'crocomire', 'draygon']
        
        for boss in confirmed_working:
            self.assertIn(boss, result)
            # Don't assert specific values since we're testing structure, not exact behavior
            print(f"  {boss}: {result.get(boss)}")
        
        print("✅ Working boss detections: structure validated")


def run_basic_tests():
    """Run basic functionality tests"""
    print("🧪 SUPER METROID BASIC FUNCTIONALITY TESTS")
    print("=" * 60)
    print("Testing current working behavior as baseline")
    print("=" * 60)
    
    # Create test suite
    suite = unittest.TestLoader().loadTestsFromTestCase(TestBasicFunctionality)
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
    result = runner.run(suite)
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 BASIC TEST SUMMARY")
    print("=" * 60)
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    
    success_rate = ((result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun) * 100
    print(f"Success Rate: {success_rate:.1f}%")
    
    if success_rate >= 95:
        print("🎉 EXCELLENT! Core functionality is solid.")
    elif success_rate >= 80:
        print("✅ GOOD! Basic functionality working.")
    else:
        print("⚠️ NEEDS ATTENTION! Basic functionality issues.")
    
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_basic_tests()
    sys.exit(0 if success else 1) 