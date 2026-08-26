package com.gamechest.core.engine

import com.gamechest.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

@kotlinx.serialization.Serializable
data class GameSessionState(
    val pack: GamePack,
    val players: List<PlayerRuntimeState>,
    val currentTurnPlayerIndex: Int = 0,
    val activeMutators: Set<MutatorId> = setOf(MutatorId.CLASSIC_GRAND_PRIX),
    val turnPhase: TurnPhase = TurnPhase.WAITING_FOR_ROLL,
    val currentRollResult: DiceRollResult? = null,
    val currentActiveDiceSpec: DiceSpec = DiceSpec.d6(),
    val winnerPlayerId: String? = null,
    val extraRollAwarded: Boolean = false,
    val extraRollReason: String? = null,
    val logHistory: List<GameLogEntry> = emptyList(),
    val turnNumber: Int = 1
)

@kotlinx.serialization.Serializable
sealed interface GameAction {
    @kotlinx.serialization.Serializable
    data class RollDice(val playerId: String) : GameAction

    @kotlinx.serialization.Serializable
    data class FinishTurn(val playerId: String) : GameAction

    @kotlinx.serialization.Serializable
    data class ResetGame(val keepSettings: Boolean = true) : GameAction
}

sealed interface GameEvent {
    data class DiceRolled(val playerId: String, val result: DiceRollResult) : GameEvent
    data class PlayerMoving(val playerId: String, val fromTileId: Int, val toTileId: Int) : GameEvent
    data class RampShortcutTriggered(val playerId: String, val ramp: TileConnection) : GameEvent
    data class HazardTriggered(val playerId: String, val hazard: TileConnection) : GameEvent
    data class TurnLost(val playerId: String, val reason: String) : GameEvent
    data class ExtraRollGranted(val playerId: String, val reason: String) : GameEvent
    data class PlayerWon(val playerId: String, val playerName: String) : GameEvent
    data class TurnChanged(val nextPlayerId: String, val turnNumber: Int) : GameEvent
}

class GameEngine(
    initialPack: GamePack,
    initialProfiles: List<PlayerProfile>,
    activeMutators: Set<MutatorId> = setOf(MutatorId.CLASSIC_GRAND_PRIX),
    private val random: Random = Random.Default
) {
    private val _state = MutableStateFlow(createInitialState(initialPack, initialProfiles, activeMutators))
    val state: StateFlow<GameSessionState> = _state.asStateFlow()

    private val _events = MutableStateFlow<GameEvent?>(null)
    val events: StateFlow<GameEvent?> = _events.asStateFlow()

    private fun createInitialState(
        pack: GamePack,
        profiles: List<PlayerProfile>,
        mutators: Set<MutatorId>
    ): GameSessionState {
        val isCustomGrid = mutators.contains(MutatorId.CUSTOM_GRID_DICE_LOADOUT)
        
        val playerStates = profiles.mapIndexed { index, profile ->
            val startingTile = if (profile.customStartTile != null) {
                profile.customStartTile
            } else if (isCustomGrid) {
                index.coerceAtMost(pack.tableLayout.finishTileId)
            } else {
                pack.tableLayout.startTileId
            }

            PlayerRuntimeState(
                profile = profile,
                currentTileId = startingTile,
                targetTileId = startingTile
            )
        }

        val initialDiceSpec = resolveActiveDiceSpec(playerStates.firstOrNull()?.profile, mutators, pack)

        return GameSessionState(
            pack = pack,
            players = playerStates,
            currentTurnPlayerIndex = 0,
            activeMutators = mutators,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            currentActiveDiceSpec = initialDiceSpec
        )
    }

    private fun resolveActiveDiceSpec(
        profile: PlayerProfile?,
        mutators: Set<MutatorId>,
        pack: GamePack
    ): DiceSpec {
        return when {
            mutators.contains(MutatorId.NITRO_TARGET_1D60) -> DiceSpec.d60()
            mutators.contains(MutatorId.NITRO_ASSIST_1D60) -> DiceSpec.d60()
            mutators.contains(MutatorId.CUSTOM_GRID_DICE_LOADOUT) && profile?.customDiceSpec != null -> profile.customDiceSpec
            else -> pack.manifest.defaultDiceSpec
        }
    }

    fun getCurrentPlayer(): PlayerRuntimeState? {
        val s = _state.value
        return s.players.getOrNull(s.currentTurnPlayerIndex)
    }

    /**
     * Executes a dice roll for the current active player and resolves movement/rules.
     */
    fun rollDice(playerId: String): DiceRollResult? {
        val s = _state.value
        val currentPlayer = s.players.getOrNull(s.currentTurnPlayerIndex) ?: return null
        if (currentPlayer.profile.id != playerId) return null
        if (s.turnPhase != TurnPhase.WAITING_FOR_ROLL || s.winnerPlayerId != null) return null

        val mutators = s.activeMutators
        val isNitroAssist = mutators.contains(MutatorId.NITRO_ASSIST_1D60)
        val isNitroTarget = mutators.contains(MutatorId.NITRO_TARGET_1D60)
        val isReverseHazard = mutators.contains(MutatorId.REVERSE_HAZARD_OVERDRIVE)
        val finishTileId = s.pack.tableLayout.finishTileId

        // Determine dice specification & dynamic bounds
        val diceSpec = resolveActiveDiceSpec(currentPlayer.profile, mutators, s.pack)
        
        val dynamicMin = if (isNitroAssist) {
            currentPlayer.currentTileId.coerceAtLeast(1)
        } else null

        val dynamicMax = if (isNitroAssist) 60 else null

        val rollResult = DiceEngine.roll(diceSpec, dynamicMin = dynamicMin, dynamicMax = dynamicMax, random = random)

        _events.value = GameEvent.DiceRolled(playerId, rollResult)

        // Log the roll
        val rollMsg = "${currentPlayer.profile.name} rolled ${rollResult.total} on ${rollResult.spec.label}!"
        val logs = s.logHistory.toMutableList()
        logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, rollMsg, "casino"))

        var nextTileId = currentPlayer.currentTileId
        var turnLost = false
        var turnLostReason = ""

        if (isNitroTarget || isNitroAssist) {
            // Nitro Target / Assist 1d60 Mode:
            // "the players move from the place they are to the new number... the first one that gets 50 on the dice wins.
            // If you throw more than 50, you do not move but lose your turn."
            if (rollResult.total > finishTileId) {
                turnLost = true
                turnLostReason = "Overshot with a roll of ${rollResult.total} (> $finishTileId)! Lost turn."
                logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, turnLostReason, "warning"))
                _events.value = GameEvent.TurnLost(playerId, turnLostReason)
            } else {
                nextTileId = rollResult.total
            }
        } else {
            // Standard / Classic Roll Mode: Advance step-by-step
            val candidateTile = currentPlayer.currentTileId + rollResult.total
            if (candidateTile > finishTileId) {
                // Classic bounce-back or stop at finish line
                val overshoot = candidateTile - finishTileId
                nextTileId = (finishTileId - overshoot).coerceAtLeast(1)
                logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "Bounced off finish line to tile $nextTileId!", "swap_horiz"))
            } else {
                nextTileId = candidateTile
            }
        }

        // Apply Ramps & Hazards unless turn was lost (inactive in 1d60 direct target mutator)
        if (!turnLost && !isNitroTarget && !isNitroAssist) {
            val connection = s.pack.tableLayout.connections.find { it.fromTileId == nextTileId }
            if (connection != null) {
                if (isReverseHazard) {
                    // Reversed effects: Slicks boost forward, Ramps overheat backward
                    when (connection.type) {
                        ConnectionType.OIL_SLICK -> {
                            val boost = Math.abs(connection.effectOffset).coerceAtLeast(6)
                            val destination = (nextTileId + boost).coerceAtMost(finishTileId)
                            logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "Reverse Hazard! Oil Drift boosted car from $nextTileId to $destination (+${boost})!", "bolt"))
                            nextTileId = destination
                        }
                        ConnectionType.TURBO_RAMP -> {
                            val penalty = Math.abs(connection.effectOffset).coerceAtLeast(6)
                            val destination = (nextTileId - penalty).coerceAtLeast(1)
                            logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "Reverse Hazard! Turbo Ramp overheated! Dropped from $nextTileId to $destination (-${penalty})!", "build"))
                            nextTileId = destination
                        }
                        else -> {
                            nextTileId = connection.toTileId
                        }
                    }
                } else {
                    // Standard effects
                    when (connection.type) {
                        ConnectionType.TURBO_RAMP -> {
                            _events.value = GameEvent.RampShortcutTriggered(playerId, connection)
                            logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "🚀 HIT TURBO RAMP! Launched from tile ${connection.fromTileId} to ${connection.toTileId}!", "rocket_launch"))
                            nextTileId = connection.toTileId
                        }
                        ConnectionType.OIL_SLICK -> {
                            _events.value = GameEvent.HazardTriggered(playerId, connection)
                            logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "⚠️ HIT OIL SLICK! Spun out from tile ${connection.fromTileId} back to ${connection.toTileId}!", "warning"))
                            nextTileId = connection.toTileId
                        }
                        else -> {
                            nextTileId = connection.toTileId
                        }
                    }
                }
            }
        }

        val hasWon = !turnLost && nextTileId >= finishTileId
        if (hasWon) {
            nextTileId = finishTileId
            logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "🏆 ${currentPlayer.profile.name} CROSSED THE FINISH LINE AND WON THE RACE!", "emoji_events"))
            _events.value = GameEvent.PlayerWon(playerId, currentPlayer.profile.name)
        }

        // ROLL AGAIN RULES:
        // 1. Classic 1d6 mode: Rolling a 6 awards an extra roll!
        // 2. Other modes (1d60 Target, Assist, Custom Dice): Landing on another player's occupied tile awards an extra roll!
        var extraRollAwarded = false
        var extraRollReason: String? = null

        if (!hasWon && !turnLost) {
            val isClassic1d6 = diceSpec.sides == 6 && !isNitroTarget && !isNitroAssist

            if (isClassic1d6 && rollResult.total == 6) {
                extraRollAwarded = true
                extraRollReason = "Rolled a 6!"
                logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "🎲 Rolled a 6! Extra roll awarded!", "casino"))
                _events.value = GameEvent.ExtraRollGranted(playerId, extraRollReason!!)
            } else if (!isClassic1d6 && nextTileId != 0 && nextTileId != finishTileId) {
                val otherPlayerOnTile = s.players.find { it.profile.id != playerId && it.currentTileId == nextTileId }
                if (otherPlayerOnTile != null) {
                    extraRollAwarded = true
                    extraRollReason = if (isNitroTarget) "Space occupied by other player" else "Drafted behind ${otherPlayerOnTile.profile.name} on tile $nextTileId!"
                    logs.add(0, GameLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), playerId, currentPlayer.profile.name, "🏎️ $extraRollReason! Roll again!", "replay"))
                    _events.value = GameEvent.ExtraRollGranted(playerId, extraRollReason!!)
                }
            }
        }

        // Update player runtime state
        val updatedPlayers = s.players.mapIndexed { idx, p ->
            if (idx == s.currentTurnPlayerIndex) {
                p.copy(
                    currentTileId = nextTileId,
                    targetTileId = nextTileId,
                    totalMoves = p.totalMoves + (if (!turnLost) 1 else 0),
                    turnsMissed = p.turnsMissed + (if (turnLost) 1 else 0),
                    lastRollResult = rollResult,
                    hasWon = hasWon
                )
            } else p
        }

        val nextPhase = when {
            hasWon -> TurnPhase.MATCH_FINISHED
            extraRollAwarded -> TurnPhase.WAITING_FOR_ROLL // Player rolls again immediately
            else -> TurnPhase.TURN_OVER
        }

        _state.value = s.copy(
            players = updatedPlayers,
            currentRollResult = rollResult,
            turnPhase = nextPhase,
            winnerPlayerId = if (hasWon) playerId else null,
            extraRollAwarded = extraRollAwarded,
            extraRollReason = extraRollReason,
            logHistory = logs.take(50)
        )

        return rollResult
    }

    /**
     * Advances to the next player's turn.
     */
    fun nextTurn() {
        val s = _state.value
        if (s.winnerPlayerId != null) return

        val nextIndex = (s.currentTurnPlayerIndex + 1) % s.players.size
        val nextPlayer = s.players[nextIndex]
        val nextDiceSpec = resolveActiveDiceSpec(nextPlayer.profile, s.activeMutators, s.pack)

        _state.value = s.copy(
            currentTurnPlayerIndex = nextIndex,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            currentRollResult = null,
            currentActiveDiceSpec = nextDiceSpec,
            extraRollAwarded = false,
            extraRollReason = null,
            turnNumber = s.turnNumber + (if (nextIndex == 0) 1 else 0)
        )

        _events.value = GameEvent.TurnChanged(nextPlayer.profile.id, _state.value.turnNumber)
    }

    fun restartGame(keepMutators: Boolean = true) {
        val s = _state.value
        val profiles = s.players.map { it.profile }
        val mutators = if (keepMutators) s.activeMutators else setOf(MutatorId.CLASSIC_GRAND_PRIX)
        _state.value = createInitialState(s.pack, profiles, mutators)
    }

    /**
     * Authoritative Network State Synchronization for Wi-Fi Co-Op.
     */
    fun syncState(newState: GameSessionState) {
        _state.value = newState
    }
}
