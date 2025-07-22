#!/usr/bin/env python3
"""
Super Metroid Game State Parser

Extracts game state parsing logic into a testable class.
Takes raw memory data and converts it to structured game state.
"""

import struct
import logging
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class SuperMetroidGameStateParser:
    """Parses raw Super Metroid memory data into structured game state"""
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        # Persistent state for Mother Brain phases - once detected, stays detected
        self.mother_brain_phase_state = {
            'mb1_detected': False,
            'mb2_detected': False
        }
        # Super Metroid memory layout
        self.memory_map = {
            'health': 0x7E09C2,
            'max_health': 0x7E09C4,
            'missiles': 0x7E09C6,
            'max_missiles': 0x7E09C8,
            'supers': 0x7E09CA,
            'max_supers': 0x7E09CC,
            'power_bombs': 0x7E09CE,
            'max_power_bombs': 0x7E09D0,
            'reserve_energy': 0x7E09D6,
            'max_reserve_energy': 0x7E09D4,
            'room_id': 0x7E079B,
            'area_id': 0x7E079F,
            'game_state': 0x7E0998,
            'player_x': 0x7E0AF6,
            'player_y': 0x7E0AFA,
            'items': 0x7E09A4,
            'beams': 0x7E09A8,
            'bosses': 0x7ED828,
        }
        
        # Area names mapping
        self.areas = {
            0: "Crateria", 1: "Brinstar", 2: "Norfair", 
            3: "Wrecked Ship", 4: "Maridia", 5: "Tourian"
        }
    
    def parse_basic_stats(self, stats_data: bytes) -> Dict[str, Any]:
        """Parse basic stats from 22-byte health block"""
        if not stats_data or len(stats_data) < 22:
            return {}
            
        return {
            'health': struct.unpack('<H', stats_data[0:2])[0],
            'max_health': struct.unpack('<H', stats_data[2:4])[0],
            'missiles': struct.unpack('<H', stats_data[4:6])[0],
            'max_missiles': struct.unpack('<H', stats_data[6:8])[0],
            'supers': struct.unpack('<H', stats_data[8:10])[0],
            'max_supers': struct.unpack('<H', stats_data[10:12])[0],
            'power_bombs': struct.unpack('<H', stats_data[12:14])[0],
            'max_power_bombs': struct.unpack('<H', stats_data[14:16])[0],
            'max_reserve_energy': struct.unpack('<H', stats_data[18:20])[0],
            'reserve_energy': struct.unpack('<H', stats_data[20:22])[0],
        }
    
    def parse_location_data(self, room_id_data: bytes, area_id_data: bytes, 
                           game_state_data: bytes, player_x_data: bytes, 
                           player_y_data: bytes) -> Dict[str, Any]:
        """Parse location and position data"""
        location = {}
        
        if room_id_data and len(room_id_data) >= 2:
            location['room_id'] = struct.unpack('<H', room_id_data)[0]
        else:
            location['room_id'] = 0
            
        if area_id_data and len(area_id_data) >= 1:
            area_id = area_id_data[0]
            location['area_id'] = area_id
            location['area_name'] = self.areas.get(area_id, "Unknown")
        else:
            location['area_id'] = 0
            location['area_name'] = "Unknown"
            
        if game_state_data and len(game_state_data) >= 2:
            location['game_state'] = struct.unpack('<H', game_state_data)[0]
        else:
            location['game_state'] = 0
            
        if player_x_data and len(player_x_data) >= 2:
            location['player_x'] = struct.unpack('<H', player_x_data)[0]
        else:
            location['player_x'] = 0
            
        if player_y_data and len(player_y_data) >= 2:
            location['player_y'] = struct.unpack('<H', player_y_data)[0]
        else:
            location['player_y'] = 0
            
        return location
    
    def parse_items(self, items_data: bytes) -> Dict[str, bool]:
        """Parse item collection status"""
        if not items_data or len(items_data) < 2:
            return {}
            
        items_value = struct.unpack('<H', items_data)[0]
        return {
            "morph": bool(items_value & 0x0004),
            "bombs": bool(items_value & 0x1000),
            "varia": bool(items_value & 0x0001),
            "gravity": bool(items_value & 0x0020),
            "hijump": bool(items_value & 0x0100),
            "speed": bool(items_value & 0x2000),
            "space": bool(items_value & 0x0200),
            "screw": bool(items_value & 0x0008),
            "spring": bool(items_value & 0x0002),
            "xray": bool(items_value & 0x8000),
            "grapple": bool(items_value & 0x4000)
        }
    
    def parse_beams(self, beams_data: bytes) -> Dict[str, bool]:
        """Parse beam weapon status"""
        if not beams_data or len(beams_data) < 2:
            return {}
            
        beams_value = struct.unpack('<H', beams_data)[0]
        return {
            "charge": bool(beams_value & 0x1000),
            "ice": bool(beams_value & 0x0002),
            "wave": bool(beams_value & 0x0001),
            "spazer": bool(beams_value & 0x0004),
            "plasma": bool(beams_value & 0x0008)
        }
    
    def parse_bosses(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any] = None) -> Dict[str, Any]:
        """Parse boss defeat status with advanced detection logic"""
        if not boss_memory_data:
            return {}
            
        bosses = {}
        
        # Basic boss flags
        main_bosses_data = boss_memory_data.get('main_bosses')
        if main_bosses_data and len(main_bosses_data) >= 2:
            bosses_value = struct.unpack('<H', main_bosses_data)[0]
            bosses.update({
                'bomb_torizo': bool(bosses_value & 0x04),
                'kraid': bool(bosses_value & 0x100),
                'spore_spawn': bool(bosses_value & 0x200),
                'draygon': bool(bosses_value & 0x1000),
                'mother_brain': bool(bosses_value & 0x01)
            })
        
        # Advanced boss detection
        crocomire_data = boss_memory_data.get('crocomire')
        if crocomire_data and len(crocomire_data) >= 2:
            crocomire_value = struct.unpack('<H', crocomire_data)[0]
            bosses['crocomire'] = bool(crocomire_value & 0x02) and (crocomire_value >= 0x0202)
        else:
            bosses['crocomire'] = False
            
        # Multi-address boss scans for complex bosses
        boss_scan_results = {}
        for key, data in boss_memory_data.items():
            if key.startswith('boss_plus_') and data and len(data) >= 2:
                boss_scan_results[key] = struct.unpack('<H', data)[0]
        
        # Fixed boss detection logic (copied from working unified server)
        phantoon_addr = boss_scan_results.get('boss_plus_3', 0)
        phantoon_detected = phantoon_addr and (phantoon_addr & 0x01)  # Fixed: removed 0x0300 requirement
        bosses['phantoon'] = phantoon_detected
        
        # Botwoon detection
        botwoon_addr_1 = boss_scan_results.get('boss_plus_2', 0)
        botwoon_addr_2 = boss_scan_results.get('boss_plus_4', 0)
        botwoon_detected = ((botwoon_addr_1 & 0x04) and (botwoon_addr_1 > 0x0100)) or \
                          ((botwoon_addr_2 & 0x02) and (botwoon_addr_2 > 0x0001))
        bosses['botwoon'] = botwoon_detected
        
        # Draygon detection - Fixed to detect the 0x0301 pattern
        boss_plus_3_val = boss_scan_results.get('boss_plus_3', 0)
        if boss_plus_3_val == 0x0301:  # Specific Draygon defeat pattern
            draygon_detected = True
        else:
            # Fallback: original logic for other patterns
            draygon_detected = False
            for val in boss_scan_results.values():
                if val and (val & 0x04):
                    draygon_detected = True
                    break
        bosses['draygon'] = draygon_detected
        
        # Ridley detection - Fixed to check correct memory addresses
        ridley_addr_2 = boss_scan_results.get('boss_plus_2', 0)
        ridley_addr_4 = boss_scan_results.get('boss_plus_4', 0)
        ridley_detected = ((ridley_addr_2 & 0x0001) != 0) or ((ridley_addr_4 & 0x0001) != 0)
        bosses['ridley'] = ridley_detected
        
        # Golden Torizo detection - Fixed threshold to detect 0x0603 pattern
        gt_addr_1 = boss_scan_results.get('boss_plus_1', 0)
        gt_addr_2 = boss_scan_results.get('boss_plus_2', 0)
        condition1 = ((gt_addr_1 & 0x0700) and (gt_addr_1 & 0x0003) and (gt_addr_1 >= 0x0603))  # Lowered from 0x0703
        condition2 = (gt_addr_2 & 0x0100) and (gt_addr_2 >= 0x0500)
        # Removed condition3 that was triggering on Draygon's 0x0301 pattern
        golden_torizo_detected = condition1 or condition2
        bosses['golden_torizo'] = golden_torizo_detected
        
        # Mother Brain detection - Handle both complete sequence AND intermediate fight states
        main_mb_detected = bosses.get('mother_brain', False)  # From original detection above
        
        # Mother Brain has 3 phases: MB1 (glass tank), MB2 (mech body), MB3 (final form)
        # We need to detect intermediate states during the active fight sequence
        
        # Check if we're in the Mother Brain room during the fight
        area_id = location_data.get('area_id', 0) if location_data else 0
        room_id = location_data.get('room_id', 0) if location_data else 0
        player_x = location_data.get('player_x', 0) if location_data else 0
        player_y = location_data.get('player_y', 0) if location_data else 0
        in_mb_room = (area_id == 5 and room_id == 56664)  # Tourian Mother Brain room
        
        if main_mb_detected:
            # Complete sequence: all phases are done
            mb1_detected = True
            mb2_detected = True
        elif in_mb_room:
            # CHECK FOR SAVE STATE CONTRADICTIONS BEFORE TRUSTING MEMORY PATTERNS
            # If we're in MB room with full missiles and high health, this suggests save state load
            # In this case, don't trust persistent memory patterns that may be stale
            save_state_contradiction = (
                location_data.get('missiles', 0) == location_data.get('max_missiles', 0) and  # Full missiles
                location_data.get('missiles', 0) > 120 and  # High missile count
                location_data.get('health', 0) > 500 and    # High health
                player_x < 400  # Not in fight area (pre-fight position)
            )
            
            if save_state_contradiction:
                logger.info(f"MB Debug - SAVE STATE CONTRADICTION detected: full missiles + high health in MB room")
                logger.info(f"MB Debug - Ignoring persistent memory patterns (likely stale from previous run)")
                mb1_detected = False
                mb2_detected = False
            else:
                # Normal detection logic when no contradiction
                # Multi-indicator detection system: use memory patterns + position + ammo as fallback
                mb_progress_val = boss_scan_results.get('boss_plus_1', 0)
                mb_alt_pattern = boss_scan_results.get('boss_plus_2', 0)
                mb_extra_pattern = boss_scan_results.get('boss_plus_3', 0)
                mb_pattern_4 = boss_scan_results.get('boss_plus_4', 0)
                mb_pattern_5 = boss_scan_results.get('boss_plus_5', 0)
                
                # Position-based indicators: where in MB room
                in_mb_fight_area = (player_x >= 700 and player_x <= 2000 and player_y >= 300)
                
                # Game state validation (active gameplay should be 0x000B according to ChatGPT)
                game_state_val = location_data.get('game_state', 0) if location_data else 0
                in_active_gameplay = (game_state_val == 0x000B)
                
                # Missile usage as supporting evidence (not primary)
                initial_missiles = location_data.get('max_missiles', 135) if location_data else 135
                current_missiles = location_data.get('missiles', 135) if location_data else 135
                missiles_used = initial_missiles - current_missiles
                
                # Log all memory values for debugging
                logger.info(f"MB Debug - Position: ({player_x}, {player_y}), Missiles: {current_missiles}/{initial_missiles}")
                logger.info(f"MB Debug - Game State: 0x{game_state_val:04X}, In Fight Area: {in_mb_fight_area}")
                logger.info(f"MB Debug - boss_plus_1: 0x{mb_progress_val:04X}, boss_plus_2: 0x{mb_alt_pattern:04X}")
                logger.info(f"MB Debug - boss_plus_3: 0x{mb_extra_pattern:04X}, boss_plus_4: 0x{mb_pattern_4:04X}, boss_plus_5: 0x{mb_pattern_5:04X}")
                
                # CONSERVATIVE but PRACTICAL detection - focus on memory patterns as primary indicator
                # Pattern 1: Strong memory progression pattern (tuned for actual values)
                strong_memory_pattern = (mb_progress_val >= 0x0700)  # Restored to detect 0x0703 (user's actual state)
                
                # Pattern 2: Alternative memory addresses with reasonable thresholds  
                alt_memory_pattern = (mb_alt_pattern >= 0x0300) or (mb_extra_pattern >= 0x0300)  # Restored reasonable thresholds
                
                # Pattern 3: Supporting evidence (conservative but not impossible)
                # - Meaningful missile usage OR position-based evidence
                # - Must have some indication of MB fight engagement
                meaningful_missile_usage = missiles_used > 20  # Reasonable missile usage requirement
                position_evidence = in_mb_fight_area or in_active_gameplay
                supporting_evidence = meaningful_missile_usage or position_evidence
                
                # MB1 Detection: Strong memory pattern OR (Alt memory + supporting evidence)  
                mb1_detected = strong_memory_pattern or (alt_memory_pattern and supporting_evidence)
                
                if mb1_detected:
                    logger.info(f"MB Debug - Detected MB1: strong_mem={strong_memory_pattern}, alt_mem={alt_memory_pattern}, support={supporting_evidence}")
                    
                    # MB2 detection - CONSERVATIVE but achievable 
                    # Pattern 1: All missiles used + strong memory pattern 
                    all_missiles_used = (current_missiles == 0 and initial_missiles > 0)
                    strong_mb2_memory = (mb_pattern_4 >= 0x0300) or (mb_pattern_5 >= 0x0150)  # Achievable thresholds
                    
                    # Pattern 2: Very high missile usage (85%+) + strong memory evidence
                    very_high_usage = (missiles_used >= (initial_missiles * 0.85)) and (mb_progress_val >= 0x0700)
                    
                    # Detect MB2 with strong but achievable evidence
                    if (all_missiles_used and strong_mb2_memory) or very_high_usage:
                        logger.info(f"MB Debug - Detected MB2: all_missiles={all_missiles_used}, strong_mb2_memory={strong_mb2_memory}, very_high_usage={very_high_usage}")
                        mb2_detected = True
                    else:
                        logger.info(f"MB Debug - MB1 only: insufficient MB2 evidence (pre-hyper beam)")
                        mb2_detected = False
                else:
                    logger.info(f"MB Debug - NO MB1 detection: insufficient memory evidence (mem_val=0x{mb_progress_val:04X}, alt_val=0x{mb_alt_pattern:04X})")
                    mb1_detected = False
                    mb2_detected = False
        else:
            # Not in Mother Brain room and main bit not set - no phases defeated
            mb1_detected = False
            mb2_detected = False
            
        # Cache logic: once true, always true (persistent state)
        if mb1_detected or self.mother_brain_phase_state['mb1_detected']:
            bosses['mother_brain_1'] = True
            self.mother_brain_phase_state['mb1_detected'] = True
        else:
            bosses['mother_brain_1'] = False

        if mb2_detected or self.mother_brain_phase_state['mb2_detected']:
            bosses['mother_brain_2'] = True
            self.mother_brain_phase_state['mb2_detected'] = True
        else:
            bosses['mother_brain_2'] = False
        # mother_brain stays as originally detected (complete sequence)
        
        # End-game detection (Samus reaching her ship)
        samus_ship_detected = self._detect_samus_ship(boss_memory_data, location_data, main_mb_detected, mb1_detected, mb2_detected)
        bosses['samus_ship'] = samus_ship_detected
        
        return bosses
    
    def _detect_samus_ship(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any], 
                          main_mb_complete: bool, mb1_complete: bool, mb2_complete: bool) -> bool:
        """Detect when Samus has reached her ship (end-game completion)"""
        if not location_data:
            logger.debug("Ship detection: No location data")
            return False
            
        area_id = location_data.get('area_id', 0)
        room_id = location_data.get('room_id', 0)
        player_x = location_data.get('player_x', 0)
        player_y = location_data.get('player_y', 0)
        
        # Debug current state
        logger.info(f"🚢 Ship Debug - Area: {area_id}, Room: {room_id}, Pos: ({player_x},{player_y})")
        logger.info(f"🚢 Ship Debug - MB Status: main={main_mb_complete}, MB1={mb1_complete}, MB2={mb2_complete}")
        
        # Must be in Crateria (area 0) for ship location
        if area_id != 0:
            logger.info(f"🚢 Ship Debug - Not in Crateria (area={area_id}), ship detection blocked")
            return False
            
        # Check if Mother Brain sequence is complete (all phases defeated)
        # This ensures we're in the post-game/escape sequence phase
        mother_brain_complete = main_mb_complete or (mb1_complete and mb2_complete)
        
        # More flexible completion check: MB1 completion might be enough if memory shows advanced state
        # or if we're clearly in end-game scenario (escape sequence)
        partial_mb_complete = mb1_complete  # MB1 completion indicates significant progress
        
        if not mother_brain_complete and not partial_mb_complete:
            logger.info(f"🚢 Ship Debug - No Mother Brain progress (main={main_mb_complete}, MB1={mb1_complete}, MB2={mb2_complete})")
            return False
        
        completion_type = "full" if mother_brain_complete else "partial (MB1)"
        logger.info(f"🚢 Ship Debug - MB completion: {completion_type}")
            
        # Landing Site room detection (approximate room ID conversion)
        # The SMILE ID 791F8 converts to a runtime room ID we need to check
        # Based on research, Landing Site is typically room ID around 31800-32000 range in decimal
        # But since room IDs can vary, we'll use a more flexible approach
        
        # Check if we're in the general Landing Site area (Crateria, western section)
        # Based on the room layout, Landing Site is in "West of the Lake" section
        # We'll check for reasonable room ID ranges and position within Crateria
        
        # Room ID ranges that could indicate Landing Site area (converted from hex ranges)
        # This covers the general Crateria "West of the Lake" area where the ship is located
        landing_site_room_range = [
            (31224, 31260),  # 0x791F8 area converted to decimal range
            (31000, 31500),  # Broader range to account for room ID variations
            (32000, 32500),  # Extended range for different ROM versions
        ]
        
        in_landing_area = any(start <= room_id <= end for start, end in landing_site_room_range)
        logger.info(f"🚢 Ship Debug - Room check: {room_id} in landing ranges? {in_landing_area}")
        
        # Also check by position - Landing Site has specific coordinates
        # The ship is typically in the upper portion of the Landing Site room
        # X coordinate around 400-600, Y coordinate around 100-300 (approximate)
        near_ship_position = (300 <= player_x <= 700) and (50 <= player_y <= 400)
        logger.info(f"🚢 Ship Debug - Position check: ({player_x},{player_y}) near ship? {near_ship_position}")
        
        # Alternative: check if we're in any Crateria room that could be post-escape
        # Since room IDs can be complex, also accept being in Crateria with MB complete
        # and reasonable positioning (not underground, not too far east/west)
        in_surface_crateria = (area_id == 0 and 
                              200 <= player_x <= 800 and  # Reasonable X range
                              50 <= player_y <= 500)      # Surface level Y range
        logger.info(f"🚢 Ship Debug - Surface Crateria check: {in_surface_crateria}")
        
        # End-game is detected if:
        # 1. Mother Brain progress (full OR partial) AND
        # 2. We're in Crateria AND
        # 3. Either in the Landing Site area OR near ship position OR in surface Crateria
        any_mb_progress = mother_brain_complete or partial_mb_complete
        end_game_detected = (any_mb_progress and 
                           area_id == 0 and 
                           (in_landing_area or near_ship_position or in_surface_crateria))
        
        if end_game_detected:
            logger.info(f"🚢 END-GAME DETECTED: MB progress + Crateria + position match!")
        else:
            logger.info(f"🚢 No end-game: mb_progress={any_mb_progress}, crateria={area_id==0}, position_match={(in_landing_area or near_ship_position or in_surface_crateria)}")
            
        return end_game_detected
    
    def maybe_reset_mb_state(self, location_data: Dict[str, Any], stats_data: Optional[bytes]):
        """Reset MB phase tracking on game start, save load, or contradiction detection"""
        if not location_data:
            return
            
        area_id = location_data.get('area_id', -1)
        room_id = location_data.get('room_id', -1)
        missiles = location_data.get('missiles', 0)
        max_missiles = location_data.get('max_missiles', 0)
        
        # Get health to detect new games (low health = likely new save)
        health = 0
        if stats_data and len(stats_data) >= 2:
            health = struct.unpack('<H', stats_data[0:2])[0]
        
        # Reset conditions (expanded for save state detection):
        
        # 1. New game detection: Crateria + low room ID + low health
        new_game_detected = (area_id == 0 and room_id < 32000 and health <= 99)
        
        # 2. Save state contradiction: If we have cached MB1/MB2 but are in Crateria with full missiles
        #    This indicates user loaded an earlier save before MB fight
        save_contradiction = (
            (self.mother_brain_phase_state['mb1_detected'] or self.mother_brain_phase_state['mb2_detected']) and
            area_id == 0 and  # In Crateria (starting area)
            missiles > 0 and max_missiles > 0 and
            missiles >= (max_missiles * 0.8)  # Has most/all missiles (inconsistent with MB fight)
        )
        
        # 3. Reset detection: High health + full missiles in early areas (pre-MB areas)
        early_area_reset = (
            (self.mother_brain_phase_state['mb1_detected'] or self.mother_brain_phase_state['mb2_detected']) and
            area_id in [0, 1, 2] and  # Crateria, Brinstar, Norfair (pre-Tourian)
            health > 200 and  # High health (not post-MB state)
            missiles > 100    # High missile count (not post-MB state)
        )
        
        # 4. Memory contradiction: Memory shows MB progression but missiles are full (save state load)
        #    This catches cases where game memory persists but user loaded earlier save
        memory_contradiction = (
            missiles > 0 and max_missiles > 0 and
            missiles == max_missiles and  # Full missiles (inconsistent with MB fight)
            area_id == 5 and room_id == 56664  # Still in MB room but with full missiles
        )
        
        # 5. General contradiction: Cached state exists but evidence suggests earlier save
        general_contradiction = (
            (self.mother_brain_phase_state['mb1_detected'] or self.mother_brain_phase_state['mb2_detected']) and
            missiles > 120 and  # High missile count suggests pre-fight state
            area_id in [5] and   # In Tourian but with pre-fight resources
            health > 500        # High health suggests pre-fight state
        )
        
        if new_game_detected or save_contradiction or early_area_reset or memory_contradiction or general_contradiction:
            reason = ("new game" if new_game_detected else 
                     "save state load (Crateria)" if save_contradiction else
                     "game reset (early area)" if early_area_reset else
                     "save state load (MB room)" if memory_contradiction else
                     "save state load (contradiction)")
            logger.info(f"🔄 Resetting MB state cache - detected {reason}")
            logger.info(f"   └── Area: {area_id}, Room: {room_id}, Health: {health}, Missiles: {missiles}/{max_missiles}")
            self.mother_brain_phase_state['mb1_detected'] = False
            self.mother_brain_phase_state['mb2_detected'] = False
    
    def reset_mb_cache(self):
        """Manually reset Mother Brain phase cache (for testing)"""
        self.mother_brain_phase_state['mb1_detected'] = False
        self.mother_brain_phase_state['mb2_detected'] = False
    
    def bootstrap_mb_cache(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any] = None):
        """Bootstrap MB cache by checking current state - useful after implementing persistent state"""
        logger.info("🔄 Bootstrapping MB cache from current game state...")
        
        # Temporarily disable cache to get raw detection
        old_mb1_state = self.mother_brain_phase_state['mb1_detected']
        old_mb2_state = self.mother_brain_phase_state['mb2_detected']
        
        # Reset to get clean detection
        self.mother_brain_phase_state['mb1_detected'] = False
        self.mother_brain_phase_state['mb2_detected'] = False
        
        # Parse current state without cache influence
        current_bosses = self.parse_bosses(boss_memory_data, location_data)
        
        # If MB phases are detected now, cache them
        if current_bosses.get('mother_brain_1', False):
            self.mother_brain_phase_state['mb1_detected'] = True
            logger.info("🎯 Bootstrapped MB1 state: detected and cached")
        else:
            self.mother_brain_phase_state['mb1_detected'] = old_mb1_state
            
        if current_bosses.get('mother_brain_2', False):
            self.mother_brain_phase_state['mb2_detected'] = True
            logger.info("🎯 Bootstrapped MB2 state: detected and cached")
        else:
            self.mother_brain_phase_state['mb2_detected'] = old_mb2_state
            
        logger.info(f"🔄 Bootstrap complete: MB1={self.mother_brain_phase_state['mb1_detected']}, MB2={self.mother_brain_phase_state['mb2_detected']}")
        
        return current_bosses
    
    def parse_complete_game_state(self, memory_data: Dict[str, bytes]) -> Dict[str, Any]:
        """Parse all memory data into complete game state"""
        try:
            game_state = {}
            
            # Basic stats
            stats_data = memory_data.get('basic_stats')
            if stats_data:
                game_state.update(self.parse_basic_stats(stats_data))
            
            # Location data
            location_data = self.parse_location_data(
                memory_data.get('room_id'),
                memory_data.get('area_id'), 
                memory_data.get('game_state'),
                memory_data.get('player_x'),
                memory_data.get('player_y')
            )
            game_state.update(location_data)
            
            # Optional: reset if new game or file load
            self.maybe_reset_mb_state(location_data, stats_data)
            
            # Items and beams
            game_state['items'] = self.parse_items(memory_data.get('items'))
            game_state['beams'] = self.parse_beams(memory_data.get('beams'))
            
            # Bosses (pass all boss-related memory data)
            boss_memory = {k: v for k, v in memory_data.items() 
                          if k.startswith('boss') or k == 'main_bosses' or k == 'crocomire'}
            game_state['bosses'] = self.parse_bosses(boss_memory, game_state)
            
            return game_state
            
        except Exception as e:
            logger.error(f"Error parsing game state: {e}")
            return {}
    
    def is_valid_game_state(self, game_state: Dict[str, Any]) -> bool:
        """Check if parsed game state looks valid"""
        if not game_state:
            return False
            
        # Basic sanity checks
        if 'health' not in game_state or 'max_health' not in game_state:
            return False
            
        health = game_state.get('health', 0)
        max_health = game_state.get('max_health', 0)
        
        # Health should be reasonable
        if health < 0 or max_health < 0 or health > 1999 or max_health > 1999:
            return False
            
        return True 