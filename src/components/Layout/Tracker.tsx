import React, { useState, useEffect, useRef } from 'react';
import { Header } from '../UI/Header';
import { Timer } from '../Timer/Timer';
import { Splits } from '../Timer/Splits';
import { Location } from '../UI/Location';
import { StatusGrid } from '../Status/StatusGrid';
import { ItemVisibilitySelector } from '../UI/ItemVisibilitySelector';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Tracker.css';

// Debug window component that matches the original tracker
const DebugWindow: React.FC = () => {
  const { gameState, serverPort, setServerPort } = useSuperMetroid();
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
        <div className="port-control">
          <label htmlFor="serverPort" style={{ fontSize: '12px', marginRight: '5px' }}>Port:</label>
          <input
            id="serverPort"
            type="number"
            value={serverPort}
            onChange={(e) => setServerPort(parseInt(e.target.value) || 8081)}
            style={{
              width: '60px',
              padding: '2px 4px',
              fontSize: '12px',
              backgroundColor: '#001100',
              color: '#00ff00',
              border: '1px solid #00cc00',
              borderRadius: '2px'
            }}
            min="1000"
            max="65535"
          />
        </div>
        <span style={{ fontSize: '12px', opacity: 0.7, marginRight: '5px'}}>Debug Log</span>
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
  const { gameState, isMinimal, setIsMinimal, startTimer, stopTimer } = useSuperMetroid();

  // Add spacebar functionality for timer
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.code === 'Space') {
        event.preventDefault(); // Prevent page scroll
        if (gameState.timer.running) {
          stopTimer();
        } else {
          startTimer();
        }
      } else if (event.code === 'Escape') {
        setIsMinimal(false); // Exit minimal mode on Escape
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [gameState.timer.running, startTimer, stopTimer, setIsMinimal]);

  return (
    <div className="tracker-container">
      <div className={`tracker ${isMinimal ? 'minimal' : ''}`}>
        {/* Always show header, but it will hide elements conditionally */}
        {!isMinimal && <Header />}

        <div className="main-content">          

          {/* Status Grid - always show but will hide layout toggle in minimal mode */}
          <StatusGrid />

          {/* Timer Section - always show */}
          <Timer />

          {/* Splits Section - always show */}
          <Splits maxSplitsDisplay={3}/>

          {/* Location - hide in minimal mode */}
          {!isMinimal && <Location />}

          {/* Item Visibility Selector - hide in minimal mode */}
          {!isMinimal && <ItemVisibilitySelector />}

          {/* Debug Window - hide in minimal mode */}
          {!isMinimal && <DebugWindow />}
        </div>
      </div>
    </div>
  );
}; 
