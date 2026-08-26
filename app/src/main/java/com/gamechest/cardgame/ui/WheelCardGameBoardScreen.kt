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
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()

    var isWheelSpinningAnim by remember { mutableStateOf(false) }
    var targetWheelResult by remember { mutableStateOf<Int?>(null) }
    var showColorChoiceDialog by remember { mutableStateOf(false) }
    var showBetPredictionDialog by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }

    val currentPlayer = state.players.getOrNull(state.currentTurnPlayerIndex)
    val isMyTurn = !isWifiCoop || (currentPlayer?.profile?.id == localPlayerId)

    // Sync dialog states with engine turnPhase
    LaunchedEffect(state.turnPhase) {
        showColorChoiceDialog = (state.turnPhase == WheelCardTurnPhase.COLOR_PICKING && isMyTurn)
        showBetPredictionDialog = (state.turnPhase == WheelCardTurnPhase.BET_PREDICTION && isMyTurn)

        if (state.turnPhase == WheelCardTurnPhase.WHEEL_SPINNING && !isWheelSpinningAnim) {
            isWheelSpinningAnim = true
        }

        if (state.turnPhase == WheelCardTurnPhase.ROUND_OVER) {
            delay(1500)
            showWinDialog = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==================== TOP BAR: MENU & OPPONENTS ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showMenuDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceDarkCard)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryNeon)
                    }

                    // Opponents Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        color = Color(0x3300E5FF),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (state.isClockwise) Icons.Default.RotateRight else Icons.Default.RotateLeft,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (state.isClockwise) "CLOCKWISE" else "COUNTER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryNeon
                            )
                        }
                    }
                }

                // ==================== CENTER: THE WHEEL & CARD PILES ====================
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
                        // 1. DRAW PILE (Tap to draw)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DRAW PILE (${state.drawPile.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            PlayingCardView(
                                card = null,
                                isFaceUp = false,
                                cardWidth = 84.dp,
                                cardHeight = 124.dp,
                                onClick = {
                                    if (isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY) {
                                        engine.drawCardFromPile()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        // 2. THE CENTER INTERACTIVE WHEEL
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "WHEEL OF PENALTY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentYellow,
                                letterSpacing = 1.sp
                            )
                            WheelComponent(
                                isSpinning = isWheelSpinningAnim,
                                targetResult = targetWheelResult,
                                onSpinComplete = { result ->
                                    if (result == -1) {
                                        // Manual center hub spin click
                                        if (isMyTurn && state.turnPhase == WheelCardTurnPhase.WHEEL_SPINNING) {
                                            isWheelSpinningAnim = true
                                        }
                                    } else {
                                        // Spin finished
                                        isWheelSpinningAnim = false
                                        engine.resolveWheelSpin(result)
                                    }
                                },
                                wheelSize = 190.dp,
                                enabled = isMyTurn && (state.turnPhase == WheelCardTurnPhase.WHEEL_SPINNING || state.turnPhase == WheelCardTurnPhase.ALL_SPIN_STEP)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        // 3. DISCARD PILE (Top Card to match)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "DISCARD (MATCH:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = state.activeColor.displayName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = parseHexColor(state.activeColor.hexColor)
                                )
                                Text(")", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            }

                            val topCard = state.discardPile.lastOrNull()
                            PlayingCardView(
                                card = topCard,
                                isFaceUp = true,
                                cardWidth = 84.dp,
                                cardHeight = 124.dp
                            )
                        }
                    }
                }

                // ==================== BOTTOM: ACTIVE PLAYER HAND & ACTION BAR ====================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Action controls: "CardWheel!" shout button & Pass Turn button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "CardWheel!" Callout Button
                        if (currentPlayer != null && currentPlayer.hand.size == 1 && !currentPlayer.calledLastCard) {
                            Button(
                                onClick = { engine.callLastCard(currentPlayer.profile.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SHOUT CARDWHEEL!", fontWeight = FontWeight.Black)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Pass Turn Button (if drew a card and chose not to play)
                        if (isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY) {
                            Button(
                                onClick = { engine.passTurn() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkCard, contentColor = TextSecondary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pass Turn", fontSize = 12.sp)
                            }
                        }
                    }

                    // Render Local Player Hand
                    currentPlayer?.let { player ->
                        PlayerHandRow(
                            player = player,
                            isCurrentTurn = isMyTurn && state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY,
                            isCardPlayable = { card -> engine.isCardPlayable(card) },
                            onPlayCard = { card ->
                                if (isMyTurn) {
                                    engine.playCard(card.id)
                                }
                            }
                        )
                    }
                }
            }

            // ==================== TURN BANNER ====================
            AnimatedVisibility(
                visible = state.turnPhase == WheelCardTurnPhase.WAITING_TO_PLAY && state.winnerPlayerId == null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                val playerColor = parseHexColor(currentPlayer?.profile?.carAvatar?.colorHex ?: "#3B82F6")

                if (isWifiCoop) {
                    if (isMyTurn) {
                        Card(
                            modifier = Modifier.border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF01E3A8A)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("It is your turn to play!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text("Play a matching card or draw from the pile.", fontSize = 12.sp, color = Color(0xFF93C5FD))
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.border(1.5.dp, playerColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF40F172A)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Waiting for ${currentPlayer?.profile?.name ?: "Player"} to play...", fontSize = 13.sp, color = playerColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
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
                                Text("Player Turn", fontSize = 11.sp, color = TextSecondary)
                                Text(currentPlayer?.profile?.name ?: "Player", fontSize = 15.sp, fontWeight = FontWeight.Black, color = playerColor)
                            }
                        }
                    }
                }
            }

            // ==================== SPIN SHIELD (7) REFLEX COUNTER BANNER ====================
            if (state.turnPhase == WheelCardTurnPhase.SPIN_SHIELD_PROMPT) {
                val target = state.wheelSpinTarget
                val victimPlayer = state.players.find { it.profile.id == target?.victimPlayerId }
                val shieldCard = victimPlayer?.hand?.find { it.isSpinShield }

                AlertDialog(
                    onDismissRequest = { engine.respondWithSpinShield(false) },
                    containerColor = Color(0xFF1E1B4B),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFDE047))
                            Text("SPIN SHIELD ALERT!", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "You are being forced to spin the Wheel! You hold a Number 7 Spin Shield in your hand.",
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Would you like to play your 7 to deflect the penalty back to the attacker?",
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { engine.respondWithSpinShield(true, shieldCard?.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color(0xFF0F172A))
                        ) {
                            Text("🛡️ DEFLECT WITH 7 SHIELD", fontWeight = FontWeight.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { engine.respondWithSpinShield(false) }) {
                            Text("Take Spin Penalty", color = TextMuted)
                        }
                    }
                )
            }

            // ==================== COLOR CHOICE WILD DIALOG ====================
            if (showColorChoiceDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = SurfaceDarkCard,
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
