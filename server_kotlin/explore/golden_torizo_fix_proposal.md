# Golden Torizo Detection Fix Proposal

## Current Issue
The current Golden Torizo detection logic is incorrectly reporting Golden Torizo as defeated when it's not. The issue is with condition1:

```kotlin
val condition1 = ((bossPlus1 and 0x0700) != 0) && ((bossPlus1 and 0x0003) != 0)
```

With the current memory values:
- bossPlus1: 0x0207
- bossPlus2: 0x0002
- bossPlus3: 0x0000
- bossPlus4: 0x0000

Condition1 evaluates to true because:
- (0x0207 & 0x0700) = 0x0200, which is not 0
- (0x0207 & 0x0003) = 0x0003, which is not 0

## Proposed Fix

Looking at the memory values and the detection logic, I propose the following changes:

1. Make condition1 more specific by requiring a specific bit pattern in bossPlus1 rather than just any non-zero value in certain bit positions.

2. Since we don't have a comparison with a save where Golden Torizo is actually defeated, we'll need to make an educated guess based on the current pattern.

3. The most likely fix is to modify condition1 to check for a specific value in bossPlus1 that would only be present when Golden Torizo is actually defeated.

```kotlin
// Original condition
val condition1 = ((bossPlus1 and 0x0700) != 0) && ((bossPlus1 and 0x0003) != 0)

// Proposed fix - require a specific bit pattern
val condition1 = (bossPlus1 and 0x0700) == 0x0700  // Require all bits in 0x0700 to be set
```

This change makes the condition more specific, requiring all bits in the 0x0700 mask to be set, rather than just any non-zero value. This should eliminate the false positive we're seeing with bossPlus1 = 0x0207, where only the 0x0200 bit is set.

## Alternative Approaches

If the above fix doesn't work, here are some alternative approaches:

1. Modify condition1 to check for a specific value of bossPlus1:
```kotlin
val condition1 = bossPlus1 == 0x0703  // Example specific value
```

2. Disable condition1 entirely and rely on the other conditions:
```kotlin
val condition1 = false  // Disable this condition
```

3. Add a new condition that specifically excludes the false positive pattern:
```kotlin
val condition1 = ((bossPlus1 and 0x0700) != 0) && ((bossPlus1 and 0x0003) != 0) && (bossPlus1 != 0x0207)
```

## Implementation Plan

1. Implement the proposed fix
2. Test with the current save file to ensure Golden Torizo is no longer detected as defeated
3. Document the changes
4. Monitor for any regressions in other boss detection logic
