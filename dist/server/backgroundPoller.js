"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BackgroundPoller = void 0;
const gameStateParser_1 = require("./gameStateParser");
/**
 * Background game state poller
 * Continuously polls RetroArch and maintains cached game state
 * Equivalent to the Kotlin BackgroundPoller
 */
class BackgroundPoller {
    backend;
    parser;
    updateInterval;
    cache;
    bootstrapAttempted = false;
    pollingInterval = null;
    isPolling = false;
    constructor(backend, updateInterval = 2500) {
        this.updateInterval = updateInterval; // 2.5 seconds
        this.backend = backend;
        this.parser = new gameStateParser_1.GameStateParser();
        this.cache = this.getEmptyServerStatus();
    }
    /**
     * Start the background polling
     */
    async start() {
        if (this.isPolling) {
            console.log('Background poller already running');
            return;
        }
        this.isPolling = true;
        console.log(`Background poller started with ${this.updateInterval}ms interval`);
        // Start the polling loop
        this.pollingInterval = setInterval(async () => {
            await this.pollLoop();
        }, this.updateInterval);
        // Do an initial poll asynchronously (don't block server startup)
        this.pollLoop().catch(err => {
            console.log(`❌ Initial poll error: ${err}`);
        });
    }
    /**
     * Stop the background polling
     */
    stop() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
        this.isPolling = false;
        console.log('Background poller stopped');
    }
    /**
     * Get the cached server status
     */
    getCachedState() {
        return { ...this.cache };
    }
    /**
     * Reset the cache
     */
    resetCache() {
        this.cache = this.getEmptyServerStatus();
        this.bootstrapAttempted = false;
        // Also reset the Mother Brain cache in the parser
        try {
            this.parser.resetMotherBrainCache();
            console.log('🧠 Mother Brain cache reset');
        }
        catch (err) {
            console.log(`⚠️ Failed to reset Mother Brain cache: ${err}`);
        }
        console.log('🔄 Background poller cache fully reset');
    }
    /**
     * Main polling loop
     */
    async pollLoop() {
        try {
            const startTime = Date.now();
            // Get connection info
            console.log(`🔄 Poll: Getting ${this.backend.getBackendType()} info...`);
            const connectionInfo = await this.backend.getConnectionInfo();
            console.log(`🔄 Poll: ${this.backend.getBackendType()} info received: connected=${connectionInfo.connected}, game_loaded=${connectionInfo.gameLoaded}`);
            // Read game state if game is loaded
            let gameState = this.getEmptyGameState();
            if (connectionInfo.gameLoaded) {
                console.log('🔄 Poll: Game is loaded, reading game state...');
                gameState = await this.readGameState();
                console.log(`🔄 Poll: Game state read: health=${gameState.health}, missiles=${gameState.missiles}, area=${gameState.area_name}`);
                // Bootstrap MB cache on first successful game read (if we haven't already)
                if (gameState.health > 0 && !this.bootstrapAttempted) {
                    console.log('🔄 Poll: First successful game read with health > 0, bootstrapping MB cache...');
                    await this.bootstrapMbCacheIfNeeded(gameState);
                    this.bootstrapAttempted = true;
                }
            }
            else {
                console.log('🔄 Poll: Game is not loaded, skipping game state read');
            }
            // Update cache
            console.log('🔄 Poll: Updating cache...');
            this.cache = {
                connected: connectionInfo.connected,
                gameLoaded: connectionInfo.gameLoaded,
                retroarchVersion: connectionInfo.retroarchVersion,
                gameInfo: connectionInfo.gameInfo,
                stats: gameState,
                lastUpdate: Date.now(),
                pollCount: this.cache.pollCount + 1,
                errorCount: this.cache.errorCount
            };
            console.log(`🔄 Poll: Cache updated: connected=${this.cache.connected}, game_loaded=${this.cache.gameLoaded}, poll_count=${this.cache.pollCount}`);
            const pollDuration = Date.now() - startTime;
            // Reduced logging frequency for cleaner output
            if (this.cache.pollCount <= 5 || this.cache.pollCount % 20 === 0) {
                console.log(`✅ Poll #${this.cache.pollCount} completed in ${pollDuration}ms`);
            }
        }
        catch (err) {
            console.log(`❌ Polling error: ${err}`);
            this.cache = {
                ...this.cache,
                errorCount: this.cache.errorCount + 1
            };
            console.log(`🔄 Poll: Error count increased to ${this.cache.errorCount}`);
        }
    }
    /**
     * Read game state from emulator
     */
    async readGameState() {
        try {
            console.log(`🔄 ReadGameState: Reading game state from ${this.backend.getBackendType()}...`);
            // BULK READ: Get all basic stats in one 22-byte read
            console.log('🔄 ReadGameState: Reading basic stats (0x7E09C2, 22 bytes)...');
            const basicStats = await this.backend.readMemoryRange(0x7E09C2, 22);
            if (!basicStats) {
                console.log('❌ ReadGameState: Failed to read basic stats');
                return this.getEmptyGameState();
            }
            console.log(`🔄 ReadGameState: Basic stats read successfully (${basicStats.length} bytes)`);
            // Individual reads for other data
            console.log('🔄 ReadGameState: Reading other memory addresses...');
            const memoryReads = await Promise.all([
                this.backend.readMemoryRange(0x7E079B, 2), // room_id
                this.backend.readMemoryRange(0x7E079F, 1), // area_id
                this.backend.readMemoryRange(0x7E0998, 2), // game_state
                this.backend.readMemoryRange(0x7E0AF6, 2), // player_x
                this.backend.readMemoryRange(0x7E0AFA, 2), // player_y
                this.backend.readMemoryRange(0x7E09A4, 2), // items
                this.backend.readMemoryRange(0x7E09A8, 2), // beams
                this.backend.readMemoryRange(0x7ED828, 2), // main_bosses
                this.backend.readMemoryRange(0x7ED829, 2), // crocomire
                this.backend.readMemoryRange(0x7ED829, 2), // boss_plus_1
                this.backend.readMemoryRange(0x7ED82A, 2), // boss_plus_2
                this.backend.readMemoryRange(0x7ED82B, 2), // boss_plus_3
                this.backend.readMemoryRange(0x7ED82C, 2), // boss_plus_4
                this.backend.readMemoryRange(0x7ED82D, 2), // boss_plus_5
                this.backend.readMemoryRange(0x7E0943, 2), // escape_timer_1
                this.backend.readMemoryRange(0x7E0945, 2), // escape_timer_2
                this.backend.readMemoryRange(0x7E09E2, 2), // escape_timer_3
                this.backend.readMemoryRange(0x7E09E0, 2), // escape_timer_4
                this.backend.readMemoryRange(0x7E0FB2, 2), // ship_ai
                this.backend.readMemoryRange(0x7ED821, 2), // event_flags
            ]);
            // Create memory data map
            console.log('🔄 ReadGameState: Creating memory data map...');
            const memoryData = {
                basic_stats: basicStats,
                room_id: memoryReads[0],
                area_id: memoryReads[1],
                game_state: memoryReads[2],
                player_x: memoryReads[3],
                player_y: memoryReads[4],
                items: memoryReads[5],
                beams: memoryReads[6],
                main_bosses: memoryReads[7],
                crocomire: memoryReads[8],
                boss_plus_1: memoryReads[9],
                boss_plus_2: memoryReads[10],
                boss_plus_3: memoryReads[11],
                boss_plus_4: memoryReads[12],
                boss_plus_5: memoryReads[13],
                escape_timer_1: memoryReads[14],
                escape_timer_2: memoryReads[15],
                escape_timer_3: memoryReads[16],
                escape_timer_4: memoryReads[17],
                ship_ai: memoryReads[18],
                event_flags: memoryReads[19]
            };
            // Filter out null values
            const filteredMemoryData = {};
            for (const [key, value] of Object.entries(memoryData)) {
                if (value !== null) {
                    filteredMemoryData[key] = value;
                }
            }
            console.log(`🔄 ReadGameState: Memory data map created with ${Object.keys(filteredMemoryData).length} entries`);
            // Parse game state
            console.log('🔄 ReadGameState: Parsing game state...');
            const parsedGameState = this.parser.parseCompleteGameState(filteredMemoryData);
            console.log('🔄 ReadGameState: Game state parsed successfully');
            return parsedGameState;
        }
        catch (err) {
            console.log(`❌ ReadGameState Error: ${err}`);
            return this.getEmptyGameState();
        }
    }
    /**
     * Bootstrap Mother Brain cache if needed
     */
    async bootstrapMbCacheIfNeeded(gameState) {
        try {
            const health = gameState.health;
            const maxHealth = gameState.max_health;
            const missiles = gameState.missiles;
            // Don't bootstrap if it looks like a new file
            if (health <= 99 && missiles <= 5) {
                console.log(`🚫 SKIPPING BOOTSTRAP: Detected new save file (Health: ${health}/${maxHealth}, Missiles: ${missiles})`);
                return;
            }
            // Check for post-game scenarios that indicate MB completion
            const hasHyperBeam = gameState.beams['hyper'] === true;
            const inTourian = gameState.area_id === 5;
            const highHealth = maxHealth > 500;
            const highMissiles = gameState.max_missiles > 200;
            if (hasHyperBeam || (inTourian && highHealth) || (highHealth && highMissiles)) {
                console.log('🔧 BOOTSTRAP: Post-game state detected, setting MB1+MB2 complete');
                // Note: In a full implementation, this would call parser.setMotherBrainComplete()
            }
            else {
                console.log('✅ BOOTSTRAP: Save file looks like mid-game, keeping MB cache as-is');
            }
        }
        catch (err) {
            console.log(`Error during MB bootstrap: ${err}`);
        }
    }
    /**
     * Get empty server status
     */
    getEmptyServerStatus() {
        return {
            connected: false,
            gameLoaded: false,
            retroarchVersion: 'Unknown',
            gameInfo: 'No game loaded',
            stats: this.getEmptyGameState(),
            lastUpdate: 0,
            pollCount: 0,
            errorCount: 0
        };
    }
    /**
     * Get empty game state
     */
    getEmptyGameState() {
        return {
            health: 0,
            max_health: 99,
            missiles: 0,
            max_missiles: 0,
            supers: 0,
            max_supers: 0,
            power_bombs: 0,
            max_power_bombs: 0,
            reserve_energy: 0,
            max_reserve_energy: 0,
            room_id: 0,
            area_id: 0,
            area_name: 'Unknown',
            game_state: 0,
            player_x: 0,
            player_y: 0,
            items: {},
            beams: {},
            bosses: {}
        };
    }
}
exports.BackgroundPoller = BackgroundPoller;
//# sourceMappingURL=backgroundPoller.js.map