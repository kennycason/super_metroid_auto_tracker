import React, { useState, useEffect, useRef } from 'react';
import { Header } from '../UI/Header';
import { Timer } from '../Timer/Timer';
import { Splits } from '../Timer/Splits';
import { ItemsGrid } from '../Items/ItemsGrid';
import { BossesGrid } from '../Bosses/BossesGrid';
import { Location } from '../UI/Location';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Tracker.css';

// Debug window component that matches the original tracker
const DebugWindow: React.FC = () => {
  const { gameState } = useSuperMetroid();
  const [logs, setLogs] = useState<string[]>([]);
  const debugRef = useRef<HTMLDivElement>(null);
  const [wasAtBottom, setWasAtBottom] = useState(true);

  // Add new log message 
  const addLog = (message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    const logEntry = `[${timestamp}] ${message}`;
    
    setLogs(prevLogs => {
      const newLogs = [...prevLogs, logEntry];
      // Keep only last 100 messages to prevent memory issues
      return newLogs.slice(-100);
    });
  };

  // Auto-scroll to bottom if user was at bottom
  useEffect(() => {
    if (debugRef.current && wasAtBottom) {
      debugRef.current.scrollTop = debugRef.current.scrollHeight;
    }
  }, [logs, wasAtBottom]);

  // Track scroll position
  const handleScroll = () => {
    if (debugRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = debugRef.current;
      setWasAtBottom(scrollTop >= (scrollHeight - clientHeight - 5));
    }
  };

  // Add log messages based on game state changes
  useEffect(() => {
    if (gameState.connected) {
      addLog('🔗 Connected to RetroArch');
    } else {
      addLog('❌ Disconnected from RetroArch');
    }
  }, [gameState.connected]);

  useEffect(() => {
    if (gameState.location.area_name) {
      addLog(`📍 Location: ${gameState.location.area_name} - ${gameState.location.room_name}`);
    }
  }, [gameState.location.area_name, gameState.location.room_name]);

  useEffect(() => {
    addLog(`💊 Health: ${gameState.stats.health}/${gameState.stats.max_health}`);
  }, [gameState.stats.health, gameState.stats.max_health]);

  const copyLogs = () => {
    const text = logs.join('\n');
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        addLog('✅ Logs copied to clipboard');
      });
    }
  };

  const clearLogs = () => {
    setLogs([]);
    addLog('🗑️ Debug logs cleared');
  };

  return (
    <div className="debug-section">
      <div className="debug-controls">
        <button onClick={copyLogs} className="debug-btn">📋 Copy</button>
        <button onClick={clearLogs} className="debug-btn">🗑️ Clear</button>
      </div>
      <div 
        ref={debugRef}
        className="debug-log"
        onScroll={handleScroll}
      >
        {logs.map((log, index) => (
          <div key={index} dangerouslySetInnerHTML={{ __html: log }} />
        ))}
      </div>
    </div>
  );
};

// Quantity tiles component (Energy Tank, Missiles, etc.)
const QuantityTiles: React.FC = () => {
  const { gameState, isItemCollected } = useSuperMetroid();
  const { stats } = gameState;

  const quantityItems = [
    {
      id: 'energy_tank',
      name: 'Energy Tanks',
      sprite: 'sprite-energy-tank',
      count: Math.floor((stats.max_health - 99) / 100),
      collected: stats.max_health > 99
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

export const Tracker: React.FC = () => {
  const { isFullscreen, gameState } = useSuperMetroid();

  return (
    <div className={`tracker ${isFullscreen ? 'fullscreen' : ''}`}>
      {!isFullscreen && <Header />}
      
      <div className="tracker-content">
        <div className="tracker-grid">
          {/* Timer Section */}
          <Timer />
          
          {/* Quantity Items Section (Energy, Missiles, etc.) */}
          <QuantityTiles />
          
          {/* Item Tracking Grid */}
          <ItemsGrid />
          
          {/* Boss Tracking Grid */}
          <BossesGrid />
          
          {/* Splits Section - always show */}
          <Splits />
          
          {/* Location */}
          <Location />
          
          {/* Debug Window - hide in fullscreen */}
          {!isFullscreen && <DebugWindow />}
        </div>
      </div>
    </div>
  );
}; 