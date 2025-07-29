export interface GameStats {
  health: number;
  max_health: number;
  missiles: number;
  max_missiles: number;
  supers: number;
  max_supers: number;
  power_bombs: number;
  max_power_bombs: number;
  room_id: number;
  area_id: number;
  area_name: string;
  game_state: number;
  player_x: number;
  player_y: number;
  max_reserve_energy: number;
  reserve_energy: number;
  items: {
    morph: boolean;
    bombs: boolean;
    varia: boolean;
    gravity: boolean;
    hijump: boolean;
    speed: boolean;
    space: boolean;
    screw: boolean;
    grapple: boolean;
    xray: boolean;
    spring: boolean;
  };
  beams: Beams;
  bosses: Bosses;
}

export interface Items {
  morph: boolean;
  bombs: boolean;
  varia: boolean;
  gravity: boolean;
  hi_jump: boolean;
  speed_booster: boolean;
  space_jump: boolean;
  screw_attack: boolean;
  grapple: boolean;
  x_ray: boolean;
}

export interface Beams {
  charge: boolean;
  ice: boolean;
  wave: boolean;
  spazer: boolean;
  plasma: boolean;
  hyper: boolean;
}

export interface Bosses {
  kraid: boolean;
  phantoon: boolean;
  draygon: boolean;
  ridley: boolean;
  spore_spawn: boolean;
  crocomire: boolean;
  botwoon: boolean;
  golden_torizo: boolean;
  bomb_torizo: boolean;
  main: boolean;
  mb1: boolean;
  mb2: boolean;
}

export interface LocationData {
  area_id: number;
  room_id: number;
  area_name: string;
  room_name: string;
  player_x: number;
  player_y: number;
}

export interface Split {
  event: string;
  time: number;
  timestamp: number;
}

export interface GameState {
  stats: GameStats;
  items: Items;
  beams: Beams;
  bosses: Bosses;
  location: LocationData;
  splits: Split[];
  timer: {
    running: boolean;
    elapsed: number;
    startTime: number | null;
  };
  connected: boolean;
  lastUpdate: number;
}

export interface TimerState {
  running: boolean;
  elapsed: number;
  startTime: number | null;
} 