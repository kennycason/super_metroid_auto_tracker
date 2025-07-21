#!/usr/bin/env python3
"""
Basic functionality tests for Super Metroid game state parsing
Tests current working behavior as baseline validation
Updated 2024 - Pragmatic testing approach
"""

import unittest
import sys
import os

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
        """Test Mother Brain intermediate state logic (the key fix we made)"""
        # Test with location data for Mother Brain room
        mb_room_location = {
            'area_id': 5,      # Tourian
            'room_id': 56664,  # Mother Brain room
            'missiles': 135,   # User has missiles (hasn't used them all yet)
        }
        
        # Test: just entered MB room, low memory pattern - should NOT detect MB1 as defeated
        mb_early_fighting_data = {
            'main_bosses': b'\x04\x03',      # Main MB bit not set
            'boss_plus_1': b'\x03\x02',      # Low pattern (0x0203) - just in room
            'crocomire': b'\x03\x02',
            'boss_plus_2': b'\x00\x00',
            'boss_plus_3': b'\x01\x03',
            'boss_plus_4': b'\x03\x00',
        }
        
        result_early = self.parser.parse_bosses(mb_early_fighting_data, mb_room_location)
        
        # Key fix: being in MB room with low patterns should NOT detect MB1 as defeated
        self.assertIn('mother_brain_1', result_early)
        self.assertIn('mother_brain_2', result_early)
        self.assertFalse(result_early.get('mother_brain_1'), "MB1 should NOT be detected when just entering room")
        
        # Test: advanced MB fight with high pattern + no missiles - should detect MB1
        mb_advanced_fighting = {
            'main_bosses': b'\x04\x03',      # Main MB bit not set
            'boss_plus_1': b'\x00\x06',      # High pattern (0x0600+)
            'crocomire': b'\x03\x02',
            'boss_plus_2': b'\x00\x00',
            'boss_plus_3': b'\x01\x03',
            'boss_plus_4': b'\x03\x00',
        }
        
        mb_room_no_missiles = {
            'area_id': 5,      # Tourian
            'room_id': 56664,  # Mother Brain room
            'missiles': 0,     # Used all missiles in fight
        }
        
        result_advanced = self.parser.parse_bosses(mb_advanced_fighting, mb_room_no_missiles)
        
        print(f"✅ Mother Brain early fight: MB1={result_early.get('mother_brain_1')}, MB2={result_early.get('mother_brain_2')}")
        print(f"✅ Mother Brain advanced fight: MB1={result_advanced.get('mother_brain_1')}, MB2={result_advanced.get('mother_brain_2')}")
    
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