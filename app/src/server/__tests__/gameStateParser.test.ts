import { describe, it, expect } from 'vitest';
import { GameStateParser } from '../gameStateParser';

describe('GameStateParser', () => {
  const parser = new GameStateParser();

  it('should create empty game state when no data provided', () => {
    const result = parser.parseCompleteGameState({});

    expect(result.health).toBe(0);
    expect(result.maxHealth).toBe(99);
    expect(result.areaName).toBe('Unknown');
    expect(result.items).toEqual({});
    expect(result.beams).toEqual({});
    expect(result.bosses).toEqual({});
  });

  it('should parse basic stats correctly', () => {
    // Create mock basic stats data (22 bytes)
    const basicStats = new Uint8Array(22);
    // Health: 150 (0x96) - little endian: 0x96, 0x00
    basicStats[0] = 0x96;
    basicStats[1] = 0x00;
    // Max Health: 199 (0xC7) - little endian: 0xC7, 0x00
    basicStats[2] = 0xC7;
    basicStats[3] = 0x00;
    // Missiles: 50 (0x32) - little endian: 0x32, 0x00
    basicStats[4] = 0x32;
    basicStats[5] = 0x00;

    const result = parser.parseCompleteGameState({
      basic_stats: basicStats
    });

    expect(result.health).toBe(150);
    expect(result.maxHealth).toBe(199);
    expect(result.missiles).toBe(50);
  });

  it('should parse items correctly', () => {
    // Create mock items data with morph ball (0x0004) and bombs (0x1000)
    const itemsData = new Uint8Array(2);
    const itemValue = 0x0004 | 0x1000; // morph + bombs
    itemsData[0] = itemValue & 0xFF;
    itemsData[1] = (itemValue >> 8) & 0xFF;

    const result = parser.parseCompleteGameState({
      items: itemsData
    });

    expect(result.items.morph).toBe(true);
    expect(result.items.bombs).toBe(true);
    expect(result.items.varia).toBe(false);
    expect(result.items.gravity).toBe(false);
  });

  it('should parse beams correctly', () => {
    // Create mock beams data with charge (0x1000) and ice (0x0002)
    const beamsData = new Uint8Array(2);
    const beamValue = 0x1000 | 0x0002; // charge + ice
    beamsData[0] = beamValue & 0xFF;
    beamsData[1] = (beamValue >> 8) & 0xFF;

    const result = parser.parseCompleteGameState({
      beams: beamsData
    });

    expect(result.beams.charge).toBe(true);
    expect(result.beams.ice).toBe(true);
    expect(result.beams.wave).toBe(false);
    expect(result.beams.plasma).toBe(false);
  });

  it('should reset Mother Brain cache', () => {
    // This should not throw an error
    expect(() => parser.resetMotherBrainCache()).not.toThrow();
  });
});
