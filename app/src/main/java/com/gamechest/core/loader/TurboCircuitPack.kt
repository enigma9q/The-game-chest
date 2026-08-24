package com.gamechest.core.loader

import com.gamechest.core.model.*

object TurboCircuitPack {

    fun createDefaultPack(): GamePack {
        val finishTileId = 49

        val manifest = GameManifest(
            id = "turbo_circuit_car_snakes_and_ladders",
            title = "Rev-Up Racers: Turbo Circuit",
            version = "1.0.0",
            description = "High-octane car racing board game inspired by Snakes & Ladders on the official Rev-Up Racers circuit! Navigate 49 racetrack tiles from START (0) to FINISH (49) with 5 Turbo Bridges (green lines: launch forward directly across the bridge), avoid 3 Oil Spill hazards (red lines: slide back directly across the spill), and race with 5 thrilling mutators.",
            category = GameCategory.HYBRID_RACE,
            minPlayers = 1,
            maxPlayers = 4,
            supportedTransports = listOf("LOCAL_PASS_AND_PLAY", "WIFI_LAN", "BLUETOOTH"),
            defaultDiceSpec = DiceSpec.d6(),
            availableMutators = listOf(
                MutatorConfig(
                    id = MutatorId.CLASSIC_GRAND_PRIX,
                    name = "1. Classic Grand Prix (1d6)",
                    description = "Roll 1d6, advance step-by-step from START (0) to FINISH ($finishTileId). Bridges launch directly across to higher tiles, oil spills slide directly back. Rolling a 6 rolls again!",
                    iconName = "speed",
                    isActiveByDefault = true,
                    forcedDiceType = StandardDiceType.D6
                ),
                MutatorConfig(
                    id = MutatorId.NITRO_TARGET_1D60,
                    name = "2. Nitro Target (1d60 Full Range)",
                    description = "Roll 1d60! Your car leaps directly to the rolled number. If you roll > $finishTileId, you overshoot and lose your turn! Hit $finishTileId to win.",
                    iconName = "bolt",
                    isActiveByDefault = false,
                    forcedDiceType = StandardDiceType.D60
                ),
                MutatorConfig(
                    id = MutatorId.NITRO_ASSIST_1D60,
                    name = "3. Nitro Assist (1d60 Forward Only)",
                    description = "Roll 1d60, but the lower roll value is locked to your current position. No backtracking! Faster race to the finish (roll > $finishTileId loses turn).",
                    iconName = "rocket_launch",
                    isActiveByDefault = false,
                    forcedDiceType = StandardDiceType.D60
                ),
                MutatorConfig(
                    id = MutatorId.REVERSE_HAZARD_OVERDRIVE,
                    name = "4. Reverse Hazard Overdrive",
                    description = "Oil slicks grant nitro drift boosts forward (+distance), while turbo ramps overheat and send cars back into pit lane!",
                    iconName = "swap_calls",
                    isActiveByDefault = false
                ),
                MutatorConfig(
                    id = MutatorId.CUSTOM_GRID_DICE_LOADOUT,
                    name = "5. Custom Grid & Dice Loadout",
                    description = "Staggered starting grid (e.g. pole positions 0, 1, 2, 3) and custom dice assigned to each player (1d2 to 1d100).",
                    iconName = "tune",
                    isActiveByDefault = false
                )
            ),
            defaultMutatorId = MutatorId.CLASSIC_GRAND_PRIX
        )

        // Exact coordinates matched to the schematic diagram (1024 x 850)
        val rawNodeCoords = listOf(
            Pair(90f, 760f),   // 0: START
            // Outer bottom row:
            Pair(208f, 765f),  // 1
            Pair(316f, 765f),  // 2
            Pair(424f, 765f),  // 3
            Pair(534f, 765f),  // 4 (Oil Spill 3 landing from 25)
            Pair(644f, 765f),  // 5 (Oil Spill 3 landing from 26)
            Pair(748f, 765f),  // 6
            Pair(858f, 760f),  // 7 (Bridge 1 launchpad: 7 -> 29)
            // Outer right column:
            Pair(886f, 626f),  // 8
            Pair(900f, 518f),  // 9
            Pair(908f, 410f),  // 10
            Pair(898f, 306f),  // 11 (Bridge 3 launchpad: 11 -> 31)
            Pair(865f, 145f),  // 12 (Oil Spill 1 landing from 33)
            // Outer top row:
            Pair(744f, 86f),   // 13
            Pair(636f, 86f),   // 14 (Bridge 4 launchpad: 14 -> 35)
            Pair(526f, 86f),   // 15
            Pair(418f, 86f),   // 16
            Pair(310f, 86f),   // 17
            Pair(198f, 108f),  // 18 (Bridge 5 launchpad: 18 -> 37)
            // Outer left column:
            Pair(126f, 186f),  // 19
            Pair(48f, 266f),   // 20
            Pair(52f, 370f),   // 21
            Pair(64f, 490f),   // 22 (Oil Spill 2 landing from 41)
            Pair(72f, 574f),   // 23
            Pair(176f, 604f),  // 24
            // Middle loop:
            Pair(284f, 608f),  // 25 (Oil Spill 3 launchpad: 25 -> 4)
            Pair(386f, 608f),  // 26 (Oil Spill 3 launchpad: 26 -> 5)
            Pair(508f, 626f),  // 27 (Bridge 2 launchpad: 27 -> 45)
            Pair(616f, 608f),  // 28
            Pair(724f, 608f),  // 29 (Bridge 1 landing from 7)
            Pair(774f, 514f),  // 30
            Pair(782f, 412f),  // 31 (Bridge 3 landing from 11)
            Pair(784f, 306f),  // 32
            Pair(724f, 222f),  // 33 (Oil Spill 1 launchpad: 33 -> 12)
            Pair(618f, 222f),  // 34
            Pair(508f, 222f),  // 35 (Bridge 4 landing from 14)
            Pair(398f, 222f),  // 36
            Pair(298f, 238f),  // 37 (Bridge 5 landing from 18)
            Pair(194f, 244f),  // 38
            Pair(136f, 310f),  // 39
            Pair(148f, 416f),  // 40
            Pair(220f, 490f),  // 41 (Oil Spill 2 launchpad: 41 -> 22)
            Pair(328f, 508f),  // 42
            Pair(430f, 508f),  // 43
            Pair(546f, 498f),  // 44
            Pair(650f, 460f),  // 45 (Bridge 2 landing from 27)
            // Inner straight to finish:
            Pair(656f, 366f),  // 46
            Pair(546f, 366f),  // 47
            Pair(436f, 366f),  // 48
            Pair(312f, 366f)   // 49: FINISH
        )

        val imageW = 1024f
        val imageH = 850f

        val tiles = rawNodeCoords.mapIndexed { i, (px, py) ->
            val type = when (i) {
                0 -> TileType.START
                49 -> TileType.FINISH
                // Bridges start at SMALLER number (Launchpad):
                7, 11, 14, 18, 27 -> TileType.TURBO_RAMP_START
                // Bridges land at BIGGER number (Landing):
                29, 31, 35, 37, 45 -> TileType.TURBO_RAMP_END
                // Oil Spills start at BIGGER number (Hazard entry):
                25, 26, 33, 41 -> TileType.OIL_SLICK_START
                // Oil Spills land at SMALLER number (Slide destination):
                4, 5, 12, 22 -> TileType.OIL_SLICK_END
                10, 20, 30, 40 -> TileType.CHECKPOINT
                else -> TileType.NORMAL
            }
            val accentColor = when (type) {
                TileType.START -> "#10B981"
                TileType.FINISH -> "#F59E0B"
                TileType.TURBO_RAMP_START, TileType.TURBO_RAMP_END -> "#06B6D4"
                TileType.OIL_SLICK_START, TileType.OIL_SLICK_END -> "#EF4444"
                TileType.CHECKPOINT -> "#8B5CF6"
                else -> "#3B82F6"
            }
            TileNode(
                id = i,
                index = i,
                label = when (i) {
                    0 -> "START"
                    49 -> "FINISH"
                    else -> "$i"
                },
                x = px / imageW,
                y = py / imageH,
                type = type,
                description = when (type) {
                    TileType.START -> "Starting Grid (Tile 0)"
                    TileType.FINISH -> "Checkered Flag Finish Line (Tile 49)"
                    TileType.TURBO_RAMP_START -> "Turbo Bridge Launchpad (Traverse directly to bigger number / Shortcut)"
                    TileType.TURBO_RAMP_END -> "Turbo Bridge Landing"
                    TileType.OIL_SLICK_START -> "Oil Spill Hazard (Traverse directly to smaller number / Slide Back)"
                    TileType.OIL_SLICK_END -> "Oil Spill Spinout Landing"
                    TileType.CHECKPOINT -> "Speed Trap Checkpoint"
                    else -> "Track Circuit Tile $i"
                },
                accentColorHex = accentColor
            )
        }

        // BRIDGES (Green lines: Smaller number -> Bigger number / Shortcuts):
        // OIL SPILLS (Red lines: Bigger number -> Smaller number / Hazards):
        val connections = listOf(
            // Bridge 1: 7 -> 29 (+22 shortcut)
            TileConnection(
                id = "bridge_1",
                fromTileId = 7,
                toTileId = 29,
                type = ConnectionType.TURBO_RAMP,
                effectOffset = 22,
                description = "Turbo Bridge 1: Mountain curve ramp directly from tile 7 to tile 29 (+22)!"
            ),
            // Bridge 2: 27 -> 45 (+18 shortcut)
            TileConnection(
                id = "bridge_2",
                fromTileId = 27,
                toTileId = 45,
                type = ConnectionType.TURBO_RAMP,
                effectOffset = 18,
                description = "Turbo Bridge 2: Long overpass ramp directly from tile 27 to tile 45 (+18)!"
            ),
            // Bridge 3: 11 -> 31 (+20 shortcut)
            TileConnection(
                id = "bridge_3",
                fromTileId = 11,
                toTileId = 31,
                type = ConnectionType.TURBO_RAMP,
                effectOffset = 20,
                description = "Turbo Bridge 3: Mountain pass ramp directly from tile 11 to tile 31 (+20)!"
            ),
            // Bridge 4: 14 -> 35 (+21 shortcut)
            TileConnection(
                id = "bridge_4",
                fromTileId = 14,
                toTileId = 35,
                type = ConnectionType.TURBO_RAMP,
                effectOffset = 21,
                description = "Turbo Bridge 4: Top highway overpass directly from tile 14 to tile 35 (+21)!"
            ),
            // Bridge 5: 18 -> 37 (+19 shortcut)
            TileConnection(
                id = "bridge_5",
                fromTileId = 18,
                toTileId = 37,
                type = ConnectionType.TURBO_RAMP,
                effectOffset = 19,
                description = "Turbo Bridge 5: Factory elevated ramp directly from tile 18 to tile 37 (+19)!"
            ),

            // Oil Spill 1: 33 -> 12 (-21 spinout)
            TileConnection(
                id = "oil_spill_1",
                fromTileId = 33,
                toTileId = 12,
                type = ConnectionType.OIL_SLICK,
                effectOffset = -21,
                description = "Oil Spill 1: Rainbow oil lake spinout directly back from tile 33 to tile 12 (-21)!"
            ),
            // Oil Spill 2: 41 -> 22 (-19 spinout)
            TileConnection(
                id = "oil_spill_2",
                fromTileId = 41,
                toTileId = 22,
                type = ConnectionType.OIL_SLICK,
                effectOffset = -19,
                description = "Oil Spill 2: Major oil lake spinout directly back from tile 41 to tile 22 (-19)!"
            ),
            // Oil Spill 3a: 25 -> 4 (-21 spinout)
            TileConnection(
                id = "oil_spill_3a",
                fromTileId = 25,
                toTileId = 4,
                type = ConnectionType.OIL_SLICK,
                effectOffset = -21,
                description = "Oil Spill 3: Skidmark slide directly back from tile 25 to tile 4 (-21)!"
            ),
            // Oil Spill 3b: 26 -> 5 (-21 spinout)
            TileConnection(
                id = "oil_spill_3b",
                fromTileId = 26,
                toTileId = 5,
                type = ConnectionType.OIL_SLICK,
                effectOffset = -21,
                description = "Oil Spill 3: Skidmark slide directly back from tile 26 to tile 5 (-21)!"
            )
        )

        val layout = TableLayoutConfig(
            layoutType = TableLayoutType.RACETRACK_CIRCUIT,
            tilesCount = 49,
            startTileId = 0,
            finishTileId = 49,
            tiles = tiles,
            connections = connections,
            backgroundImageAsset = "rev_up_racers_board.jpg",
            aspectRatio = 1024f / 850f
        )

        return GamePack(
            manifest = manifest,
            tableLayout = layout
        )
    }
}
