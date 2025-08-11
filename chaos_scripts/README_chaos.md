# Super Metroid Chaos Generator 🌪️

A delightfully destructive script that gradually corrupts Super Metroid's graphics and sprite data to create a visual decay effect while preserving game logic and playability.

## ⚠️ What This Does

The script randomly corrupts visual data to create effects like:
- **Garbled sprites** - Samus and enemies become abstract art
- **Corrupted tiles** - Walls and backgrounds decay into chaos  
- **Color madness** - Palettes shift into psychedelic nightmares
- **Animation glitches** - Movement becomes beautifully broken
- **Progressive decay** - Corruption intensifies over time

## 🎯 What It DOESN'T Touch

The script carefully avoids critical game logic:
- ✅ **Player stats** remain intact (health, missiles, items)
- ✅ **Boss states** preserved (progression still works) 
- ✅ **Room/area data** untouched (no softlocks)
- ✅ **Game state** protected (saves still work)
- ✅ **System memory** avoided (no crashes)

## 🎮 Memory Targets

### Graphics Regions (Corrupted)
```
VRAM Tiles      : 0x7E2000 - 0x7E8000  (sprite graphics)
Sprite Tables   : 0x7E1000 - 0x7E1800  (sprite definitions)  
Animation Data  : 0x7E1800 - 0x7E2000  (movement frames)
Graphics Buffers: 0x7F0000 - 0x7F4000  (rendering data)
Palette Data    : 0x7EC000 - 0x7EC200  (color tables)
Background Maps : 0x7F8000 - 0x7FA000  (tile layouts)
```

### Protected Regions (Never Touched)
```
Items/Stats     : 0x7E09A0 - 0x7E09E0  (grapple beam, etc.)
Boss States     : 0x7ED820 - 0x7ED860  (Ridley defeated, etc.)
Room/Area Data  : 0x7E0790 - 0x7E07B0  (current location)
Game State      : 0x7E0990 - 0x7E09A0  (save data)
System Memory   : 0x7E0000 - 0x7E0100  (critical systems)
```

## 📈 Chaos Progression

The script gradually escalates:

1. **Phase 1**: Single byte corruptions every 2 seconds
2. **Phase 2**: Multi-byte corruptions, faster intervals  
3. **Phase 3**: Weighted chaos targeting critical graphics
4. **Phase 4**: Maximum entropy - visual madness

## 🛠️ Corruption Types

- **Bit Flips**: Subtle single-bit changes (realistic glitches)
- **Random Values**: Complete chaos (psychedelic effects)
- **Pattern Corruption**: Artistic patterns (0xAA, 0x55, etc.)
- **Gradual Shifts**: Slow color/brightness changes
- **Palette Madness**: Color cycling effects

## 🚀 Usage

```bash
# Load Super Metroid in RetroArch first!
python3 super_metroid_chaos.py
```

### Safety Prompts
The script includes confirmation prompts:
```
🤔 Are you ready to embrace the chaos? (y/N):
```

### Live Monitoring
```
🔥 Corrupted VRAM Tiles 1 at 0x7E2A4F: B7
🔥 Corrupted Palette Data at 0x7EC12A: FF 00
📊 Chaos level: 50 corruptions, intensity: 3.2, interval: 1.8s
🌊 Chaos escalated! Now corrupting 2 bytes at once
```

## 🛑 Stopping the Chaos

- **Ctrl+C** - Stop corruption immediately
- **Reload save** - Restore original graphics
- **Reset game** - Back to normal state

## ⚡ Advanced Features

### Weighted Targeting
Different regions have different corruption probabilities:
- **VRAM Tiles**: High weight (most visible)
- **Sprites**: High weight (character corruption)  
- **Palettes**: Low weight (dramatic but dangerous)
- **Background**: Medium weight (environmental decay)

### Intelligent Escalation
- Corruption frequency increases over time
- Multi-byte corruptions unlock progressively
- Intensity scales from 1.0 to 10.0
- Intervals decrease from 2.0s to 0.5s minimum

### Protection System
Double-checks every memory write against protected regions to ensure the game remains playable.

## 🎭 Expected Visual Effects

### Early Stages (Minutes 1-2)
- Subtle sprite glitches
- Minor tile corruption
- Occasional color shifts

### Mid Chaos (Minutes 3-5)  
- Samus becomes abstract art
- Wall textures decay
- Color palettes shift wildly
- Animation frames corrupt

### Maximum Entropy (Minutes 5+)
- Complete visual madness
- Psychedelic color cycling
- Unrecognizable sprites
- Beautiful digital decay

## 🧪 Technical Notes

### Memory Layout
Based on Super Metroid's actual memory map targeting graphics-only regions while avoiding the game logic areas used by the tracker.

### Corruption Algorithm
```python
# Example corruption types
bit_flip = original_byte ^ (1 << random_bit)
random_chaos = random.randint(0, 255)  
pattern = 0xAA if random() < 0.5 else 0x55
```

### Safety Mechanisms
- Protected region checking
- Game status monitoring  
- Graceful error handling
- User confirmation prompts

## ⚠️ Warnings

- **Save your game first!** Corruption is temporary but dramatic
- **Visual seizure warning** - Flashing/chaotic colors possible
- **Screenshot worthy** - The results can be genuinely artistic
- **Addictive** - You might want to watch the decay for hours

## 🎨 Philosophy 

This isn't about breaking the game - it's about creating emergent digital art through controlled chaos. Watch as your familiar Super Metroid slowly transforms into an abstract, glitched masterpiece while remaining perfectly playable underneath.

*"In chaos, there is beauty. In corruption, there is art."* 🎭