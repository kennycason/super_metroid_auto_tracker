import React from 'react';
import { Status } from './Status';
import type { StatusConfig } from './Status';

// Test configurations for all three types
const testQuantityItems: StatusConfig[] = [
  {
    id: 'energy_tank',
    name: 'Energy Tanks',
    sprite: 'sprite-energy-tank',
    enabled: true,
    count: '399/399',
    category: 'quantity'
  },
  {
    id: 'missile_tank',
    name: 'Missiles',
    sprite: 'sprite-missile',
    enabled: true,
    count: '43/65',
    category: 'quantity'
  }
];

const testPowerups: StatusConfig[] = [
  {
    id: 'morph',
    name: 'Morph Ball',
    sprite: 'sprite-morph-ball',
    enabled: true,
    category: 'major'
  },
  {
    id: 'spring',
    name: 'Spring Ball',
    sprite: 'sprite-spring',
    enabled: true,
    category: 'major'
  },
  {
    id: 'charge',
    name: 'Charge Beam',
    sprite: 'sprite-charge-beam',
    enabled: true,
    category: 'beam'
  }
];

const testBosses: StatusConfig[] = [
  {
    id: 'kraid',
    name: 'Kraid',
    sprite: 'boss-sprite-kraid',
    enabled: true,
    category: 'major'
  },
  {
    id: 'ridley',
    name: 'Ridley',
    sprite: 'boss-sprite-ridley',
    enabled: true,
    category: 'major'
  }
];

export const StatusTest: React.FC = () => {
  return (
    <div style={{ padding: '20px', background: '#000', color: '#fff' }}>
      <h2>Status Component Test</h2>
      
      <h3>Quantity Items</h3>
      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        {testQuantityItems.map(item => (
          <Status key={item.id} type="quantity" config={item} />
        ))}
      </div>

      <h3>Powerup Items</h3>
      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        {testPowerups.map(item => (
          <Status key={item.id} type="powerup" config={item} />
        ))}
      </div>

      <h3>Bosses</h3>
      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        {testBosses.map(boss => (
          <Status key={boss.id} type="boss" config={boss} />
        ))}
      </div>
    </div>
  );
}; 