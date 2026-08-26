package com.gamechest.cardgame.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gamechest.cardgame.engine.*
import com.gamechest.cardgame.model.*
import com.gamechest.cardgame.ui.components.*
import com.gamechest.core.network.NetworkPacket
import com.gamechest.core.network.WifiLanTransport
import com.gamechest.ui.platform.PreferenceStore
import com.gamechest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WheelCardGameBoardScreen(
    engine: WheelCardEngine,
    wifiTransport: WifiLanTransport? = null,
    localPlayerId: String? = null,
    isHost: Boolean = true,
    isWifiCoop: Boolean = false,
    isDesktop: Boolean = false,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()

    val cardScale = if (isDesktop) 1.5f else 1.0f
    val centerCardWidth = (84 * cardScale).dp
    val centerCardHeight = (124 * cardScale).dp
    val centerWheelSize = (190 * cardScale).dp
    val handCardWidth = (76 * cardScale).dp
    val handCardHeight = (114 * cardScale).dp

    var isWheelSpinningAnim by remember { mutableStateOf(false) }
    var targetWheelResult by remember { mutableStateOf<Int?>(null) }
    var showColorChoiceDialog by remember { mutableStateOf(false) }
    var showBetPredictionDialog by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }

    val currentPlayer = state.players.getOrNull(state.currentTurnPlayerIndex)
    val isMyTurn = !isWifiCoop || (currentPlayer?.profile?.id == localPlayerId)

    // Sync dialog states and sequential auto-spin with engine turnPhase
    LaunchedEffect(state.turnPhase, state.wheelSpinTarget?.victimPlayerId, state.wheelSpinTarget?.spinsRemainingForCurrentVictim) {
        showColorChoiceDialog = (state.turnPhase == WheelCardTurnPhase.COLOR_PICKING && isMyTurn)
        showBetPredictionDialog = (state.turnPhase == WheelCardTurnPhase.BET_PREDICTION && isMyTurn)

        if ((state.turnPhase == WheelCardTurnPhase.WHEEL_SPINNING || state.turnPhase == WheelCardTurnPhase.ALL_SPIN_STEP) && !isWheelSpinningAnim) {
            delay(500) // Brief visual pause so all players observe who is spinning
            isWheelSpinningAnim = true
        }

        if (state.turnPhase == WheelCardTurnPhase.DRAWN_CARD_WAITING) {
            delay(3000)
            engine.completeDrawnCardPass()
        }

        if (state.turnPhase == WheelCardTurnPhase.ROUND_OVER) {
            delay(1500)
            showWinDialog = true
        }
    }

    var isWhiteTheme by remember {
        mutableStateOf(PreferenceStore.getBoolean("wheel_card_white_theme", false))
    }

    val bgTableColor = if (isWhiteTheme) Color(0xFFF8FAFC) else DarkBackground
    val primaryTextColor = if (isWhiteTheme) Color(0xFF0F172A) else TextPrimary
    val secondaryTextColor = if (isWhiteTheme) Color(0xFF475569) else TextSecondary
    val cardContainerColor = if (isWhiteTheme) Color(0xFFFFFFFF) else SurfaceDarkCard
    val tableBorderColor = if (isWhiteTheme) Color(0xFFCBD5E1) else BorderDark

    Surface(
        modifier = modifier.fillMaxSize(),
        color = bgTableColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==================== TOP BAR: MENU, OPPONENTS & THEME TOGGLE ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showMenuDialog = true },
                            modifier = Modifier
                                .size(if (isDesktop) 48.dp else 40.dp)
                                .clip(CircleShape)
                                .background(cardContainerColor)
                                .border(1.dp, tableBorderColor, CircleShape)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = if (isWhiteTheme) Color(0xFF0284C7) else PrimaryNeon, modifier = Modifier.size(if (isDesktop) 26.dp else 22.dp))
                        }

                        // Theme Toggle Button (Light / Dark)
                        IconButton(
                            onClick = {
                                val nextTheme = !isWhiteTheme
                                isWhiteTheme = nextTheme
                                PreferenceStore.setBoolean("wheel_card_white_theme", nextTheme)
                            },
                            modifier = Modifier
                                .size(if (isDesktop) 48.dp else 40.dp)
                                .clip(CircleShape)
                                .background(cardContainerColor)
                                .border(1.dp, tableBorderColor, CircleShape)
                        ) {
                            Icon(
                                if (isWhiteTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isWhiteTheme) Color(0xFF334155) else AccentYellow,
                                modifier = Modifier.size(if (isDesktop) 24.dp else 20.dp)
                            )
                        }
                    }

                    // Opponents Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.players.forEachIndexed { idx, player ->
                            if (player.profile.id != currentPlayer?.profile?.id || state.players.size > 1) {
                                OpponentHandBadge(
                                    player = player,
                                    isCurrentTurn = idx == state.currentTurnPlayerIndex
                                )
                            }
                        }
                    }

                    // Play Direction Badge
                    Surface(
                        color = if (isWhiteTheme) Color(0xFFE0F2FE) else Color(0x3300E5FF),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isWhiteTheme) Color(0xFF38BDF8) else Color(0x6600E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isDesktop) 14.dp else 10.dp, vertical = if (isDesktop) 8.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (state.isClockwise) Icons.Default.RotateRight else Icons.Default.RotateLeft,
                                contentDescription = null,
                                tint = if (isWhiteTheme) Color(0xFF0284C7) else PrimaryNeon,
                                modifier = Modifier.size(if (isDesktop) 20.dp else 16.dp)
                            )
                            Text(
                                text = if (state.isClockwise) "CLOCKWISE" else "COUNTER",
                                fontSize = if (isDesktop) 13.sp else 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isWhiteTheme) Color(0xFF0284C7) else PrimaryNeon
                            )
                        }
                    }
                }

                // ==================== CENTER: THE WHEEL & CARD PILES (50% LARGER ON DESKTOP) ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasPlayable = engine.hasPlayableCard()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DRAW PILE (${state.drawPile.size})",
                                fontSize = if (isDesktop) 14.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor
                            )
                            PlayingCardView(
                                card = null,
                                isFaceUp = false,
                                cardWidth = centerCardWidth,
                                cardHeight = centerCardHeight,
                                onClick = {
                                    if (isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY && !hasPlayable) {
                                        engine.drawCardFromPile()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isDesktop) 44.dp else 28.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "WHEEL OF PENALTY",
                                fontSize = if (isDesktop) 14.sp else 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isWhiteTheme) Color(0xFFB45309) else AccentYellow,
                                letterSpacing = 1.sp
                            )
                            WheelComponent(
                                isSpinning = isWheelSpinningAnim,
                                targetResult = targetWheelResult,
                                onSpinComplete = { result ->
                                    if (result == -1) {
                                        if (isMyTurn && (state.turnPhase == WheelCardTurnPhase.WHEEL_SPINNING || state.turnPhase == WheelCardTurnPhase.ALL_SPIN_STEP)) {
                                            isWheelSpinningAnim = true
                                        }
                                    } else {
                                        isWheelSpinningAnim = false
                                        scope.launch {
                                            delay(1000)
                                            engine.resolveWheelSpin(result)
                                        }
                                    }
                                },
                                wheelSize = centerWheelSize,
                                enabled = isMyTurn && (state.turnPhase == WheelCardTurnPhase.WHEEL_SPINNING || state.turnPhase == WheelCardTurnPhase.ALL_SPIN_STEP)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isDesktop) 44.dp else 28.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "DISCARD (MATCH:",
                                    fontSize = if (isDesktop) 14.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryTextColor
                                )
                                Text(
                                    text = state.activeColor.displayName.uppercase(),
                                    fontSize = if (isDesktop) 14.sp else 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = parseHexColor(state.activeColor.hexColor)
                                )
                                Text(")", fontSize = if (isDesktop) 14.sp else 11.sp, fontWeight = FontWeight.Bold, color = secondaryTextColor)
                            }

                            val topCard = state.discardPile.lastOrNull()
                            if (topCard != null) {
                                PlayingCardView(
                                    card = topCard,
                                    isFaceUp = true,
                                    isPlayable = false,
                                    cardWidth = centerCardWidth,
                                    cardHeight = centerCardHeight
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMyTurn && currentPlayer?.hand?.size == 1 && !currentPlayer.calledLastCard) {
                            Button(
                                onClick = { engine.callLastCard(currentPlayer.profile.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shout CARDWHEEL!", fontSize = if (isDesktop) 13.sp else 12.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY) {
                            Button(
                                onClick = { engine.passTurn() },
                                colors = ButtonDefaults.buttonColors(containerColor = cardContainerColor, contentColor = secondaryTextColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pass Turn", fontSize = if (isDesktop) 13.sp else 12.sp)
                            }
                        }
                    }

                    currentPlayer?.let { player ->
                        val effectiveHand = remember(player.hand, state.activeColor, state.discardPile.lastOrNull(), isMyTurn) {
                            if (player.hand.size > 9 && isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY) {
                                player.hand.sortedWith(
                                    compareByDescending<WheelCard> { engine.isCardPlayable(it) }
                                        .thenBy { it.color.ordinal }
                                        .thenBy { it.number ?: 99 }
                                )
                            } else {
                                player.hand
                            }
                        }
                        PlayerHandRow(
                            player = player.copy(hand = effectiveHand),
                            isCurrentTurn = isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY,
                            isCardPlayable = { card -> engine.isCardPlayable(card) },
                            cardWidth = handCardWidth,
                            cardHeight = handCardHeight,
                            onPlayCard = { card ->
                                if (isMyTurn) {
                                    engine.playCard(card.id)
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY && state.winnerPlayerId == null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                val playerColor = parseHexColor(currentPlayer?.profile?.carAvatar?.colorHex ?: "#3B82F6")
                Card(
                    modifier = Modifier.border(2.dp, playerColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF40F172A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Player Turn", fontSize = 11.sp, color = secondaryTextColor)
                            Text(currentPlayer?.profile?.name ?: "Player", fontSize = 15.sp, fontWeight = FontWeight.Black, color = playerColor)
                        }
                    }
                }
            }

            if (state.turnPhase == WheelCardTurnPhase.SPIN_STACK_PROMPT || state.turnPhase == WheelCardTurnPhase.SPIN_SHIELD_PROMPT) {
                val target = state.wheelSpinTarget
                val victimPlayer = state.players.find { it.profile.id == target?.victimPlayerId }
                val spinCardsInHand = victimPlayer?.hand?.filter { it.type == CardType.BASIC_SPIN || it.type == CardType.SUPER_SPIN } ?: emptyList()
                val shieldCard = victimPlayer?.hand?.find { it.isSpinShield }
                val isVictimLocal = !isWifiCoop || (victimPlayer?.profile?.id == localPlayerId)

                if (isVictimLocal) {
                    AlertDialog(
                        onDismissRequest = {},
                        containerColor = Color(0xFF1E1B4B),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentYellow)
                                Text(
                                    text = if ((target?.stackedSpins ?: 1) > 1) "SPIN STACK ATTACK! (${target?.stackedSpins} Spins)" else "WHEEL SPIN ATTACK!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "You are targeted for ${target?.stackedSpins ?: 1} Wheel spin(s)! You can counter with a Spin card to pile up the spins or use a 7 Shield to deflect.",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 14.sp
                                )

                                if (spinCardsInHand.isNotEmpty()) {
                                    Text("Stack a Spin Card (+1 Spin to next player):", color = AccentYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        spinCardsInHand.forEach { card ->
                                            Button(
                                                onClick = { engine.respondWithSpinStack(stackCardId = card.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(card.color.hexColor), contentColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("⚡ Stack ${card.color.displayName} ${card.type.displayName}", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            if (shieldCard != null) {
                                Button(
                                    onClick = { engine.respondWithSpinStack(useShieldId = shieldCard.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color(0xFF0F172A))
                                ) {
                                    Text("🛡️ DEFLECT WITH 7 SHIELD", fontWeight = FontWeight.Black)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { engine.respondWithSpinStack() }) {
                                Text("Take ${target?.stackedSpins ?: 1} Spin(s)", color = TextMuted, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            if (showColorChoiceDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Text("Choose Next Color", color = TextPrimary, fontWeight = FontWeight.Black)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select the color for the next player to match:", color = TextSecondary, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW).forEach { color ->
                                    Button(
                                        onClick = { engine.selectActiveColor(color) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = parseHexColor(color.hexColor),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text(color.displayName, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // ==================== BET & SPIN PREDICTION DIALOG ====================
            if (showBetPredictionDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = Color(0xFF1E1B4B),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFDE047))
                            Text("BET & SPIN PREDICTION", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Predict the exact Wheel outcome! If correct, all opponents draw +2 cards. If wrong, you draw +2 cards.",
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (0..4).forEach { guess ->
                                    Button(
                                        onClick = { engine.submitBetPrediction(guess) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color(0xFF0F172A)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+$guess", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // ==================== WINNER DIALOG ====================
            if (showWinDialog) {
                val winner = state.players.find { it.profile.id == state.winnerPlayerId }
                val winnerColor = parseHexColor(winner?.profile?.carAvatar?.colorHex ?: "#3B82F6")

                Dialog(onDismissRequest = {}) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .border(3.dp, AccentYellow, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(64.dp))
                            Text("ROUND VICTORY!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                            Text("${winner?.profile?.name} played their last card and won WheelCard Clash!", textAlign = TextAlign.Center, color = TextPrimary, fontSize = 15.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showWinDialog = false
                                        engine.restartGame()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("PLAY AGAIN", fontWeight = FontWeight.Black)
                                }
                                Button(
                                    onClick = onExitGame,
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = TextSecondary),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("EXIT TO LOBBY", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ==================== IN-GAME MENU DIALOG ====================
            if (showMenuDialog) {
                AlertDialog(
                    onDismissRequest = { showMenuDialog = false },
                    containerColor = SurfaceDarkCard,
                    title = { Text("Game Menu", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    showMenuDialog = false
                                    engine.restartGame()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                            ) {
                                Text("Restart Match", color = TextPrimary)
                            }
                            Button(
                                onClick = {
                                    showMenuDialog = false
                                    onExitGame()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444), contentColor = Color(0xFFEF4444))
                            ) {
                                Text("Exit Game to Lobby")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMenuDialog = false }) { Text("Close", color = PrimaryNeon) }
                    }
                )
            }
        }
    }
}
