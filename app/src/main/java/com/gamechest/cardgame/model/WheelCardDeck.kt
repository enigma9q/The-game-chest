package com.gamechest.cardgame.model

import java.util.UUID

object WheelCardDeck {

    /**
     * Creates a full, balanced standard deck of 108 cards for WheelCard Clash:
     * - Numbers 1-10 in 4 colors (2 sets of 1-10 per color = 80 number cards)
     *   (Including 8 "7" Spin Shield cards!)
     * - 4 Direction Reverse cards (1 per color = 4 cards)
     * - 4 Double Play (+1 card) cards (1 per color = 4 cards)
     * - 8 Basic Spin cards (2 per color = 8 cards)
     * - 2 Super Spin cards (Wild = 2 cards)
     * - 4 Color Choice Wild cards (4 cards)
     * - 2 All-Spin Wild cards (2 cards)
     * - 4 Bet & Spin Wild cards (4 cards)
     * Total = 108 cards
     */
    fun createStandardDeck(): List<WheelCard> {
        val cards = mutableListOf<WheelCard>()
        val baseColors = listOf(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)

        val numberTypes = listOf(
            CardType.NUM_1, CardType.NUM_2, CardType.NUM_3, CardType.NUM_4, CardType.NUM_5,
            CardType.NUM_6, CardType.NUM_7, CardType.NUM_8, CardType.NUM_9, CardType.NUM_10
        )

        // 1. Number Cards (2 copies of 1-10 per color = 80 cards)
        baseColors.forEach { color ->
            repeat(2) { copyIndex ->
                numberTypes.forEach { numType ->
                    cards.add(
                        WheelCard(
                            id = "card_${color.name.lowercase()}_num_${numType.displayName}_$copyIndex",
                            color = color,
                            type = numType
                        )
                    )
                }
            }
        }

        // 2. Action Cards
        baseColors.forEach { color ->
            // Direction Reverse (1 per color = 4 cards)
            cards.add(
                WheelCard(
                    id = "card_${color.name.lowercase()}_reverse",
                    color = color,
                    type = CardType.DIRECTION_REVERSE
                )
            )

            // Double Play (1 per color = 4 cards)
            cards.add(
                WheelCard(
                    id = "card_${color.name.lowercase()}_double_play",
                    color = color,
                    type = CardType.DOUBLE_PLAY
                )
            )

            // Basic Spin (2 per color = 8 cards)
            repeat(2) { copyIdx ->
                cards.add(
                    WheelCard(
                        id = "card_${color.name.lowercase()}_basic_spin_$copyIdx",
                        color = color,
                        type = CardType.BASIC_SPIN
                    )
                )
            }
        }

        // 3. Special Wild Cards (12 cards)
        // Super Spin (2 cards)
        repeat(2) { idx ->
            cards.add(
                WheelCard(
                    id = "card_wild_super_spin_$idx",
                    color = CardColor.WILD,
                    type = CardType.SUPER_SPIN
                )
            )
        }

        // Color Choice (4 cards)
        repeat(4) { idx ->
            cards.add(
                WheelCard(
                    id = "card_wild_color_choice_$idx",
                    color = CardColor.WILD,
                    type = CardType.COLOR_CHOICE
                )
            )
        }

        // All-Spin (2 cards)
        repeat(2) { idx ->
            cards.add(
                WheelCard(
                    id = "card_wild_all_spin_$idx",
                    color = CardColor.WILD,
                    type = CardType.ALL_SPIN
                )
            )
        }

        // Bet & Spin (4 cards)
        repeat(4) { idx ->
            cards.add(
                WheelCard(
                    id = "card_wild_bet_and_spin_$idx",
                    color = CardColor.WILD,
                    type = CardType.BET_AND_SPIN
                )
            )
        }

        return cards.shuffled()
    }
}
