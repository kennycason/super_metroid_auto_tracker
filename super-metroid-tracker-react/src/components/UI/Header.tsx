import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Header.css';

export const Header: React.FC = () => {
  const { gameState, isFullscreen, setIsFullscreen } = useSuperMetroid();

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  };

  return (
    <div className="header">
      <div className="header-left">
        <h1 className="tracker-title">TRACKING SUPER METROID</h1>
      </div>
      
      <div className="header-right">
        <div className={`connection-status ${gameState.connected ? 'connected' : 'disconnected'}`}>
          <span className="status-dot"></span>
          {gameState.connected ? 'Connected' : 'Disconnected'}
        </div>
        
        <button 
          className="fullscreen-btn"
          onClick={toggleFullscreen}
          title="Toggle Fullscreen"
        >
          ⛶
        </button>
      </div>
    </div>
  );
}; 