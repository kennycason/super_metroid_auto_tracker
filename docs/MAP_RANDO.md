# Map Randomizer Integration

## Overview

The tracker detects Map Randomizer seeds by reading the seed name from SRAM, then fetches full metadata from the maprando.com website.

## How It Works

1. **Seed Detection**: `MapRandoDataService.readSeedNameFromMemory()` reads 16 bytes at `0xDFFEF0` (null-terminated ASCII)
2. **API Fetch**: On seed change, fetches `https://maprando.com/seed/{seedName}/`
3. **HTML Scraping**: Extracts settings via regex on `col-5`/`col-7` div patterns
4. **Stats from SRAM**: Death count (0x701F64), reload count (0x701F66), reset count (0x701F6A)

## Why Scraping (Not API)?

Settings like objectives, difficulty, item progression, QoL, and map layout are **baked into the ROM** at seed generation. They are NOT in RAM. The only way to get them is from the maprando.com seed page.

## Display

`MapRandoInfoPanel.kt` shows:
- Objectives, Difficulty, Item Progression, QoL, Map Layout (from web)
- Deaths, Reloads (from SRAM, displayed side-by-side)
- Resets (from SRAM, hidden by default)

## Configuration

- Visibility/ordering: `~/.smtracker/maprando-info-config.json`
- Font size: stored in `smtracker.json` (5 size options)
- Panel visibility: via UIVisibilityService

## Auto-Detection

When a valid Map Rando seed name is detected in memory, the UI automatically:
- Switches icon view mode to MAP_RANDO (different grid layout)
- Shows the Map Rando info panel
- Fetches and displays seed settings

## Persistence Files

- `~/.smtracker/maprando-info-config.json` — Item visibility and ordering
- Font size stored in main `smtracker.json` config
