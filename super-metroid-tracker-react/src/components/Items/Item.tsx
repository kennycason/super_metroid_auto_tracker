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
      energy_tank: 'sprite-energy-tank',
      reserve_tank: 'sprite-reserve-tank',
      missile_tank: 'sprite-missile',
      super_tank: 'sprite-super-missile',
      power_bomb_tank: 'sprite-power-bomb',

      charge: 'sprite-charge-beam',
      spazer: 'sprite-spazer',
      ice: 'sprite-ice',
      wave: 'sprite-wave',
      plasma: 'sprite-plasma',

      morph: 'sprite-morph-ball',
      bombs: 'sprite-bomb',
      spring: 'sprite-spring-ball',
      hi_jump: 'sprite-hijump',
      varia: 'sprite-varia',
      speed_booster: 'sprite-speed',
      grapple: 'sprite-grapple',
      x_ray: 'sprite-xray',
      gravity: 'sprite-gravity',
      space_jump: 'sprite-space',
      screw_attack: 'sprite-screw',
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