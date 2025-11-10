# Map Rando Settings Persistence

All Map Rando layout and configuration settings are persisted to disk in `~/.smtracker/`

## Configuration Files

### 1. `~/.smtracker/smtracker.json` (Main App Config)

Stores Map Rando UI visibility and font size settings:

```json
{
  "showMapRandoInfo": true,        // Toggle info panel on/off
  "mapRandoInfoFontSize": "Medium" // Font size: Very Small, Small, Medium, Large, Very Large
}
```

**Services:**
- `UIVisibilityService` manages `showMapRandoInfo`
- `MapRandoInfoFontSizeService` manages `mapRandoInfoFontSize`

**When saved:**
- Immediately when toggled in Settings panel
- Automatically persisted on change

---

### 2. `~/.smtracker/maprando-info-config.json` (Info Panel Configuration)

Stores visibility and order of Map Rando info items:

```json
{
  "items": [
    {
      "id": "objectives",
      "displayName": "Objectives",
      "visible": true,
      "order": 1
    },
    {
      "id": "difficulty",
      "displayName": "Difficulty",
      "visible": true,
      "order": 2
    },
    {
      "id": "itemProgression",
      "displayName": "Item Progression",
      "visible": true,
      "order": 3
    },
    {
      "id": "qualityOfLife",
      "displayName": "Quality of Life",
      "visible": true,
      "order": 4
    },
    {
      "id": "mapLayout",
      "displayName": "Map Layout",
      "visible": true,
      "order": 5
    },
    {
      "id": "deaths",
      "displayName": "Deaths",
      "visible": true,
      "order": 6
    },
    {
      "id": "reloads",
      "displayName": "Reloads",
      "visible": true,
      "order": 7
    },
    {
      "id": "resets",
      "displayName": "Resets",
      "visible": false,
      "order": 8
    }
  ]
}
```

**Service:** `MapRandoInfoConfigService`

**When saved:**
- When toggling item visibility
- When reordering items (drag & drop)
- When resetting to defaults

**Features:**
- Automatic merging with defaults (new items added automatically)
- Preserves user customizations across updates
- Full serialization including default values (`encodeDefaults = true`)

---

## Settings Panel Integration

### Icon Layout → Map Rando Settings Section

**Available when:** Icon View Mode = "Map Rando"

**Settings:**
1. **Show Info Panel** (Boolean)
   - Toggle Map Rando info column on/off
   - Persisted to: `smtracker.json` → `showMapRandoInfo`

2. **Info Panel Font Size** (Dropdown)
   - Options: Very Small, Small, Medium, Large, Very Large
   - Dynamically adjusts panel width and font sizes
   - Persisted to: `smtracker.json` → `mapRandoInfoFontSize`

3. **Map Rando Info Management Section**
   - Toggle visibility for each info item
   - Reorder items (drag & drop)
   - Reset to defaults button
   - Persisted to: `maprando-info-config.json`

---

## Verification

To verify settings are persisting:

```bash
# Check main config
cat ~/.smtracker/smtracker.json | jq '.showMapRandoInfo, .mapRandoInfoFontSize'

# Check info panel config
cat ~/.smtracker/maprando-info-config.json | jq '.items[] | {id, visible, order}'
```

---

## Recent Fix (Nov 9, 2025)

**Issue:** `visible` field was not being serialized, and "reloads" item was missing from old configs.

**Solution:**
1. Added `encodeDefaults = true` to Json configuration in `MapRandoInfoConfigService`
2. Old config file deleted and will be recreated with full default configuration on next app start
3. Merge function ensures new items (like "reloads") are automatically added to existing configs

**Status:** ✅ All settings now persist correctly with full field serialization

---

## UI Layout Optimizations (Nov 9, 2025)

### Space-Saving Layout for Counters

**Deaths and Reloads:** Displayed side-by-side in a single row to save vertical space
- Both counters have short labels/values (e.g., "DEATHS: 3", "RELOADS: 7")
- Rendered in a `Row` with equal weights when both are visible
- If only one is visible, it renders normally (full width)

**Resets:** Disabled by default
- Most users don't need this counter (only increments on emulator/console restart)
- Can be enabled in Settings → Icons → Map Rando Info Management
- When enabled, renders on its own row

### Visual Hierarchy

```
┌─────────────────────────────┐
│ OBJECTIVES    Bosses        │
│ DIFFICULTY    Basic          │
│ ITEM PROG     Normal         │
│ QUALITY       Max            │
│ MAP LAYOUT    Small          │
│ DEATHS: 3     RELOADS: 7    │  ← Side-by-side!
│ (RESETS: 0 if enabled)      │
└─────────────────────────────┘
```

**Benefits:**
- More compact info panel
- Better use of horizontal space
- Deaths and Reloads are naturally grouped (both are "failure" counters)
- Panel height reduced by one row when both are visible

