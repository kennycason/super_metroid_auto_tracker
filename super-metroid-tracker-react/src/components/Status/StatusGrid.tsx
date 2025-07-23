import React, { useState } from 'react';
import { Status } from './Status';
import type { StatusConfig } from './Status';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './StatusGrid.css';

type LayoutMode = 'by-type' | 'one-grid';

export const StatusGrid: React.FC = () => {
  const { gameState, config } = useSuperMetroid();
  const [layoutMode, setLayoutMode] = useState<LayoutMode>('by-type');
  const { stats } = gameState;

  // EXACT COPY of QuantityTiles (5 items)
  const quantityItems: StatusConfig[] = [
    {
      id: 'energy_tank',
      name: 'Energy Tanks',
      sprite: 'sprite-energy-tank',
      enabled: true,
      count: `${stats.health}/${stats.max_health}`,
      category: 'quantity'
    },
    {
      id: 'reserve_tank', 
      name: 'Reserve Tanks',
      sprite: 'sprite-reserve-tank',
      count: `${stats.reserve_energy}/${stats.max_reserve_energy}`,
      enabled: true,
      category: 'quantity'
    },
    {
      id: 'missile_tank',
      name: 'Missiles',
      sprite: 'sprite-missile',
      count: `${stats.missiles}/${stats.max_missiles}`,
      enabled: true,
      category: 'quantity'
    },
    {
      id: 'super_tank',
      name: 'Super Missiles',
      sprite: 'sprite-super-missile',
      count: `${stats.supers}/${stats.max_supers}`,
      enabled: true,
      category: 'quantity'
    },
    {
      id: 'power_bomb_tank',
      name: 'Power Bombs',
      sprite: 'sprite-power-bomb',
      count: `${stats.power_bombs}/${stats.max_power_bombs}`,
      enabled: true,
      category: 'quantity'
    }
  ];

  // Sprite mapping for items (to match Item.tsx)
  const itemSpriteMapping: Record<string, string> = {
    morph: 'sprite-morph-ball',
    bombs: 'sprite-bomb',
    charge: 'sprite-charge-beam',
    spazer: 'sprite-spazer',
    varia: 'sprite-varia',
    hi_jump: 'sprite-hijump',
    speed_booster: 'sprite-speed',
    wave: 'sprite-wave',
    ice: 'sprite-ice',
    grapple: 'sprite-grapple',
    spring: 'sprite-spring',
    x_ray: 'sprite-xray',
    plasma: 'sprite-plasma',
    gravity: 'sprite-gravity',
    space_jump: 'sprite-space-jump',
    screw_attack: 'sprite-screw'
  };

  // EXACT COPY of ItemsGrid logic (16 items sorted by row/col)
  const enabledItems = config.items
    .filter(item => item.enabled)
    .sort((a, b) => {
      if (a.row !== b.row) return a.row - b.row;
      return a.col - b.col;
    });

  const powerupItems: StatusConfig[] = enabledItems.map(item => ({
    id: item.id,
    name: item.name,
    sprite: itemSpriteMapping[item.id] || `sprite-${item.id}`,
    enabled: item.enabled,
    row: item.row,
    col: item.col,
    category: item.category
  }));

  // Sprite mapping for bosses (to match Boss.tsx)
  const bossSpriteMapping: Record<string, string> = {
    bomb_torizo: 'boss-sprite-bomb-torizo',
    spore_spawn: 'boss-sprite-spore-spawn',
    kraid: 'boss-sprite-kraid',
    crocomire: 'boss-sprite-crocomire',
    phantoon: 'boss-sprite-phantoon',
    botwoon: 'boss-sprite-botwoon',
    draygon: 'boss-sprite-draygon',
    golden_torizo: 'boss-sprite-golden-torizo',
    ridley: 'boss-sprite-ridley',
    main: 'boss-sprite-samus-ship',
    mb1: 'boss-sprite-mother-brain-1',
    mb2: 'boss-sprite-mother-brain-2'
  };

  // EXACT COPY of BossesGrid logic (12 bosses sorted by row/col)
  const enabledBosses = config.bosses
    .filter(boss => boss.enabled)
    .sort((a, b) => {
      if (a.row !== b.row) return a.row - b.row;
      return a.col - b.col;
    });

  const bossItems: StatusConfig[] = enabledBosses.map(boss => ({
    id: boss.id,
    name: boss.name,
    sprite: bossSpriteMapping[boss.id] || 'boss-sprite-kraid',
    enabled: boss.enabled,
    row: boss.row,
    col: boss.col,
    category: boss.category
  }));

  // For one-grid layout, combine all in proper order
  const allItems = [...quantityItems, ...powerupItems, ...bossItems];

  return (
    <div className="status-grid-container">
      {/* Layout Toggle */}
      <div className="layout-toggle">
        <button 
          className={`toggle-btn ${layoutMode === 'by-type' ? 'active' : ''}`}
          onClick={() => setLayoutMode('by-type')}
        >
          By Type
        </button>
        <button 
          className={`toggle-btn ${layoutMode === 'one-grid' ? 'active' : ''}`}
          onClick={() => setLayoutMode('one-grid')}
        >
          One Grid
        </button>
      </div>

      {/* Render based on layout mode */}
      {layoutMode === 'by-type' ? (
        <div className="status-grid-by-type">
          {/* Quantity Items Section */}
          <div className="status-section">
            <div className="status-row">
              {quantityItems.map(item => (
                <Status key={`quantity-${item.id}`} type="quantity" config={item} />
              ))}
            </div>
          </div>

          {/* Powerup Items Section */}
          <div className="status-section">
            <div className="status-row">
              {powerupItems.map(item => (
                <Status key={`powerup-${item.id}`} type="powerup" config={item} />
              ))}
            </div>
          </div>

          {/* Boss Items Section */}
          <div className="status-section">
            <div className="status-row">
              {bossItems.map(item => (
                <Status key={`boss-${item.id}`} type="boss" config={item} />
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="status-grid-one-grid">
          {allItems.map(item => {
            const type = quantityItems.some(q => q.id === item.id) ? 'quantity' :
                        bossItems.some(b => b.id === item.id) ? 'boss' : 'powerup';
            return (
              <Status key={`all-${item.id}`} type={type as any} config={item} />
            );
          })}
        </div>
      )}
    </div>
  );
}; 