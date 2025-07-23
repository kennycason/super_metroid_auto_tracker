import React, { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import type { GameState } from '../types/gameState';
import type { TrackerConfig } from '../types/config';
import { useGameState } from '../hooks/useGameState';
import { defaultConfig } from '../config/defaultConfig';

interface SuperMetroidContextType {
  // Game state
  gameState: GameState;
  isPolling: boolean;
  
  // Timer controls
  startTimer: () => void;
  stopTimer: () => void;
  resetTimer: () => void;
  
  // Polling controls
  startPolling: () => void;
  stopPolling: () => void;
  fetchGameState: () => void;
  
  // Configuration
  config: TrackerConfig;
  setConfig: (config: TrackerConfig) => void;
  
  // UI state
  isFullscreen: boolean;
  setIsFullscreen: (fullscreen: boolean) => void;
  
  // Utility functions
  formatTime: (ms: number) => string;
  isItemCollected: (itemId: string) => boolean;
  isBossDefeated: (bossId: string) => boolean;
}

const SuperMetroidContext = createContext<SuperMetroidContextType | undefined>(undefined);

interface SuperMetroidProviderProps {
  children: ReactNode;
}

export const SuperMetroidProvider: React.FC<SuperMetroidProviderProps> = ({ children }) => {
  const gameStateHook = useGameState();
  const [config, setConfig] = useState<TrackerConfig>(defaultConfig);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // Utility function to format time in MM:SS.mmm format
  const formatTime = (ms: number): string => {
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    const milliseconds = Math.floor((ms % 1000) / 10); // Show only 2 decimal places
    
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${milliseconds.toString().padStart(2, '0')}`;
  };

  // Check if an item is collected based on the game state
  const isItemCollected = (itemId: string): boolean => {
    const { stats } = gameStateHook.gameState;
    
    switch (itemId) {
      // Major items (from stats.items)
      case 'morph': return stats.items?.morph || false;
      case 'bombs': return stats.items?.bombs || false;
      case 'varia': return stats.items?.varia || false;
      case 'gravity': return stats.items?.gravity || false;
      case 'hi_jump': return stats.items?.hijump || false;
      case 'speed_booster': return stats.items?.speed || false;
      case 'space_jump': return stats.items?.space || false;
      case 'screw_attack': return stats.items?.screw || false;
      case 'grapple': return stats.items?.grapple || false;
      case 'x_ray': return stats.items?.xray || false;
      case 'spring': return stats.items?.spring || false;
      
      // Beams (from stats.beams)
      case 'charge': return stats.beams?.charge || false;
      case 'ice': return stats.beams?.ice || false;
      case 'wave': return stats.beams?.wave || false;
      case 'spazer': return stats.beams?.spazer || false;
      case 'plasma': return stats.beams?.plasma || false;
      
      // Expansion items (special handling)
      case 'energy_tank': return (stats.max_health || 99) > 99;
      case 'missile_tank': return (stats.max_missiles || 0) > 0;
      case 'super_tank': return (stats.max_supers || 0) > 0;
      case 'power_bomb_tank': return (stats.max_power_bombs || 0) > 0;
      case 'reserve_tank': return (stats.max_reserve_energy || 0) > 0;
      
      default: return false;
    }
  };

  // Check if a boss is defeated based on the game state
  const isBossDefeated = (bossId: string): boolean => {
    const { stats } = gameStateHook.gameState;
    
    switch (bossId) {
      case 'kraid': return stats.bosses?.kraid || false;
      case 'phantoon': return stats.bosses?.phantoon || false;
      case 'draygon': return stats.bosses?.draygon || false;
      case 'ridley': return stats.bosses?.ridley || false;
      case 'spore_spawn': return stats.bosses?.spore_spawn || false;
      case 'crocomire': return stats.bosses?.crocomire || false;
      case 'botwoon': return stats.bosses?.botwoon || false;
      case 'golden_torizo': return stats.bosses?.golden_torizo || false;
      case 'bomb_torizo': return stats.bosses?.bomb_torizo || false;
      case 'mb1': return stats.bosses?.mb1 || false;
      case 'mb2': return stats.bosses?.mb2 || false;
      case 'main': return stats.bosses?.main || false;
      default: return false;
    }
  };

  const contextValue: SuperMetroidContextType = {
    // Game state
    gameState: gameStateHook.gameState,
    isPolling: gameStateHook.isPolling,
    
    // Timer controls
    startTimer: gameStateHook.startTimer,
    stopTimer: gameStateHook.stopTimer,
    resetTimer: gameStateHook.resetTimer,
    
    // Polling controls
    startPolling: gameStateHook.startPolling,
    stopPolling: gameStateHook.stopPolling,
    fetchGameState: gameStateHook.fetchGameState,
    
    // Configuration
    config,
    setConfig,
    
    // UI state
    isFullscreen,
    setIsFullscreen,
    
    // Utility functions
    formatTime,
    isItemCollected,
    isBossDefeated,
  };

  return (
    <SuperMetroidContext.Provider value={contextValue}>
      {children}
    </SuperMetroidContext.Provider>
  );
};

// Custom hook to use the SuperMetroid context
export const useSuperMetroid = (): SuperMetroidContextType => {
  const context = useContext(SuperMetroidContext);
  if (context === undefined) {
    throw new Error('useSuperMetroid must be used within a SuperMetroidProvider');
  }
  return context;
}; 