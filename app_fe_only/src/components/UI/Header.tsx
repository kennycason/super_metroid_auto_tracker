import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Header.css';

export const Header: React.FC = () => {
  const { gameState, isMinimal, setIsMinimal } = useSuperMetroid();

  const toggleMinimal = () => {
    setIsMinimal(!isMinimal);
  };

  return (
    <div className="header">
     
      <div className="header-right">
        {/* Only show minimal mode button when NOT in minimal mode */}
        <button 
          className="minimal-btn"
          onClick={toggleMinimal}
          title="Toggle Minimal Mode"
        >
          ⛶
        </button>

        {/* Only show connection status when NOT in minimal mode */}
        <div className={`connection-status ${gameState.connected ? 'connected' : 'disconnected'}`}>
          <span className="status-dot"></span>
          {gameState.connected ? 'Connected' : 'Disconnected'}
        </div>
      </div>
    </div>
  );
}; 