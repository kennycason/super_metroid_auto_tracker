export interface GameState {
    health: number;
    max_health: number;
    missiles: number;
    max_missiles: number;
    supers: number;
    max_supers: number;
    power_bombs: number;
    max_power_bombs: number;
    reserve_energy: number;
    max_reserve_energy: number;
    room_id: number;
    area_id: number;
    area_name: string;
    game_state: number;
    player_x: number;
    player_y: number;
    items: Record<string, boolean>;
    beams: Record<string, boolean>;
    bosses: Record<string, boolean>;
}
export interface Items {
    morph: boolean;
    bombs: boolean;
    varia: boolean;
    gravity: boolean;
    hijump: boolean;
    speed: boolean;
    space: boolean;
    screw: boolean;
    spring: boolean;
    grapple: boolean;
    xray: boolean;
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
    bomb_torizo: boolean;
    kraid: boolean;
    spore_spawn: boolean;
    mother_brain: boolean;
    crocomire: boolean;
    phantoon: boolean;
    botwoon: boolean;
    draygon: boolean;
    ridley: boolean;
    golden_torizo: boolean;
    mother_brain_1: boolean;
    mother_brain_2: boolean;
    samus_ship: boolean;
}
export interface ConnectionInfo {
    connected: boolean;
    gameLoaded: boolean;
    retroarchVersion?: string;
    gameInfo?: string;
}
export interface ServerStatus {
    connected: boolean;
    gameLoaded: boolean;
    retroarchVersion?: string;
    gameInfo?: string;
    stats: GameState;
    lastUpdate: number;
    pollCount: number;
    errorCount: number;
}
export interface MemoryData {
    [key: string]: Uint8Array | null;
}
//# sourceMappingURL=types.d.ts.map