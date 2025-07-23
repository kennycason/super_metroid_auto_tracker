import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Status.css';

export type StatusType = 'quantity' | 'powerup' | 'boss';

export interface StatusConfig {
  id: string;
  name: string;
  sprite: string;
  enabled: boolean;
  row?: number;
  col?: number;
  category?: string;
  count?: string | number; // For quantity items
}

interface StatusProps {
  type: StatusType;
  config: StatusConfig;
}

export const Status: React.FC<StatusProps> = ({ type, config }) => {
  const { gameState, isItemCollected, isBossDefeated } = useSuperMetroid();

  const getStatusClass = (): string => {
    switch (type) {
      case 'quantity':
        return config.count !== undefined && config.count !== 0 ? 'obtained' : 'grayed-out';
      case 'powerup':
        return isItemCollected(config.id) ? 'obtained' : 'grayed-out';
      case 'boss':
        return isBossDefeated(config.id) ? 'boss-defeated' : 'grayed-out';
      default:
        return 'grayed-out';
    }
  };

  const getWrapperClass = (): string => {
    switch (type) {
      case 'quantity':
        return 'quantity-tile';
      case 'powerup':
        return 'item';
      case 'boss':
        return 'boss';
      default:
        return 'status-tile';
    }
  };

  const getSpriteClass = (): string => {
    switch (type) {
      case 'quantity':
        return 'sprite';
      case 'powerup':
        return 'item-sprite';
      case 'boss':
        return 'boss-sprite';
      default:
        return 'sprite';
    }
  };

  return (
    <div 
      className={`${getWrapperClass()} ${getStatusClass()} ${config.category || ''}`}
      title={config.name}
    >
      <span className={`${getSpriteClass()} ${config.sprite}`} />
      
      {/* Quantity display for quantity tiles */}
      {type === 'quantity' && config.count !== undefined && (
        <div className="quantity-display">{config.count}</div>
      )}
      
      {/* Name display for powerups (items show counts inline) */}
      {type === 'powerup' && (
        <>
          {(['energy_tank', 'missile_tank', 'super_tank', 'power_bomb_tank'].includes(config.id)) && (
            <div className="item-count">{config.count || ''}</div>
          )}
        </>
      )}
    </div>
  );
}; 