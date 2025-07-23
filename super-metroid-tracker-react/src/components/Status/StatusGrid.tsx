import React, { useState } from 'react';
import { Status } from './Status';
import type { StatusConfig } from './Status';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './StatusGrid.css';

type LayoutMode = 'by-type' | 'one-grid';

export const StatusGrid: React.FC = () => {
  const { gameState, isMinimal } = useSuperMetroid();
  const [layoutMode, setLayoutMode] = useState<LayoutMode>('by-type');
  const { stats } = gameState;

  // Early return if game state isn't loaded yet
  if (!stats) {
    return <div>Loading...</div>;
  }

  // EXACT COPY of original quantity items (5 items)
  const quantityItems: StatusConfig[] = [
    {
      id: 'energy_tank',
      name: 'Energy Tanks',
      sprite: 'sprite-energy-tank',
      enabled: true,
      count: `${stats.health}/${stats.max_health}`,
    },
    {
      id: 'reserve_tank',
      name: 'Reserve Tanks',
      sprite: 'sprite-reserve-tank',
      enabled: true,
      count: `${stats.reserve_energy}/${stats.max_reserve_energy}`,
    },
    {
      id: 'missile_tank',
      name: 'Missiles',
      sprite: 'sprite-missile',
      enabled: true,
      count: `${stats.missiles}/${stats.max_missiles}`,
    },
    {
      id: 'super_tank',
      name: 'Super Missiles',
      sprite: 'sprite-super-missile',
      enabled: true,
      count: `${stats.supers}/${stats.max_supers}`,
    },
    {
      id: 'power_bomb_tank',
      name: 'Power Bombs',
      sprite: 'sprite-power-bomb',
      enabled: true,
      count: `${stats.power_bombs}/${stats.max_power_bombs}`,
    },
  ];

  // EXACT COPY of original items (16 items in correct order)
  const items: StatusConfig[] = [
    { id: 'morph', name: 'Morph Ball', sprite: 'sprite-morph-ball', enabled: true },
    { id: 'bombs', name: 'Bombs', sprite: 'sprite-bomb', enabled: true },
    { id: 'charge', name: 'Charge Beam', sprite: 'sprite-charge-beam', enabled: true },
    { id: 'spazer', name: 'Spazer Beam', sprite: 'sprite-spazer', enabled: true },
    { id: 'varia', name: 'Varia Suit', sprite: 'sprite-varia', enabled: true },
    { id: 'hi_jump', name: 'Hi-Jump Boots', sprite: 'sprite-hijump', enabled: true },
    { id: 'speed_booster', name: 'Speed Booster', sprite: 'sprite-speed', enabled: true },
    { id: 'wave', name: 'Wave Beam', sprite: 'sprite-wave', enabled: true },
    { id: 'ice', name: 'Ice Beam', sprite: 'sprite-ice', enabled: true },
    { id: 'grapple', name: 'Grappling Beam', sprite: 'sprite-grapple', enabled: true },
    { id: 'x_ray', name: 'X-Ray Scope', sprite: 'sprite-xray', enabled: true },
    { id: 'plasma', name: 'Plasma Beam', sprite: 'sprite-plasma', enabled: true },
    { id: 'gravity', name: 'Gravity Suit', sprite: 'sprite-gravity', enabled: true },
    { id: 'space_jump', name: 'Space Jump', sprite: 'sprite-space', enabled: true },
    { id: 'spring', name: 'Spring Ball', sprite: 'sprite-spring', enabled: true },
    { id: 'screw_attack', name: 'Screw Attack', sprite: 'sprite-screw', enabled: true },
  ];

  // EXACT COPY of original bosses (12 bosses in correct order - NO DUPLICATES!)
  const bosses: StatusConfig[] = [
    { id: 'bomb_torizo', name: 'B.Torizo', sprite: 'boss-sprite-b-torizo', enabled: true },
    { id: 'spore_spawn', name: 'Spore', sprite: 'boss-sprite-spore-spawn', enabled: true },
    { id: 'kraid', name: 'Kraid', sprite: 'boss-sprite-kraid', enabled: true },
    { id: 'crocomire', name: 'Crocomire', sprite: 'boss-sprite-crocomire', enabled: true },
    { id: 'phantoon', name: 'Phantoon', sprite: 'boss-sprite-phantoon', enabled: true },
    { id: 'botwoon', name: 'Botwoon', sprite: 'boss-sprite-botwoon', enabled: true },
    { id: 'draygon', name: 'Draygon', sprite: 'boss-sprite-draygon', enabled: true },
    { id: 'golden_torizo', name: 'G.Torizo', sprite: 'boss-sprite-golden-torizo', enabled: true },
    { id: 'ridley', name: 'Ridley', sprite: 'boss-sprite-ridley', enabled: true },
    { id: 'mb1', name: 'MB1', sprite: 'boss-sprite-mother-brain-1', enabled: true },
    { id: 'mb2', name: 'MB2', sprite: 'boss-sprite-mother-brain-2', enabled: true },
    { id: 'main', name: 'Ship', sprite: 'boss-sprite-samus-ship', enabled: true },
  ];

  const allItems = layoutMode === 'by-type' ? [] : [...quantityItems, ...items, ...bosses];

  return (
    <div className="status-grid-container">
      {layoutMode === 'by-type' ? (
        <div className="status-grid-by-type">
          {/* Quantity Items Section */}
          <div className="status-section">
            <div className="status-row">
              {quantityItems.map((item) => (
                <Status key={item.id} type="quantity" config={item} />
              ))}
            </div>
          </div>

          {/* Items Section */}
          <div className="status-section">
            <div className="status-row">
              {items.map((item) => (
                <Status key={item.id} type="powerup" config={item} />
              ))}
            </div>
          </div>

          {/* Bosses Section */}
          <div className="status-section">
            <div className="status-row">
              {bosses.map((boss) => (
                <Status key={boss.id} type="boss" config={boss} />
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="status-grid-one-grid">
          {allItems.map((item) => {
            const type = quantityItems.includes(item) ? 'quantity' : 
                        items.includes(item) ? 'powerup' : 'boss';
            return (
              <Status key={item.id} type={type} config={item} />
            );
          })}
        </div>
      )}

      {/* Layout Toggle - hide in minimal mode */}
      {!isMinimal && (
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
      )}
    </div>
  );
}; 