import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Stats.css';

export const Stats: React.FC = () => {
  const { gameState } = useSuperMetroid();
  
  const { stats } = gameState;

  return (
    <div className="stats-section">
      <div className="stats-header">Stats</div>
      <div className="stats-grid">
        <div className="stat-item health">
          <div className="stat-label">Energy</div>
          <div className="stat-value">
            {stats.health}/{stats.max_health}
          </div>
        </div>
        
        <div className="stat-item missiles">
          <div className="stat-label">Missiles</div>
          <div className="stat-value">
            {stats.missiles}/{stats.max_missiles}
          </div>
        </div>
        
        <div className="stat-item supers">
          <div className="stat-label">Supers</div>
          <div className="stat-value">
            {stats.supers}/{stats.max_supers}
          </div>
        </div>
        
        <div className="stat-item power-bombs">
          <div className="stat-label">Power Bombs</div>
          <div className="stat-value">
            {stats.power_bombs}/{stats.max_power_bombs}
          </div>
        </div>
      </div>
    </div>
  );
}; 