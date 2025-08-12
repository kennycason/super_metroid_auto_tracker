import React from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './Location.css';

export const Location: React.FC = () => {
  const { gameState } = useSuperMetroid();
  
  const { location } = gameState;

  return (
    <div className="location-section">
      <div className="location-display">
        <span className="area-name">{location.area_name}</span>
        {location.room_name && location.room_name !== 'Unknown' && (
          <>
            <span className="separator">: </span>
            <span className="room-name">Room {location.room_id}</span>
          </>
        )}
      </div>
    </div>
  );
}; 