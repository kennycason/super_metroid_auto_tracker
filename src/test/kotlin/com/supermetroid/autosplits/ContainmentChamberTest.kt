package com.supermetroid.autosplits

import com.supermetroid.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import strikt.api.expectThat
import strikt.assertions.*

/**
 * Tests for Containment Chamber split profile, room-entry split triggers,
 * manual split functionality, and new item split handlers.
 */
class ContainmentChamberTest {

    private lateinit var engine: AutoSplitsEngine

    @BeforeEach
    fun setup() {
        engine = AutoSplitsEngine()
    }

    // --- Containment Chamber profile tests ---

    @Test
    fun `containment chamber profile has correct number of splits`() {
        val profile = SplitProfiles.CONTAINMENT_CHAMBER
        expectThat(profile.splits).hasSize(12)
    }

    @Test
    fun `containment chamber profile has correct id and name`() {
        val profile = SplitProfiles.CONTAINMENT_CHAMBER
        expectThat(profile.id).isEqualTo("containment-chamber")
        expectThat(profile.name).isEqualTo("Containment Chamber")
    }

    @Test
    fun `containment chamber profile is in ALL_PROFILES`() {
        expectThat(SplitProfiles.ALL_PROFILES).contains(SplitProfiles.CONTAINMENT_CHAMBER)
    }

    @Test
    fun `containment chamber profile is findable by id`() {
        val found = SplitProfiles.getProfileById("containment-chamber")
        expectThat(found).isEqualTo(SplitProfiles.CONTAINMENT_CHAMBER)
    }

    @Test
    fun `containment chamber missile split has triggerRoomId for Terminator Room`() {
        val missileSplit = SplitProfiles.CONTAINMENT_CHAMBER.splits.find { it.id == "missile" }
        expectThat(missileSplit).isNotNull()
        expectThat(missileSplit!!.triggerRoomId).isEqualTo(0x990D)
        expectThat(missileSplit.type).isEqualTo("room_entry")
    }

    @Test
    fun `containment chamber super missiles split has triggerRoomId for Milly Mays Room`() {
        val superSplit = SplitProfiles.CONTAINMENT_CHAMBER.splits.find { it.id == "super_missiles" }
        expectThat(superSplit).isNotNull()
        expectThat(superSplit!!.triggerRoomId).isEqualTo(0xA1D8)
        expectThat(superSplit.type).isEqualTo("room_entry")
    }

    @Test
    fun `containment chamber split order matches expected route`() {
        val ids = SplitProfiles.CONTAINMENT_CHAMBER.splits.map { it.id }
        expectThat(ids).isEqualTo(listOf(
            "morph_ball", "charge_beam", "bomb", "missile", "super_missiles",
            "speed_booster", "hi_jump", "grapple_beam", "ice_beam",
            "gravity_suit", "spring_ball", "ship"
        ))
    }

    // --- Room-entry split trigger tests ---

    @Test
    fun `room entry split triggers when player enters target room`() {
        val profile = SplitProfile(
            id = "test-room-entry",
            name = "Test Room Entry",
            splits = listOf(
                Split("test_room", "Test Room", "room_entry", "Enter test room", triggerRoomId = 0x990D)
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // First poll: in a different room
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234))
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("test_room")

        // Second poll: entered the target room
        engine.processGameState(GameState(gameState = 8, roomId = 0x990D))

        // Split should have triggered
        expectThat(engine.getCurrentSplit()).isNull()
        val run = engine.splitsState.value.currentRun
        expectThat(run).isNotNull()
        expectThat(run!!.completedSplits).hasSize(1)
        expectThat(run.completedSplits[0].splitId).isEqualTo("test_room")
    }

    @Test
    fun `room entry split auto-skips when player is already in trigger room after previous split`() {
        // This tests the exact bug: Bombs auto-skips, player is already in Terminator Room (0x990D),
        // Missiles split should auto-skip too since player is already there
        val profile = SplitProfile(
            id = "test-autoskip-room",
            name = "Test AutoSkip Room",
            splits = listOf(
                Split("bomb", "Bombs", "item", "Bombs acquired"),
                Split("missile", "Missiles", "room_entry", "Entered Terminator Room", triggerRoomId = 0x990D),
                Split("next_split", "Next", "item", "Next item")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Player already has bombs and is in the trigger room
        val state = GameState(gameState = 8, roomId = 0x990D, items = Items(bombs = true))
        engine.processGameState(state)
        engine.processGameState(state) // Second poll to ensure stability

        // Both Bombs and Missiles should have been auto-skipped
        val run = engine.splitsState.value.currentRun!!
        val completedIds = run.completedSplits.map { it.splitId }
        expectThat(completedIds).contains("bomb")
        expectThat(completedIds).contains("missile")
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("next_split")
    }

    @Test
    fun `room entry split auto-skips when player is already in target room from start`() {
        val profile = SplitProfile(
            id = "test-room-entry",
            name = "Test Room Entry",
            splits = listOf(
                Split("test_room", "Test Room", "room_entry", "Enter test room", triggerRoomId = 0x990D)
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Both polls: already in the target room (auto-skip should fire)
        engine.processGameState(GameState(gameState = 8, roomId = 0x990D))
        engine.processGameState(GameState(gameState = 8, roomId = 0x990D))

        // Split should auto-skip since player is already in the trigger room
        expectThat(engine.getCurrentSplit()).isNull()
        val run = engine.splitsState.value.currentRun!!
        expectThat(run.completedSplits).hasSize(1)
        expectThat(run.completedSplits[0].splitId).isEqualTo("test_room")
    }

    @Test
    fun `room entry split does NOT auto-skip when player is in a different room`() {
        val profile = SplitProfile(
            id = "test-room-entry",
            name = "Test Room Entry",
            splits = listOf(
                Split("test_room", "Test Room", "room_entry", "Enter test room", triggerRoomId = 0x990D)
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Player is in a different room - should NOT auto-skip
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234))
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234))

        // Split should still be pending
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("test_room")
    }

    @Test
    fun `room entry split isConditionAlreadyMet returns true when player is in trigger room`() {
        val roomSplit = Split("test_room", "Test Room", "room_entry", "Enter test room", triggerRoomId = 0x990D)
        val inRoom = GameState(gameState = 8, roomId = 0x990D)
        val notInRoom = GameState(gameState = 8, roomId = 0x1234)
        // Room-entry splits are "already met" when the player is currently in the trigger room
        expectThat(engine.isConditionAlreadyMet(roomSplit, inRoom)).isTrue()
        expectThat(engine.isConditionAlreadyMet(roomSplit, notInRoom)).isFalse()
    }

    @Test
    fun `splits without triggerRoomId are not treated as room entry splits`() {
        val normalSplit = Split("morph_ball", "Morph Ball", "item", "Morph Ball acquired")
        expectThat(normalSplit.triggerRoomId).isNull()
    }

    // --- New item split handler tests ---

    @Test
    fun `grapple beam isConditionAlreadyMet detects grapple`() {
        val split = Split("grapple_beam", "Grapple", "item")
        val noGrapple = GameState(gameState = 8, items = Items(grapple = false))
        val hasGrapple = GameState(gameState = 8, items = Items(grapple = true))
        expectThat(engine.isConditionAlreadyMet(split, noGrapple)).isFalse()
        expectThat(engine.isConditionAlreadyMet(split, hasGrapple)).isTrue()
    }

    @Test
    fun `screw attack isConditionAlreadyMet detects screw`() {
        val split = Split("screw_attack", "Screw Attack", "item")
        val noScrew = GameState(gameState = 8, items = Items(screw = false))
        val hasScrew = GameState(gameState = 8, items = Items(screw = true))
        expectThat(engine.isConditionAlreadyMet(split, noScrew)).isFalse()
        expectThat(engine.isConditionAlreadyMet(split, hasScrew)).isTrue()
    }

    @Test
    fun `spring ball isConditionAlreadyMet detects spring`() {
        val split = Split("spring_ball", "Spring Ball", "item")
        val noSpring = GameState(gameState = 8, items = Items(spring = false))
        val hasSpring = GameState(gameState = 8, items = Items(spring = true))
        expectThat(engine.isConditionAlreadyMet(split, noSpring)).isFalse()
        expectThat(engine.isConditionAlreadyMet(split, hasSpring)).isTrue()
    }

    @Test
    fun `reserve tank isConditionAlreadyMet detects reserve energy`() {
        val split = Split("reserve_tank", "Reserve", "item")
        val noReserve = GameState(gameState = 8, maxReserveEnergy = 0)
        val hasReserve = GameState(gameState = 8, maxReserveEnergy = 100)
        expectThat(engine.isConditionAlreadyMet(split, noReserve)).isFalse()
        expectThat(engine.isConditionAlreadyMet(split, hasReserve)).isTrue()
    }

    // --- Manual split tests ---

    @Test
    fun `manual split returns false when no run is active`() {
        engine.loadProfile(SplitProfiles.KPDR_ANY)
        expectThat(engine.manualSplit()).isFalse()
    }

    @Test
    fun `manual split triggers current split and advances`() {
        val profile = SplitProfile(
            id = "test-manual",
            name = "Test Manual",
            splits = listOf(
                Split("split_a", "Split A", "event"),
                Split("split_b", "Split B", "event")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Current split should be split_a
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("split_a")

        val result = engine.manualSplit()
        expectThat(result).isTrue()

        // Should advance to split_b
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("split_b")

        // First split should be completed
        val run = engine.splitsState.value.currentRun
        expectThat(run).isNotNull()
        expectThat(run!!.completedSplits).hasSize(1)
        expectThat(run.completedSplits[0].splitId).isEqualTo("split_a")
    }

    @Test
    fun `manual split completes run when last split is triggered`() {
        val profile = SplitProfile(
            id = "test-manual",
            name = "Test Manual",
            splits = listOf(
                Split("only_split", "Only Split", "event")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        val result = engine.manualSplit()
        expectThat(result).isTrue()

        // Run should be complete (no more splits)
        expectThat(engine.getCurrentSplit()).isNull()

        val run = engine.splitsState.value.currentRun
        expectThat(run).isNotNull()
        expectThat(run!!.completedSplits).hasSize(1)
        expectThat(run.endTime).isNotNull()
    }

    @Test
    fun `manual split returns false after run is finished`() {
        val profile = SplitProfile(
            id = "test-manual",
            name = "Test Manual",
            splits = listOf(
                Split("only_split", "Only Split", "event")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Complete the run
        engine.manualSplit()

        // Trying again should return false
        expectThat(engine.manualSplit()).isFalse()
    }

    @Test
    fun `manual split records valid time data`() {
        val profile = SplitProfile(
            id = "test-manual",
            name = "Test Manual",
            splits = listOf(
                Split("split_a", "Split A", "event"),
                Split("split_b", "Split B", "event")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Small delay to ensure nonzero time
        Thread.sleep(50)

        engine.manualSplit()

        val run = engine.splitsState.value.currentRun!!
        val splitTime = run.completedSplits[0].time
        expectThat(splitTime.totalTime).isGreaterThan(0)
        expectThat(splitTime.segmentTime).isGreaterThan(0)
        expectThat(splitTime.totalTime).isEqualTo(splitTime.segmentTime) // First split
    }

    // --- Grapple/Screw/Spring split detection via processGameState ---

    @Test
    fun `grapple beam split triggers on item acquisition transition`() {
        val profile = SplitProfile(
            id = "test-grapple",
            name = "Test Grapple",
            splits = listOf(
                Split("grapple_beam", "Grapple", "item")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // State without grapple
        engine.processGameState(GameState(gameState = 8, items = Items(grapple = false)))
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("grapple_beam")

        // State with grapple acquired
        engine.processGameState(GameState(gameState = 8, items = Items(grapple = true)))
        expectThat(engine.getCurrentSplit()).isNull()
    }

    @Test
    fun `spring ball split triggers on item acquisition transition`() {
        val profile = SplitProfile(
            id = "test-spring",
            name = "Test Spring",
            splits = listOf(
                Split("spring_ball", "Spring Ball", "item")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        engine.processGameState(GameState(gameState = 8, items = Items(spring = false)))
        engine.processGameState(GameState(gameState = 8, items = Items(spring = true)))
        expectThat(engine.getCurrentSplit()).isNull()
    }

    @Test
    fun `screw attack split triggers on item acquisition transition`() {
        val profile = SplitProfile(
            id = "test-screw",
            name = "Test Screw",
            splits = listOf(
                Split("screw_attack", "Screw Attack", "item")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        engine.processGameState(GameState(gameState = 8, items = Items(screw = false)))
        engine.processGameState(GameState(gameState = 8, items = Items(screw = true)))
        expectThat(engine.getCurrentSplit()).isNull()
    }

    @Test
    fun `reserve tank split triggers when max reserve energy transitions from 0`() {
        val profile = SplitProfile(
            id = "test-reserve",
            name = "Test Reserve",
            splits = listOf(
                Split("reserve_tank", "Reserve", "item")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        engine.processGameState(GameState(gameState = 8, maxReserveEnergy = 0))
        engine.processGameState(GameState(gameState = 8, maxReserveEnergy = 100))
        expectThat(engine.getCurrentSplit()).isNull()
    }

    // --- requiredItems tests ---

    @Test
    fun `room entry split with requiredItems does NOT trigger without required item`() {
        val profile = SplitProfile(
            id = "test-required-items",
            name = "Test Required Items",
            splits = listOf(
                Split("guarded_room", "Guarded Room", "room_entry", "Requires bombs",
                    triggerRoomId = 0x92FD, requiredItems = listOf("bombs"))
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Enter target room WITHOUT bombs
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234))
        engine.processGameState(GameState(gameState = 8, roomId = 0x92FD, items = Items(bombs = false)))

        // Split should NOT have triggered
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("guarded_room")
    }

    @Test
    fun `room entry split with requiredItems triggers WITH required item`() {
        val profile = SplitProfile(
            id = "test-required-items",
            name = "Test Required Items",
            splits = listOf(
                Split("guarded_room", "Guarded Room", "room_entry", "Requires bombs",
                    triggerRoomId = 0x92FD, requiredItems = listOf("bombs"))
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Enter target room WITH bombs
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234, items = Items(bombs = true)))
        engine.processGameState(GameState(gameState = 8, roomId = 0x92FD, items = Items(bombs = true)))

        // Split should have triggered
        expectThat(engine.getCurrentSplit()).isNull()
        val run = engine.splitsState.value.currentRun!!
        expectThat(run.completedSplits).hasSize(1)
        expectThat(run.completedSplits[0].splitId).isEqualTo("guarded_room")
    }

    @Test
    fun `room entry split with multiple requiredItems needs ALL items`() {
        val profile = SplitProfile(
            id = "test-multi-required",
            name = "Test Multi Required",
            splits = listOf(
                Split("multi_guard", "Multi Guard", "room_entry", "Requires bombs + speed",
                    triggerRoomId = 0x9AD9, requiredItems = listOf("bombs", "speed_booster"))
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Enter room with only bombs (missing speed booster)
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234, items = Items(bombs = true)))
        engine.processGameState(GameState(gameState = 8, roomId = 0x9AD9, items = Items(bombs = true, speed = false)))
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("multi_guard")

        // Leave and re-enter with both items
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234, items = Items(bombs = true, speed = true)))
        engine.processGameState(GameState(gameState = 8, roomId = 0x9AD9, items = Items(bombs = true, speed = true)))
        expectThat(engine.getCurrentSplit()).isNull()
    }

    @Test
    fun `isConditionAlreadyMet returns false for splits with requiredItems to force transition check`() {
        // Splits with requiredItems must NOT auto-skip — they need a room transition
        // This prevents false triggers when picking up the required item while already in the room
        val guardedSplit = Split("guarded", "Guarded", "room_entry", triggerRoomId = 0x92FD, requiredItems = listOf("bombs"))

        // Even in room with bombs, auto-skip should NOT fire
        val inRoomWithBombs = GameState(gameState = 8, roomId = 0x92FD, items = Items(bombs = true))
        expectThat(engine.isConditionAlreadyMet(guardedSplit, inRoomWithBombs)).isFalse()

        // Unguarded split (no requiredItems) CAN auto-skip
        val unguardedSplit = Split("unguarded", "Unguarded", "room_entry", triggerRoomId = 0x92FD)
        expectThat(engine.isConditionAlreadyMet(unguardedSplit, inRoomWithBombs)).isTrue()
    }

    @Test
    fun `puzzle 4 same room as puzzle 1 works via sequential splits and requiredItems`() {
        // Simulates: puzzle 1 (0x92FD, no guard) then puzzle 4 (0x92FD, requires bombs)
        val profile = SplitProfile(
            id = "test-same-room",
            name = "Test Same Room",
            splits = listOf(
                Split("puzzle_1", "Puzzle 1", "room_entry", triggerRoomId = 0x92FD),
                Split("puzzle_2", "Puzzle 2", "room_entry", triggerRoomId = 0x9A90),
                Split("puzzle_4", "Puzzle 4", "room_entry", triggerRoomId = 0x92FD, requiredItems = listOf("bombs"))
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Enter 0x92FD without bombs → puzzle 1 fires
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234))
        engine.processGameState(GameState(gameState = 8, roomId = 0x92FD))
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("puzzle_2")

        // Walk back through 0x92FD without bombs → puzzle 4 should NOT fire (no bombs)
        engine.processGameState(GameState(gameState = 8, roomId = 0x9A90))
        // Complete puzzle 2
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("puzzle_4")

        // Walk through 0x92FD without bombs
        engine.processGameState(GameState(gameState = 8, roomId = 0x92FD, items = Items(bombs = false)))
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("puzzle_4") // Still not triggered

        // Now enter with bombs
        engine.processGameState(GameState(gameState = 8, roomId = 0x1234, items = Items(bombs = true)))
        engine.processGameState(GameState(gameState = 8, roomId = 0x92FD, items = Items(bombs = true)))
        expectThat(engine.getCurrentSplit()).isNull() // Puzzle 4 triggered
    }

    // --- CC Puzzles profile tests ---

    @Test
    fun `containment chamber puzzles profile has correct number of splits`() {
        val profile = SplitProfiles.CONTAINMENT_CHAMBER_PUZZLES
        expectThat(profile.splits).hasSize(25) // 9 + 15 (A-O) + ship
    }

    @Test
    fun `containment chamber puzzles profile is in ALL_PROFILES`() {
        expectThat(SplitProfiles.ALL_PROFILES).contains(SplitProfiles.CONTAINMENT_CHAMBER_PUZZLES)
    }

    @Test
    fun `puzzle 4 has bombs requiredItems guard`() {
        val puzzle4 = SplitProfiles.CONTAINMENT_CHAMBER_PUZZLES.splits.find { it.id == "puzzle_4" }
        expectThat(puzzle4).isNotNull()
        expectThat(puzzle4!!.triggerRoomId).isEqualTo(0x92FD)
        expectThat(puzzle4.requiredItems).isEqualTo(listOf("bombs"))
    }

    @Test
    fun `puzzle 7 has speed_booster requiredItems guard`() {
        val puzzle7 = SplitProfiles.CONTAINMENT_CHAMBER_PUZZLES.splits.find { it.id == "puzzle_7" }
        expectThat(puzzle7).isNotNull()
        expectThat(puzzle7!!.triggerRoomId).isEqualTo(0x9AD9)
        expectThat(puzzle7.requiredItems).isEqualTo(listOf("speed_booster"))
    }

    @Test
    fun `CC puzzles ship split uses ship event detector`() {
        val ship = SplitProfiles.CONTAINMENT_CHAMBER_PUZZLES.splits.find { it.id == "ship" }
        expectThat(ship).isNotNull()
        // Keep this as an event split: AutoSplitsEngine.checkShip handles the
        // Containment Chamber shipAI transition in Landing Site.
        expectThat(ship!!.type).isEqualTo("event")
        expectThat(ship.triggerRoomId).isNull()
    }

    // --- Discard run tests ---

    @Test
    fun `discard run clears current run without saving`() {
        val profile = SplitProfile(
            id = "test-discard",
            name = "Test Discard",
            splits = listOf(
                Split("split_a", "Split A", "event"),
                Split("split_b", "Split B", "event")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Complete one split
        engine.manualSplit()
        expectThat(engine.splitsState.value.currentRun).isNotNull()
        expectThat(engine.splitsState.value.currentRun!!.completedSplits).hasSize(1)

        // Discard
        engine.discardRun()

        // Run should be gone
        expectThat(engine.splitsState.value.currentRun).isNull()
    }

    @Test
    fun `discard run does nothing when no run is active`() {
        engine.loadProfile(SplitProfiles.KPDR_ANY)
        // Should not throw
        engine.discardRun()
        expectThat(engine.splitsState.value.currentRun).isNull()
    }

    @Test
    fun `discard run resets split index so next run starts fresh`() {
        val profile = SplitProfile(
            id = "test-discard-reset",
            name = "Test Discard Reset",
            splits = listOf(
                Split("split_a", "Split A", "event"),
                Split("split_b", "Split B", "event")
            )
        )
        engine.loadProfile(profile)
        engine.toggleRunState(profile.id)

        // Advance past first split
        engine.manualSplit()
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("split_b")

        // Discard
        engine.discardRun()

        // Start a new run - should start from split_a
        engine.toggleRunState(profile.id)
        expectThat(engine.getCurrentSplit()?.id).isEqualTo("split_a")
    }
}
