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
    SPIN_STACK_PROMPT,
    BET_PREDICTION,
    ALL_SPIN_STEP,
    DRAWN_CARD_WAITING,
    ROUND_OVER
}

@Serializable
data class WheelSpinTarget(
    val attackerPlayerId: String,
    val victimPlayerId: String,
    val stackedSpins: Int = 1,
    val bonusCards: Int = 0,
    val isSuperSpin: Boolean = false,
    val isBet: Boolean = false,
    val betGuess: Int? = null,
    val allSpinRemainingPlayers: List<String> = emptyList(),
    val spinsRemainingForCurrentVictim: Int = 1
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
                if (s.players.size == 2) {
                    // In a 2-player game, Reverse acts as a SKIP, giving the same player another turn!
                    val desc = "🔄 ${curP.profile.name} played Reverse! (2-Player: Skip! ${curP.profile.name} plays again!)"
                    _state.value = s.copy(
                        players = updatedPlayers,
                        discardPile = newDiscard,
                        activeColor = newActiveColor,
                        isClockwise = newDirection,
                        doublePlayActive = false,
                        lastActionDescription = desc,
                        logHistory = listOf(desc) + s.logHistory
                    )
                    return true
                } else {
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
                    stackedSpins = 1,
                    bonusCards = if (isSuper) 2 else 0,
                    isSuperSpin = isSuper,
                    spinsRemainingForCurrentVictim = 1
                )

                // Check if victim can Stack Spin or use Spin Shield (Number 7)
                val hasSpinCard = victimPlayer.hand.any { it.type == CardType.BASIC_SPIN || it.type == CardType.SUPER_SPIN }
                val hasShield = victimPlayer.hand.any { it.isSpinShield }

                val nextPhase = when {
                    hasSpinCard || hasShield -> WheelCardTurnPhase.SPIN_STACK_PROMPT
                    else -> WheelCardTurnPhase.WHEEL_SPINNING
                }

                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = newDiscard,
                    activeColor = newActiveColor,
                    wheelSpinTarget = target,
                    turnPhase = nextPhase,
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
     * Spin Stack & Counter Response:
     * - stackCardId: Play a spin card from hand to pile on and pass to next player
     * - useShieldId: Play 7 Spin Shield to deflect all accumulated spins back to attacker
     * - (both null): Accept spins and proceed to wheel spinning
     */
    fun respondWithSpinStack(stackCardId: String? = null, useShieldId: String? = null) {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.SPIN_STACK_PROMPT && s.turnPhase != WheelCardTurnPhase.SPIN_SHIELD_PROMPT) return
        val target = s.wheelSpinTarget ?: return

        val victimIdx = s.players.indexOfFirst { it.profile.id == target.victimPlayerId }
        val victim = s.players[victimIdx]

        if (useShieldId != null) {
            val shieldCard = victim.hand.find { it.id == useShieldId && it.isSpinShield }
            if (shieldCard != null) {
                val newHand = victim.hand.filter { it.id != useShieldId }
                val updatedPlayers = s.players.toMutableList().also {
                    it[victimIdx] = victim.copy(hand = newHand)
                }

                // Deflect all stacked spins back to attacker!
                val reflectedTarget = target.copy(
                    victimPlayerId = target.attackerPlayerId,
                    attackerPlayerId = target.victimPlayerId,
                    spinsRemainingForCurrentVictim = target.stackedSpins
                )

                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = s.discardPile + shieldCard,
                    wheelSpinTarget = reflectedTarget,
                    turnPhase = WheelCardTurnPhase.WHEEL_SPINNING,
                    lastActionDescription = "🛡️ ${victim.profile.name} played 7 SPIN SHIELD! Deflected ${target.stackedSpins} spin(s) back to ${s.players.find { it.profile.id == target.attackerPlayerId }?.profile?.name}!"
                )
                return
            }
        }

        if (stackCardId != null) {
            val spinCard = victim.hand.find { it.id == stackCardId && (it.type == CardType.BASIC_SPIN || it.type == CardType.SUPER_SPIN) }
            if (spinCard != null) {
                val newHand = victim.hand.filter { it.id != stackCardId }
                val updatedPlayers = s.players.toMutableList().also {
                    it[victimIdx] = victim.copy(hand = newHand)
                }

                val isSuper = spinCard.type == CardType.SUPER_SPIN
                val nextVictimIdx = getNextPlayerIndex(victimIdx, s.isClockwise, s.players.size)
                val nextVictim = updatedPlayers[nextVictimIdx]

                val newStackedSpins = target.stackedSpins + 1
                val newBonus = target.bonusCards + (if (isSuper) 2 else 0)

                val nextTarget = target.copy(
                    attackerPlayerId = victim.profile.id,
                    victimPlayerId = nextVictim.profile.id,
                    stackedSpins = newStackedSpins,
                    bonusCards = newBonus,
                    spinsRemainingForCurrentVictim = newStackedSpins
                )

                val nextHasSpin = nextVictim.hand.any { it.type == CardType.BASIC_SPIN || it.type == CardType.SUPER_SPIN }
                val nextHasShield = nextVictim.hand.any { it.isSpinShield }

                val nextPhase = when {
                    nextHasSpin || nextHasShield -> WheelCardTurnPhase.SPIN_STACK_PROMPT
                    else -> WheelCardTurnPhase.WHEEL_SPINNING
                }

                _state.value = s.copy(
                    players = updatedPlayers,
                    discardPile = s.discardPile + spinCard,
                    wheelSpinTarget = nextTarget,
                    turnPhase = nextPhase,
                    lastActionDescription = "⚡ ${victim.profile.name} stacked a Spin card! Total: $newStackedSpins spins targeting ${nextVictim.profile.name}!"
                )
                return
            }
        }

        // Accept spins -> Start spinning for all stacked spins
        _state.value = s.copy(
            wheelSpinTarget = target.copy(spinsRemainingForCurrentVictim = target.stackedSpins),
            turnPhase = WheelCardTurnPhase.WHEEL_SPINNING,
            lastActionDescription = "${victim.profile.name} must take ${target.stackedSpins} spin(s) of penalty!"
        )
    }

    /**
     * Spin Shield Counter Response backward compatibility.
     */
    fun respondWithSpinShield(useShield: Boolean, cardId: String? = null) {
        if (useShield && cardId != null) {
            respondWithSpinStack(useShieldId = cardId)
        } else {
            respondWithSpinStack()
        }
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
                // Continue All-Spin sequence for next player
                val nextTarget = target.copy(
                    victimPlayerId = nextVictimId,
                    allSpinRemainingPlayers = target.allSpinRemainingPlayers.drop(1)
                )
                val nextVictimName = s.players.find { it.profile.id == nextVictimId }?.profile?.name ?: "Player"
                _state.value = s.copy(
                    players = mutablePlayers,
                    drawPile = mutableDeck,
                    discardPile = mutableDiscard,
                    wheelSpinTarget = nextTarget,
                    lastWheelResult = spinResult,
                    turnPhase = WheelCardTurnPhase.ALL_SPIN_STEP,
                    lastActionDescription = "$victimName drew +$totalPenalty cards! Next spinning: $nextVictimName."
                )
                return
            } else {
                // All-spin completed! Finish turn for the attacker and advance to next player
                val desc = "$victimName drew +$totalPenalty cards. ALL-SPIN complete!"
                val attackerIdx = s.players.indexOfFirst { it.profile.id == target.attackerPlayerId }.let { if (it >= 0) it else s.currentTurnPlayerIndex }
                val nextIdx = getNextPlayerIndex(attackerIdx, s.isClockwise, s.players.size)

                _state.value = s.copy(
                    players = mutablePlayers,
                    drawPile = mutableDeck,
                    discardPile = mutableDiscard,
                    wheelSpinTarget = null,
                    lastWheelResult = spinResult,
                    currentTurnPlayerIndex = nextIdx,
                    turnPhase = WheelCardTurnPhase.WAITING_TO_PLAY,
                    doublePlayActive = false,
                    lastActionDescription = desc,
                    logHistory = listOf(desc) + s.logHistory,
                    turnNumber = s.turnNumber + (if (nextIdx == 0) 1 else 0)
                )
                return
            }
        }

        // Standard Basic/Super Spin with Multi-Spin Stack Support
        val victimIdx = s.players.indexOfFirst { it.profile.id == target.victimPlayerId }
        val victim = s.players[victimIdx]
        val totalPenalty = spinResult + (if (target.spinsRemainingForCurrentVictim == target.stackedSpins) target.bonusCards else 0)

        val cardsDrawn = drawCardsFromDeck(totalPenalty, mutableDeck, mutableDiscard)
        mutablePlayers[victimIdx] = victim.copy(hand = victim.hand + cardsDrawn)

        val remainingSpins = target.spinsRemainingForCurrentVictim - 1

        val desc = if (spinResult == 6) {
            "💣 BOOM! Wheel landed on the BOMB! ${victim.profile.name} drew +$totalPenalty cards!"
        } else if (spinResult == 0) {
            "🍀 LUCKY ESCAPE! Wheel landed on 0! ${victim.profile.name} drew 0 penalty cards!"
        } else {
            "🎡 Wheel landed on +$spinResult! ${victim.profile.name} drew +$totalPenalty cards."
        }

        if (remainingSpins > 0) {
            // More stacked spins remaining for this victim!
            _state.value = s.copy(
                players = mutablePlayers,
                drawPile = mutableDeck,
                discardPile = mutableDiscard,
                wheelSpinTarget = target.copy(spinsRemainingForCurrentVictim = remainingSpins),
                lastWheelResult = spinResult,
                turnPhase = WheelCardTurnPhase.WHEEL_SPINNING,
                lastActionDescription = "$desc ($remainingSpins more spin(s) left!)"
            )
        } else {
            // All stacked spins finished!
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
    }

    /**
     * Checks if current player holds any playable card in hand.
     */
    fun hasPlayableCard(): Boolean {
        return currentPlayer.hand.any { isCardPlayable(it) }
    }

    /**
     * Draws 1 card from the draw pile. Blocked if player already holds a playable card.
     */
    fun drawCardFromPile(): WheelCard? {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.WAITING_TO_PLAY) return null

        // Cannot draw if player already has something to play!
        if (hasPlayableCard()) return null

        var mutableDeck = s.drawPile.toMutableList()
        var mutableDiscard = s.discardPile.toMutableList()

        val drawn = drawCardsFromDeck(1, mutableDeck, mutableDiscard).firstOrNull() ?: return null

        val curIdx = s.currentTurnPlayerIndex
        val curP = s.players[curIdx]
        val newHand = curP.hand + drawn

        val isPlayable = drawn.matches(topDiscardCard, s.activeColor)

        if (!isPlayable) {
            // Unplayable -> Enter DRAWN_CARD_WAITING for 3 seconds so player sees the drawn card
            val desc = "⏳ ${curP.profile.name} drew ${drawn.color.displayName} ${drawn.type.displayName} (Unplayable) - Passing turn in 3s..."
            _state.value = s.copy(
                players = s.players.toMutableList().also { it[curIdx] = curP.copy(hand = newHand) },
                drawPile = mutableDeck,
                discardPile = mutableDiscard,
                turnPhase = WheelCardTurnPhase.DRAWN_CARD_WAITING,
                lastActionDescription = desc,
                logHistory = listOf("${curP.profile.name} drew 1 unplayable card.") + s.logHistory
            )
        } else {
            // Playable -> keep turn active so player can play it or choose to pass
            _state.value = s.copy(
                players = s.players.toMutableList().also { it[curIdx] = curP.copy(hand = newHand) },
                drawPile = mutableDeck,
                discardPile = mutableDiscard,
                lastActionDescription = "✨ ${curP.profile.name} drew a playable ${drawn.color.displayName} ${drawn.type.displayName}!"
            )
        }

        return drawn
    }

    /**
     * Completes automatic turn pass after 3s waiting on unplayable drawn card.
     */
    fun completeDrawnCardPass() {
        val s = _state.value
        if (s.turnPhase != WheelCardTurnPhase.DRAWN_CARD_WAITING) return
        val desc = "${currentPlayer.profile.name} passed turn."
        advanceTurnInternal(
            stateToAdvance = s.copy(
                turnPhase = WheelCardTurnPhase.WAITING_TO_PLAY,
                lastActionDescription = desc
            )
        )
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
