import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './QuantityTiles.css';

export const QuantityTiles: React.FC = () => {
  const { gameState } = useSuperMetroid();
  const { stats } = gameState;

  const quantityItems = [
    {
      id: 'energy_tank',
      name: 'Energy Tanks',
      sprite: 'sprite-energy-tank',
      count: `${stats.health}/${stats.max_health}`,
      collected: stats.max_health > 99
    },
    {
      id: 'reserve_tank',
      name: 'Reserve Tanks',
      sprite: 'sprite-reserve-tank',
      count:`${stats.reserve_energy}/${stats.max_reserve_energy}`,
      collected: stats.max_reserve_energy > 0
    },
    {
      id: 'missile_tank',
      name: 'Missiles',
      sprite: 'sprite-missile',
      count: `${stats.missiles}/${stats.max_missiles}`,
      collected: stats.max_missiles > 0
    },
    {
      id: 'super_tank',
      name: 'Super Missiles', 
      sprite: 'sprite-super-missile',
      count: `${stats.supers}/${stats.max_supers}`,
      collected: stats.max_supers > 0
    },
    {
      id: 'power_bomb_tank',
      name: 'Power Bombs',
      sprite: 'sprite-power-bomb', 
      count: `${stats.power_bombs}/${stats.max_power_bombs}`,
      collected: stats.max_power_bombs > 0
    }
  ];

  return (
    <div className="tracking-section">
      <div className="tracking-grid" id="quantityGrid">
        {quantityItems.map(item => (
          <div 
            key={item.id}
            className={`quantity-tile ${item.collected ? 'obtained' : 'grayed-out'}`}
            title={item.name}
          >
            <span className={`sprite ${item.sprite}`} />
            <div className="quantity-display">{item.count}</div>
          </div>
        ))}
      </div>
    </div>
  );
}; 