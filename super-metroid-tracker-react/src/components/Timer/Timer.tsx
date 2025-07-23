import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Timer.css';

export const Timer: React.FC = () => {
  const { gameState, startTimer, stopTimer, resetTimer, formatTime } = useSuperMetroid();
  
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
    </div>
  );
}; 