"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GameStateParser = void 0;
/**
 * Super Metroid Game State Parser
 * Converts raw memory data into structured game state
 * Equivalent to the Kotlin GameStateParser
 */
class GameStateParser {
    // Persistent state for Mother Brain phases - once detected, stays detected
    motherBrainPhaseState = {
        mb1_detected: false,
        mb2_detected: false
    };
    // Area names mapping
    areas = {
        0: 'Crateria',
        1: 'Brinstar',
        2: 'Norfair',
        3: 'Wrecked Ship',
        4: 'Maridia',
        5: 'Tourian'
    };
    /**
     * Parse complete game state from memory data
     */
    parseCompleteGameState(memoryData) {
        try {
            // Parse basic stats from bulk read
            const basicStats = memoryData['basic_stats'];
            const stats = basicStats && basicStats.length >= 22
                ? this.parseBasicStats(basicStats)
                : {};
            // Parse location data
            const locationData = this.parseLocationData(memoryData['room_id'], memoryData['area_id'], memoryData['game_state'], memoryData['player_x'], memoryData['player_y']);
            // Parse items with reset detection
            const items = this.parseItems(memoryData['items'], locationData, stats.health || 0, stats.missiles || 0, stats.max_missiles || 0);
            // Parse beams with reset detection
            const beams = this.parseBeams(memoryData['beams'], locationData, stats.health || 0, stats.missiles || 0, stats.max_missiles || 0);
            // Parse bosses
            const bosses = this.parseBosses(memoryData['main_bosses'], memoryData['crocomire'], memoryData['boss_plus_1'], memoryData['boss_plus_2'], memoryData['boss_plus_3'], memoryData['boss_plus_4'], memoryData['boss_plus_5'], memoryData['escape_timer_1'], memoryData['escape_timer_2'], memoryData['escape_timer_3'], memoryData['escape_timer_4'], locationData, memoryData['ship_ai'], memoryData['event_flags']);
            return {
                health: stats.health || 0,
                max_health: stats.max_health || 99,
                missiles: stats.missiles || 0,
                max_missiles: stats.max_missiles || 0,
                supers: stats.supers || 0,
                max_supers: stats.max_supers || 0,
                power_bombs: stats.power_bombs || 0,
                max_power_bombs: stats.max_power_bombs || 0,
                reserve_energy: stats.reserve_energy || 0,
                max_reserve_energy: stats.max_reserve_energy || 0,
                room_id: locationData.room_id || 0,
                area_id: locationData.area_id || 0,
                area_name: locationData.area_name || 'Unknown',
                game_state: locationData.game_state || 0,
                player_x: locationData.player_x || 0,
                player_y: locationData.player_y || 0,
                items,
                beams,
                bosses
            };
        }
        catch (err) {
            console.log(`Error parsing game state: ${err}`);
            return this.getEmptyGameState();
        }
    }
    /**
     * Parse basic stats from memory data
     */
    parseBasicStats(data) {
        if (data.length < 22)
            return {};
        try {
            return {
                health: this.readInt16LE(data, 0),
                max_health: this.readInt16LE(data, 2),
                missiles: this.readInt16LE(data, 4),
                max_missiles: this.readInt16LE(data, 6),
                supers: this.readInt16LE(data, 8),
                max_supers: this.readInt16LE(data, 10),
                power_bombs: this.readInt16LE(data, 12),
                max_power_bombs: this.readInt16LE(data, 14),
                max_reserve_energy: this.readInt16LE(data, 18),
                reserve_energy: this.readInt16LE(data, 20)
            };
        }
        catch (err) {
            console.log(`Error parsing basic stats: ${err}`);
            return {};
        }
    }
    /**
     * Parse location data
     */
    parseLocationData(roomIdData, areaIdData, gameStateData, playerXData, playerYData) {
        try {
            const roomId = roomIdData ? this.readInt16LE(roomIdData, 0) : 0;
            const areaId = areaIdData && areaIdData.length > 0 ? areaIdData[0] : 0;
            const gameState = gameStateData ? this.readInt16LE(gameStateData, 0) : 0;
            const playerX = playerXData ? this.readInt16LE(playerXData, 0) : 0;
            const playerY = playerYData ? this.readInt16LE(playerYData, 0) : 0;
            return {
                room_id: roomId,
                area_id: areaId,
                area_name: this.areas[areaId] || 'Unknown',
                game_state: gameState,
                player_x: playerX,
                player_y: playerY
            };
        }
        catch (err) {
            console.log(`Error parsing location data: ${err}`);
            return {};
        }
    }
    /**
     * Check if we should reset item state (new game detection)
     */
    shouldResetItemState(locationData, health, missiles, maxMissiles) {
        const areaId = locationData.area_id || 0;
        const roomId = locationData.room_id || 0;
        // Reset scenarios - CONSERVATIVE:
        // 1. Intro scene (very specific early game indicators)
        const inStartingArea = areaId === 0; // Crateria
        const hasStartingHealth = health <= 99;
        const inStartingRooms = roomId < 1000;
        if (inStartingArea && hasStartingHealth && inStartingRooms) {
            console.log(`🔄 ITEM STATE RESET: intro scene detected - Area:${areaId}, Room:${roomId}, Health:${health}`);
            return true;
        }
        // 2. Very specific new game indicators only
        const definiteNewGame = (health === 99 && missiles === 0 && maxMissiles === 0 && roomId < 1000);
        if (definiteNewGame) {
            console.log(`🔄 ITEM STATE RESET: new game detected - Area:${areaId}, Room:${roomId}, Health:${health}, Missiles:${missiles}/${maxMissiles}`);
            return true;
        }
        return false;
    }
    /**
     * Parse items from memory data
     */
    parseItems(itemsData, locationData, health, missiles, maxMissiles) {
        if (!itemsData || itemsData.length < 2)
            return {};
        // Check if we should reset item state
        if (this.shouldResetItemState(locationData, health, missiles, maxMissiles)) {
            return {
                morph: false,
                bombs: false,
                varia: false,
                gravity: false,
                hijump: false,
                speed: false,
                space: false,
                screw: false,
                spring: false,
                grapple: false,
                xray: false
            };
        }
        const itemValue = this.readInt16LE(itemsData, 0);
        return {
            morph: (itemValue & 0x0004) !== 0,
            bombs: (itemValue & 0x1000) !== 0,
            varia: (itemValue & 0x0001) !== 0,
            gravity: (itemValue & 0x0020) !== 0,
            hijump: (itemValue & 0x0100) !== 0,
            speed: (itemValue & 0x2000) !== 0,
            space: (itemValue & 0x0200) !== 0,
            screw: (itemValue & 0x0008) !== 0,
            spring: (itemValue & 0x0002) !== 0,
            grapple: (itemValue & 0x4000) !== 0,
            xray: (itemValue & 0x8000) !== 0
        };
    }
    /**
     * Parse beams from memory data
     */
    parseBeams(beamsData, locationData, health, missiles, maxMissiles) {
        if (!beamsData || beamsData.length < 2)
            return {};
        // Check if we should reset beam state (same logic as items)
        if (this.shouldResetItemState(locationData, health, missiles, maxMissiles)) {
            console.log('🔄 BEAM STATE RESET: Resetting to starting state (no beams)');
            return {
                charge: false, // Charge beam must be collected
                ice: false,
                wave: false,
                spazer: false,
                plasma: false,
                hyper: false
            };
        }
        const beamValue = this.readInt16LE(beamsData, 0);
        return {
            charge: (beamValue & 0x1000) !== 0,
            ice: (beamValue & 0x0002) !== 0,
            wave: (beamValue & 0x0001) !== 0,
            spazer: (beamValue & 0x0004) !== 0,
            plasma: (beamValue & 0x0008) !== 0,
            hyper: (beamValue & 0x0010) !== 0
        };
    }
    /**
     * Parse bosses from memory data - Advanced detection using multiple memory addresses
     * Converted from working Kotlin GameStateParser.kt
     */
    parseBosses(mainBossesData, crocomireData, bossPlus1Data, bossPlus2Data, bossPlus3Data, bossPlus4Data, _bossPlus5Data, escapeTimer1Data, escapeTimer2Data, escapeTimer3Data, escapeTimer4Data, locationData, shipAiData = null, eventFlagsData = null) {
        // Basic boss flags - EXACT Kotlin bit patterns
        const mainBosses = mainBossesData ? this.readInt16LE(mainBossesData, 0) : 0;
        // Parse crocomire separately (matches Kotlin logic)
        const crocomireValue = crocomireData ? this.readInt16LE(crocomireData, 0) : 0;
        const crocomireDefeated = ((crocomireValue & 0x0002) !== 0) && (crocomireValue >= 0x0202);
        // Advanced boss detection using boss_plus patterns (matches Kotlin logic)
        const bossPlus1 = bossPlus1Data ? this.readInt16LE(bossPlus1Data, 0) : 0;
        const bossPlus2 = bossPlus2Data ? this.readInt16LE(bossPlus2Data, 0) : 0;
        const bossPlus3 = bossPlus3Data ? this.readInt16LE(bossPlus3Data, 0) : 0;
        const bossPlus4 = bossPlus4Data ? this.readInt16LE(bossPlus4Data, 0) : 0;
        // Phantoon detection - Fixed to match Kotlin logic
        const phantoonDetected = (bossPlus3 !== 0) && ((bossPlus3 & 0x01) !== 0);
        // Botwoon detection - Fixed to match Kotlin logic
        const botwoonDetected = (((bossPlus2 & 0x04) !== 0) && (bossPlus2 > 0x0100)) ||
            (((bossPlus4 & 0x02) !== 0) && (bossPlus4 > 0x0001));
        // Draygon detection - Use event flags (0x0008 = Draygon statue is grey) + fallback to boss_plus pattern
        let draygonDetected = false;
        // Primary method: Check event flags for "Draygon statue is grey" (event 0x0008)
        if (eventFlagsData && eventFlagsData.length > 0) {
            // Event flags are stored as a bitmask. Event 0x0008 would be bit 8
            // We need to check if this bit is set in the event flags
            const eventFlags = this.readInt16LE(eventFlagsData, 0);
            const draygonStatueGrey = (eventFlags & 0x0100) !== 0; // Bit 8 (0x0008 = 1 << 8 = 0x0100)
            if (draygonStatueGrey) {
                draygonDetected = true;
                console.log(`🐉 DRAYGON DEBUG: Detected via event flags - eventFlags: 0x${eventFlags.toString(16).toUpperCase().padStart(4, '0')}, bit 8 set: ${draygonStatueGrey}`);
            }
        }
        // Fallback method: Use boss_plus_3 pattern (in case event flags aren't working)
        if (!draygonDetected && (bossPlus3 === 0x0301)) {
            draygonDetected = true;
            console.log(`🐉 DRAYGON DEBUG: Detected via fallback boss_plus_3 pattern: 0x${bossPlus3.toString(16).toUpperCase().padStart(4, '0')}`);
        }
        console.log(`🐉 DRAYGON DEBUG: Final detection result: ${draygonDetected}`);
        // Ridley detection - Fixed to match Kotlin logic and avoid false positives
        let ridleyDetected = false;
        // Check for specific Ridley patterns while excluding known false positives
        if ((bossPlus2 & 0x0001) !== 0) { // Check boss_plus_2 first
            // Current Ridley pattern: 0x0107, Draygon false positive: 0x0203
            if (bossPlus2 >= 0x0100 && bossPlus2 !== 0x0203) {
                ridleyDetected = true;
            }
        }
        else if ((bossPlus4 & 0x0001) !== 0) { // Check boss_plus_4 only as fallback
            // Exclude known Botwoon patterns (0x0003, 0x0007, etc.) and require higher values
            if (bossPlus4 >= 0x0011 && bossPlus4 !== 0x0003 && bossPlus4 !== 0x0007) {
                ridleyDetected = true;
            }
        }
        // Golden Torizo detection - More specific patterns to avoid false positives
        console.log(`🏆 GOLDEN TORIZO DEBUG: Memory values - bossPlus1: 0x${bossPlus1.toString(16).toUpperCase().padStart(4, '0')}, bossPlus2: 0x${bossPlus2.toString(16).toUpperCase().padStart(4, '0')}, bossPlus3: 0x${bossPlus3.toString(16).toUpperCase().padStart(4, '0')}, bossPlus4: 0x${bossPlus4.toString(16).toUpperCase().padStart(4, '0')}`);
        // FIXED: Make condition1 more specific to avoid false positives with bossPlus1 = 0x0207
        const condition1 = (bossPlus1 & 0x0700) === 0x0700; // Require all bits in 0x0700 to be set
        const condition2 = ((bossPlus2 & 0x0100) !== 0) && (bossPlus2 >= 0x0400); // Lowered threshold
        const condition3 = (bossPlus1 >= 0x0603); // Direct value check
        const condition4 = ((bossPlus3 & 0x0100) !== 0); // Alternative address pattern
        console.log(`🏆 GOLDEN TORIZO DEBUG: Conditions - condition1: ${condition1} (bossPlus1 & 0x0700 === 0x0700)`);
        console.log(`🏆 GOLDEN TORIZO DEBUG: Conditions - condition2: ${condition2} (bossPlus2 & 0x0100 !== 0 && bossPlus2 >= 0x0400)`);
        console.log(`🏆 GOLDEN TORIZO DEBUG: Conditions - condition3: ${condition3} (bossPlus1 >= 0x0603)`);
        console.log(`🏆 GOLDEN TORIZO DEBUG: Conditions - condition4: ${condition4} (bossPlus3 & 0x0100 !== 0)`);
        const golden_torizo_detected = (condition1 || condition2 || condition3 || condition4);
        console.log(`🏆 GOLDEN TORIZO DEBUG: Final detection result: ${golden_torizo_detected}`);
        // Mother Brain phase detection
        // According to SMMM reference: 0002 = Mother Brain's glass case is broken (bit 1, not bit 0)
        const motherBrainGlassBroken = (mainBosses & 0x0002) !== 0; // bit 1 - glass case broken
        const motherBrainDefeated = (mainBosses & 0x0001) !== 0; // bit 0 - final defeat
        console.log(`🧠 MB DEBUG: mainBosses=0x${mainBosses.toString(16)}, glassBroken=${motherBrainGlassBroken}, defeated=${motherBrainDefeated}`);
        // Check if we're in the Mother Brain room
        const areaId = locationData.area_id || 0;
        const roomId = locationData.room_id || 0;
        const inMotherBrainRoom = (areaId === 5 && roomId === 56664);
        console.log(`🧠 MB DEBUG: area=${areaId}, room=${roomId}, inMBRoom=${inMotherBrainRoom}`);
        // Check escape timers for MB2 detection
        const escapeTimer1 = escapeTimer1Data ? this.readInt16LE(escapeTimer1Data, 0) : 0;
        const escapeTimer2 = escapeTimer2Data ? this.readInt16LE(escapeTimer2Data, 0) : 0;
        const escapeTimer3 = escapeTimer3Data ? this.readInt16LE(escapeTimer3Data, 0) : 0;
        const escapeTimer4 = escapeTimer4Data ? this.readInt16LE(escapeTimer4Data, 0) : 0;
        const escapeTimerActive = escapeTimer1 > 0 || escapeTimer2 > 0 || escapeTimer3 > 0 || escapeTimer4 > 0;
        console.log(`🧠 MB DEBUG: escapeTimers=[${escapeTimer1}, ${escapeTimer2}, ${escapeTimer3}, ${escapeTimer4}], active=${escapeTimerActive}`);
        // Nuclear reset scenario - reset MB2 if we're back in MB room with reasonable missile count
        if (inMotherBrainRoom && this.motherBrainPhaseState.mb2_detected === true) {
            // Check if we have reasonable missile count (indicating a new fight)
            const missiles = locationData.missiles || 0;
            const maxMissiles = locationData.max_missiles || 1;
            const hasReasonableMissiles = missiles > (maxMissiles * 0.7);
            if (hasReasonableMissiles) {
                console.log(`🚨 NUCLEAR RESET: In MB room with ${missiles}/${maxMissiles} missiles - force clearing MB2`);
                this.motherBrainPhaseState.mb2_detected = false;
            }
        }
        // MB1 detection - glass case broken OR escape timer active (you're at least past phase 1)
        if (motherBrainGlassBroken && inMotherBrainRoom) {
            this.motherBrainPhaseState.mb1_detected = true;
            console.log('🧠 MB1 DETECTED: Glass case broken in MB room');
        }
        else if (escapeTimerActive && inMotherBrainRoom) {
            this.motherBrainPhaseState.mb1_detected = true;
            console.log('🧠 MB1 DETECTED: Escape timer active in MB room (backup method)');
        }
        // MB2 detection - only if final Mother Brain bit is set (full defeat)
        // The escape timer can be active during MB2 fight, but MB2 is only complete when mainBosses bit 0 is set
        if (motherBrainDefeated && inMotherBrainRoom) {
            this.motherBrainPhaseState.mb1_detected = true; // Ensure MB1 is also set
            this.motherBrainPhaseState.mb2_detected = true;
            console.log('🧠 MB2 DETECTED: Mother Brain fully defeated (bit 0 set)');
        }
        return {
            // Basic bosses - Kotlin bit patterns (REMOVED incorrect Draygon detection)
            bomb_torizo: (mainBosses & 0x0004) !== 0, // bit 2 ✅
            kraid: (mainBosses & 0x0100) !== 0, // bit 8 ✅
            spore_spawn: (mainBosses & 0x0200) !== 0, // bit 9 ✅
            mother_brain: motherBrainDefeated, // bit 0 ✅
            // Advanced bosses using correct patterns
            crocomire: crocomireDefeated, // Special crocomire logic ✅
            phantoon: phantoonDetected, // Advanced detection ✅
            botwoon: botwoonDetected, // Advanced detection ✅
            draygon: draygonDetected, // FIXED: Only 0x0301 pattern ✅
            ridley: ridleyDetected, // Advanced detection with multiple patterns ✅
            golden_torizo: golden_torizo_detected, // Advanced detection with multiple conditions ✅
            mother_brain_1: this.motherBrainPhaseState.mb1_detected,
            mother_brain_2: this.motherBrainPhaseState.mb2_detected,
            samus_ship: this.detectSamusShip(// End-game detection
            shipAiData, eventFlagsData, locationData, motherBrainDefeated, this.motherBrainPhaseState.mb1_detected, this.motherBrainPhaseState.mb2_detected)
        };
    }
    /**
     * Reset Mother Brain cache
     */
    resetMotherBrainCache() {
        this.motherBrainPhaseState.mb1_detected = false;
        this.motherBrainPhaseState.mb2_detected = false;
        console.log('🧠 Mother Brain cache reset to default (not detected)');
    }
    /**
     * Detect when Samus has reached her ship (end-game completion)
     * Based on the Kotlin implementation's detectSamusShip method
     */
    detectSamusShip(shipAiData, eventFlagsData, locationData, motherBrainDefeated, mb1Detected, mb2Detected) {
        // Debug logging
        console.log('🚢 Ship Detection: Starting detection process');
        // Check if we have location data
        if (!locationData || Object.keys(locationData).length === 0) {
            console.log('🚢 Ship Detection: No location data available');
            return false;
        }
        // Extract location data
        const areaId = locationData.area_id || 0;
        const roomId = locationData.room_id || 0;
        const playerX = locationData.player_x || 0;
        const playerY = locationData.player_y || 0;
        const health = locationData.health || 0;
        const missiles = locationData.missiles || 0;
        const maxMissiles = locationData.max_missiles || 0;
        // Debug current state
        console.log(`🚢 Ship Debug - Area: ${areaId}, Room: ${roomId}, Pos: (${playerX},${playerY})`);
        console.log(`🚢 Ship Debug - MB Status: main=${motherBrainDefeated}, MB1=${mb1Detected}, MB2=${mb2Detected}`);
        console.log(`🚢 Ship Debug - Stats: Health=${health}, Missiles=${missiles}/${maxMissiles}`);
        // RESET DETECTION: Check if we're in a new game or early game scenario
        // Similar to shouldResetItemState logic
        const inStartingArea = areaId === 0; // Crateria
        const hasStartingHealth = health <= 99;
        const inStartingRooms = roomId < 1000;
        const definiteNewGame = (health === 99 && missiles === 0 && maxMissiles === 0 && roomId < 1000);
        if ((inStartingArea && hasStartingHealth && inStartingRooms) || definiteNewGame) {
            console.log(`🚢 Ship Detection RESET: New game or early game detected - Area:${areaId}, Room:${roomId}, Health:${health}, Missiles:${missiles}/${maxMissiles}`);
            return false;
        }
        // Check if Mother Brain sequence is complete
        const motherBrainComplete = motherBrainDefeated || (mb1Detected && mb2Detected);
        const partialMbComplete = mb1Detected; // MB1 completion indicates significant progress
        if (!motherBrainComplete && !partialMbComplete) {
            console.log('🚢 Ship Debug - No Mother Brain progress');
            return false;
        }
        // METHOD 1: OFFICIAL AUTOSPLITTER DETECTION (high priority)
        let shipAiVal = 0;
        let eventFlagsVal = 0;
        if (shipAiData && shipAiData.length >= 2) {
            shipAiVal = this.readInt16LE(shipAiData, 0);
        }
        if (eventFlagsData && eventFlagsData.length > 0) {
            eventFlagsVal = eventFlagsData[0] & 0xFF;
        }
        const zebesAblaze = (eventFlagsVal & 0x40) !== 0;
        const shipAiReached = (shipAiVal === 0xaa4f);
        const officialShipDetection = zebesAblaze && shipAiReached;
        console.log(`🚢 OFFICIAL DETECTION - shipAI: 0x${shipAiVal.toString(16).toUpperCase().padStart(4, '0')}, eventFlags: 0x${eventFlagsVal.toString(16).toUpperCase().padStart(2, '0')}`);
        console.log(`🚢 zebesAblaze: ${zebesAblaze}, shipAI_reached: ${shipAiReached}`);
        if (officialShipDetection) {
            console.log('🚢 ✅ OFFICIAL SHIP DETECTION: Zebes ablaze + shipAI 0xaa4f = SHIP REACHED!');
            return true;
        }
        // METHOD 2: RELAXED AREA DETECTION
        // Try both traditional Crateria (area 0) AND escape sequence areas
        const inCrateria = (areaId === 0);
        const inPossibleEscapeArea = (areaId >= 0 && areaId <= 5); // Be more permissive with areas
        console.log(`🚢 AREA CHECK - inCrateria: ${inCrateria}, inPossibleEscape: ${inPossibleEscapeArea}`);
        // METHOD 3: EMERGENCY SHIP DETECTION - If MB2 complete + reasonable position data
        const emergencyConditions = (mb2Detected && // MB2 must be complete
            (areaId === 0 || areaId === 5) && // Common areas during escape/ship sequence
            (playerX > 1200 && playerY > 1300) // Must be in very specific ship coordinates
        );
        if (emergencyConditions) {
            console.log('🚢 🚨 EMERGENCY SHIP DETECTION: MB2 complete + valid area/position!');
            return true;
        }
        // If we have MB2 complete, be VERY permissive with area detection
        const areaCheckPassed = mb2Detected ? inPossibleEscapeArea : inCrateria;
        if (!areaCheckPassed) {
            console.log(`🚢 Ship Debug - Area check failed (area=${areaId}), ship detection blocked`);
            return false;
        }
        // METHOD 3: POSITION-BASED DETECTION (backup - was working before)
        const preciseLandingSiteRooms = [31224, 37368]; // Known working rooms
        const reasonableShipRoomRanges = [
            { min: 31220, max: 31230 },
            { min: 37360, max: 37375 },
            { min: 0, max: 100 } // Added room 0 range for escape
        ];
        const inExactShipRoom = preciseLandingSiteRooms.includes(roomId);
        const inShipRoomRange = reasonableShipRoomRanges.some(range => roomId >= range.min && roomId <= range.max);
        // Position-based criteria (RELAXED for escape sequence)
        const shipExactXRange = (1150 <= playerX && playerX <= 1350); // Precise ship coordinates
        const shipExactYRange = (1080 <= playerY && playerY <= 1380); // Extended downward for ship entry
        const preciseShipPosition = shipExactXRange && shipExactYRange;
        const shipEscapeXRange = (1100 <= playerX && playerX <= 1400); // Much more restrictive X range for ship area
        const shipEscapeYRange = (1050 <= playerY && playerY <= 1400); // Extended downward for ship area
        const broadShipPosition = shipEscapeXRange && shipEscapeYRange;
        console.log(`🚢 POSITION DETECTION - Room: ${roomId}, ExactRoom: ${inExactShipRoom}, RangeRoom: ${inShipRoomRange}`);
        console.log(`🚢 POSITION DETECTION - Pos: (${playerX},${playerY}), PrecisePos: ${preciseShipPosition}, BroadPos: ${broadShipPosition}`);
        // Position-based ship criteria
        const exactPositionDetection = inExactShipRoom && preciseShipPosition;
        const escapeSequenceDetection = inShipRoomRange && broadShipPosition; // RELAXED: Any reasonable room + broad position
        const positionShipDetection = exactPositionDetection || escapeSequenceDetection;
        if (positionShipDetection) {
            console.log('🚢 ✅ POSITION-BASED SHIP DETECTION: Valid room + position = SHIP REACHED!');
            return true;
        }
        // If we get here, no detection method succeeded
        console.log('🚢 ❌ SHIP NOT DETECTED: All detection methods failed');
        return false;
    }
    /**
     * Read 16-bit little-endian integer from Uint8Array
     */
    readInt16LE(data, offset) {
        if (offset + 1 >= data.length)
            return 0;
        return data[offset] | (data[offset + 1] << 8);
    }
    /**
     * Get empty game state
     */
    getEmptyGameState() {
        return {
            health: 0,
            max_health: 99,
            missiles: 0,
            max_missiles: 0,
            supers: 0,
            max_supers: 0,
            power_bombs: 0,
            max_power_bombs: 0,
            reserve_energy: 0,
            max_reserve_energy: 0,
            room_id: 0,
            area_id: 0,
            area_name: 'Unknown',
            game_state: 0,
            player_x: 0,
            player_y: 0,
            items: {},
            beams: {},
            bosses: {}
        };
    }
}
exports.GameStateParser = GameStateParser;
//# sourceMappingURL=gameStateParser.js.map