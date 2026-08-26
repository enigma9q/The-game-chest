package com.gamechest.core.loader

import com.gamechest.core.model.*

object WheelCardClashPack {

    fun createPack(): GamePack {
        val manifest = GameManifest(
            id = "wheel_card_clash",
            title = "WheelCard Clash",
            version = "1.0.0",
            description = "High-speed tactical card shedding game! Match by color, number, or symbol, spin the Penalty Wheel, deflect attacks with 7 Spin Shields, and be the first to empty your hand!",
            category = GameCategory.CARD_GAME,
            minPlayers = 1,
            maxPlayers = 4,
            supportedTransports = listOf("LOCAL_PASS_AND_PLAY", "WIFI_LAN", "BLUETOOTH"),
            defaultDiceSpec = DiceSpec.d6(),
            availableMutators = listOf(
                MutatorConfig(
                    id = MutatorId.CLASSIC_GRAND_PRIX,
                    name = "1. Classic WheelCard Rules",
                    description = "Standard 7-card deal, interactive Penalty Wheel (+0 to +4), 7 Spin Shield deflections, and Bet & Spin predictions.",
                    iconName = "casino",
                    isActiveByDefault = true
                ),
                MutatorConfig(
                    id = MutatorId.NITRO_TARGET_1D60,
                    name = "2. Chaos Wheel (Double Penalties)",
                    description = "All wheel card draw penalties are doubled! A +4 spin forces drawing 8 cards!",
                    iconName = "bolt",
                    isActiveByDefault = false
                ),
                MutatorConfig(
                    id = MutatorId.REVERSE_HAZARD_OVERDRIVE,
                    name = "3. Shield Overdrive (7s & 10s Shield)",
                    description = "Both Number 7 and Number 10 cards act as Spin Shield counters to reflect spins.",
                    iconName = "shield",
                    isActiveByDefault = false
                ),
                MutatorConfig(
                    id = MutatorId.CUSTOM_GRID_DICE_LOADOUT,
                    name = "4. Quick Blitz (4 Cards)",
                    description = "Fast-paced match! Players start with only 4 cards in hand.",
                    iconName = "speed",
                    isActiveByDefault = false
                )
            ),
            defaultMutatorId = MutatorId.CLASSIC_GRAND_PRIX,
            previewImage = "preview_wheel_card.png"
        )

        val tableLayout = TableLayoutConfig(
            layoutType = TableLayoutType.CARD_TABLE_WITH_HANDS,
            boardWidth = 1024f,
            boardHeight = 768f,
            tilesCount = 0,
            startTileId = 0,
            finishTileId = 0,
            tiles = emptyList(),
            connections = emptyList()
        )

        return GamePack(
            manifest = manifest,
            tableLayout = tableLayout,
            rulesDescription = """
                # WheelCard Clash - Official Rules
                
                ## Objective
                Be the first player to get rid of all cards in your hand.
                
                ## Turn Structure
                Match the top card of the Discard Pile by:
                - Color (Red, Blue, Green, Yellow)
                - Number (1 through 10)
                - Symbol / Action (Reverse, Double Play, Spin)
                - Playing a Wild card (Color Choice, Bet & Spin, All-Spin)
                
                If you have no playable card in hand, draw 1 card from the Draw Pile. If the drawn card can be played, you may place it down immediately; if you still cannot play, you pass the round.
                
                ## Action Cards
                - **Direction Reverse**: Reverses clockwise/counter-clockwise play direction.
                - **Double Play (+1 card)**: Immediately play a second matching card.
                - **Basic Spin**: Forces the next player to spin the Wheel and draw penalty cards (+0, +1, +2, +3, or +4).
                - **Super Spin**: Forces next player to spin the Wheel and draw result + 2 bonus cards (+2 to +6).
                - **Number 7 (Spin Shield)**: Regular number card, but also acts as an out-of-turn reflex counter to block spins and reflect the penalty back to the attacker!
                - **Color Choice (Wild)**: Choose the active color.
                - **Bet & Spin (Wild)**: Predict the Wheel result before spinning. Correct = opponents draw 2 cards. Incorrect = you draw 2 cards.
                - **All-Spin (Wild)**: Every player spins the Wheel in turn order and draws their penalty cards.
                
                ## Last Card Warning
                When you have only 1 card left, you must shout 'CardWheel!' before the next turn begins.
            """.trimIndent()
        )
    }
}
