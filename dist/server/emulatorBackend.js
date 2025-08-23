"use strict";
/**
 * Emulator Backend Interface
 *
 * Provides a common interface for different emulator backends (RetroArch, Mesen, etc.)
 * This allows the tracker to support multiple emulators without changing the core logic.
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_BACKEND_CONFIG = void 0;
exports.createEmulatorBackend = createEmulatorBackend;
exports.parseBackendConfig = parseBackendConfig;
/**
 * Default backend configurations
 */
exports.DEFAULT_BACKEND_CONFIG = {
    type: 'retroarch',
    retroarch: {
        host: 'localhost',
        port: 55355
    },
    mesen: {
        host: 'localhost',
        httpPort: 8080,
        websocketPort: 8081
    }
};
/**
 * Factory function to create the appropriate emulator backend
 * @param config Backend configuration
 * @returns EmulatorBackend instance
 */
async function createEmulatorBackend(config = exports.DEFAULT_BACKEND_CONFIG) {
    console.log(`🏭 Backend Factory: Creating ${config.type} backend`);
    switch (config.type) {
        case 'retroarch': {
            // Import RetroArchBackend dynamically using ES modules
            const { RetroArchBackend } = await Promise.resolve().then(() => __importStar(require('./retroArchBackend.js')));
            const retroConfig = config.retroarch || exports.DEFAULT_BACKEND_CONFIG.retroarch;
            console.log(`🏭 Backend Factory: RetroArch config - ${retroConfig.host}:${retroConfig.port}`);
            return new RetroArchBackend(retroConfig.host, retroConfig.port);
        }
        case 'mesen': {
            // Import MesenBackend dynamically using ES modules
            const { MesenBackend } = await Promise.resolve().then(() => __importStar(require('./mesenBackend.js')));
            const mesenConfig = config.mesen || exports.DEFAULT_BACKEND_CONFIG.mesen;
            console.log(`🏭 Backend Factory: Mesen config - ${mesenConfig.host}:${mesenConfig.httpPort}`);
            return new MesenBackend(mesenConfig.host, mesenConfig.httpPort);
        }
        default:
            console.log(`❌ Backend Factory: Unknown backend type '${config.type}', falling back to RetroArch`);
            const { RetroArchBackend } = await Promise.resolve().then(() => __importStar(require('./retroArchBackend.js')));
            const fallbackConfig = exports.DEFAULT_BACKEND_CONFIG.retroarch;
            return new RetroArchBackend(fallbackConfig.host, fallbackConfig.port);
    }
}
/**
 * Parse backend configuration from environment variables or command line arguments
 * @param args Command line arguments
 * @returns BackendConfig
 */
function parseBackendConfig(args = []) {
    // Check environment variables first
    const envBackendType = process.env.EMULATOR_BACKEND;
    const envRetroArchHost = process.env.RETROARCH_HOST;
    const envRetroArchPort = process.env.RETROARCH_PORT;
    const envMesenHost = process.env.MESEN_HOST;
    const envMesenPort = process.env.MESEN_HTTP_PORT;
    // Check command line arguments
    const backendTypeArg = args.find(arg => arg.startsWith('--backend='))?.split('=')[1];
    const retroArchHostArg = args.find(arg => arg.startsWith('--retroarch-host='))?.split('=')[1];
    const retroArchPortArg = args.find(arg => arg.startsWith('--retroarch-port='))?.split('=')[1];
    const mesenHostArg = args.find(arg => arg.startsWith('--mesen-host='))?.split('=')[1];
    const mesenPortArg = args.find(arg => arg.startsWith('--mesen-port='))?.split('=')[1];
    // Build configuration with precedence: CLI args > env vars > defaults
    const config = {
        type: backendTypeArg || envBackendType || exports.DEFAULT_BACKEND_CONFIG.type,
        retroarch: {
            host: retroArchHostArg || envRetroArchHost || exports.DEFAULT_BACKEND_CONFIG.retroarch.host,
            port: parseInt(retroArchPortArg || envRetroArchPort || String(exports.DEFAULT_BACKEND_CONFIG.retroarch.port))
        },
        mesen: {
            host: mesenHostArg || envMesenHost || exports.DEFAULT_BACKEND_CONFIG.mesen.host,
            httpPort: parseInt(mesenPortArg || envMesenPort || String(exports.DEFAULT_BACKEND_CONFIG.mesen.httpPort))
        }
    };
    console.log(`🔧 Backend Config: Using ${config.type} backend`);
    if (config.type === 'retroarch') {
        console.log(`🔧 Backend Config: RetroArch - ${config.retroarch.host}:${config.retroarch.port}`);
    }
    else if (config.type === 'mesen') {
        console.log(`🔧 Backend Config: Mesen - ${config.mesen.host}:${config.mesen.httpPort}`);
    }
    return config;
}
//# sourceMappingURL=emulatorBackend.js.map