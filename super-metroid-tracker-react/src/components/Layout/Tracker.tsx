import React, { useState, useEffect, useRef } from 'react';
import { Header } from '../UI/Header';
import { Timer } from '../Timer/Timer';
import { Splits } from '../Timer/Splits';
import { ItemsGrid } from '../Items/ItemsGrid';
import { BossesGrid } from '../Bosses/BossesGrid';
import { QuantityTiles } from '../QuantityTiles/QuantityTiles';
import { Location } from '../UI/Location';
import { StatusTest } from '../Status/StatusTest'; // TEST IMPORT
import { StatusGrid } from '../Status/StatusGrid'; // TEST IMPORT
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
    const newLog = `[${timestamp}] ${message}`;
    setLogs(prev => [...prev.slice(-49), newLog]); // Keep last 50 messages
  };

  // Monitor game state changes and log them
  useEffect(() => {
    if (gameState.connected) {
      addLog(`Connected to game - Area: ${gameState.location.area_name}`);
      if (gameState.stats.health > 0) {
        addLog(`Health: ${gameState.stats.health}/${gameState.stats.max_health}`);
      }
    } else {
      addLog('Disconnected from game');
    }
  }, [gameState.connected, gameState.location.area_name]);

  // Auto-scroll to bottom when new logs are added
  useEffect(() => {
    if (debugRef.current && wasAtBottom) {
      debugRef.current.scrollTop = debugRef.current.scrollHeight;
    }
  }, [logs, wasAtBottom]);

  // Track if user is at bottom of scroll
  const handleScroll = () => {
    if (debugRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = debugRef.current;
      setWasAtBottom(scrollTop + clientHeight >= scrollHeight - 5);
    }
  };

  const clearLogs = () => setLogs([]);

  return (
    <div className="debug-section">
      <div className="debug-controls">
        <button className="debug-btn" onClick={clearLogs}>Clear</button>
        <span style={{ fontSize: '12px', opacity: 0.7 }}>Debug Log</span>
      </div>
      <div 
        className="debug-log" 
        ref={debugRef}
        onScroll={handleScroll}
      >
        {logs.map((log, index) => (
          <div key={index}>{log}</div>
        ))}
        {logs.length === 0 && (
          <div style={{ opacity: 0.5, fontStyle: 'italic' }}>
            Debug messages will appear here...
          </div>
        )}
      </div>
    </div>
  );
};

export const Tracker: React.FC = () => {
  const { isFullscreen } = useSuperMetroid();

  return (
    <div className={`tracker ${isFullscreen ? 'fullscreen' : ''}`}>
      {!isFullscreen && <Header />}
      
      <div className="tracker-content">
        <div className="tracker-grid">         
          <StatusGrid />
          
          {/* Timer Section */}
          <Timer />

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