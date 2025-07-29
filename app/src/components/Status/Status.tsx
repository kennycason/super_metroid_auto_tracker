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

  // Get dynamic count for quantity items
  const getDynamicCount = (): string | number | undefined => {
    const { stats } = gameState;
    switch (config.id) {
      case 'energy_tank': 
        return `${stats.health}/${stats.max_health}`;
      case 'missile_tank': 
        return `${stats.missiles}/${stats.max_missiles}`;
      case 'super_tank': 
        return `${stats.supers}/${stats.max_supers}`;
      case 'power_bomb_tank': 
        return `${stats.power_bombs}/${stats.max_power_bombs}`;
      case 'reserve_tank': 
        return `${stats.reserve_energy}/${stats.max_reserve_energy}`;
      default: 
        return undefined;
    }
  };

  // Check if quantity item has any progress
  const hasQuantityProgress = (): boolean => {
    const { stats } = gameState;
    switch (config.id) {
      case 'energy_tank': 
        return stats.max_health > 99;
      case 'missile_tank': 
        return stats.max_missiles > 0;
      case 'super_tank': 
        return stats.max_supers > 0;
      case 'power_bomb_tank': 
        return stats.max_power_bombs > 0;
      case 'reserve_tank': 
        return stats.max_reserve_energy > 0;
      default: 
        return false;
    }
  };

  const getStatusClass = (): string => {
    switch (type) {
      case 'quantity':
        return hasQuantityProgress() ? 'obtained' : 'grayed-out';
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

  const currentCount = getDynamicCount();

  return (
    <div className={`${getWrapperClass()} ${getStatusClass()}`}>
      <span className={`${getSpriteClass()} ${config.sprite}`}></span>
      {type === 'quantity' && currentCount !== undefined && (
        <div className="quantity-display">{currentCount}</div>
      )}
    </div>
  );
}; 