package com.gamechest.core.loader

import com.gamechest.core.model.*

object SaveTheSheepPack {

    fun createPack(): GamePack {
        val finishTileId = 40

        val manifest = GameManifest(
            id = "save_the_sheep_board_game",
            title = "Save the Sheep! («Σώσε τα Προβατάκια»)",
            version = "1.0.0",
            description = "Cooperative & racing family board game inspired by 'Σώσε τα Προβατάκια'! Guide your flock across 40 meadow tiles to the safe cottage barn before the sneaky wolf reaches the pasture.",
            category = GameCategory.BOARD_GAME,
            minPlayers = 1,
            maxPlayers = 4,
            supportedTransports = listOf("LOCAL_PASS_AND_PLAY", "WIFI_LAN", "BLUETOOTH"),
            defaultDiceSpec = DiceSpec.d6(),
            availableMutators = listOf(
                MutatorConfig(
                    id = MutatorId.CLASSIC_GRAND_PRIX,
                    name = "1. Co-op Meadow Rescue (1d6)",
                    description = "Standard 1d6 roll. Rolling a 6 grants a bonus flock sprint and extra roll!",
                    iconName = "speed",
                    isActiveByDefault = true,
                    forcedDiceType = StandardDiceType.D6
                ),
                MutatorConfig(
                    id = MutatorId.NITRO_TARGET_1D60,
                    name = "2. Wolf Alert Sprint (1d60)",
                    description = "1d60 direct target jump. Rolling > $finishTileId triggers the wolf alert, losing your turn!",
                    iconName = "bolt",
                    isActiveByDefault = false,
                    forcedDiceType = StandardDiceType.D60
                ),
                MutatorConfig(
                    id = MutatorId.NITRO_ASSIST_1D60,
                    name = "3. Sheepdog Escort (1d60 Assist)",
                    description = "1d60 forward assist bounded by your current meadow tile. Rolling > $finishTileId loses turn.",
                    iconName = "rocket_launch",
                    isActiveByDefault = false,
                    forcedDiceType = StandardDiceType.D60
                ),
                MutatorConfig(
                    id = MutatorId.REVERSE_HAZARD_OVERDRIVE,
                    name = "4. Reverse Wolf Tracks",
                    description = "Wolf trails become sheepdog speed boosts, while bridge shortcuts become muddy detours!",
                    iconName = "swap_calls",
                    isActiveByDefault = false
                ),
                MutatorConfig(
                    id = MutatorId.CUSTOM_GRID_DICE_LOADOUT,
                    name = "5. Custom Shepherd Dice",
                    description = "Assign each player their own custom die (1d2 to 1d100) and custom starting pasture tile.",
                    iconName = "tune",
                    isActiveByDefault = false
                )
            ),
            defaultMutatorId = MutatorId.CLASSIC_GRAND_PRIX
        )

        // 40 Meadow Nodes on Save the Sheep Board Layout
        val tiles = listOf(
            // START: Pasture Gate (Tile 0)
            TileNode(id = 0, index = 0, label = "START", x = 0.11f, y = 0.13f, type = TileType.START, description = "Pasture Gate Start! Ready to guide the sheep to safety.", accentColorHex = "#10B981"),

            // Top-Left Meadow Loop (1..8)
            TileNode(id = 1, index = 1, label = "1", x = 0.18f, y = 0.17f, type = TileType.NORMAL, description = "Sunny Meadow Path"),
            TileNode(id = 2, index = 2, label = "2", x = 0.23f, y = 0.16f, type = TileType.NORMAL, description = "Clover Field"),
            TileNode(id = 3, index = 3, label = "3", x = 0.30f, y = 0.14f, type = TileType.NORMAL, description = "Daisy Patch"),
            TileNode(id = 4, index = 4, label = "4", x = 0.35f, y = 0.16f, type = TileType.NORMAL, description = "Fenced Meadow"),
            TileNode(id = 5, index = 5, label = "5", x = 0.41f, y = 0.22f, type = TileType.TURBO_RAMP_START, description = "Safe Wooden Bridge 1 (Shortcut to Tile 18!)", accentColorHex = "#06B6D4"),
            TileNode(id = 6, index = 6, label = "6", x = 0.37f, y = 0.29f, type = TileType.NORMAL, description = "Riverside Path"),
            TileNode(id = 7, index = 7, label = "7", x = 0.29f, y = 0.33f, type = TileType.NORMAL, description = "Gentle Slope"),
            TileNode(id = 8, index = 8, label = "8", x = 0.22f, y = 0.42f, type = TileType.NORMAL, description = "Windy Corner"),

            // Left Stream Crossing (9..16)
            TileNode(id = 9, index = 9, label = "9", x = 0.16f, y = 0.49f, type = TileType.NORMAL, description = "Bridge Approach"),
            TileNode(id = 10, index = 10, label = "10", x = 0.12f, y = 0.58f, type = TileType.NORMAL, description = "Haystack Trail"),
            TileNode(id = 11, index = 11, label = "11", x = 0.10f, y = 0.67f, type = TileType.NORMAL, description = "Wildflower Meadow"),
            TileNode(id = 12, index = 12, label = "12", x = 0.12f, y = 0.77f, type = TileType.NORMAL, description = "Bottom Pasture"),
            TileNode(id = 13, index = 13, label = "13", x = 0.17f, y = 0.86f, type = TileType.NORMAL, description = "Farm Fence"),
            TileNode(id = 14, index = 14, label = "14", x = 0.25f, y = 0.88f, type = TileType.OIL_SLICK_START, description = "Wolf Prowl 1! (Slide back to Tile 4)", accentColorHex = "#EF4444"),
            TileNode(id = 15, index = 15, label = "15", x = 0.34f, y = 0.87f, type = TileType.NORMAL, description = "Stream Bank"),
            TileNode(id = 16, index = 16, label = "16", x = 0.44f, y = 0.84f, type = TileType.NORMAL, description = "Southern Bridge Crossing"),

            // Bottom & South-East Meadow (17..24)
            TileNode(id = 17, index = 17, label = "17", x = 0.53f, y = 0.88f, type = TileType.NORMAL, description = "Poppy Field Trail"),
            TileNode(id = 18, index = 18, label = "18", x = 0.63f, y = 0.90f, type = TileType.TURBO_RAMP_END, description = "Bridge 1 Landing Pad"),
            TileNode(id = 19, index = 19, label = "19", x = 0.73f, y = 0.91f, type = TileType.NORMAL, description = "Sunny Meadow Path"),
            TileNode(id = 20, index = 20, label = "20", x = 0.82f, y = 0.88f, type = TileType.TURBO_RAMP_START, description = "Safe Wooden Bridge 2 (Shortcut to Tile 32!)", accentColorHex = "#06B6D4"),
            TileNode(id = 21, index = 21, label = "21", x = 0.89f, y = 0.81f, type = TileType.NORMAL, description = "Safe Haven Approach"),
            TileNode(id = 22, index = 22, label = "22", x = 0.92f, y = 0.71f, type = TileType.NORMAL, description = "Outer Eastern Path"),
            TileNode(id = 23, index = 23, label = "23", x = 0.90f, y = 0.60f, type = TileType.NORMAL, description = "East River Bank"),
            TileNode(id = 24, index = 24, label = "24", x = 0.86f, y = 0.49f, type = TileType.NORMAL, description = "East Bridge Road"),

            // North-East Meadow (25..32)
            TileNode(id = 25, index = 25, label = "25", x = 0.88f, y = 0.38f, type = TileType.NORMAL, description = "Pine Tree Trail"),
            TileNode(id = 26, index = 26, label = "26", x = 0.89f, y = 0.28f, type = TileType.NORMAL, description = "Wolf Den Ridge"),
            TileNode(id = 27, index = 27, label = "27", x = 0.84f, y = 0.21f, type = TileType.NORMAL, description = "High Pasture"),
            TileNode(id = 28, index = 28, label = "28", x = 0.76f, y = 0.17f, type = TileType.NORMAL, description = "Northern Meadow"),
            TileNode(id = 29, index = 29, label = "29", x = 0.68f, y = 0.17f, type = TileType.NORMAL, description = "Sheep Flock Path"),
            TileNode(id = 30, index = 30, label = "30", x = 0.60f, y = 0.21f, type = TileType.OIL_SLICK_START, description = "Wolf Prowl 2! (Slide back to Tile 16)", accentColorHex = "#EF4444"),
            TileNode(id = 31, index = 31, label = "31", x = 0.54f, y = 0.26f, type = TileType.NORMAL, description = "Cottage Valley"),
            TileNode(id = 32, index = 32, label = "32", x = 0.62f, y = 0.35f, type = TileType.TURBO_RAMP_END, description = "Bridge 2 Landing Pad"),

            // Inner Bridge Incline to Cottage (33..39)
            TileNode(id = 33, index = 33, label = "33", x = 0.71f, y = 0.44f, type = TileType.NORMAL, description = "Cottage Gate Path"),
            TileNode(id = 34, index = 34, label = "34", x = 0.67f, y = 0.54f, type = TileType.NORMAL, description = "Barn Crossing"),
            TileNode(id = 35, index = 35, label = "35", x = 0.61f, y = 0.63f, type = TileType.TURBO_RAMP_START, description = "Cottage Wooden Bridge (Shortcut to Tile 39!)", accentColorHex = "#06B6D4"),
            TileNode(id = 36, index = 36, label = "36", x = 0.51f, y = 0.67f, type = TileType.NORMAL, description = "Front Lawn"),
            TileNode(id = 37, index = 37, label = "37", x = 0.43f, y = 0.63f, type = TileType.NORMAL, description = "Garden Path"),
            TileNode(id = 38, index = 38, label = "38", x = 0.40f, y = 0.54f, type = TileType.NORMAL, description = "Barnyard Gate"),
            TileNode(id = 39, index = 39, label = "39", x = 0.44f, y = 0.45f, type = TileType.TURBO_RAMP_END, description = "Cottage Porch"),

            // FINISH: Cozy Barn (Tile 40)
            TileNode(id = 40, index = 40, label = "HOME", x = 0.51f, y = 0.46f, type = TileType.FINISH, description = "Cozy Barn Home! All sheep safe from the wolf!", accentColorHex = "#F59E0B")
        )

        // 3 Safe Bridge Shortcuts (Smaller -> Bigger) and 2 Wolf Prowl Hazards (Bigger -> Smaller)
        val connections = listOf(
            TileConnection(
                id = "bridge_1",
                fromTileId = 5,
                toTileId = 18,
                type = ConnectionType.TURBO_RAMP,
                title = "Safe Wooden Bridge 1",
                description = "Cross the stream safely! Jump ahead from Tile 5 to Tile 18 (+13 spaces)!",
                effectOffset = 13
            ),
            TileConnection(
                id = "bridge_2",
                fromTileId = 20,
                toTileId = 32,
                type = ConnectionType.TURBO_RAMP,
                title = "Safe Wooden Bridge 2",
                description = "Sprint across Bridge 2! Jump ahead from Tile 20 to Tile 32 (+12 spaces)!",
                effectOffset = 12
            ),
            TileConnection(
                id = "bridge_3",
                fromTileId = 35,
                toTileId = 39,
                type = ConnectionType.TURBO_RAMP,
                title = "Cottage Bridge",
                description = "Cross directly onto the cottage porch from Tile 35 to Tile 39 (+4 spaces)!",
                effectOffset = 4
            ),
            TileConnection(
                id = "wolf_1",
                fromTileId = 14,
                toTileId = 4,
                type = ConnectionType.OIL_SLICK,
                title = "Wolf Prowl 1",
                description = "Sneaky Wolf spotted! Scatter back from Tile 14 to Tile 4 (-10 spaces)!",
                effectOffset = -10
            ),
            TileConnection(
                id = "wolf_2",
                fromTileId = 30,
                toTileId = 16,
                type = ConnectionType.OIL_SLICK,
                title = "Wolf Prowl 2",
                description = "Wolf howl in the meadow! Flee back from Tile 30 to Tile 16 (-14 spaces)!",
                effectOffset = -14
            )
        )

        val tableLayout = TableLayoutConfig(
            layoutType = TableLayoutType.RACETRACK_CIRCUIT,
            boardWidth = 1024f,
            boardHeight = 768f,
            tilesCount = 40,
            startTileId = 0,
            finishTileId = finishTileId,
            tiles = tiles,
            connections = connections,
            backgroundImageAsset = "save_the_sheep_board.jpg",
            aspectRatio = 1024f / 768f
        )

        return GamePack(
            manifest = manifest,
            tableLayout = tableLayout,
            rulesDescription = "Guide all sheep to the cozy barn in the center of the meadow before the wolf catches up!"
        )
    }
}
