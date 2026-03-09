# Architecture Overview

## Project Summary

**Super Metroid Auto Tracker** — A Kotlin/Compose Desktop application that connects to SNES emulators (via SNI gRPC or RetroArch UDP) to automatically track items, bosses, and speedrun splits for Super Metroid.

## Tech Stack

- **Language**: Kotlin 1.9.21
- **UI**: Jetpack Compose Desktop 1.5.11 (Material 3)
- **Networking**: gRPC/Protobuf (SNI), UDP sockets (RetroArch), Ktor (HTTP for Map Rando API)
- **Serialization**: kotlinx.serialization (JSON)
- **Testing**: JUnit 5, Strikt assertions
- **Build**: Gradle with Compose plugin, cross-platform packaging (DMG/MSI/DEB)

## Package Structure

```
com.supermetroid/
├── AppDependencies.kt          # DI container (manual, no framework)
├── autosplits/
│   ├── AutoSplitsEngine.kt     # Core split detection & timer engine (~1600 lines)
│   └── SplitProfiles.kt        # 4 predefined split profiles (KPDR, Late Ice, Low%, 100%)
├── gamestate/
│   └── GameStateParser.kt      # SNES memory → GameState parsing (bit flags, HP thresholds)
├── jobs/
│   └── PersonalBestDerivationJob.kt  # CLI tool for PB recalculation
├── livesplit/
│   ├── ExportRunsToLiveSplit.kt  # CLI: JSON runs → .lss export
│   ├── LiveSplitConverter.kt     # Bidirectional LSS ↔ internal model conversion
│   ├── LiveSplitData.kt          # LSS XML data model
│   ├── LiveSplitParser.kt        # LSS XML parser
│   └── LiveSplitWriter.kt        # LSS XML writer
├── model/
│   ├── AppConfig.kt              # 35-field persistent config
│   ├── GameState.kt              # Full game state (health, items, beams, bosses, position)
│   ├── IconConfig.kt             # Icon visibility/ordering (39 default icons)
│   ├── MapRandoInfoConfig.kt     # Map Rando panel config (8 info items)
│   ├── MapRandoInfoFontSize.kt   # Font size enum (5 sizes)
│   ├── MapRandoSettings.kt       # Map Rando seed metadata + API models
│   ├── RoomDatabase.kt           # 143 rooms across 7 areas with O(1) lookups
│   ├── RunHistory.kt             # Run storage, PB derivation, statistics
│   ├── RunSummaries.kt           # Lightweight run summary cache
│   └── Splits.kt                 # Split/Run/PB data structures
├── network/
│   ├── DualMemoryAdapter.kt      # Auto-detecting adapter (SNI preferred, RetroArch fallback)
│   ├── MemoryAdapter.kt          # Base interface + ConnectionState/AdapterType enums
│   ├── MemoryAdapterDetectionService.kt  # Concurrent adapter discovery
│   ├── RetroArchMemoryAdapter.kt # RetroArch UDP adapter
│   ├── RetroArchUdpClient.kt     # Low-level UDP client + SM memory addresses
│   └── SNIMemoryAdapter.kt       # SNI gRPC adapter
├── service/
│   ├── GameGenieCodeService.kt   # Apply Game Genie codes via RetroArch UDP
│   ├── GameGenieDecoder.kt       # SNES Game Genie code decoder
│   ├── GameGenieService.kt       # Game Genie toggle persistence
│   ├── GameStateService.kt       # Main polling loop (200ms), stability checks, backoff
│   ├── IconConfigService.kt      # Icon visibility/ordering persistence
│   ├── IconSizeService.kt        # Icon size preference
│   ├── IconViewModeService.kt    # View mode (DEFAULT vs MAP_RANDO)
│   ├── MapRandoDataService.kt    # Seed detection + maprando.com API scraping
│   ├── MapRandoInfoConfigService.kt  # Map Rando panel config persistence
│   ├── MapRandoInfoFontSizeService.kt  # Font size persistence
│   ├── RoomNameService.kt        # Room name display toggle
│   ├── RunHistoryManager.kt      # Run history file I/O
│   ├── SoundService.kt           # WAV/MP3 sound effects for items/bosses
│   ├── SplitDisplayModeService.kt  # Split column visibility (8 settings)
│   ├── SplitFormatService.kt     # JSON vs LSS format management
│   ├── SplitIconSizeService.kt   # Split icon size preference
│   ├── SplitProfileService.kt    # Profile selection with engine sync
│   ├── ThemeService.kt           # 7 color themes
│   └── UIVisibilityService.kt    # Panel visibility (6 toggles)
├── storage/
│   └── FileStorageService.kt     # File-based persistence (~/.smtracker/)
├── ui/
│   ├── SuperMetroidTracker.kt    # Main entry point, window setup, keyboard shortcuts
│   ├── components/
│   │   ├── GameGenieTab.kt       # Game Genie cheat UI
│   │   ├── Header.kt             # Connection status display
│   │   ├── MapRandoInfoPanel.kt  # Map Rando seed info side panel
│   │   ├── SettingsPanel.kt      # Settings tabs (~2000 lines)
│   │   ├── SimpleStatusGrid.kt   # Icon grid (DEFAULT + MAP_RANDO layouts)
│   │   ├── SplitsList.kt         # Splits display with timing columns (~911 lines)
│   │   ├── SpriteIcon.kt         # Sprite sheet rendering (items + bosses)
│   │   ├── StatusGrid.kt         # Alternative status grid (legacy)
│   │   ├── Timer.kt              # Real-time timer at ~60fps
│   │   ├── VFXSettings.kt        # Placeholder for future VFX
│   │   ├── VisibilityControls.kt # Panel toggle checkboxes
│   │   └── common/               # Reusable UI components
│   └── theme/
│       ├── Colors.kt             # Dynamic theme color system
│       └── Typography.kt         # Monospace typography
└── util/
    └── Logging.kt                # kotlin-logging mixin interface
```

## Data Flow

```
SNES Emulator
    ↓ (gRPC or UDP)
DualMemoryAdapter → SNIMemoryAdapter / RetroArchMemoryAdapter
    ↓ (raw bytes)
GameStateService (200ms poll loop)
    ↓ (Map<String, ByteArray>)
GameStateParser → GameState
    ↓
AutoSplitsEngine (split condition checks)
    ↓ (triggers)
RunSession / CompletedSplit → FileStorageService (JSON)
                            → LiveSplitWriter (.lss)
    ↓
SplitsList UI / Timer UI (Compose StateFlow)
```

## File Storage Layout

```
~/.smtracker/
├── smtracker.json          # AppConfig (35 settings)
├── icon-config.json        # Icon visibility/ordering
├── maprando-info-config.json  # Map Rando panel config
├── sounds.json             # Sound effect config
├── sounds/                 # Custom sound files (WAV/MP3)
├── run-history.json        # Complete run history + derived PBs
├── run-summaries.json      # Cached summary data
├── runs/                   # Individual run files
│   └── {profileId}_{date}_{timestamp}.json
└── exports/                # LSS export output
    └── {profileId}.lss
```

## Key Design Decisions

1. **Manual DI** via `AppDependencies` data class — no framework overhead
2. **Dual adapter** with auto-detection — SNI preferred (more reliable), RetroArch fallback
3. **Individual run files** instead of monolithic JSON — better scalability, crash resilience
4. **PBs derived from runs** not stored independently — ensures consistency
5. **StateFlow** for reactive UI updates — standard Compose pattern
6. **7 predefined themes** — retro/cyberpunk aesthetic matching the game
7. **ASL-compatible split logic** — conditions match the community LiveSplit autosplitter exactly
