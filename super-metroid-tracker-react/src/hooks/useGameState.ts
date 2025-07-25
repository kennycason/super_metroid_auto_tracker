import { useState, useEffect, useCallback } from 'react';
import type { GameState } from '../types/gameState';

const BACKEND_URL = 'http://localhost:8081'; // CONSISTENT PORT - no more hopping!
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
        
        // Transform the Kotlin server data to match our GameState interface
        setGameState(prev => ({
          stats: {
            health: data.stats?.health || 99,
            max_health: data.stats?.maxHealth || 99,
            missiles: data.stats?.missiles || 0,
            max_missiles: data.stats?.maxMissiles || 0,
            supers: data.stats?.supers || 0,
            max_supers: data.stats?.maxSupers || 0,
            power_bombs: data.stats?.powerBombs || 0,
            max_power_bombs: data.stats?.maxPowerBombs || 0,
            room_id: data.stats?.roomId || 0,
            area_id: data.stats?.areaId || 0,
            area_name: data.stats?.areaName || 'Crateria',
            game_state: data.stats?.gameState || 0,
            player_x: data.stats?.playerX || 0,
            player_y: data.stats?.playerY || 0,
            max_reserve_energy: data.stats?.maxReserveEnergy || 0,
            reserve_energy: data.stats?.reserveEnergy || 0,
            items: {
              morph: data.stats?.items?.morph || false,
              bombs: data.stats?.items?.bombs || false,
              varia: data.stats?.items?.varia || false,
              gravity: data.stats?.items?.gravity || false,
              hijump: data.stats?.items?.hijump || false,
              speed: data.stats?.items?.speed || false,
              space: data.stats?.items?.spacejump || false,
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
            space_jump: data.stats?.items?.spacejump || false,
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
            area_id: data.stats?.areaId || 0,
            room_id: data.stats?.roomId || 0,
            area_name: data.stats?.areaName || 'Crateria',
            room_name: data.stats?.room_name || 'Unknown',
            player_x: data.stats?.playerX || 0,
            player_y: data.stats?.playerY || 0,
          },
          splits: prev.splits, // Keep existing splits state - backend doesn't provide this yet
          timer: prev.timer, // Keep existing timer state
          connected: data.connected || false,
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
    
    // Clear tracking state so splits can be detected again
    setLastTrackedBosses({});
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

  // Auto-pause timer when ship status becomes true (game completed)
  useEffect(() => {
    const isShipCompleted = gameState.stats.bosses.main;
    
    // If ship is completed and timer is running, auto-pause
    if (isShipCompleted && gameState.timer.running) {
      console.log('🚢 Game completed! Auto-pausing timer...');
      stopTimer();
    }
  }, [gameState.stats.bosses.main, gameState.timer.running, stopTimer]);

  // Track previous state for splits detection
  const [lastTrackedBosses, setLastTrackedBosses] = useState<Record<string, boolean>>({});

  // Add split functionality (based on original HTML implementation)
  const addSplit = useCallback((eventName: string) => {
    if (!gameState.timer.running) return;
    
    const currentTime = gameState.timer.elapsed;
    const split = {
      event: eventName,
      time: currentTime,
      timestamp: Date.now()
    };
    
    setGameState(prev => ({
      ...prev,
      splits: [...prev.splits, split]
    }));
    
    console.log(`⭐ Split: ${eventName} at ${Math.floor(currentTime / 60000)}:${String(Math.floor((currentTime % 60000) / 1000)).padStart(2, '0')}.${String(Math.floor((currentTime % 1000) / 100))}`);
  }, [gameState.timer.running, gameState.timer.elapsed]);

  // Check for new splits (based on original HTML logic)
  useEffect(() => {
    if (!gameState.timer.running) return;

    // Check for new bosses (only bosses as per user requirement)
    const trackableBosses = ['bomb_torizo', 'spore_spawn', 'kraid', 'crocomire', 'phantoon', 'botwoon', 'draygon', 'golden_torizo', 'ridley', 'mb1', 'mb2'];
    
    const bossNames: Record<string, string> = {
      'bomb_torizo': 'Bomb Torizo',
      'spore_spawn': 'Spore Spawn', 
      'kraid': 'Kraid',
      'crocomire': 'Crocomire',
      'phantoon': 'Phantoon',
      'botwoon': 'Botwoon',
      'draygon': 'Draygon',
      'golden_torizo': 'Golden Torizo',
      'ridley': 'Ridley',
      'mb1': 'Mother Brain 1',
      'mb2': 'Mother Brain 2'
    };

    // Check for newly defeated bosses
    for (const boss of trackableBosses) {
      const currentState = (gameState.stats.bosses as any)[boss];
      if (currentState && !lastTrackedBosses[boss]) {
        addSplit(bossNames[boss] || boss);
      }
    }

    // Check for ship completion (game complete)
    if (gameState.stats.bosses.main && !lastTrackedBosses['main']) {
      addSplit('GAME COMPLETE!');
    }

    // Update tracking state
    const newTrackedBosses: Record<string, boolean> = {};
    for (const boss of trackableBosses) {
      newTrackedBosses[boss] = (gameState.stats.bosses as any)[boss] || false;
    }
    newTrackedBosses['main'] = gameState.stats.bosses.main || false;
    
    setLastTrackedBosses(newTrackedBosses);
  }, [
    gameState.stats.bosses, 
    gameState.timer.running, 
    lastTrackedBosses, 
    addSplit
  ]);

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