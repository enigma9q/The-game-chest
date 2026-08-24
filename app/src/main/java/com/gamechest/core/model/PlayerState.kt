package com.gamechest.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CarAvatar(val displayName: String, val colorHex: String, val assetName: String) {
    SPEEDSTER_RED("Speedster Red", "#EF4444", "car_red"),
    TURBO_BLUE("Turbo Blue", "#3B82F6", "car_blue"),
    CYBER_YELLOW("Cyber Yellow", "#F59E0B", "car_yellow"),
    NITRO_GREEN("Nitro Green", "#10B981", "car_green"),
    APEX_PURPLE("Apex Purple", "#8B5CF6", "car_purple"),
    MAGMA_ORANGE("Magma Orange", "#F97316", "car_orange"),
    NEON_CYAN("Neon Cyan", "#06B6D4", "car_cyan"),
    HOT_PINK("Hot Pink", "#EC4899", "car_pink")
}

@Serializable
data class PlayerProfile(
    val id: String,
    val name: String,
    val carAvatar: CarAvatar = CarAvatar.SPEEDSTER_RED,
    val customDiceSpec: DiceSpec? = null,
    val customStartTile: Int? = null,
    val isBot: Boolean = false,
    val isLocal: Boolean = true
)

@Serializable
data class PlayerRuntimeState(
    val profile: PlayerProfile,
    val currentTileId: Int = 0,
    val targetTileId: Int = 0,
    val lapsCompleted: Int = 0,
    val totalMoves: Int = 0,
    val turnsMissed: Int = 0,
    val lastRollResult: DiceRollResult? = null,
    val hasWon: Boolean = false,
    val placement: Int = 0
)

@Serializable
enum class TurnPhase {
    WAITING_FOR_ROLL,
    ROLLING,
    ANIMATING_MOVE,
    RESOLVING_EFFECT,
    TURN_OVER,
    MATCH_FINISHED
}

@Serializable
data class GameLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playerId: String,
    val playerName: String,
    val message: String,
    val icon: String = "info"
)
