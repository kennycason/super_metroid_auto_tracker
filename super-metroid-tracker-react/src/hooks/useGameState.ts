import { useState, useEffect, useCallback } from 'react';
import type { GameState } from '../types/gameState';

const BACKEND_URL = 'http://localhost:8000';
const POLL_INTERVAL = 1000; // 1 second

const initialGameState: GameState = {
  stats: {
    health: 99,
    max_health: 99,
    missiles: 0,
    max_missiles: 0,
    supers: 0,
    max_supers: 0,
    power_bombs: 0,
    max_power_bombs: 0,
    room_id: 0,
    area_id: 0,
    area_name: 'Crateria',
    game_state: 0,
    player_x: 0,
    player_y: 0,
    max_reserve_energy: 0,
    reserve_energy: 0,
    items: {
      morph: false,
      bombs: false,
      varia: false,
      gravity: false,
      hijump: false,
      speed: false,
      space: false,
      screw: false,
      grapple: false,
      xray: false,
      spring: false,
    },
    beams: {
      charge: false,
      ice: false,
      wave: false,
      spazer: false,
      plasma: false,
      hyper: false,
    },
    bosses: {
      kraid: false,
      phantoon: false,
      draygon: false,
      ridley: false,
      spore_spawn: false,
      crocomire: false,
      botwoon: false,
      golden_torizo: false,
      bomb_torizo: false,
      main: false,
      mb1: false,
      mb2: false,
    },
  },
  items: {
    morph: false,
    bombs: false,
    varia: false,
    gravity: false,
    hi_jump: false,
    speed_booster: false,
    space_jump: false,
    screw_attack: false,
    grapple: false,
    x_ray: false,
  },
  beams: {
    charge: false,
    ice: false,
    wave: false,
    spazer: false,
    plasma: false,
    hyper: false,
  },
  bosses: {
    kraid: false,
    phantoon: false,
    draygon: false,
    ridley: false,
    spore_spawn: false,
    crocomire: false,
    botwoon: false,
    golden_torizo: false,
    bomb_torizo: false,
    main: false,
    mb1: false,
    mb2: false,
  },
  location: {
    area_id: 0,
    room_id: 0,
    area_name: 'Crateria',
    room_name: 'Unknown',
    player_x: 0,
    player_y: 0,
  },
  splits: [],
  timer: {
    running: false,
    elapsed: 0,
    startTime: null,
  },
  connected: false,
  lastUpdate: 0,
};

export const useGameState = () => {
  const [gameState, setGameState] = useState<GameState>(initialGameState);
  const [isPolling, setIsPolling] = useState(false);

  const fetchGameState = useCallback(async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/api/status`);
      if (response.ok) {
        const data = await response.json();
        
        // Transform the backend data to match our GameState interface
        setGameState(prev => ({
          stats: {
            health: data.stats?.health || 99,
            max_health: data.stats?.max_health || 99,
            missiles: data.stats?.missiles || 0,
            max_missiles: data.stats?.max_missiles || 0,
            supers: data.stats?.supers || 0,
            max_supers: data.stats?.max_supers || 0,
            power_bombs: data.stats?.power_bombs || 0,
            max_power_bombs: data.stats?.max_power_bombs || 0,
            room_id: data.stats?.room_id || 0,
            area_id: data.stats?.area_id || 0,
            area_name: data.stats?.area_name || 'Crateria',
            game_state: data.stats?.game_state || 0,
            player_x: data.stats?.player_x || 0,
            player_y: data.stats?.player_y || 0,
            max_reserve_energy: data.stats?.max_reserve_energy || 0,
            reserve_energy: data.stats?.reserve_energy || 0,
            items: {
              morph: data.stats?.items?.morph || false,
              bombs: data.stats?.items?.bombs || false,
              varia: data.stats?.items?.varia || false,
              gravity: data.stats?.items?.gravity || false,
              hijump: data.stats?.items?.hijump || false,
              speed: data.stats?.items?.speed || false,
              space: data.stats?.items?.space || false,
              screw: data.stats?.items?.screw || false,
              grapple: data.stats?.items?.grapple || false,
              xray: data.stats?.items?.xray || false,
              spring: data.stats?.items?.spring || false,
            },
            beams: {
              charge: data.stats?.beams?.charge || false,
              ice: data.stats?.beams?.ice || false,
              wave: data.stats?.beams?.wave || false,
              spazer: data.stats?.beams?.spazer || false,
              plasma: data.stats?.beams?.plasma || false,
              hyper: data.stats?.beams?.hyper || false,
            },
            bosses: {
              kraid: data.stats?.bosses?.kraid || false,
              phantoon: data.stats?.bosses?.phantoon || false,
              draygon: data.stats?.bosses?.draygon || false,
              ridley: data.stats?.bosses?.ridley || false,
              spore_spawn: data.stats?.bosses?.spore_spawn || false,
              crocomire: data.stats?.bosses?.crocomire || false,
              botwoon: data.stats?.bosses?.botwoon || false,
              golden_torizo: data.stats?.bosses?.golden_torizo || false,
              bomb_torizo: data.stats?.bosses?.bomb_torizo || false,
              main: data.stats?.bosses?.samus_ship || false,
              mb1: data.stats?.bosses?.mother_brain_1 || false,
              mb2: data.stats?.bosses?.mother_brain_2 || false,
            },
          },
          items: {
            morph: data.stats?.items?.morph || false,
            bombs: data.stats?.items?.bombs || false,
            varia: data.stats?.items?.varia || false,
            gravity: data.stats?.items?.gravity || false,
            hi_jump: data.stats?.items?.hijump || false,
            speed_booster: data.stats?.items?.speed || false,
            space_jump: data.stats?.items?.space || false,
            screw_attack: data.stats?.items?.screw || false,
            grapple: data.stats?.items?.grapple || false,
            x_ray: data.stats?.items?.xray || false,
          },
          beams: {
            charge: data.stats?.beams?.charge || false,
            ice: data.stats?.beams?.ice || false,
            wave: data.stats?.beams?.wave || false,
            spazer: data.stats?.beams?.spazer || false,
            plasma: data.stats?.beams?.plasma || false,
            hyper: data.stats?.beams?.hyper || false,
          },
          bosses: {
            kraid: data.stats?.bosses?.kraid || false,
            phantoon: data.stats?.bosses?.phantoon || false,
            draygon: data.stats?.bosses?.draygon || false,
            ridley: data.stats?.bosses?.ridley || false,
            spore_spawn: data.stats?.bosses?.spore_spawn || false,
            crocomire: data.stats?.bosses?.crocomire || false,
            botwoon: data.stats?.bosses?.botwoon || false,
            golden_torizo: data.stats?.bosses?.golden_torizo || false,
            bomb_torizo: data.stats?.bosses?.bomb_torizo || false,
            main: data.stats?.bosses?.samus_ship || false,
            mb1: data.stats?.bosses?.mother_brain_1 || false,
            mb2: data.stats?.bosses?.mother_brain_2 || false,
          },
          location: {
            area_id: data.stats?.area_id || 0,
            room_id: data.stats?.room_id || 0,
            area_name: data.stats?.area_name || 'Crateria',
            room_name: data.stats?.room_name || 'Unknown',
            player_x: data.stats?.player_x || 0,
            player_y: data.stats?.player_y || 0,
          },
          splits: prev.splits, // Keep existing splits state - backend doesn't provide this yet
          timer: prev.timer, // Keep existing timer state
          connected: true,
          lastUpdate: Date.now(),
        }));
      } else {
        // Connection failed
        setGameState(prev => ({ ...prev, connected: false }));
      }
    } catch (error) {
      console.error('Failed to fetch game state:', error);
      setGameState(prev => ({ ...prev, connected: false }));
    }
  }, []); // Removed gameState.timer dependency to fix polling

  const startPolling = useCallback(() => {
    setIsPolling(true);
  }, []);

  const stopPolling = useCallback(() => {
    setIsPolling(false);
  }, []);

  // Timer functions
  const startTimer = useCallback(() => {
    setGameState(prev => ({
      ...prev,
      timer: {
        running: true,
        elapsed: prev.timer.elapsed,
        startTime: Date.now() - prev.timer.elapsed,
      }
    }));
  }, []);

  const stopTimer = useCallback(() => {
    setGameState(prev => ({
      ...prev,
      timer: {
        running: false,
        elapsed: prev.timer.elapsed,
        startTime: null,
      }
    }));
  }, []);

  const resetTimer = useCallback(() => {
    setGameState(prev => ({
      ...prev,
      timer: {
        running: false,
        elapsed: 0,
        startTime: null,
      },
      splits: [], // Clear splits when timer resets
    }));
  }, []);

  // Effect for polling
  useEffect(() => {
    if (!isPolling) return;

    const interval = setInterval(fetchGameState, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [isPolling, fetchGameState]);

  // Effect for timer updates
  useEffect(() => {
    if (!gameState.timer.running || !gameState.timer.startTime) return;

    const interval = setInterval(() => {
      setGameState(prev => ({
        ...prev,
        timer: {
          ...prev.timer,
          elapsed: Date.now() - (prev.timer.startTime || 0),
        }
      }));
    }, 100); // Update every 100ms for smooth timer

    return () => clearInterval(interval);
  }, [gameState.timer.running, gameState.timer.startTime]);

  // Auto-start polling when hook is used
  useEffect(() => {
    startPolling();
    return () => stopPolling();
  }, [startPolling, stopPolling]);

  return {
    gameState,
    isPolling,
    startPolling,
    stopPolling,
    startTimer,
    stopTimer,
    resetTimer,
    fetchGameState,
  };
}; 