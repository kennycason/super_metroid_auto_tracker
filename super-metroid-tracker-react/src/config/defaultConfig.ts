import type { TrackerConfig } from '../types/config';

export const defaultConfig: TrackerConfig = {
  id: 'default',
  name: 'Full Tracker',
  description: 'Complete Super Metroid tracker with all items and bosses',
  items: [
    // Row 1 - Major Items Row 1
    { id: 'morph', name: 'Morph Ball', sprite: 'morph_ball.png', enabled: true, row: 0, col: 0, category: 'major' },
    { id: 'bombs', name: 'Bombs', sprite: 'bombs.png', enabled: true, row: 0, col: 1, category: 'major' },
    { id: 'charge', name: 'Charge Beam', sprite: 'charge_beam.png', enabled: true, row: 0, col: 2, category: 'beam' },
    { id: 'spazer', name: 'Spazer', sprite: 'spazer.png', enabled: true, row: 0, col: 3, category: 'beam' },
    { id: 'varia', name: 'Varia Suit', sprite: 'varia.png', enabled: true, row: 0, col: 4, category: 'major' },

    // Row 2 - Major Items Row 2
    { id: 'hi_jump', name: 'Hi-Jump Boots', sprite: 'hi_jump.png', enabled: true, row: 1, col: 0, category: 'major' },
    { id: 'speed_booster', name: 'Speed Booster', sprite: 'speed_booster.png', enabled: true, row: 1, col: 1, category: 'major' },
    { id: 'wave', name: 'Wave Beam', sprite: 'wave.png', enabled: true, row: 1, col: 2, category: 'beam' },
    { id: 'ice', name: 'Ice Beam', sprite: 'ice.png', enabled: true, row: 1, col: 3, category: 'beam' },
    { id: 'grapple', name: 'Grappling Beam', sprite: 'grapple.png', enabled: true, row: 1, col: 4, category: 'major' },

    // Row 3 - Major Items Row 3
    { id: 'x_ray', name: 'X-Ray Scope', sprite: 'x_ray.png', enabled: true, row: 2, col: 0, category: 'major' },
    { id: 'plasma', name: 'Plasma Beam', sprite: 'plasma.png', enabled: true, row: 2, col: 1, category: 'beam' },
    { id: 'gravity', name: 'Gravity Suit', sprite: 'gravity.png', enabled: true, row: 2, col: 2, category: 'major' },
    { id: 'space_jump', name: 'Space Jump', sprite: 'space_jump.png', enabled: true, row: 2, col: 3, category: 'major' },
    { id: 'screw_attack', name: 'Screw Attack', sprite: 'screw_attack.png', enabled: true, row: 2, col: 4, category: 'major' },
  ],
  bosses: [
    // Row 1 - Major bosses
    { id: 'bomb_torizo', name: 'Bomb Torizo', sprite: 'bomb_torizo.png', enabled: true, row: 0, col: 0, category: 'mini' },
    { id: 'spore_spawn', name: 'Spore Spawn', sprite: 'spore_spawn.png', enabled: true, row: 0, col: 1, category: 'mini' },
    { id: 'kraid', name: 'Kraid', sprite: 'kraid.png', enabled: true, row: 0, col: 2, category: 'major' },
    { id: 'crocomire', name: 'Crocomire', sprite: 'crocomire.png', enabled: true, row: 0, col: 3, category: 'mini' },
    { id: 'phantoon', name: 'Phantoon', sprite: 'phantoon.png', enabled: true, row: 0, col: 4, category: 'major' },

    // Row 2 - More bosses  
    { id: 'botwoon', name: 'Botwoon', sprite: 'botwoon.png', enabled: true, row: 1, col: 0, category: 'mini' },
    { id: 'draygon', name: 'Draygon', sprite: 'draygon.png', enabled: true, row: 1, col: 1, category: 'major' },
    { id: 'golden_torizo', name: 'Golden Torizo', sprite: 'golden_torizo.png', enabled: true, row: 1, col: 2, category: 'mini' },
    { id: 'ridley', name: 'Ridley', sprite: 'ridley.png', enabled: true, row: 1, col: 3, category: 'major' },
    { id: 'main', name: 'Ship', sprite: 'ship.png', enabled: true, row: 1, col: 4, category: 'major' },

    // Row 3 - Final bosses
    { id: 'mb1', name: 'Mother Brain 1', sprite: 'mother_brain_1.png', enabled: true, row: 2, col: 0, category: 'major' },
    { id: 'mb2', name: 'Mother Brain 2', sprite: 'mother_brain_2.png', enabled: true, row: 2, col: 1, category: 'major' },
  ],
  layout: [
    {
      type: 'timer',
      enabled: true,
      position: { row: 0, col: 0, width: 4 }
    },
    {
      type: 'items',
      enabled: true,
      position: { row: 1, col: 0, width: 4, height: 4 }
    },
    {
      type: 'bosses',
      enabled: true,
      position: { row: 5, col: 0, width: 4, height: 3 }
    },
    {
      type: 'splits',
      enabled: true,
      position: { row: 8, col: 0, width: 4, height: 2 }
    },
    {
      type: 'location',
      enabled: true,
      position: { row: 10, col: 0, width: 4 }
    }
  ],
  settings: {
    showStats: true,
    showTimer: true,
    showSplits: true,
    showLocation: true,
    autoSplit: true,
    fullscreenAvailable: true,
    theme: 'dark'
  }
}; 