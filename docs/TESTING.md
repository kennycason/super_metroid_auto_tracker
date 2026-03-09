# Testing Guide

## Running Tests

```bash
./gradlew test
```

## Test Suite (26 test files)

### AutoSplits Tests
| File | Purpose |
|------|---------|
| `AutoSplitsEngineTest` | Core condition detection, setTimer, MB phases |
| `AutoSkipLogicTest` | Auto-skip for mid-run starts |
| `G4AutoSkipBugTest` | G4 auto-skip regression (requires Statues room + all 4 bosses) |
| `PersonalBestTotalTimeTest` | PB uses fastest run, not first run |
| `RunCompletionFreezeTest` | Deltas frozen at run start |
| `RunTimeCalculationTest` | Segment times, deltas, PB preservation |
| `SplitConditionDetectionTest` | All split conditions with Strikt assertions |
| `SplitSaveTest` | Disk persistence after splits |

### GameState Tests
| File | Purpose |
|------|---------|
| `GameStateParserTest` | Boss flag parsing, ship detection, G4 flags |
| `GameStateValidationTest` | Valid gameplay states, area IDs |
| `ShipDetectionTest` | Ship requires 3 conditions (Zebes Ablaze + Ship AI + MB) |

### LiveSplit Tests
| File | Purpose |
|------|---------|
| `LiveSplitParserTest` | Time parsing, LSS file parsing, round-trips |
| `LiveSplitWriterTest` | Round-trip preservation, file I/O |
| `LiveSplitConverterTest` | Name→ID mapping, profile conversion, PB extraction |
| `LiveSplitWriteIntegrationTest` | Full write flow, multi-run appending, DNF |

### Service Tests
| File | Purpose |
|------|---------|
| `GameGenieDecoderTest` | Code decoding, validation, RetroArch format |
| `GameStateCallbackTest` | Callback mechanism |
| `IconSizeServiceTest` | Size persistence, enum values |
| `RoomNameServiceTest` | Room name toggle persistence |
| `RunHistoryTest` | Storage, PB derivation, incomplete run filtering |

### Storage Tests
| File | Purpose |
|------|---------|
| `FileStorageServiceRunsTest` | Save/load round-trip, best segment derivation |
| `IncompleteRunBestSplitBugTest` | Critical: incomplete runs must NOT contaminate PBs |

### UI Tests
| File | Purpose |
|------|---------|
| `BestPossibleDeltaCalculationTest` | BP delta is segment-by-segment, not cumulative |

### Other
| File | Purpose |
|------|---------|
| `AppConfigTest` | Serialization, backward compatibility |
| `DualMemoryAdapterTest` | Adapter creation, initial state |
| `SNIMemoryAdapterTest` | Adapter creation (2 tests @Disabled, need live SNI) |

## Test Resources

Located in `src/test/resources/`:
- `livesplit/100_percent.lss` — Real 100% LSS file for parsing tests
- `livesplit/kpdr.lss` — Real KPDR LSS file
- `runs/{username}/` — Real user run data for bug regression tests

## Coverage Gaps

1. **Pause/resume timing** — Tests were removed due to timing assumptions
2. **Memory adapter failover** — Only basic creation tests, no failover scenarios
3. **Corrupted file handling** — Limited negative testing
4. **Concurrent operations** — No concurrent save/load tests
5. **State machine transitions** — No tests for menu→gameplay→cutscene flows
6. **LiveSplit game time** — Tests focus on real time only
