import { useState, useEffect, useCallback, useRef } from 'react';
import type { GameState } from '../types/gameState';

const POLL_INTERVAL = 1000; // 1 second
const TIMER_STORAGE_KEY = 'super-metroid-timer';

// Load timer state from localStorage
const loadTimerState = () => {
  try {
    const stored = localStorage.getItem(TIMER_STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        running: false, // Always start paused to prevent auto-start
        elapsed: parsed.elapsed || 0,
        startTime: null,
      };
    }
  } catch (error) {
    console.warn('Failed to load timer state from localStorage:', error);
  }
  return {
    running: false,
    elapsed: 0,
    startTime: null,
  };
};

// Save timer state to localStorage
const saveTimerState = (timer: { running: boolean; elapsed: number; startTime: number | null }) => {
  try {
    localStorage.setItem(TIMER_STORAGE_KEY, JSON.stringify({
      elapsed: timer.elapsed,
      // Don't save running state or startTime - always resume paused
    }));
  } catch (error) {
    console.warn('Failed to save timer state to localStorage:', error);
  }
};

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

export const useGameState = (serverPort: number = 8081) => {
  const BACKEND_URL = `http://localhost:${serverPort}`;

  const [gameState, setGameState] = useState<GameState>(() => {
    // Initialize only once, even in StrictMode
    const initialTimer = loadTimerState();
    return {
      ...initialGameState,
      timer: initialTimer,
    };
  });
  const [isPolling, setIsPolling] = useState(false);

  // Use refs to track timer state and prevent stale closures
  const timerIntervalRef = useRef<number | null>(null);
  const isTimerMountedRef = useRef(true);
  const hasInitialized = useRef(false);
  const timerStartTimeRef = useRef<number | null>(null);
  const timerRunningRef = useRef(false);

  const fetchGameState = useCallback(async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/api/status`);
      if (response.ok) {
        const data = await response.json();

        // Transform the Kotlin server data to match our GameState interface
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
              spring: data.stats?.items?.spring || false,
              grapple: data.stats?.items?.grapple || false,
              xray: data.stats?.items?.xray || false,
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
              crocomire: data.stats?.bosses?.crocomire || false,
              spore_spawn: data.stats?.bosses?.spore_spawn || false,
              botwoon: data.stats?.bosses?.botwoon || false,
              golden_torizo: data.stats?.bosses?.golden_torizo || false,
              bomb_torizo: data.stats?.bosses?.bomb_torizo || false,
              mb1: data.stats?.bosses?.mother_brain_1 || false,
              mb2: data.stats?.bosses?.mother_brain_2 || false,
              main: data.stats?.bosses?.mother_brain || false,
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
            main: data.stats?.bosses?.mother_brain || false,
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
          timer: {
            ...prev.timer, // Preserve all timer state completely
            // Ensure timer state is never accidentally reset by backend data
          },
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
  }, [BACKEND_URL]); // Removed gameState.timer dependency to fix polling

  const startPolling = useCallback(() => {
    setIsPolling(true);
  }, []);

  const stopPolling = useCallback(() => {
    setIsPolling(false);
  }, []);

  // Timer functions
  const startTimer = useCallback(() => {
    console.log('⏰ Starting timer...');
    setGameState(prev => {
      // Prevent double-starting
      if (prev.timer.running) {
        console.log('Timer already running, ignoring start command');
        return prev;
      }

      const startTime = Date.now() - prev.timer.elapsed;
      const newTimer = {
        running: true,
        elapsed: prev.timer.elapsed,
        startTime,
      };

      // Update refs for interval
      timerRunningRef.current = true;
      timerStartTimeRef.current = startTime;

      saveTimerState(newTimer);
      return {
        ...prev,
        timer: newTimer,
      };
    });
  }, []);

  const stopTimer = useCallback(() => {
    console.log('⏸️ Stopping timer...');
    setGameState(prev => {
      // Prevent double-stopping
      if (!prev.timer.running) {
        console.log('Timer already stopped, ignoring stop command');
        return prev;
      }

      const newTimer = {
        running: false,
        elapsed: prev.timer.elapsed,
        startTime: null,
      };

      // Update refs for interval
      timerRunningRef.current = false;
      timerStartTimeRef.current = null;

      saveTimerState(newTimer);
      return {
        ...prev,
        timer: newTimer,
      };
    });
  }, []);

  const resetTimer = useCallback(() => {
    console.log('🔄 Resetting timer and clearing localStorage...');
    // Clear localStorage
    try {
      localStorage.removeItem(TIMER_STORAGE_KEY);
    } catch (error) {
      console.warn('Failed to clear timer from localStorage:', error);
    }

    // Update refs for interval
    timerRunningRef.current = false;
    timerStartTimeRef.current = null;

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
    // Disable splits until timer starts again
    setSplitsEnabled(false);
    // Reset manual adjustments flag
    setHasManualAdjustments(false);
  }, []);

  const adjustTimer = useCallback((adjustment: number) => {
    // Mark that manual adjustments have been made
    setHasManualAdjustments(true);

    setGameState(prev => {
      const newElapsed = Math.max(0, prev.timer.elapsed + adjustment);
      const newStartTime = prev.timer.running ? Date.now() - newElapsed : prev.timer.startTime;

      const newTimer = {
        ...prev.timer,
        elapsed: newElapsed,
        startTime: newStartTime,
      };

      // Update refs immediately to prevent state inconsistencies
      if (prev.timer.running && newStartTime) {
        timerStartTimeRef.current = newStartTime;
      }

      saveTimerState(newTimer);
      return {
        ...prev,
        timer: newTimer,
      };
    });
  }, []);

  // Effect for polling
  useEffect(() => {
    if (!isPolling) return;

    const interval = setInterval(fetchGameState, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [isPolling, fetchGameState]);

  // Effect for timer updates - use refs to prevent restart loops
  useEffect(() => {
    // Start a single interval that runs continuously
    if (timerIntervalRef.current) return; // Already running

    timerIntervalRef.current = window.setInterval(() => {
      if (!isTimerMountedRef.current) return;
      if (!timerRunningRef.current || !timerStartTimeRef.current) return;

      setGameState(prev => {
        // Use refs for timer checks to avoid stale state
        if (!timerRunningRef.current || !timerStartTimeRef.current) return prev;

        const newElapsed = Date.now() - timerStartTimeRef.current;
        const newTimer = {
          ...prev.timer,
          elapsed: newElapsed,
        };

        // Save to localStorage every second (reduce I/O)
        if (Math.floor(newElapsed / 1000) !== Math.floor(prev.timer.elapsed / 1000)) {
          saveTimerState(newTimer);
        }

        return {
          ...prev,
          timer: newTimer,
        };
      });
    }, 50); // Update every 50ms for smooth timer

    return () => {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
        timerIntervalRef.current = null;
      }
    };
  }, []); // No dependencies - run once and let refs control behavior

  // Sync refs with timer state only when running state changes (not elapsed time)
  useEffect(() => {
    timerRunningRef.current = gameState.timer.running;
    if (gameState.timer.startTime !== timerStartTimeRef.current) {
      timerStartTimeRef.current = gameState.timer.startTime;
    }
  }, [gameState.timer.running]); // Only depend on running state

  // Mount/unmount tracking
  useEffect(() => {
    isTimerMountedRef.current = true;

    return () => {
      isTimerMountedRef.current = false;
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
        timerIntervalRef.current = null;
      }
    };
  }, []);

  // Track previous state for splits detection
  const [lastTrackedBosses, setLastTrackedBosses] = useState<Record<string, boolean>>({});
  const [isInitialized, setIsInitialized] = useState(false);
  const [splitsEnabled, setSplitsEnabled] = useState(false); // Prevent splits during initialization
  const [hasManualAdjustments, setHasManualAdjustments] = useState(false); // Track if timer has been manually adjusted

  // Auto-pause timer when ship status becomes true (game completed)
  // Made more robust to prevent false positives - requires multiple conditions
  useEffect(() => {
    const isShipCompleted = gameState.stats.bosses.main;
    const timerHasBeenRunning = gameState.timer.elapsed > 300000; // 5 minutes minimum
    const hasMultipleBossesDefeated = Object.values(gameState.stats.bosses).filter(Boolean).length >= 4;

    // Only auto-pause if ALL conditions are met:
    // 1. Ship is completed
    // 2. Timer has been running for at least 5 minutes (not 10 seconds)
    // 3. Multiple bosses have been defeated (indicates real gameplay)
    // 4. Timer is currently running
    // 5. No manual adjustments have been made (to avoid false positives from manual time changes)
    if (isShipCompleted && gameState.timer.running && timerHasBeenRunning && hasMultipleBossesDefeated && !hasManualAdjustments) {
      console.log('🚢 Game completed! Auto-pausing timer... (5+ min runtime, multiple bosses defeated, no manual adjustments)');
      stopTimer();
    }
  }, [gameState.stats.bosses.main, gameState.timer.running, gameState.timer.elapsed, stopTimer, hasManualAdjustments]);

  // Add split functionality (based on original HTML implementation)
  const addSplit = useCallback((eventName: string) => {
    // Only add splits if timer is running, splits are enabled, and timer has been running for at least 1 second
    if (!gameState.timer.running || !splitsEnabled || gameState.timer.elapsed < 1000) return;

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

    // Format time consistently with new HH:MM:SS.ss format
    const totalSeconds = Math.floor(currentTime / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const milliseconds = Math.floor((currentTime % 1000) / 10);
    const timeString = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${milliseconds.toString().padStart(2, '0')}`;

    console.log(`⭐ Split: ${eventName} at ${timeString}`);
  }, [gameState.timer.running, splitsEnabled]);

  // Initialize tracking state on first successful data fetch
  useEffect(() => {
    if (!isInitialized && gameState.connected) {
      const trackableBosses = ['bomb_torizo', 'spore_spawn', 'kraid', 'crocomire', 'phantoon', 'botwoon', 'draygon', 'golden_torizo', 'ridley', 'mb1', 'mb2'];
      const initialTrackedBosses: Record<string, boolean> = {};

      for (const boss of trackableBosses) {
        initialTrackedBosses[boss] = (gameState.stats.bosses as any)[boss] || false;
      }
      initialTrackedBosses['main'] = gameState.stats.bosses.main || false;

      setLastTrackedBosses(initialTrackedBosses);
      setIsInitialized(true);
      console.log('🎯 Boss tracking initialized with current state');
    }
  }, [gameState.connected, isInitialized, gameState.stats.bosses]);

  // Enable splits when timer starts for the first time
  useEffect(() => {
    if (gameState.timer.running && !splitsEnabled) {
      // Only enable splits when timer actually starts running
      setSplitsEnabled(true);
    }
  }, [gameState.timer.running, splitsEnabled]);

  // Check for new splits (based on original HTML logic)
  useEffect(() => {
    if (!gameState.timer.running || !isInitialized || !splitsEnabled) return;

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
    isInitialized,
    splitsEnabled,
    addSplit
    // Note: lastTrackedBosses removed to prevent infinite loop
  ]);

  // Note: Polling is now managed by SuperMetroidContext, not here

  return {
    gameState,
    isPolling,
    startPolling,
    stopPolling,
    startTimer,
    stopTimer,
    resetTimer,
    adjustTimer,
    fetchGameState,
  };
}; 
