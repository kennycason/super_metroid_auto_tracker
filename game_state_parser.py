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
        
        # Always log raw beam value for debugging
        logger.info(f"🔍 Raw beam value: 0x{beams_value:04X} ({beams_value})")
        
        beams = {
            "charge": bool(beams_value & 0x1000),
            "ice": bool(beams_value & 0x0002),
            "wave": bool(beams_value & 0x0001),
            "spazer": bool(beams_value & 0x0004),
            "plasma": bool(beams_value & 0x0008)
        }
        
        # HYPER BEAM DETECTION - Test multiple bit positions
        # Hyper beam is only given after MB1 is defeated, making it a perfect indicator
        hyper_beam_bits = [0x0010, 0x0020, 0x0040, 0x0080, 0x0100, 0x0200, 0x0400, 0x0800]
        hyper_detected = False
        
        for bit in hyper_beam_bits:
            if beams_value & bit:
                hyper_detected = True
                logger.info(f"🌟 HYPER BEAM detected at bit position 0x{bit:04X} (value: 0x{beams_value:04X})")
                break
        
        # If not detected with common bits, check if any unknown bits are set
        known_bits = 0x1000 | 0x0002 | 0x0001 | 0x0004 | 0x0008  # charge, ice, wave, spazer, plasma
        unknown_bits = beams_value & ~known_bits
        if unknown_bits and not hyper_detected:
            logger.info(f"🔍 Unknown beam bits detected: 0x{unknown_bits:04X} - could be hyper beam")
            # Try treating any unknown bit as hyper beam
            hyper_detected = True
        
        beams["hyper"] = hyper_detected
        
        # Log final beam analysis
        logger.info(f"🔍 Beam analysis: charge={beams['charge']}, ice={beams['ice']}, wave={beams['wave']}, spazer={beams['spazer']}, plasma={beams['plasma']}, hyper={beams['hyper']}")
        
        return beams
    
    def parse_bosses(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any] = None) -> Dict[str, Any]:
        """Parse boss defeat status with advanced detection logic"""
        if not boss_memory_data:
            return {}
        
        # CRITICAL: Initialize ALL variables at function start to prevent scope errors
        mb_official_hp = 0
        mb1_transition = False
        mb2_transition = False
        escape_timer_active = False
        has_live_boss_data = False
        
        # Initialize all escape timer values
        escape_timer_1_val = 0
        escape_timer_2_val = 0
        escape_timer_3_val = 0
        escape_timer_4_val = 0
        escape_timer_5_val = 0
        escape_timer_6_val = 0
        
        # Initialize all boss HP values
        boss_hp_1_val = 0
        boss_hp_2_val = 0
        boss_hp_3_val = 0
        
        # Initialize location variables
        area_id = location_data.get('area_id', 0) if location_data else 0
        room_id = location_data.get('room_id', 0) if location_data else 0
        in_mb_room = (area_id == 5 and room_id == 56664)  # Tourian Mother Brain room
        
        # Initialize detection flags
        mb1_detected = False
        mb2_detected = False
        
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
        
        # Advanced Mother Brain detection using multiple reliable indicators
        
        # Define all variables used throughout MB detection to avoid scope issues
        # main_mb_detected = bosses.get('mother_brain', False)
        # mb1_detected = False
        # mb2_detected = False
        
        # OFFICIAL AUTOSPLITTER MOTHER BRAIN HP EXTRACTION
        # Extract official Mother Brain HP early for use throughout detection
        if boss_memory_data.get('mother_brain_official_hp') and len(boss_memory_data['mother_brain_official_hp']) >= 2:
            mb_official_hp = struct.unpack('<H', boss_memory_data['mother_brain_official_hp'])[0]
            
        # Get previous HP from cache
        if not hasattr(self, 'previous_mb_hp'):
            self.previous_mb_hp = 0
            
        # OFFICIAL PHASE THRESHOLDS (from autosplitter community)
        PHASE_1_HP = 3000    # 0xBB8
        PHASE_2_HP = 18000   # 0x4650  
        PHASE_3_HP = 36000   # 0x8CA0
        
        logger.info(f"🤖 Official MB HP: Previous={self.previous_mb_hp}, Current={mb_official_hp}")
        
        # SAVE STATE CONTRADICTION DETECTION
        # If we're in MB room with full/high missiles and high health, likely a save state reload
        current_missiles = location_data.get('missiles', 0) if location_data else 0
        max_missiles = location_data.get('max_missiles', 0) if location_data else 0
        current_health = location_data.get('health', 0) if location_data else 0
        max_health = location_data.get('max_health', 0) if location_data else 0
        
        missiles_ratio = current_missiles / max_missiles if max_missiles > 0 else 0
        health_ratio = current_health / max_health if max_health > 0 else 0
        
        if in_mb_room and missiles_ratio >= 0.9 and health_ratio >= 0.85:
            logger.info(f"🔄 SAVE STATE CONTRADICTION detected: {missiles_ratio:.1%} missiles + {health_ratio:.1%} health in MB room")
            logger.info(f"🔄 Likely save state reload - resetting MB detection cache")
            # Force reset regardless of other detection
            mb1_detected = False
            mb2_detected = False
            if hasattr(self, 'mother_brain_phase_state'):
                self.mother_brain_phase_state = {
                    'mb1_detected': False,
                    'mb2_detected': False
                }
                logger.info(f"🗑️ Forced cache reset due to contradiction")
            
        # ESCAPE TIMER APPROACH - Much more reliable than memory patterns
        if boss_memory_data.get('escape_timer_1') and len(boss_memory_data['escape_timer_1']) >= 2:
            escape_timer_1_val = struct.unpack('<H', boss_memory_data['escape_timer_1'])[0]
        if boss_memory_data.get('escape_timer_2') and len(boss_memory_data['escape_timer_2']) >= 2:
            escape_timer_2_val = struct.unpack('<H', boss_memory_data['escape_timer_2'])[0]
        if boss_memory_data.get('escape_timer_3') and len(boss_memory_data['escape_timer_3']) >= 2:
            escape_timer_3_val = struct.unpack('<H', boss_memory_data['escape_timer_3'])[0]
        if boss_memory_data.get('escape_timer_4') and len(boss_memory_data['escape_timer_4']) >= 2:
            escape_timer_4_val = struct.unpack('<H', boss_memory_data['escape_timer_4'])[0]
        if boss_memory_data.get('escape_timer_5') and len(boss_memory_data['escape_timer_5']) >= 2:
            escape_timer_5_val = struct.unpack('<H', boss_memory_data['escape_timer_5'])[0]
        if boss_memory_data.get('escape_timer_6') and len(boss_memory_data['escape_timer_6']) >= 2:
            escape_timer_6_val = struct.unpack('<H', boss_memory_data['escape_timer_6'])[0]
            
        # Escape timer indicates MB2 completion (timer starts after MB2 dies)
        escape_timer_active = (escape_timer_1_val > 0) or (escape_timer_2_val > 0) or \
                             (escape_timer_3_val > 0) or (escape_timer_4_val > 0) or \
                             (escape_timer_5_val > 0) or (escape_timer_6_val > 0)
        
        # Log all escape timer values for debugging
        logger.info(f"🎯 Enhanced Escape Timer Detection:")
        logger.info(f"   Timer1: {escape_timer_1_val:04X}, Timer2: {escape_timer_2_val:04X}")
        logger.info(f"   Timer3: {escape_timer_3_val:04X}, Timer4: {escape_timer_4_val:04X}")
        logger.info(f"   Timer5: {escape_timer_5_val:04X}, Timer6: {escape_timer_6_val:04X}")
        logger.info(f"   Active: {escape_timer_active}")
        
        # BOSS HP APPROACH - Direct detection via boss health
        if boss_memory_data.get('boss_hp_1') and len(boss_memory_data['boss_hp_1']) >= 2:
            boss_hp_1_val = struct.unpack('<H', boss_memory_data['boss_hp_1'])[0]
        if boss_memory_data.get('boss_hp_2') and len(boss_memory_data['boss_hp_2']) >= 2:
            boss_hp_2_val = struct.unpack('<H', boss_memory_data['boss_hp_2'])[0] 
        if boss_memory_data.get('boss_hp_3') and len(boss_memory_data['boss_hp_3']) >= 2:
            boss_hp_3_val = struct.unpack('<H', boss_memory_data['boss_hp_3'])[0]
        
        # HYPER BEAM APPROACH - TODO: When we find the correct bit
        # hyper_beam_enabled = location_data.get('beams', {}).get('hyper', False) if location_data else False
        
        logger.info(f"🎯 Reliable MB Detection - Escape Timer 1: {escape_timer_1_val:04X}, Timer 2: {escape_timer_2_val:04X}")
        logger.info(f"🩸 Boss HP Detection - HP1: {boss_hp_1_val:04X}, HP2: {boss_hp_2_val:04X}, HP3: {boss_hp_3_val:04X}")
        
        # Check if we're in Mother Brain room for context
        # area_id = location_data.get('area_id', 0) if location_data else 0
        # room_id = location_data.get('room_id', 0) if location_data else 0
        # in_mb_room = (area_id == 5 and room_id == 56664)  # Tourian Mother Brain room
        
        # Define variables used later regardless of detection path
        # main_mb_detected = bosses.get('mother_brain', False)
        # mb1_detected = False
        # mb2_detected = False
        
        # INTEGRATED MOTHER BRAIN DETECTION SYSTEM
        # Primary: Official autosplitter transitions (most reliable)
        # Secondary: Hyper Beam detection (perfect MB1 indicator)
        # Backup: Our existing escape timer + HP analysis systems
        
        # CRITICAL: Capture original cache state BEFORE any modifications
        original_mb1_state = self.mother_brain_phase_state.get('mb1_detected', False)
        original_mb2_state = self.mother_brain_phase_state.get('mb2_detected', False)
        
        logger.info(f"🔄 Original cache state: MB1={original_mb1_state}, MB2={original_mb2_state}")
        
        # HYPER BEAM DETECTION - Perfect MB1 completion indicator
        hyper_beam_detected = False
        if location_data and location_data.get('beams', {}).get('hyper', False):
            hyper_beam_detected = True
            logger.info(f"🌟 HYPER BEAM DETECTED! This confirms MB1 completion")
        
        # Official autosplitter HP transition detection
        if in_mb_room and mb_official_hp > 0:
            mb1_transition = (self.previous_mb_hp == 0 and mb_official_hp == PHASE_2_HP)
            mb2_transition = (self.previous_mb_hp == 0 and mb_official_hp == PHASE_3_HP)
        
        # DETECTION PRIORITY SYSTEM (most reliable first)
        detection_method = "none"
        
        # 1. OFFICIAL AUTOSPLITTER DETECTION (highest priority)
        if in_mb_room and (mb1_transition or mb2_transition):
            detection_method = "official_transitions"
            if mb1_transition:
                mb1_detected = True
                logger.info(f"🏆 MB1 detected via OFFICIAL autosplitter transition")
            if mb2_transition:
                mb1_detected = True  # MB2 implies MB1 complete
                mb2_detected = True
                logger.info(f"🏆 MB2 detected via OFFICIAL autosplitter transition")
        
        # 2. HYPER BEAM DETECTION (very high priority - perfect MB1 indicator)
        elif hyper_beam_detected:
            detection_method = "hyper_beam"
            logger.info(f"🌟 MB1 CONFIRMED via Hyper Beam! MB2 status preserved from cache")
            mb1_detected = True  # Hyper beam = MB1 definitely complete
            mb2_detected = original_mb2_state  # Preserve MB2 state from cache
        
        # 3. ESCAPE TIMER DETECTION (high priority backup)
        elif escape_timer_active:
            detection_method = "escape_timer"
            logger.info(f"🚨 ESCAPE TIMER DETECTED! MB2 completed, escape sequence active")
            mb1_detected = True  # If MB2 is done, MB1 must be done
            mb2_detected = True
        
        # 4. LIVE BOSS HP ANALYSIS (when in MB room with HP data)
        elif in_mb_room and (boss_hp_1_val > 0 or boss_hp_2_val > 0 or boss_hp_3_val > 0):
            detection_method = "live_hp_analysis"
            max_hp = max(boss_hp_1_val, boss_hp_2_val, boss_hp_3_val)
            current_hp = boss_hp_3_val if boss_hp_3_val > 0 else max_hp
            
            logger.info(f"🩸 LIVE HP Analysis - Current: {current_hp}")
            
            # MUCH MORE CONSERVATIVE RESET LOGIC
            # Only reset if we're clearly at the very beginning (original pre-fight HP ~41,760)
            is_genuine_reset = (current_hp >= 40000 and current_hp <= 42000)  # Very narrow range
            
            if is_genuine_reset:
                # This is clearly a save state reload or new attempt
                logger.info(f"🔄 GENUINE RESET: Original pre-fight HP ({current_hp}) - clearing cache")
                mb1_detected = False
                mb2_detected = False
                self.mother_brain_phase_state = {'mb1_detected': False, 'mb2_detected': False}
            elif current_hp <= 15000:
                # HP is low enough to indicate MB1 progression
                logger.info(f"🩸 MB1 active/complete: Current HP {current_hp} <= 15000")
                mb1_detected = True
                mb2_detected = (current_hp < 5000) or original_mb2_state  # Preserve MB2 if already detected
            else:
                # HP is higher but not in reset range - preserve cache state
                logger.info(f"🔒 PRESERVING CACHE: HP {current_hp} not in reset range, keeping MB1={original_mb1_state}, MB2={original_mb2_state}")
                mb1_detected = original_mb1_state
                mb2_detected = original_mb2_state
        
        # 5. SMART FALLBACK (outside MB room - rely heavily on cache)
        else:
            detection_method = "smart_fallback"
            mb_progress_val = boss_scan_results.get('boss_plus_1', 0)
            mb_alt_pattern = boss_scan_results.get('boss_plus_2', 0)
            
            # Check for strong memory patterns
            strong_memory_evidence = (mb_progress_val >= 0x0700)
            
            # Smart MB2 inference for escape areas
            in_tourian_escape = (area_id == 5 and room_id != 56664)
            in_crateria_post_escape = (area_id == 0)
            no_boss_hp = (boss_hp_1_val == 0 and boss_hp_2_val == 0 and boss_hp_3_val == 0)
            
            if strong_memory_evidence:
                logger.info(f"🔄 Strong memory evidence: 0x{mb_progress_val:04X}")
                mb1_detected = True
                # Smart MB2 inference
                if (in_tourian_escape or in_crateria_post_escape) and no_boss_hp and original_mb1_state:
                    mb2_detected = True
                    logger.info(f"🧠 SMART MB2 INFERENCE: MB1 done + escape area + no boss HP")
                else:
                    mb2_detected = original_mb2_state  # Preserve cache
            else:
                # No strong evidence - rely on cache completely
                logger.info(f"🔒 NO EVIDENCE: Preserving cache state MB1={original_mb1_state}, MB2={original_mb2_state}")
                mb1_detected = original_mb1_state
                mb2_detected = original_mb2_state
        
        logger.info(f"🎯 Detection method: {detection_method} → MB1={mb1_detected}, MB2={mb2_detected}")
        
        # ROBUST CACHE MANAGEMENT - Once detected, stays detected unless genuine reset
        
        # Update cache immediately when phases are detected (prevent loss)
        if mb1_detected:
            self.mother_brain_phase_state['mb1_detected'] = True
        if mb2_detected:
            self.mother_brain_phase_state['mb2_detected'] = True
        
        # Final boss state assignment - prioritize detection over cache for reliability
        bosses['mother_brain_1'] = mb1_detected or self.mother_brain_phase_state['mb1_detected']
        bosses['mother_brain_2'] = mb2_detected or self.mother_brain_phase_state['mb2_detected']
        
        # Log final state for debugging
        final_mb1 = bosses['mother_brain_1']
        final_mb2 = bosses['mother_brain_2']
        logger.info(f"🎯 FINAL MB STATE: MB1={final_mb1}, MB2={final_mb2} (method: {detection_method})")
        
        # End-game detection (Samus reaching her ship)
        samus_ship_detected = self._detect_samus_ship(boss_memory_data, location_data, original_mb1_state, final_mb1, final_mb2)
        bosses['samus_ship'] = samus_ship_detected
        
        # Update previous HP cache for next detection cycle
        self.previous_mb_hp = mb_official_hp
        
        return bosses
    
    def _detect_samus_ship(self, boss_memory_data: Dict[str, bytes], location_data: Dict[str, Any], 
                          main_mb_complete: bool, mb1_complete: bool, mb2_complete: bool) -> bool:
        """Detect when Samus has reached her ship (end-game completion) - hybrid approach"""
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
        
        # Check if Mother Brain sequence is complete
        mother_brain_complete = main_mb_complete or (mb1_complete and mb2_complete)
        partial_mb_complete = mb1_complete  # MB1 completion indicates significant progress
        
        if not mother_brain_complete and not partial_mb_complete:
            logger.info(f"🚢 Ship Debug - No Mother Brain progress")
            return False
        
        # METHOD 1: OFFICIAL AUTOSPLITTER DETECTION (high priority)
        ship_ai_val = 0
        event_flags_val = 0
        
        if boss_memory_data.get('ship_ai') and len(boss_memory_data['ship_ai']) >= 2:
            ship_ai_val = struct.unpack('<H', boss_memory_data['ship_ai'])[0]
        if boss_memory_data.get('event_flags') and len(boss_memory_data['event_flags']) >= 1:
            event_flags_val = struct.unpack('<B', boss_memory_data['event_flags'])[0]
            
        zebes_ablaze = (event_flags_val & 0x40) > 0
        ship_ai_reached = (ship_ai_val == 0xaa4f)
        official_ship_detection = zebes_ablaze and ship_ai_reached
        
        logger.info(f"🚢 OFFICIAL DETECTION - shipAI: 0x{ship_ai_val:04X}, eventFlags: 0x{event_flags_val:02X}")
        logger.info(f"🚢 zebesAblaze: {zebes_ablaze}, shipAI_reached: {ship_ai_reached}")
        
        if official_ship_detection:
            logger.info(f"🚢 ✅ OFFICIAL SHIP DETECTION: Zebes ablaze + shipAI 0xaa4f = SHIP REACHED!")
            return True
        
        # METHOD 2: POSITION-BASED DETECTION (backup - was working before)
        precise_landing_site_rooms = [31224, 37368]  # Known working rooms
        reasonable_ship_room_ranges = [(31220, 31230), (37360, 37375)]
        
        in_exact_ship_room = room_id in precise_landing_site_rooms
        in_ship_room_range = any(start <= room_id <= end for start, end in reasonable_ship_room_ranges)
        
        # Position checks (restored from working version)
        precise_ship_position = (350 <= player_x <= 450) and (180 <= player_y <= 250)
        broad_ship_position = (300 <= player_x <= 500) and (150 <= player_y <= 300)
        
        logger.info(f"🚢 POSITION DETECTION - Room: {room_id}, ExactRoom: {in_exact_ship_room}, RangeRoom: {in_ship_room_range}")
        logger.info(f"🚢 POSITION DETECTION - Pos: ({player_x},{player_y}), PrecisePos: {precise_ship_position}, BroadPos: {broad_ship_position}")
        
        # Position-based ship criteria (require BOTH room AND position)
        exact_position_detection = in_exact_ship_room and precise_ship_position
        reasonable_position_detection = in_ship_room_range and broad_ship_position
        position_ship_detection = exact_position_detection or reasonable_position_detection
        
        if position_ship_detection:
            logger.info(f"🚢 ✅ POSITION SHIP DETECTION: MB complete + Crateria + correct room + ship position!")
            return True
        
        logger.info(f"🚢 ❌ Ship not detected by either method")
        return False
    
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