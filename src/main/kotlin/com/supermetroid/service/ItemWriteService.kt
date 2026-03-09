package com.supermetroid.service

import com.supermetroid.model.GameState
import com.supermetroid.network.MemoryAdapter
import com.supermetroid.util.Logging

/**
 * Write-only service for modifying items, beams, and ammo in SNES memory.
 *
 * IMPORTANT: This service does NOT read from memory. All current state comes from
 * the GameState flow (already polled by GameStateService every 200ms). This avoids
 * read conflicts with the polling loop over the shared adapter.
 *
 * Super Metroid has separate addresses for "equipped" vs "collected" items/beams.
 * To give + equip an item, both addresses must be written. Since equipped (0x7E09A2)
 * and collected (0x7E09A4) are adjacent, we write them as a single 4-byte batch
 * to avoid timing issues between separate writes.
 *
 * Tracks pending bitmask state locally so rapid toggles (within the 200ms poll window)
 * build on each other rather than reverting to stale gameState.
 */
class ItemWriteService(
    private val memoryAdapter: MemoryAdapter
) : Logging {

    // Item bit flags (shared with GameStateParser)
    enum class Item(val displayName: String, val bit: Int) {
        VARIA_SUIT("Varia Suit", 0x0001),
        SPRING_BALL("Spring Ball", 0x0002),
        MORPH_BALL("Morph Ball", 0x0004),
        SCREW_ATTACK("Screw Attack", 0x0008),
        GRAVITY_SUIT("Gravity Suit", 0x0020),
        HI_JUMP_BOOTS("Hi-Jump Boots", 0x0100),
        SPACE_JUMP("Space Jump", 0x0200),
        BOMBS("Bombs", 0x1000),
        SPEED_BOOSTER("Speed Booster", 0x2000),
        GRAPPLE_BEAM("Grapple Beam", 0x4000),
        X_RAY_SCOPE("X-Ray Scope", 0x8000)
    }

    enum class Beam(val displayName: String, val bit: Int) {
        WAVE("Wave Beam", 0x0001),
        ICE("Ice Beam", 0x0002),
        SPAZER("Spazer", 0x0004),
        PLASMA("Plasma Beam", 0x0008),
        CHARGE("Charge Beam", 0x1000)
    }

    companion object {
        // Equipped = what's currently active, Collected = what's been picked up
        // These are adjacent in memory: equipped at base, collected at base+2
        const val EQUIPPED_ITEMS = 0x7E09A2
        const val COLLECTED_ITEMS = 0x7E09A4
        const val EQUIPPED_BEAMS = 0x7E09A6
        const val COLLECTED_BEAMS = 0x7E09A8

        // Ammo addresses (current / max) — also adjacent pairs
        const val CURRENT_ENERGY = 0x7E09C2
        const val MAX_ENERGY = 0x7E09C4
        const val CURRENT_MISSILES = 0x7E09C6
        const val MAX_MISSILES = 0x7E09C8
        const val CURRENT_SUPERS = 0x7E09CA
        const val MAX_SUPERS = 0x7E09CC
        const val CURRENT_POWER_BOMBS = 0x7E09CE
        const val MAX_POWER_BOMBS = 0x7E09D0
        const val CURRENT_RESERVE = 0x7E09D6
        const val MAX_RESERVE = 0x7E09D4

        /** Reconstruct the item bit mask from GameState booleans. */
        fun itemBitsFromGameState(gs: GameState): Int {
            var bits = 0
            if (gs.items.varia) bits = bits or Item.VARIA_SUIT.bit
            if (gs.items.spring) bits = bits or Item.SPRING_BALL.bit
            if (gs.items.morph) bits = bits or Item.MORPH_BALL.bit
            if (gs.items.screw) bits = bits or Item.SCREW_ATTACK.bit
            if (gs.items.gravity) bits = bits or Item.GRAVITY_SUIT.bit
            if (gs.items.hiJump) bits = bits or Item.HI_JUMP_BOOTS.bit
            if (gs.items.spaceJump) bits = bits or Item.SPACE_JUMP.bit
            if (gs.items.bombs) bits = bits or Item.BOMBS.bit
            if (gs.items.speed) bits = bits or Item.SPEED_BOOSTER.bit
            if (gs.items.grapple) bits = bits or Item.GRAPPLE_BEAM.bit
            if (gs.items.xray) bits = bits or Item.X_RAY_SCOPE.bit
            return bits
        }

        /** Reconstruct the beam bit mask from GameState booleans. */
        fun beamBitsFromGameState(gs: GameState): Int {
            var bits = 0
            if (gs.beams.wave) bits = bits or Beam.WAVE.bit
            if (gs.beams.ice) bits = bits or Beam.ICE.bit
            if (gs.beams.spazer) bits = bits or Beam.SPAZER.bit
            if (gs.beams.plasma) bits = bits or Beam.PLASMA.bit
            if (gs.beams.charge) bits = bits or Beam.CHARGE.bit
            return bits
        }

        /** How long to use pending (locally tracked) bitmask before falling back to polled gameState. */
        private const val PENDING_WINDOW_MS = 1000L
    }

    // Pending bitmask state — prevents rapid toggles from reverting each other
    // due to stale gameState (polled every 200ms)
    private var pendingItemBits: Int? = null
    private var pendingBeamBits: Int? = null
    private var lastItemWriteTime: Long = 0
    private var lastBeamWriteTime: Long = 0

    /**
     * Get the effective item bitmask: use pending state if recent, otherwise from gameState.
     */
    private fun effectiveItemBits(gs: GameState): Int {
        val now = System.currentTimeMillis()
        val pending = pendingItemBits
        return if (pending != null && now - lastItemWriteTime < PENDING_WINDOW_MS) {
            logger.debug { "Using pending item bits 0x${pending.toString(16)} (${now - lastItemWriteTime}ms since last write)" }
            pending
        } else {
            pendingItemBits = null
            itemBitsFromGameState(gs)
        }
    }

    private fun effectiveBeamBits(gs: GameState): Int {
        val now = System.currentTimeMillis()
        val pending = pendingBeamBits
        return if (pending != null && now - lastBeamWriteTime < PENDING_WINDOW_MS) {
            pending
        } else {
            pendingBeamBits = null
            beamBitsFromGameState(gs)
        }
    }

    /**
     * Toggle an item on or off. Uses pending state or current game state to compute new bit mask.
     * Writes equipped + collected as a single 4-byte batch (adjacent addresses).
     */
    suspend fun toggleItem(item: Item, enable: Boolean, currentGameState: GameState): Boolean {
        return try {
            val currentBits = effectiveItemBits(currentGameState)
            val newBits = if (enable) currentBits or item.bit else currentBits and item.bit.inv()

            // Write equipped (0x7E09A2) + collected (0x7E09A4) as 4 bytes in one shot
            val success = writeDword(EQUIPPED_ITEMS, newBits, newBits)
            if (success) {
                pendingItemBits = newBits
                lastItemWriteTime = System.currentTimeMillis()
                logger.info { "${if (enable) "+" else "-"} ${item.displayName} (wrote 0x${newBits.toString(16)} to items via ${memoryAdapter.getAdapterName()})" }
            }
            success
        } catch (e: Exception) {
            logger.error(e) { "Failed to toggle ${item.displayName}" }
            false
        }
    }

    /**
     * Toggle a beam on or off. Uses pending state or current game state to compute new bit mask.
     * Spazer and Plasma are mutually exclusive in vanilla SM.
     */
    suspend fun toggleBeam(beam: Beam, enable: Boolean, currentGameState: GameState): Boolean {
        return try {
            var newBits = effectiveBeamBits(currentGameState)
            newBits = if (enable) newBits or beam.bit else newBits and beam.bit.inv()

            // Spazer and Plasma are mutually exclusive — enabling one disables the other
            if (enable && beam == Beam.SPAZER) {
                newBits = newBits and Beam.PLASMA.bit.inv()
            } else if (enable && beam == Beam.PLASMA) {
                newBits = newBits and Beam.SPAZER.bit.inv()
            }

            // Write equipped (0x7E09A6) + collected (0x7E09A8) as 4 bytes in one shot
            val success = writeDword(EQUIPPED_BEAMS, newBits, newBits)
            if (success) {
                pendingBeamBits = newBits
                lastBeamWriteTime = System.currentTimeMillis()
                logger.info { "${if (enable) "+" else "-"} ${beam.displayName} (wrote 0x${newBits.toString(16)} to beams via ${memoryAdapter.getAdapterName()})" }
            }
            success
        } catch (e: Exception) {
            logger.error(e) { "Failed to toggle ${beam.displayName}" }
            false
        }
    }

    /**
     * Set ammo current and max values. No reads — values provided by caller from GameState.
     */
    suspend fun setAmmo(currentAddr: Int, maxAddr: Int, current: Int, max: Int): Boolean {
        return try {
            logger.info { "Setting ammo at 0x${currentAddr.toString(16)}: $current / $max (via ${memoryAdapter.getAdapterName()})" }
            // Write current + max as adjacent pair (current is at lower address, max at +2)
            writeDword(currentAddr, current.coerceAtMost(max), max)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set ammo at 0x${currentAddr.toString(16)}" }
            false
        }
    }

    /**
     * Refill: set current = max. Max value provided by caller from GameState.
     */
    suspend fun refillAmmo(currentAddr: Int, maxValue: Int): Boolean {
        if (maxValue <= 0) return true // Nothing to refill
        logger.info { "Refilling 0x${currentAddr.toString(16)} to $maxValue (via ${memoryAdapter.getAdapterName()})" }
        return writeWord(currentAddr, maxValue)
    }

    /** Refill all ammo from GameState values. */
    suspend fun refillAll(gs: GameState): Boolean {
        return refillAmmo(CURRENT_ENERGY, gs.maxHealth) &&
            refillAmmo(CURRENT_MISSILES, gs.maxMissiles) &&
            refillAmmo(CURRENT_SUPERS, gs.maxSupers) &&
            refillAmmo(CURRENT_POWER_BOMBS, gs.maxPowerBombs) &&
            refillAmmo(CURRENT_RESERVE, gs.maxReserveEnergy)
    }

    /** Give all items and beams — write full bit masks directly. */
    suspend fun giveAllItems(): Boolean {
        // All items = OR of all item bits
        val allItems = Item.entries.fold(0) { acc, item -> acc or item.bit }
        // All beams except Spazer (Plasma is strictly better and they conflict)
        val allBeams = Beam.entries.filter { it != Beam.SPAZER }.fold(0) { acc, beam -> acc or beam.bit }

        logger.info { "Giving all items (0x${allItems.toString(16)}) and beams (0x${allBeams.toString(16)}) via ${memoryAdapter.getAdapterName()}" }

        // Write items (equipped + collected) and beams (equipped + collected) as batch writes
        val itemsOk = writeDword(EQUIPPED_ITEMS, allItems, allItems)
        val beamsOk = writeDword(EQUIPPED_BEAMS, allBeams, allBeams)

        if (itemsOk) {
            pendingItemBits = allItems
            lastItemWriteTime = System.currentTimeMillis()
        }
        if (beamsOk) {
            pendingBeamBits = allBeams
            lastBeamWriteTime = System.currentTimeMillis()
        }

        return itemsOk && beamsOk
    }

    /**
     * Write a 16-bit little-endian value to a single address (2 bytes).
     */
    private suspend fun writeWord(address: Int, value: Int): Boolean {
        val lo = value and 0xFF
        val hi = (value shr 8) and 0xFF
        val bytes = byteArrayOf(lo.toByte(), hi.toByte())
        logger.debug { "writeWord(0x${address.toString(16)}, $value) → [${"%02x".format(lo)},${"%02x".format(hi)}]" }
        val success = memoryAdapter.writeMemory(address, bytes)
        if (!success) {
            logger.warn { "writeWord FAILED for 0x${address.toString(16)}, value=$value" }
        }
        return success
    }

    /**
     * Write two adjacent 16-bit LE values as a single 4-byte write.
     * Used for equipped+collected pairs (e.g., 0x7E09A2 equipped, 0x7E09A4 collected).
     */
    private suspend fun writeDword(baseAddress: Int, lowWord: Int, highWord: Int): Boolean {
        val b0 = lowWord and 0xFF
        val b1 = (lowWord shr 8) and 0xFF
        val b2 = highWord and 0xFF
        val b3 = (highWord shr 8) and 0xFF
        val bytes = byteArrayOf(b0.toByte(), b1.toByte(), b2.toByte(), b3.toByte())
        logger.debug { "writeDword(0x${baseAddress.toString(16)}, 0x${lowWord.toString(16)}, 0x${highWord.toString(16)}) → [${bytes.joinToString(",") { "%02x".format(it.toInt() and 0xFF) }}]" }
        val success = memoryAdapter.writeMemory(baseAddress, bytes)
        if (!success) {
            logger.warn { "writeDword FAILED for 0x${baseAddress.toString(16)}" }
        }
        return success
    }
}
