import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Timer.css';

export const Timer: React.FC = () => {
  const { gameState, startTimer, stopTimer, resetTimer, adjustTimer, formatTime, isMinimal } = useSuperMetroid();
  
  const { timer } = gameState;

  return (
    <div className="timer-section">
      <div className="timer-header">Timer</div>
      <div className="timer-display">
        {formatTime(timer.elapsed)}
      </div>
      <div className="timer-controls">
        {!timer.running ? (
          <button 
            className="timer-btn start-btn"
            onClick={startTimer}
          >
            START
          </button>
        ) : (
          <button 
            className="timer-btn pause-btn"
            onClick={stopTimer}
          >
            PAUSE
          </button>
        )}
        <button 
          className="timer-btn reset-btn"
          onClick={resetTimer}
        >
          RESET
        </button>
      </div>
      
      {/* Timer adjustment buttons - hide in minimal mode */}
      {!isMinimal && (
        <div className="timer-adjustments">
          <div className="adjustment-group">
            <span className="adjustment-label">Hours:</span>
            <button 
              className="adjustment-btn"
              onClick={() => adjustTimer(3600000)} // +1 hour
              title="Add 1 hour"
            >
              H+
            </button>
            <button 
              className="adjustment-btn"
              onClick={() => adjustTimer(-3600000)} // -1 hour
              title="Remove 1 hour"
            >
              H-
            </button>
          </div>
          
          <div className="adjustment-group">
            <span className="adjustment-label">Minutes:</span>
            <button 
              className="adjustment-btn"
              onClick={() => adjustTimer(60000)} // +1 minute
              title="Add 1 minute"
            >
              M+
            </button>
            <button 
              className="adjustment-btn"
              onClick={() => adjustTimer(-60000)} // -1 minute
              title="Remove 1 minute"
            >
              M-
            </button>
          </div>
          
          <div className="adjustment-group">
            <span className="adjustment-label">Seconds:</span>
            <button 
              className="adjustment-btn"
              onClick={() => adjustTimer(1000)} // +1 second
              title="Add 1 second"
            >
              S+
            </button>
            <button 
              className="adjustment-btn"
              onClick={() => adjustTimer(-1000)} // -1 second
              title="Remove 1 second"
            >
              S-
            </button>
          </div>
        </div>
      )}
    </div>
  );
}; 