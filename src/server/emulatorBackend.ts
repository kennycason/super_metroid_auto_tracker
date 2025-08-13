/**
 * Emulator Backend Interface
 * 
 * Provides a common interface for different emulator backends (RetroArch, Mesen, etc.)
 * This allows the tracker to support multiple emulators without changing the core logic.
 */

import type { ConnectionInfo } from './types';

/**
 * Common interface that all emulator backends must implement
 */
export interface EmulatorBackend {
  /**
   * Connect to the emulator (if needed)
   * @returns Promise that resolves to true if connection successful
   */
  connect(): Promise<boolean>;

  /**
   * Check if currently connected to the emulator
   * @returns Promise that resolves to true if connected
   */
  isConnected(): Promise<boolean>;

  /**
   * Read a range of memory from the emulator
   * @param address Memory address to read from
   * @param size Number of bytes to read
   * @returns Promise that resolves to Uint8Array of memory data, or null if failed
   */
  readMemoryRange(address: number, size: number): Promise<Uint8Array | null>;

  /**
   * Get connection and game information from the emulator
   * @returns Promise that resolves to ConnectionInfo
   */
  getConnectionInfo(): Promise<ConnectionInfo>;

  /**
   * Get the backend type identifier
   * @returns String identifier for this backend type
   */
  getBackendType(): string;

  /**
   * Disconnect from the emulator (cleanup)
   * @returns Promise that resolves when disconnection is complete
   */
  disconnect(): Promise<void>;
}

/**
 * Configuration for different backend types
 */
export interface BackendConfig {
  type: 'retroarch' | 'mesen';
  retroarch?: {
    host: string;
    port: number;
  };
  mesen?: {
    host: string;
    httpPort: number;
    websocketPort?: number;
  };
}

/**
 * Default backend configurations
 */
export const DEFAULT_BACKEND_CONFIG: BackendConfig = {
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
export async function createEmulatorBackend(config: BackendConfig = DEFAULT_BACKEND_CONFIG): Promise<EmulatorBackend> {
  console.log(`🏭 Backend Factory: Creating ${config.type} backend`);

  switch (config.type) {
    case 'retroarch': {
      // Import RetroArchBackend dynamically using ES modules
      const { RetroArchBackend } = await import('./retroArchBackend.js');
      const retroConfig = config.retroarch || DEFAULT_BACKEND_CONFIG.retroarch!;
      console.log(`🏭 Backend Factory: RetroArch config - ${retroConfig.host}:${retroConfig.port}`);
      return new RetroArchBackend(retroConfig.host, retroConfig.port);
    }

    case 'mesen': {
      // Import MesenBackend dynamically using ES modules
      const { MesenBackend } = await import('./mesenBackend.js');
      const mesenConfig = config.mesen || DEFAULT_BACKEND_CONFIG.mesen!;
      console.log(`🏭 Backend Factory: Mesen config - ${mesenConfig.host}:${mesenConfig.httpPort}`);
      return new MesenBackend(mesenConfig.host, mesenConfig.httpPort);
    }

    default:
      console.log(`❌ Backend Factory: Unknown backend type '${config.type}', falling back to RetroArch`);
      const { RetroArchBackend } = await import('./retroArchBackend.js');
      const fallbackConfig = DEFAULT_BACKEND_CONFIG.retroarch!;
      return new RetroArchBackend(fallbackConfig.host, fallbackConfig.port);
  }
}

/**
 * Parse backend configuration from environment variables or command line arguments
 * @param args Command line arguments
 * @returns BackendConfig
 */
export function parseBackendConfig(args: string[] = []): BackendConfig {
  // Check environment variables first
  const envBackendType = process.env.EMULATOR_BACKEND as 'retroarch' | 'mesen' | undefined;
  const envRetroArchHost = process.env.RETROARCH_HOST;
  const envRetroArchPort = process.env.RETROARCH_PORT;
  const envMesenHost = process.env.MESEN_HOST;
  const envMesenPort = process.env.MESEN_HTTP_PORT;

  // Check command line arguments
  const backendTypeArg = args.find(arg => arg.startsWith('--backend='))?.split('=')[1] as 'retroarch' | 'mesen' | undefined;
  const retroArchHostArg = args.find(arg => arg.startsWith('--retroarch-host='))?.split('=')[1];
  const retroArchPortArg = args.find(arg => arg.startsWith('--retroarch-port='))?.split('=')[1];
  const mesenHostArg = args.find(arg => arg.startsWith('--mesen-host='))?.split('=')[1];
  const mesenPortArg = args.find(arg => arg.startsWith('--mesen-port='))?.split('=')[1];

  // Build configuration with precedence: CLI args > env vars > defaults
  const config: BackendConfig = {
    type: backendTypeArg || envBackendType || DEFAULT_BACKEND_CONFIG.type,
    retroarch: {
      host: retroArchHostArg || envRetroArchHost || DEFAULT_BACKEND_CONFIG.retroarch!.host,
      port: parseInt(retroArchPortArg || envRetroArchPort || String(DEFAULT_BACKEND_CONFIG.retroarch!.port))
    },
    mesen: {
      host: mesenHostArg || envMesenHost || DEFAULT_BACKEND_CONFIG.mesen!.host,
      httpPort: parseInt(mesenPortArg || envMesenPort || String(DEFAULT_BACKEND_CONFIG.mesen!.httpPort))
    }
  };

  console.log(`🔧 Backend Config: Using ${config.type} backend`);
  if (config.type === 'retroarch') {
    console.log(`🔧 Backend Config: RetroArch - ${config.retroarch!.host}:${config.retroarch!.port}`);
  } else if (config.type === 'mesen') {
    console.log(`🔧 Backend Config: Mesen - ${config.mesen!.host}:${config.mesen!.httpPort}`);
  }

  return config;
}
