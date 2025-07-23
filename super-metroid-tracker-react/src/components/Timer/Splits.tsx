import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Splits.css';

export const Splits: React.FC = () => {
  const { gameState, formatTime } = useSuperMetroid();
  
  const { splits } = gameState;

  // Show all splits from newest to oldest (most recent first)
  const sortedSplits = [...splits].reverse();

  // Hide the splits section entirely if no splits (matches original behavior)
  if (sortedSplits.length === 0) {
    return null;
  }

  return (
    <div className="splits-section">
      <div className="splits-header">Splits</div>
      <div className="splits-list">
        {sortedSplits.map((split, index) => (
          <div key={`${split.event}-${split.timestamp}`} className="split-item">
            <span className="split-event">{split.event}</span>
            <span className="split-time">{formatTime(split.time)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}; 