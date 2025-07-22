import React from 'react';
import type { ItemConfig } from '../../types/config';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Item.css';

interface ItemProps {
  config: ItemConfig;
  showCount?: boolean;
}

export const Item: React.FC<ItemProps> = ({ config, showCount = false }) => {
  const { isItemCollected, gameState } = useSuperMetroid();
  
  const isCollected = isItemCollected(config.id);
  
  // Get count for expansion items
  const getItemCount = (): number | string | undefined => {
    if (!showCount) return undefined;
    
    const { stats } = gameState;
    switch (config.id) {
      case 'energy_tank': return Math.floor((stats.max_health - 99) / 100);
      case 'missile_tank': return `${stats.missiles}/${stats.max_missiles}`;
      case 'super_tank': return `${stats.supers}/${stats.max_supers}`;
      case 'power_bomb_tank': return `${stats.power_bombs}/${stats.max_power_bombs}`;
      case 'reserve_tank': return 0; // TODO: Add reserve tank detection
      default: return undefined;
    }
  };

  const count = getItemCount();
  
  // Map item IDs to sprite class names (matching original tracker)
  const getSpriteClass = () => {
    const spriteMapping: Record<string, string> = {
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
      gravity: 'sprite-gravity',
      space_jump: 'sprite-space',
      plasma: 'sprite-plasma',
      screw_attack: 'sprite-screw',
      x_ray: 'sprite-xray',
      energy_tank: 'sprite-energy-tank',
      missile_tank: 'sprite-missile',
      super_tank: 'sprite-super-missile',
      power_bomb_tank: 'sprite-power-bomb',
      reserve_tank: 'sprite-reserve-tank',
    };
    
    return spriteMapping[config.id] || 'sprite-morph-ball';
  };

  return (
    <div 
      className={`item ${isCollected ? 'obtained' : 'grayed-out'} ${config.category}`}
      title={config.name}
    >
      <span className={`item-sprite ${getSpriteClass()}`} />
      {count !== undefined && (
        <div className="item-count">{count}</div>
      )}
    </div>
  );
}; 