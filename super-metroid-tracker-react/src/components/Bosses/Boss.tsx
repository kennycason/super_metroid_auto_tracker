import React from 'react';
import type { BossConfig } from '../../types/config';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Boss.css';

interface BossProps {
  config: BossConfig;
}

export const Boss: React.FC<BossProps> = ({ config }) => {
  const { isBossDefeated } = useSuperMetroid();
  
  const isDefeated = isBossDefeated(config.id);
  
  // Map boss IDs to sprite class names (matching original tracker)
  const getSpriteClass = () => {
    const spriteMapping: Record<string, string> = {
      bomb_torizo: 'boss-sprite-b-torizo',
      spore_spawn: 'boss-sprite-spore-spawn',
      kraid: 'boss-sprite-kraid',
      crocomire: 'boss-sprite-crocomire',
      phantoon: 'boss-sprite-phantoon',
      botwoon: 'boss-sprite-botwoon',
      draygon: 'boss-sprite-draygon',
      golden_torizo: 'boss-sprite-golden-torizo',
      ridley: 'boss-sprite-ridley',
      mb1: 'boss-sprite-mother-brain-1',
      mb2: 'boss-sprite-mother-brain-2',
      main: 'boss-sprite-samus-ship',
    };
    
    return spriteMapping[config.id] || 'boss-sprite-kraid';
  };

  return (
    <div 
      className={`boss ${isDefeated ? 'boss-defeated' : 'grayed-out'} ${config.category}`}
      title={config.name}
    >
      <span className={`boss-sprite ${getSpriteClass()}`} />
    </div>
  );
}; 