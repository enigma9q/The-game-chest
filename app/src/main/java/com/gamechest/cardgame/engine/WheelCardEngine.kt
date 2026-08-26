package com.gamechest.cardgame.engine

import com.gamechest.cardgame.model.*
import com.gamechest.core.model.PlayerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class WheelCardPlayer(
    val profile: PlayerProfile,
    val hand: List<WheelCard> = emptyList(),
    val calledLastCard: Boolean = false,
    val score: Int = 0
)

@Serializable
enum class WheelCardTurnPhase {
    WAITING_TO_PLAY,
    COLOR_PICKING,
    WHEEL_SPINNING,
    SPIN_SHIELD_PROMPT,
    BET_PREDICTION,
    ALL_SPIN_STEP,
    ROUND_OVER
}

@Serializable
data class WheelSpinTarget(
    val attackerPlayerId: String,
    val victimPlayerId: String,
    val bonusCards: Int = 0,
    val isSuperSpin: Boolean = false,
    val isBet: Boolean = false,
    val betGuess: Int? = null,
    val allSpinRemainingPlayers: List<String> = emptyList()
)

@Serializable
data class WheelCardSessionState(
    val players: List<WheelCardPlayer>,
    val drawPile: List<WheelCard>,
    val discardPile: List<WheelCard>,
    val activeColor: CardColor,
    val currentTurnPlayerIndex: Int = 0,
    val isClockwise: Boolean = true,
    val turnPhase: WheelCardTurnPhase = WheelCardTurnPhase.WAITING_TO_PLAY,
    val doublePlayActive: Boolean = false,
    val wheelSpinTarget: WheelSpinTarget? = null,
    val lastWheelResult: Int? = null,
    val winnerPlayerId: String? = null,
    val lastActionDescription: String? = null,
    val turnNumber: Int = 1,
    val logHistory: List<String> = emptyList()
)

class WheelCardEngine(
    initialProfiles: List<PlayerProfile>,
    private val random: Random = Random.Default
) {
    private val _state = MutableStateFlow(createInitialSession(initialProfiles))
    val state: StateFlow<WheelCardSessionState> = _state.asStateFlow()

    private fun createInitialSession(profiles: List<PlayerProfile>): WheelCardSessionState {
        val fullDeck = WheelCardDeck.createStandardDeck().toMutableList()

        // Deal 7 cards to each player
        val players = profiles.map { profile ->
            val hand = fullDeck.take(7)
            repeat(7) { fullDeck.removeAt(0) }
            WheelCardPlayer(profile = profile, hand = hand)
        }

        // Top discard card must be a regular number card
        var firstCard = fullDeck.removeAt(0)
        while (firstCard.isWild || firstCard.isAction) {
            fullDeck.add(firstCard)
            firstCard = fullDeck.removeAt(0)
        }

        val discardPile = listOf(firstCard)

        return WheelCardSessionState(
            players = players,
            drawPile = fullDeck,
            discardPile = discardPile,
            activeColor = firstCard.color,
            currentTurnPlayerIndex = 0,
            isClockwise = true,
            turnPhase = WheelCardTurnPhase.WAITING_TO_PLAY,
            lastActionDescription = "Game started. First card is ${firstCard.color.displayName} ${firstCard.number}."
        )
    }

    val topDiscardCard: WheelCard get() = _state.value.discardPile.last()
    val currentPlayer: WheelCardPlayer get() = _state.value.players[_state.value.currentTurnPlayerIndex]

    fun isCardPlayable(card: WheelCard): Boolean {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.WAITING_TO_PLAY) return false
        return card.matches(topDiscardCard, s.activeColor)
    }

    /**
     * Plays a card from the current player's hand.
     */
    fun playCard(cardId: String): Boolean {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.WAITING_TO_PLAY) return false

        val curP = currentPlayer
        val card = curP.hand.find { it.id == cardId } ?: return false
        if (!card.matches(topDiscardCard, s.activeColor)) return false

        val newHand = curP.hand.filter { it.id != cardId }
        val updatedPlayers = s.players.toMutableList().also {
            it[s.currentTurnPlayerIndex] = curP.copy(
                hand = newHand,
                calledLastCard = if (newHand.size == 1) curP.calledLastCard else false
            )
        }

        val newDiscard = s.discardPile + card
        var newActiveColor = if (card.color != CardColor.WILD) card.color else s.activeColor

        // Check Victory
        if (newHand.isEmpty()) {
            _state.value = s.copy(
                players = updatedPlayers,
                discardPile = newDiscard,
                winnerPlayerId = curP.profile.id,
                turnPhase = WheelCardTurnPhase.ROUND_OVER,
                lastActionDescription = "${curP.profile.name} played ${card.color.displayName} ${card.type.displayName} and WON THE GAME! 🎉",
                logHistory = listOf("${curP.profile.name} has won the match!") + s.logHistory
            )
            return true
        }

        // Handle Card Action Effects
        when (card.type) {
            CardType.COLOR_CHOICE -> {
                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = newDiscard,
                    turnPhase = WheelCardTurnPhase.COLOR_PICKING,
                    lastActionDescription = "${curP.profile.name} played Color Choice Wild! Choose next color."
                )
                return true
            }
            CardType.DIRECTION_REVERSE -> {
                val newDirection = !s.isClockwise
                val desc = "${curP.profile.name} played Direction Reverse! Play direction is now ${if (newDirection) "Clockwise" else "Counter-Clockwise"}."
                advanceTurnInternal(
                    stateToAdvance = s.copy(
                        players = updatedPlayers,
                        discardPile = newDiscard,
                        activeColor = newActiveColor,
                        isClockwise = newDirection,
                        lastActionDescription = desc,
                        logHistory = listOf(desc) + s.logHistory
                    )
                )
                return true
            }
            CardType.DOUBLE_PLAY -> {
                // Double Play (+1 card) allows immediately playing a second card!
                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = newDiscard,
                    activeColor = newActiveColor,
                    doublePlayActive = true,
                    lastActionDescription = "${curP.profile.name} played Double Play! You can place down a 2nd card.",
                    logHistory = listOf("${curP.profile.name} used Double Play.") + s.logHistory
                )
                return true
            }
            CardType.BASIC_SPIN, CardType.SUPER_SPIN -> {
                val isSuper = card.type == CardType.SUPER_SPIN
                val victimIndex = getNextPlayerIndex(s.currentTurnPlayerIndex, s.isClockwise, s.players.size)
                val victimPlayer = s.players[victimIndex]

                val target = WheelSpinTarget(
                    attackerPlayerId = curP.profile.id,
                    victimPlayerId = victimPlayer.profile.id,
                    bonusCards = if (isSuper) 2 else 0,
                    isSuperSpin = isSuper
                )

                // Check if victim has a Spin Shield (Number 7)
                val hasShield = victimPlayer.hand.any { it.isSpinShield }

                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = newDiscard,
                    activeColor = newActiveColor,
                    wheelSpinTarget = target,
                    turnPhase = if (hasShield) WheelCardTurnPhase.SPIN_SHIELD_PROMPT else WheelCardTurnPhase.WHEEL_SPINNING,
                    lastActionDescription = "${curP.profile.name} played ${if (isSuper) "Super Spin (+2 bonus)" else "Basic Spin"} on ${victimPlayer.profile.name}!"
                )
                return true
            }
            CardType.BET_AND_SPIN -> {
                val target = WheelSpinTarget(
                    attackerPlayerId = curP.profile.id,
                    victimPlayerId = curP.profile.id,
                    isBet = true
                )
                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = newDiscard,
                    wheelSpinTarget = target,
                    turnPhase = WheelCardTurnPhase.BET_PREDICTION,
                    lastActionDescription = "${curP.profile.name} played Bet & Spin! Predict the Wheel outcome (+0, +1, +2, +3, +4)."
                )
                return true
            }
            CardType.ALL_SPIN -> {
                // Every player in turn order spins the wheel
                val turnOrderIds = s.players.indices.map { offset ->
                    val idx = (s.currentTurnPlayerIndex + offset) % s.players.size
                    s.players[idx].profile.id
                }

                val target = WheelSpinTarget(
                    attackerPlayerId = curP.profile.id,
                    victimPlayerId = turnOrderIds.first(),
                    allSpinRemainingPlayers = turnOrderIds.drop(1)
                )

                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = newDiscard,
                    wheelSpinTarget = target,
                    turnPhase = WheelCardTurnPhase.ALL_SPIN_STEP,
                    lastActionDescription = "${curP.profile.name} played ALL-SPIN! Everyone must spin the Wheel!"
                )
                return true
            }
            else -> {
                // Standard number card
                val desc = "${curP.profile.name} played ${card.color.displayName} ${card.type.displayName}."
                advanceTurnInternal(
                    stateToAdvance = s.copy(
                        players = updatedPlayers,
                        discardPile = newDiscard,
                        activeColor = newActiveColor,
                        doublePlayActive = false,
                        lastActionDescription = desc,
                        logHistory = listOf(desc) + s.logHistory
                    )
                )
                return true
            }
        }
    }

    /**
     * Choose color for Color Choice Wild card.
     */
    fun selectActiveColor(color: CardColor) {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.COLOR_PICKING) return

        val desc = "${currentPlayer.profile.name} chose ${color.displayName}!"
        advanceTurnInternal(
            stateToAdvance = s.copy(
                activeColor = color,
                turnPhase = WheelCardTurnPhase.WAITING_TO_PLAY,
                lastActionDescription = desc,
                logHistory = listOf(desc) + s.logHistory
            )
        )
    }

    /**
     * Spin Shield Counter Response (plays a 7 out-of-turn to deflect the spin back to attacker).
     */
    fun respondWithSpinShield(useShield: Boolean, cardId: String? = null) {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.SPIN_SHIELD_PROMPT) return
        val target = s.wheelSpinTarget ?: return

        if (useShield && cardId != null) {
            val victimIdx = s.players.indexOfFirst { it.profile.id == target.victimPlayerId }
            val victim = s.players[victimIdx]
            val shieldCard = victim.hand.find { it.id == cardId && it.isSpinShield }

            if (shieldCard != null) {
                val newHand = victim.hand.filter { it.id != cardId }
                val updatedPlayers = s.players.toMutableList().also {
                    it[victimIdx] = victim.copy(hand = newHand)
                }

                // Deflect spin back to attacker!
                val reflectedTarget = target.copy(
                    victimPlayerId = target.attackerPlayerId,
                    attackerPlayerId = target.victimPlayerId
                )

                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = s.discardPile + shieldCard,
                    wheelSpinTarget = reflectedTarget,
                    turnPhase = WheelCardTurnPhase.WHEEL_SPINNING,
                    lastActionDescription = "🛡️ ${victim.profile.name} played 7 SPIN SHIELD! Deflected back to ${s.players.find { it.profile.id == target.attackerPlayerId }?.profile?.name}!"
                )
                return
            }
        }

        // Declined or no shield -> proceed to spinning
        _state.value = s.copy(
            turnPhase = WheelCardTurnPhase.WHEEL_SPINNING
        )
    }

    /**
     * Submit prediction for Bet & Spin (+0, +1, +2, +3, +4).
     */
    fun submitBetPrediction(guessNumber: Int) {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.BET_PREDICTION) return
        val target = s.wheelSpinTarget ?: return

        _state.value = s.copy(
            wheelSpinTarget = target.copy(betGuess = guessNumber),
            turnPhase = WheelCardTurnPhase.WHEEL_SPINNING,
            lastActionDescription = "${currentPlayer.profile.name} predicted Wheel will land on +$guessNumber! Spinning now..."
        )
    }

    /**
     * Completes wheel spin result and applies card draw penalties.
     */
    fun resolveWheelSpin(spinResult: Int) {
        val s = _state.value
        val target = s.wheelSpinTarget ?: return

        var mutableDeck = s.drawPile.toMutableList()
        var mutableDiscard = s.discardPile.toMutableList()
        val mutablePlayers = s.players.toMutableList()

        if (target.isBet) {
            val guess = target.betGuess ?: 0
            val isCorrect = (guess == spinResult)
            val curIdx = s.players.indexOfFirst { it.profile.id == target.victimPlayerId }

            if (isCorrect) {
                // Correct: Every opponent draws 2 cards!
                val desc = "🎯 BET WON! Wheel landed on +$spinResult. All opponents draw +2 cards!"
                mutablePlayers.indices.forEach { idx ->
                    if (idx != curIdx) {
                        val cardsDrawn = drawCardsFromDeck(2, mutableDeck, mutableDiscard)
                        mutablePlayers[idx] = mutablePlayers[idx].copy(hand = mutablePlayers[idx].hand + cardsDrawn)
                    }
                }
                advanceTurnInternal(
                    stateToAdvance = s.copy(
                        players = mutablePlayers,
                        drawPile = mutableDeck,
                        discardPile = mutableDiscard,
                        wheelSpinTarget = null,
                        lastWheelResult = spinResult,
                        lastActionDescription = desc,
                        logHistory = listOf(desc) + s.logHistory
                    )
                )
            } else {
                // Incorrect: Player draws 2 cards!
                val desc = "❌ BET MISSED! Wheel landed on +$spinResult (Guessed +$guess). ${currentPlayer.profile.name} draws +2 cards."
                val cardsDrawn = drawCardsFromDeck(2, mutableDeck, mutableDiscard)
                mutablePlayers[curIdx] = mutablePlayers[curIdx].copy(hand = mutablePlayers[curIdx].hand + cardsDrawn)
                advanceTurnInternal(
                    stateToAdvance = s.copy(
                        players = mutablePlayers,
                        drawPile = mutableDeck,
                        discardPile = mutableDiscard,
                        wheelSpinTarget = null,
                        lastWheelResult = spinResult,
                        lastActionDescription = desc,
                        logHistory = listOf(desc) + s.logHistory
                    )
                )
            }
            return
        }

        if (target.allSpinRemainingPlayers.isNotEmpty() || s.turnPhase == WheelCardTurnPhase.ALL_SPIN_STEP) {
            // Apply spin to current victim
            val victimIdx = s.players.indexOfFirst { it.profile.id == target.victimPlayerId }
            val totalPenalty = spinResult
            val cardsDrawn = drawCardsFromDeck(totalPenalty, mutableDeck, mutableDiscard)
            mutablePlayers[victimIdx] = mutablePlayers[victimIdx].copy(hand = mutablePlayers[victimIdx].hand + cardsDrawn)

            val victimName = s.players[victimIdx].profile.name
            val nextVictimId = target.allSpinRemainingPlayers.firstOrNull()

            if (nextVictimId != null) {
                // Continue All-Spin sequence
                val nextTarget = target.copy(
                    victimPlayerId = nextVictimId,
                    allSpinRemainingPlayers = target.allSpinRemainingPlayers.drop(1)
                )
                _state.value = s.copy(
                    players = mutablePlayers,
                    drawPile = mutableDeck,
                    discardPile = mutableDiscard,
                    wheelSpinTarget = nextTarget,
                    lastWheelResult = spinResult,
                    turnPhase = WheelCardTurnPhase.ALL_SPIN_STEP,
                    lastActionDescription = "$victimName drew +$totalPenalty cards! Next up: ${s.players.find { it.profile.id == nextVictimId }?.profile?.name}."
                )
                return
            } else {
                // All-spin completed!
                val desc = "$victimName drew +$totalPenalty cards. All-Spin completed!"
                advanceTurnInternal(
                    stateToAdvance = s.copy(
                        players = mutablePlayers,
                        drawPile = mutableDeck,
                        discardPile = mutableDiscard,
                        wheelSpinTarget = null,
                        lastWheelResult = spinResult,
                        lastActionDescription = desc,
                        logHistory = listOf(desc) + s.logHistory
                    )
                )
                return
            }
        }

        // Standard Basic/Super Spin
        val victimIdx = s.players.indexOfFirst { it.profile.id == target.victimPlayerId }
        val victim = s.players[victimIdx]
        val totalPenalty = spinResult + target.bonusCards

        val cardsDrawn = drawCardsFromDeck(totalPenalty, mutableDeck, mutableDiscard)
        mutablePlayers[victimIdx] = victim.copy(hand = victim.hand + cardsDrawn)

        val desc = if (spinResult == 6) {
            "💣 BOOM! Wheel landed on the BOMB! ${victim.profile.name} drew +$totalPenalty cards!"
        } else if (spinResult == 0 && target.bonusCards == 0) {
            "🍀 LUCKY ESCAPE! Wheel landed on 0! ${victim.profile.name} drew 0 penalty cards!"
        } else {
            "🎡 Wheel landed on +$spinResult${if (target.bonusCards > 0) " (+2 Super Spin bonus)" else ""}! ${victim.profile.name} drew +$totalPenalty cards."
        }

        advanceTurnInternal(
            stateToAdvance = s.copy(
                players = mutablePlayers,
                drawPile = mutableDeck,
                discardPile = mutableDiscard,
                wheelSpinTarget = null,
                lastWheelResult = spinResult,
                lastActionDescription = desc,
                logHistory = listOf(desc) + s.logHistory
            )
        )
    }

    /**
     * Draws 1 card from the draw pile on player turn when having no playable cards.
     */
    fun drawCardFromPile(): WheelCard? {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.WAITING_TO_PLAY) return null

        var mutableDeck = s.drawPile.toMutableList()
        var mutableDiscard = s.discardPile.toMutableList()

        val drawn = drawCardsFromDeck(1, mutableDeck, mutableDiscard).firstOrNull() ?: return null

        val curIdx = s.currentTurnPlayerIndex
        val curP = s.players[curIdx]
        val newHand = curP.hand + drawn

        val isPlayable = drawn.matches(topDiscardCard, s.activeColor)

        if (!isPlayable) {
            // Cannot play drawn card -> turn ends automatically
            val desc = "${curP.profile.name} drew 1 card and passed."
            advanceTurnInternal(
                stateToAdvance = s.copy(
                    players = s.players.toMutableList().also { it[curIdx] = curP.copy(hand = newHand) },
                    drawPile = mutableDeck,
                    discardPile = mutableDiscard,
                    lastActionDescription = desc,
                    logHistory = listOf(desc) + s.logHistory
                )
            )
        } else {
            // Can play -> keep turn active so player can play it or pass
            _state.value = s.copy(
                players = s.players.toMutableList().also { it[curIdx] = curP.copy(hand = newHand) },
                drawPile = mutableDeck,
                discardPile = mutableDiscard,
                lastActionDescription = "${curP.profile.name} drew a playable card!"
            )
        }

        return drawn
    }

    /**
     * Pass turn if drawn card is not played.
     */
    fun passTurn() {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.WAITING_TO_PLAY) return
        val desc = "${currentPlayer.profile.name} passed turn."
        advanceTurnInternal(
            stateToAdvance = s.copy(
                lastActionDescription = desc,
                logHistory = listOf(desc) + s.logHistory
            )
        )
    }

    /**
     * Callout "CardWheel!" when at 1 card.
     */
    fun callLastCard(playerId: String) {
        val s = _state.value
        val pIdx = s.players.indexOfFirst { it.profile.id == playerId }
        if (pIdx >= 0) {
            val p = s.players[pIdx]
            if (p.hand.size == 1) {
                _state.value = s.copy(
                    players = s.players.toMutableList().also { it[pIdx] = p.copy(calledLastCard = true) },
                    lastActionDescription = "🔔 ${p.profile.name} called CARDWHEEL! (1 Card Remaining!)",
                    logHistory = listOf("${p.profile.name} shouted 'CardWheel!'") + s.logHistory
                )
            }
        }
    }

    private fun drawCardsFromDeck(
        count: Int,
        deck: MutableList<WheelCard>,
        discard: MutableList<WheelCard>
    ): List<WheelCard> {
        val drawn = mutableListOf<WheelCard>()
        repeat(count) {
            if (deck.isEmpty()) {
                if (discard.size > 1) {
                    val top = discard.removeLast()
                    deck.addAll(discard.shuffled())
                    discard.clear()
                    discard.add(top)
                }
            }
            if (deck.isNotEmpty()) {
                drawn.add(deck.removeAt(0))
            }
        }
        return drawn
    }

    private fun advanceTurnInternal(stateToAdvance: WheelCardSessionState) {
        val nextIdx = getNextPlayerIndex(
            stateToAdvance.currentTurnPlayerIndex,
            stateToAdvance.isClockwise,
            stateToAdvance.players.size
        )

        _state.value = stateToAdvance.copy(
            currentTurnPlayerIndex = nextIdx,
            turnPhase = WheelCardTurnPhase.WAITING_TO_PLAY,
            doublePlayActive = false,
            turnNumber = stateToAdvance.turnNumber + (if (nextIdx == 0) 1 else 0)
        )
    }

    private fun getNextPlayerIndex(current: Int, clockwise: Boolean, total: Int): Int {
        return if (clockwise) {
            (current + 1) % total
        } else {
            (current - 1 + total) % total
        }
    }

    fun restartGame() {
        _state.value = createInitialSession(_state.value.players.map { it.profile })
    }

    fun syncState(newState: WheelCardSessionState) {
        _state.value = newState
    }
}
