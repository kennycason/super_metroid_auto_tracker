import type { GameStats } from '../types/gameState';

/**
 * Frontend Game State Parser
 * Converts raw memory data into structured game state for the browser
 */
export class FrontendGameStateParser {
  // Persistent state for Mother Brain phases - once detected, stays detected
  private motherBrainPhaseState = {
    mb1_detected: false,
    mb2_detected: false
  };

  // Area names mapping
  private readonly areas = {
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
  parseCompleteGameState(memoryData: Record<string, Uint8Array | null>): GameStats {
    try {
      // Parse basic stats from bulk read
      const basicStats = memoryData['basic_stats'];
      const stats = basicStats && basicStats.length >= 22 
        ? this.parseBasicStats(basicStats) 
        : {};

      // Parse location data
      const locationData = this.parseLocationData(
        memoryData['room_id'],
        memoryData['area_id'],
        memoryData['game_state'],
        memoryData['player_x'],
        memoryData['player_y']
      );

      // Parse items with reset detection
      const items = this.parseItems(
        memoryData['items'],
        locationData,
        stats.health || 0,
        stats.missiles || 0,
        stats.max_missiles || 0
      );

      // Parse beams with reset detection
      const beams = this.parseBeams(
        memoryData['beams'],
        locationData,
        stats.health || 0,
        stats.missiles || 0,
        stats.max_missiles || 0
      );

      // Parse bosses
      const bosses = this.parseBosses(
        memoryData['main_bosses'],
        memoryData['crocomire'],
        memoryData['boss_plus_1'],
        memoryData['boss_plus_2'],
        memoryData['boss_plus_3'],
        memoryData['boss_plus_4'],
        memoryData['boss_plus_5'],
        memoryData['escape_timer_1'],
        memoryData['escape_timer_2'],
        memoryData['escape_timer_3'],
        memoryData['escape_timer_4'],
        locationData,
        memoryData['ship_ai'],
        memoryData['event_flags']
      );

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
        items: {
          morph: items.morph || false,
          bombs: items.bombs || false,
          varia: items.varia || false,
          gravity: items.gravity || false,
          hijump: items.hijump || false,
          speed: items.speed || false,
          space: items.space || false,
          screw: items.screw || false,
          grapple: items.grapple || false,
          xray: items.xray || false,
          spring: items.spring || false,
        },
        beams: {
          charge: beams.charge || false,
          ice: beams.ice || false,
          wave: beams.wave || false,
          spazer: beams.spazer || false,
          plasma: beams.plasma || false,
          hyper: beams.hyper || false,
        },
        bosses: {
          bomb_torizo: bosses.bomb_torizo || false,
          kraid: bosses.kraid || false,
          spore_spawn: bosses.spore_spawn || false,
          mother_brain: bosses.mother_brain || false,
          crocomire: bosses.crocomire || false,
          phantoon: bosses.phantoon || false,
          botwoon: bosses.botwoon || false,
          draygon: bosses.draygon || false,
          ridley: bosses.ridley || false,
          golden_torizo: bosses.golden_torizo || false,
          mother_brain_1: bosses.mother_brain_1 || false,
          mother_brain_2: bosses.mother_brain_2 || false,
          samus_ship: bosses.samus_ship || false,
        }
      };
    } catch (err) {
      console.log(`Error parsing game state: ${err}`);
      return this.getEmptyGameState();
    }
  }

  /**
   * Parse basic stats from memory data
   */
  private parseBasicStats(data: Uint8Array): Record<string, number> {
    if (data.length < 22) return {};

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
    } catch (err) {
      console.log(`Error parsing basic stats: ${err}`);
      return {};
    }
  }

  /**
   * Parse location data
   */
  private parseLocationData(
    roomIdData: Uint8Array | null,
    areaIdData: Uint8Array | null,
    gameStateData: Uint8Array | null,
    playerXData: Uint8Array | null,
    playerYData: Uint8Array | null
  ): Record<string, any> {
    try {
      const roomId = roomIdData ? this.readInt16LE(roomIdData, 0) : 0;
      const areaId = areaIdData && areaIdData.length > 0 ? areaIdData[0] : 0;
      const gameState = gameStateData ? this.readInt16LE(gameStateData, 0) : 0;
      const playerX = playerXData ? this.readInt16LE(playerXData, 0) : 0;
      const playerY = playerYData ? this.readInt16LE(playerYData, 0) : 0;

      return {
        room_id: roomId,
        area_id: areaId,
        area_name: this.areas[areaId as keyof typeof this.areas] || 'Unknown',
        game_state: gameState,
        player_x: playerX,
        player_y: playerY
      };
    } catch (err) {
      console.log(`Error parsing location data: ${err}`);
      return {};
    }
  }

  /**
   * Check if we should reset item state (new game detection)
   */
  private shouldResetItemState(
    locationData: Record<string, any>,
    health: number,
    missiles: number,
    maxMissiles: number
  ): boolean {
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
  private parseItems(
    itemsData: Uint8Array | null,
    locationData: Record<string, any>,
    health: number,
    missiles: number,
    maxMissiles: number
  ): Record<string, boolean> {
    if (!itemsData || itemsData.length < 2) return {};

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
  private parseBeams(
    beamsData: Uint8Array | null,
    locationData: Record<string, any>,
    health: number,
    missiles: number,
    maxMissiles: number
  ): Record<string, boolean> {
    if (!beamsData || beamsData.length < 2) return {};

    // Check if we should reset beam state (same logic as items)
    if (this.shouldResetItemState(locationData, health, missiles, maxMissiles)) {
      console.log('🔄 BEAM STATE RESET: Resetting to starting state (no beams)');
      return {
        charge: false,  // Charge beam must be collected
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
   * Parse bosses from memory data (simplified version)
   */
  private parseBosses(
    mainBossesData: Uint8Array | null,
    crocomireData: Uint8Array | null,
    _bossPlus1Data: Uint8Array | null,
    _bossPlus2Data: Uint8Array | null,
    _bossPlus3Data: Uint8Array | null,
    _bossPlus4Data: Uint8Array | null,
    _bossPlus5Data: Uint8Array | null,
    _escapeTimer1Data: Uint8Array | null,
    _escapeTimer2Data: Uint8Array | null,
    _escapeTimer3Data: Uint8Array | null,
    _escapeTimer4Data: Uint8Array | null,
    _locationData: Record<string, any>,
    _shipAiData?: Uint8Array | null,
    _eventFlagsData?: Uint8Array | null
  ): Record<string, boolean> {
    // Basic boss flags - using simplified logic for now
    const mainBosses = mainBossesData ? this.readInt16LE(mainBossesData, 0) : 0;

    return {
      bomb_torizo: (mainBosses & 0x0004) !== 0,
      kraid: (mainBosses & 0x0001) !== 0,
      spore_spawn: (mainBosses & 0x0002) !== 0,
      mother_brain: (mainBosses & 0x0001) !== 0,
      crocomire: crocomireData ? (this.readInt16LE(crocomireData, 0) & 0x0001) !== 0 : false,
      phantoon: (mainBosses & 0x0100) !== 0,
      botwoon: (mainBosses & 0x0200) !== 0,
      draygon: (mainBosses & 0x0400) !== 0,
      ridley: (mainBosses & 0x0010) !== 0,
      golden_torizo: (mainBosses & 0x0020) !== 0,
      mother_brain_1: this.motherBrainPhaseState.mb1_detected,
      mother_brain_2: this.motherBrainPhaseState.mb2_detected,
      samus_ship: false // Placeholder for end game detection
    };
  }

  /**
   * Reset Mother Brain cache
   */
  resetMotherBrainCache(): void {
    this.motherBrainPhaseState.mb1_detected = false;
    this.motherBrainPhaseState.mb2_detected = false;
    console.log('🧠 Mother Brain cache reset to default (not detected)');
  }

  /**
   * Read 16-bit little-endian integer from Uint8Array
   */
  private readInt16LE(data: Uint8Array, offset: number): number {
    if (offset + 1 >= data.length) return 0;
    return data[offset] | (data[offset + 1] << 8);
  }

  /**
   * Get empty game state
   */
  private getEmptyGameState(): GameStats {
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
      items: {
        morph: false,
        bombs: false,
        varia: false,
        gravity: false,
        hijump: false,
        speed: false,
        space: false,
        screw: false,
        grapple: false,
        xray: false,
        spring: false,
      },
      beams: {
        charge: false,
        ice: false,
        wave: false,
        spazer: false,
        plasma: false,
        hyper: false,
      },
      bosses: {
        bomb_torizo: false,
        kraid: false,
        spore_spawn: false,
        mother_brain: false,
        crocomire: false,
        phantoon: false,
        botwoon: false,
        draygon: false,
        ridley: false,
        golden_torizo: false,
        mother_brain_1: false,
        mother_brain_2: false,
        samus_ship: false,
      }
    };
  }
}
