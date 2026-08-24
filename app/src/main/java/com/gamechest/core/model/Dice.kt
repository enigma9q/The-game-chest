package com.gamechest.core.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Standard dice types supported natively across the engine:
 * 1d2 (coin flip), 1d3, 1d4, 1d5, 1d6, 1d8, 1d10, 1d12, 1d20, 1d30, 1d60, 1d100,
 * plus custom NdS+Mod and dynamic range-bounded dice.
 */
@Serializable
enum class StandardDiceType(val sides: Int, val displayName: String) {
    D2(2, "1d2"),
    D3(3, "1d3"),
    D4(4, "1d4"),
    D5(5, "1d5"),
    D6(6, "1d6"),
    D8(8, "1d8"),
    D10(10, "1d10"),
    D12(12, "1d12"),
    D20(20, "1d20"),
    D30(30, "1d30"),
    D60(60, "1d60"),
    D100(100, "1d100");

    companion object {
        fun fromSides(sides: Int): StandardDiceType? = entries.find { it.sides == sides }
    }
}

@Serializable
data class DiceSpec(
    val count: Int = 1,
    val sides: Int = 6,
    val modifier: Int = 0,
    val minOverride: Int? = null,
    val maxOverride: Int? = null,
    val label: String = "${count}d${sides}"
) {
    companion object {
        fun standard(type: StandardDiceType): DiceSpec = DiceSpec(count = 1, sides = type.sides, label = type.displayName)
        fun d6(): DiceSpec = standard(StandardDiceType.D6)
        fun d60(): DiceSpec = standard(StandardDiceType.D60)
        fun custom(count: Int, sides: Int, modifier: Int = 0): DiceSpec =
            DiceSpec(count = count, sides = sides, modifier = modifier, label = "${count}d${sides}${if (modifier > 0) "+$modifier" else if (modifier < 0) "$modifier" else ""}")
        
        /**
         * Dynamic range bounded dice (used for mutators like Nitro Assist where min is player's current tile)
         */
        fun dynamicRange(min: Int, max: Int, label: String = "Range $min..$max"): DiceSpec =
            DiceSpec(count = 1, sides = max, minOverride = min, maxOverride = max, label = label)
    }
}

@Serializable
data class DiceRollResult(
    val spec: DiceSpec,
    val rolls: List<Int>,
    val modifier: Int,
    val total: Int,
    val isMax: Boolean,
    val isMin: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object DiceEngine {
    fun roll(spec: DiceSpec, dynamicMin: Int? = null, dynamicMax: Int? = null, random: Random = Random.Default): DiceRollResult {
        val min = dynamicMin ?: spec.minOverride ?: 1
        val max = dynamicMax ?: spec.maxOverride ?: spec.sides

        if (spec.minOverride != null || dynamicMin != null || spec.maxOverride != null || dynamicMax != null) {
            val clampedMin = min.coerceAtLeast(1)
            val clampedMax = max.coerceAtLeast(clampedMin)
            val rollValue = if (clampedMin == clampedMax) clampedMin else random.nextInt(clampedMin, clampedMax + 1)
            val total = (rollValue + spec.modifier).coerceAtLeast(1)
            return DiceRollResult(
                spec = spec,
                rolls = listOf(rollValue),
                modifier = spec.modifier,
                total = total,
                isMax = rollValue == clampedMax,
                isMin = rollValue == clampedMin
            )
        }

        val individualRolls = (1..spec.count.coerceAtLeast(1)).map {
            random.nextInt(1, spec.sides + 1)
        }
        val total = individualRolls.sum() + spec.modifier
        val maxPossible = spec.count * spec.sides + spec.modifier
        val minPossible = spec.count + spec.modifier

        return DiceRollResult(
            spec = spec,
            rolls = individualRolls,
            modifier = spec.modifier,
            total = total,
            isMax = total >= maxPossible,
            isMin = total <= minPossible
        )
    }
}
