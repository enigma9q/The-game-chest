package com.gamechest

import com.gamechest.core.engine.GameEngine
import com.gamechest.core.loader.GamePackManager
import com.gamechest.core.loader.TurboCircuitPack
import com.gamechest.core.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val testPack = TurboCircuitPack.createDefaultPack()
    private val testProfiles = listOf(
        PlayerProfile("p1", "Racer 1", CarAvatar.SPEEDSTER_RED),
        PlayerProfile("p2", "Racer 2", CarAvatar.TURBO_BLUE)
    )

    @Test
    fun testStandardDiceSupport_AllTypes() {
        StandardDiceType.entries.forEach { type ->
            val spec = DiceSpec.standard(type)
            val result = DiceEngine.roll(spec, random = Random(42))
            assertTrue("Roll ${result.total} for ${type.displayName} should be >= 1", result.total >= 1)
            assertTrue("Roll ${result.total} for ${type.displayName} should be <= ${type.sides}", result.total <= type.sides)
        }
    }

    @Test
    fun testClassicGrandPrix_MovementAndRamp() {
        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = testProfiles,
            activeMutators = setOf(MutatorId.CLASSIC_GRAND_PRIX)
        )

        val player1 = engine.getCurrentPlayer()
        assertNotNull(player1)
        assertEquals(0, player1?.currentTileId)

        // Roll dice
        val roll = engine.rollDice("p1")
        assertNotNull(roll)
        assertTrue("1d6 roll must be between 1 and 6 (was ${roll?.total})", roll!!.total in 1..6)
        val stateAfterRoll = engine.state.value
        val updatedP1 = stateAfterRoll.players.first()

        assertTrue("Player 1 should move forward from tile 0", updatedP1.currentTileId > 0)
        assertTrue("Player 1 position should be <= 6 after one 1d6 roll from tile 0 (or 14 if hit ramp at 4)", updatedP1.currentTileId in 1..14)
    }

    @Test
    fun testNitroTarget1d60_OvershootLosesTurn() {
        // Deterministic random generator returning 55 (> 50)
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 55
        }

        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = testProfiles,
            activeMutators = setOf(MutatorId.NITRO_TARGET_1D60),
            random = fixedRandom
        )

        val roll = engine.rollDice("p1")
        assertNotNull(roll)
        assertEquals(55, roll?.total)

        val state = engine.state.value
        val p1 = state.players.first()
        assertEquals("Player should remain on start tile 0 on overshoot", 0, p1.currentTileId)
        assertEquals("Turn should be marked as missed", 1, p1.turnsMissed)
        assertNull("Should not win on overshoot", state.winnerPlayerId)
    }

    @Test
    fun testNitroTarget1d60_Exact50Wins() {
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 49
        }

        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = testProfiles,
            activeMutators = setOf(MutatorId.NITRO_TARGET_1D60),
            random = fixedRandom
        )

        val roll = engine.rollDice("p1")
        assertNotNull(roll)
        assertEquals(49, roll?.total)

        val state = engine.state.value
        val p1 = state.players.first()
        assertEquals(49, p1.currentTileId)
        assertTrue(p1.hasWon)
        assertEquals("p1", state.winnerPlayerId)
    }

    @Test
    fun testNitroAssist1d60_NoBacktrackingLowerBound() {
        // When player is on tile 20, roll should be in range 20..60
        val profilesOnTile20 = listOf(
            PlayerProfile("p1", "Racer 1", CarAvatar.SPEEDSTER_RED, customStartTile = 20)
        )

        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = profilesOnTile20,
            activeMutators = setOf(MutatorId.NITRO_ASSIST_1D60)
        )

        val roll = engine.rollDice("p1")
        assertNotNull(roll)
        assertTrue("Roll with Nitro Assist on tile 20 must be >= 20", (roll?.total ?: 0) >= 20)
    }

    @Test
    fun testCustomGridAndDiceLoadout_Mutator() {
        val customProfiles = listOf(
            PlayerProfile("p1", "Racer 1", CarAvatar.SPEEDSTER_RED, customDiceSpec = DiceSpec.standard(StandardDiceType.D20), customStartTile = 10),
            PlayerProfile("p2", "Racer 2", CarAvatar.TURBO_BLUE, customDiceSpec = DiceSpec.standard(StandardDiceType.D12), customStartTile = 15)
        )

        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = customProfiles,
            activeMutators = setOf(MutatorId.CUSTOM_GRID_DICE_LOADOUT)
        )

        val state = engine.state.value
        assertEquals(10, state.players[0].currentTileId)
        assertEquals(15, state.players[1].currentTileId)
        assertEquals("1d20", state.currentActiveDiceSpec.label)
    }

    @Test
    fun testClassic1d6_Roll6AwardsExtraRoll() {
        val fixedSixRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }

        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = testProfiles,
            activeMutators = setOf(MutatorId.CLASSIC_GRAND_PRIX),
            random = fixedSixRandom
        )

        val roll = engine.rollDice("p1")
        assertNotNull(roll)
        assertEquals(6, roll?.total)

        val state = engine.state.value
        assertTrue("Rolling a 6 in 1d6 mode must award an extra roll", state.extraRollAwarded)
        assertEquals("Player 1 must keep their turn to roll again", TurnPhase.WAITING_FOR_ROLL, state.turnPhase)
        assertEquals("Player 1 should still be current player", 0, state.currentTurnPlayerIndex)
    }

    @Test
    fun testOtherMode_CollisionDraftAwardsExtraRoll() {
        // Player 2 is on tile 30. Player 1 rolls 30 in Nitro Target mode.
        val profilesWithP2Ahead = listOf(
            PlayerProfile("p1", "Racer 1", CarAvatar.SPEEDSTER_RED, customStartTile = 0),
            PlayerProfile("p2", "Racer 2", CarAvatar.TURBO_BLUE, customStartTile = 30)
        )

        val fixedThirtyRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 30
        }

        val engine = GameEngine(
            initialPack = testPack,
            initialProfiles = profilesWithP2Ahead,
            activeMutators = setOf(MutatorId.CUSTOM_GRID_DICE_LOADOUT, MutatorId.NITRO_TARGET_1D60),
            random = fixedThirtyRandom
        )

        val roll = engine.rollDice("p1")
        assertNotNull(roll)
        assertEquals(30, roll?.total)

        val state = engine.state.value
        assertTrue("Landing on another player's spot in non-1d6 modes must award an extra roll", state.extraRollAwarded)
        assertEquals("Player 1 must stay in WAITING_FOR_ROLL phase", TurnPhase.WAITING_FOR_ROLL, state.turnPhase)
        assertTrue(state.extraRollReason?.contains("Racer 2") == true)
    }

    @Test
    fun testGamePackManager_BuiltInPackLoaded() {
        val manager = GamePackManager()
        val packs = manager.getAllPacks()
        assertTrue("Built-in Turbo Circuit pack must be loaded", packs.isNotEmpty())
        assertEquals("turbo_circuit_car_snakes_and_ladders", packs.first().manifest.id)
        assertEquals(49, packs.first().tableLayout.tilesCount)
        assertEquals(5, packs.first().manifest.availableMutators.size)
    }
}
