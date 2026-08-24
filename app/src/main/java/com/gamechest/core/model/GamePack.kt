package com.gamechest.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameCategory {
    BOARD_GAME,
    CARD_GAME,
    HYBRID_RACE
}

@Serializable
enum class TableLayoutType {
    RACETRACK_CIRCUIT,
    GRID_TILES,
    SNAKE_LADDER_MATRIX,
    CARD_TABLE_WITH_HANDS,
    FREEFORM_GRAPH
}

@Serializable
enum class TileType {
    START,
    NORMAL,
    CHECKPOINT,
    TURBO_RAMP_START,
    TURBO_RAMP_END,
    OIL_SLICK_START,
    OIL_SLICK_END,
    FINISH
}

@Serializable
data class TileNode(
    val id: Int,
    val index: Int,
    val label: String,
    val x: Float, // Normalized 0.0f .. 1.0f on board canvas
    val y: Float, // Normalized 0.0f .. 1.0f on board canvas
    val type: TileType = TileType.NORMAL,
    val description: String = "",
    val accentColorHex: String = "#3B82F6"
)

@Serializable
enum class ConnectionType {
    TURBO_RAMP, // Ladder / Shortcut forward
    OIL_SLICK,  // Snake / Hazard backward
    PORTAL,
    DETOUR
}

@Serializable
data class TileConnection(
    val id: String,
    val fromTileId: Int,
    val toTileId: Int,
    val type: ConnectionType,
    val title: String = "",
    val description: String = "",
    val effectOffset: Int = 0 // e.g. +14 for ladder or -10 for hazard
)

@Serializable
enum class MutatorId {
    CLASSIC_GRAND_PRIX,      // Mutator 1: Standard 1d6 roll & advance
    NITRO_TARGET_1D60,       // Mutator 2: 1d60 direct target jump (roll > 50 loses turn)
    NITRO_ASSIST_1D60,       // Mutator 3: 1d60 forward-only (lower value is current tile position, no backtracking, >50 loses turn)
    REVERSE_HAZARD_OVERDRIVE,// Mutator 4: Oil slicks become speed boosts, ramps overheat/penalize
    CUSTOM_GRID_DICE_LOADOUT // Mutator 5: Staggered starting grid + custom dice per player (1d2 to 1d100)
}

@Serializable
data class MutatorConfig(
    val id: MutatorId,
    val name: String,
    val description: String,
    val iconName: String = "flag",
    val isActiveByDefault: Boolean = false,
    val forcedDiceType: StandardDiceType? = null
)

@Serializable
data class TableLayoutConfig(
    val layoutType: TableLayoutType = TableLayoutType.RACETRACK_CIRCUIT,
    val boardWidth: Float = 1024f,
    val boardHeight: Float = 850f,
    val tilesCount: Int = 56,
    val startTileId: Int = 0,
    val finishTileId: Int = 56,
    val tiles: List<TileNode> = emptyList(),
    val connections: List<TileConnection> = emptyList(),
    val backgroundImageAsset: String? = null,
    val aspectRatio: Float = 1024f / 850f
)

@Serializable
data class GameManifest(
    val id: String,
    val title: String,
    val version: String,
    val description: String,
    val category: GameCategory,
    val minPlayers: Int = 1,
    val maxPlayers: Int = 4,
    val supportedTransports: List<String> = listOf("LOCAL_PASS_AND_PLAY", "WIFI_LAN", "BLUETOOTH"),
    val defaultDiceSpec: DiceSpec = DiceSpec.d6(),
    val availableMutators: List<MutatorConfig> = emptyList(),
    val defaultMutatorId: MutatorId = MutatorId.CLASSIC_GRAND_PRIX,
    val previewImage: String = "preview.png"
)

@Serializable
data class GamePack(
    val manifest: GameManifest,
    val tableLayout: TableLayoutConfig,
    val rulesDescription: String = ""
)
