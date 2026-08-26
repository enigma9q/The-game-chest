package com.gamechest.cardgame.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardColor(val displayName: String, val hexColor: String, val secondaryHex: String) {
    RED("Red", "#EF4444", "#991B1B"),
    BLUE("Blue", "#3B82F6", "#1E40AF"),
    GREEN("Green", "#10B981", "#065F46"),
    YELLOW("Yellow", "#F59E0B", "#B45309"),
    WILD("Wild", "#8B5CF6", "#4C1D95")
}

@Serializable
enum class CardType(val displayName: String, val isAction: Boolean, val isWild: Boolean) {
    NUM_1("1", false, false),
    NUM_2("2", false, false),
    NUM_3("3", false, false),
    NUM_4("4", false, false),
    NUM_5("5", false, false),
    NUM_6("6", false, false),
    NUM_7("7", false, false), // Number 7 = Spin Shield!
    NUM_8("8", false, false),
    NUM_9("9", false, false),
    NUM_10("10", false, false),

    DIRECTION_REVERSE("Reverse", true, false),
    DOUBLE_PLAY("Double Play", true, false),
    BASIC_SPIN("Basic Spin", true, false),
    SUPER_SPIN("Super Spin", true, true),

    COLOR_CHOICE("Color Choice", false, true),
    BET_AND_SPIN("Bet & Spin", true, true),
    ALL_SPIN("All-Spin", true, true)
}

@Serializable
data class WheelCard(
    val id: String,
    val color: CardColor,
    val type: CardType,
    val number: Int? = when (type) {
        CardType.NUM_1 -> 1
        CardType.NUM_2 -> 2
        CardType.NUM_3 -> 3
        CardType.NUM_4 -> 4
        CardType.NUM_5 -> 5
        CardType.NUM_6 -> 6
        CardType.NUM_7 -> 7
        CardType.NUM_8 -> 8
        CardType.NUM_9 -> 9
        CardType.NUM_10 -> 10
        else -> null
    }
) {
    val isSpinShield: Boolean get() = type == CardType.NUM_7
    val isWild: Boolean get() = type.isWild || color == CardColor.WILD
    val isAction: Boolean get() = type.isAction

    fun matches(topCard: WheelCard, activeColor: CardColor = topCard.color): Boolean {
        // Wilds can always be played
        if (this.isWild) return true

        // Match active color
        if (this.color == activeColor) return true

        // Match number (if both are number cards)
        if (this.number != null && topCard.number != null && this.number == topCard.number) return true

        // Match action symbol (e.g. Reverse on Reverse, Spin on Spin, etc.)
        if (this.type == topCard.type && this.type != CardType.COLOR_CHOICE) return true

        return false
    }
}
