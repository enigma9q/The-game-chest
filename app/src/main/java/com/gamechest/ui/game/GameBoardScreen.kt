package com.gamechest.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gamechest.core.engine.*
import com.gamechest.core.model.*
import com.gamechest.ui.components.DiceRollerComponent
import com.gamechest.ui.components.RacetrackCanvas
import com.gamechest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

import com.gamechest.core.network.NetworkPacket
import com.gamechest.core.network.WifiLanTransport
import com.gamechest.ui.platform.PreferenceStore
import com.gamechest.ui.theme.parseHexColor

@Composable
fun GameBoardScreen(
    engine: GameEngine,
    wifiTransport: WifiLanTransport? = null,
    localPlayerId: String? = null,
    isHost: Boolean = true,
    isWifiCoop: Boolean = false,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()

    // 2. Persistent Quick Play Setting via Multiplatform PreferenceStore
    var isQuickPlayEnabled by remember {
        mutableStateOf(PreferenceStore.getBoolean("quick_play_enabled", false))
    }

    var quickPlayToastText by remember { mutableStateOf<String?>(null) }
    var isRollingAnimation by remember { mutableStateOf(false) }
    var selectedTileInfo by remember { mutableStateOf<TileNode?>(null) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showRollAgainBanner by remember { mutableStateOf(false) }

    val currentPlayer = state.players.getOrNull(state.currentTurnPlayerIndex)
    val isMyTurn = !isWifiCoop || (currentPlayer?.profile?.id == localPlayerId)

    // ==================== AUTHORITATIVE NETWORK SYNC LOOP ====================
    LaunchedEffect(isWifiCoop, isHost, wifiTransport) {
        if (isWifiCoop && wifiTransport != null) {
            wifiTransport.receivedPackets.collect { packet ->
                when (packet) {
                    is NetworkPacket.ActionBroadcast -> {
                        if (isHost) {
                            when (val action = packet.action) {
                                is GameAction.RollDice -> {
                                    scope.launch {
                                        isRollingAnimation = true
                                        delay(650)
                                        engine.rollDice(action.playerId)
                                        isRollingAnimation = false
                                        wifiTransport.sendPacket(NetworkPacket.StateSync(engine.state.value))
                                    }
                                }
                                is GameAction.FinishTurn -> {
                                    engine.nextTurn()
                                    wifiTransport.sendPacket(NetworkPacket.StateSync(engine.state.value))
                                }
                                is GameAction.ResetGame -> {
                                    engine.restartGame(action.keepSettings)
                                    wifiTransport.sendPacket(NetworkPacket.StateSync(engine.state.value))
                                }
                            }
                        }
                    }
                    is NetworkPacket.StateSync -> {
                        if (!isHost) {
                            val wasWaiting = (state.turnPhase == TurnPhase.WAITING_FOR_ROLL)
                            val nowDone = (packet.sessionState.turnPhase != TurnPhase.WAITING_FOR_ROLL && packet.sessionState.currentRollResult != null)
                            if (wasWaiting && nowDone) {
                                isRollingAnimation = true
                                delay(500)
                                isRollingAnimation = false
                            }
                            engine.syncState(packet.sessionState)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // Quick Play Auto-Advance (Only Host drives turn timer in Wi-Fi Co-Op)
    LaunchedEffect(state.turnPhase, isQuickPlayEnabled, state.extraRollAwarded, isWifiCoop, isHost) {
        if ((!isWifiCoop || isHost) && isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER && state.winnerPlayerId == null && !state.extraRollAwarded) {
            delay(1200)
            engine.nextTurn()
            if (isWifiCoop && isHost && wifiTransport != null) {
                wifiTransport.sendPacket(NetworkPacket.StateSync(engine.state.value))
            }
        }
    }

    // "ROLL AGAIN!" Banner Auto-Dismiss after 1.5 seconds
    LaunchedEffect(state.extraRollAwarded) {
        if (state.extraRollAwarded) {
            showRollAgainBanner = true
            delay(1500)
            showRollAgainBanner = false
        } else {
            showRollAgainBanner = false
        }
    }

    // Winner sequence: Wait for piece to arrive at finish before showing final victory dialog
    LaunchedEffect(state.winnerPlayerId) {
        if (state.winnerPlayerId != null) {
            delay(2400)
            showWinDialog = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ==================== LEFT SIDEBAR: MENU + PLAYER LIST ====================
                LeftPlayersSidebar(
                    state = state,
                    onOpenMenu = { showMenuDialog = true }
                )

                // ==================== CENTER: BOARD CANVAS ====================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    RacetrackCanvas(
                        layout = state.pack.tableLayout,
                        players = state.players,
                        activeMutators = state.activeMutators,
                        onTileClicked = { selectedTileInfo = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Tile inspection toast
                    if (selectedTileInfo != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard.copy(alpha = 0.95f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tile ${selectedTileInfo?.index}: ${selectedTileInfo?.description}",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { selectedTileInfo = null },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                }
                            }
                        }
                    }
                }

                // ==================== RIGHT: DICE AREA WITH QUICK PLAY & ROLLS LIST ====================
                RightDiceArea(
                    state = state,
                    currentPlayer = currentPlayer,
                    isMyTurn = isMyTurn,
                    isQuickPlayEnabled = isQuickPlayEnabled,
                    isRollingAnimation = isRollingAnimation,
                    onToggleQuickPlay = {
                        val newValue = !isQuickPlayEnabled
                        isQuickPlayEnabled = newValue
                        PreferenceStore.setBoolean("quick_play_enabled", newValue)
                        quickPlayToastText = if (newValue) "⚡ QUICK PLAY ACTIVATED" else "⚡ QUICK PLAY DEACTIVATED"
                        scope.launch {
                            delay(1500)
                            quickPlayToastText = null
                        }
                    },
                    onNextTurn = {
                        if (isWifiCoop && !isHost) {
                            scope.launch {
                                wifiTransport?.sendPacket(NetworkPacket.ActionBroadcast(GameAction.FinishTurn(localPlayerId ?: "")))
                            }
                        } else {
                            engine.nextTurn()
                            if (isWifiCoop && isHost) {
                                scope.launch {
                                    wifiTransport?.sendPacket(NetworkPacket.StateSync(engine.state.value))
                                }
                            }
                        }
                    },
                    onRollDice = {
                        if (currentPlayer != null && (state.turnPhase == TurnPhase.WAITING_FOR_ROLL || state.turnPhase == TurnPhase.TURN_OVER)) {
                            if (isWifiCoop) {
                                if (isHost) {
                                    scope.launch {
                                        isRollingAnimation = true
                                        delay(650)
                                        engine.rollDice(currentPlayer.profile.id)
                                        isRollingAnimation = false
                                        wifiTransport?.sendPacket(NetworkPacket.StateSync(engine.state.value))
                                    }
                                } else {
                                    scope.launch {
                                        isRollingAnimation = true
                                        wifiTransport?.sendPacket(NetworkPacket.ActionBroadcast(GameAction.RollDice(localPlayerId ?: currentPlayer.profile.id)))
                                        delay(650)
                                        isRollingAnimation = false
                                    }
                                }
                            } else {
                                scope.launch {
                                    if (isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER) {
                                        engine.nextTurn()
                                        delay(100)
                                    }
                                    val activePlayer = engine.getCurrentPlayer() ?: return@launch
                                    isRollingAnimation = true
                                    delay(650)
                                    engine.rollDice(activePlayer.profile.id)
                                    isRollingAnimation = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .width(245.dp)
                        .fillMaxHeight()
                )
            }

            // ==================== QUICK PLAY NOTIFICATION BANNER ====================
            AnimatedVisibility(
                visible = quickPlayToastText != null,
                enter = scaleIn(tween(200)) + fadeIn(tween(150)),
                exit = scaleOut(tween(200)) + fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .border(2.dp, if (isQuickPlayEnabled) NitroGreen else TextMuted, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isQuickPlayEnabled) NitroGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = quickPlayToastText ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isQuickPlayEnabled) NitroGreen else TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // ==================== TURN BANNER (CO-OP vs SAME-DEVICE LOCAL) ====================
            AnimatedVisibility(
                visible = state.turnPhase == TurnPhase.WAITING_FOR_ROLL && state.winnerPlayerId == null && !isRollingAnimation && !showRollAgainBanner,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                val playerColor = parseHexColor(currentPlayer?.profile?.carAvatar?.colorHex ?: "#3B82F6")

                if (isWifiCoop) {
                    if (isMyTurn) {
                        // Wi-Fi Co-Op: "It is your turn to play! \n Roll the dice."
                        Card(
                            modifier = Modifier
                                .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF01E3A8A)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "It is your turn to play!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Roll the dice.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }
                        }
                    } else {
                        // Wi-Fi Co-Op (Other player's turn): Waiting banner
                        Card(
                            modifier = Modifier
                                .border(1.5.dp, playerColor, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF40F172A)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(playerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text(
                                        text = "Waiting for turn...",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${currentPlayer?.profile?.name ?: "Player"} is rolling",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = playerColor
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Same-Device Local (Pass & Play): "Player turn \n %playername" styled with player's color
                    Card(
                        modifier = Modifier
                            .border(2.dp, playerColor, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF40F172A)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(playerColor)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Player turn",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = currentPlayer?.profile?.name ?: "Player 1",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = playerColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==================== ROLL AGAIN! 1.5s ANIMATED BANNER ====================
            AnimatedVisibility(
                visible = showRollAgainBanner,
                enter = scaleIn(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = scaleOut(tween(200)) + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    modifier = Modifier
                        .border(3.dp, NitroGreen, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF40F172A)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                tint = NitroGreen,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Roll again!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = NitroGreen,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.extraRollReason ?: "Space occupied by other player",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ==================== CONFETTI EFFECT BURST ON WIN ====================
            if (state.winnerPlayerId != null) {
                ConfettiBurstEffect()
            }
        }
    }

    // Final Win Celebration Dialog (Appears after piece has reached the finish)
    if (showWinDialog && state.winnerPlayerId != null) {
        val winner = state.players.find { it.profile.id == state.winnerPlayerId }
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CHECKERED FLAG!", fontWeight = FontWeight.Black, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "${winner?.profile?.name ?: "Racer"} won the Grand Prix!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NitroGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total turns: ${state.turnNumber} • Moves: ${winner?.totalMoves ?: 0}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWinDialog = false
                        engine.restartGame(keepMutators = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Race Again", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onExitGame) {
                    Text("Exit to Lobby", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Centered Popup Menu Dialog
    if (showMenuDialog) {
        GameMenuDialog(
            state = state,
            onDismiss = { showMenuDialog = false },
            onRestart = {
                showMenuDialog = false
                engine.restartGame(keepMutators = true)
            },
            onExit = {
                showMenuDialog = false
                onExitGame()
            }
        )
    }
}

/**
 * Left Sidebar: Menu Button (Top) + All Players list with white outline & clearly visible names
 */
@Composable
private fun LeftPlayersSidebar(
    state: GameSessionState,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(74.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp)),
        color = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Menu Icon
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceDarkCard)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Game Menu",
                    tint = PrimaryNeon,
                    modifier = Modifier.size(22.dp)
                )
            }

            // MIDDLE: Players List with White Outline Chips
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.players.forEachIndexed { idx, player ->
                    val isCurrentTurn = idx == state.currentTurnPlayerIndex
                    val carColor = parseHexColor(player.profile.carAvatar.colorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(if (isCurrentTurn) 1f else 0.68f)
                            .alpha(if (isCurrentTurn) 1f else 0.6f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(carColor)
                                .border(
                                    width = if (isCurrentTurn) 2.5.dp else 1.2.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = player.profile.name,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrentTurn) FontWeight.Black else FontWeight.Bold,
                            color = if (isCurrentTurn) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Right Dice Area:
 * - Top-Right: Quick Play Thunder toggle button (30% smaller)
 * - Left side of area: Roll History Feed (transparent background, white outline, latest roll on top, older rolls 40% smaller, up/down arrows)
 * - Center: Dice Box (replaced by WINNER! card on victory)
 */
@Composable
private fun RightDiceArea(
    state: GameSessionState,
    currentPlayer: PlayerRuntimeState?,
    isMyTurn: Boolean = true,
    isQuickPlayEnabled: Boolean,
    isRollingAnimation: Boolean,
    onToggleQuickPlay: () -> Unit,
    onNextTurn: () -> Unit,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winner = state.players.find { it.profile.id == state.winnerPlayerId }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Roll History Vertical Sidebar with Transparent Background & White Outline
        RollsHistorySidebar(
            state = state,
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Main Dice & Actions Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP ROW: Next Racer Button (left) + Quick Play Thunder Button (top-right, 30% smaller)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Racer Button
                if (!isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER && state.winnerPlayerId == null) {
                    Button(
                        onClick = onNextTurn,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("NEXT RACER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Quick Play Thunder Toggle Button (30% smaller, icon same size with padding)
                IconButton(
                    onClick = onToggleQuickPlay,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isQuickPlayEnabled) NitroGreen else Color(0x33FFFFFF))
                        .border(1.dp, if (isQuickPlayEnabled) NitroGreen else Color(0x66FFFFFF), RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "Quick Play",
                        tint = if (isQuickPlayEnabled) Color(0xFF0F172A) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // MIDDLE: Dice Box OR Winner Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (winner != null) {
                    // Winner Display in Dice Area
                    val winnerColor = parseHexColor(winner.profile.carAvatar.colorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .border(2.dp, AccentYellow, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = AccentYellow,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "WINNER!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentYellow,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = winner.profile.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = winnerColor
                            )
                        }
                    }
                } else {
                    val isDiceActive = isMyTurn && ((state.turnPhase == TurnPhase.WAITING_FOR_ROLL) || (isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER))
                    DiceRollerComponent(
                        diceSpec = state.currentActiveDiceSpec,
                        lastRoll = state.currentRollResult,
                        isRolling = isRollingAnimation,
                        isCurrentPlayerTurn = isDiceActive,
                        playerName = currentPlayer?.profile?.name ?: "",
                        extraRollAwarded = state.extraRollAwarded,
                        onRollClick = onRollDice
                    )
                }
            }

            // BOTTOM: Player Current Tile info
            if (currentPlayer != null && winner == null) {
                Text(
                    text = if (currentPlayer.currentTileId == 0) "Grid: Start (0)" else "Tile: ${currentPlayer.currentTileId} / ${state.pack.tableLayout.finishTileId}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Rolls History Sidebar:
 * - Outline of the rolls list container removed
 * - Square cards with only the number in center
 * - Bottom right: Refresh circle icon if 6 was rolled, up/down arrows if bridge/oil used
 * - Latest roll on TOP (full size), older rolls smaller by 40%
 */
@Composable
private fun RollsHistorySidebar(
    state: GameSessionState,
    modifier: Modifier = Modifier
) {
    val rollLogs = state.logHistory.filter { it.icon == "casino" }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ROLLS",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (rollLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("-", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(rollLogs) { idx, log ->
                    val isLatest = idx == 0
                    val player = state.players.find { it.profile.id == log.playerId }
                    val playerColor = player?.let {
                        parseHexColor(it.profile.carAvatar.colorHex)
                    } ?: PrimaryNeon

                    val rollNumber = log.message.substringAfter("rolled ").substringBefore(" on").trim()
                    val isSix = rollNumber == "6"

                    // Check if this roll was accompanied by a bridge or oil spill in logs
                    val hasBridge = state.logHistory.any { 
                        it.playerId == log.playerId && Math.abs(it.timestamp - log.timestamp) < 5000 && it.message.contains("TURBO RAMP") 
                    }
                    val hasOil = state.logHistory.any { 
                        it.playerId == log.playerId && Math.abs(it.timestamp - log.timestamp) < 5000 && it.message.contains("OIL SLICK") 
                    }

                    val squareSize = if (isLatest) 34.dp else 22.dp
                    val textSize = if (isLatest) 15.sp else 10.sp

                    Box(
                        modifier = Modifier
                            .size(squareSize)
                            .alpha(if (isLatest) 1f else 0.6f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, if (isLatest) playerColor else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .background(playerColor.copy(alpha = if (isLatest) 0.35f else 0.15f))
                    ) {
                        // Centered Rolled Number
                        Text(
                            text = rollNumber,
                            fontSize = textSize,
                            fontWeight = FontWeight.Black,
                            color = playerColor,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Bottom-right Icon: Refresh circle for 6, or arrows for ramp/oil
                        if (isSix) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = NitroGreen,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(1.5.dp)
                                    .size(if (isLatest) 9.dp else 6.5.dp)
                            )
                        } else if (hasBridge) {
                            Text(
                                text = "↑",
                                color = TurboCyan,
                                fontSize = if (isLatest) 8.sp else 5.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(1.dp)
                            )
                        } else if (hasOil) {
                            Text(
                                text = "↓",
                                color = HazardRed,
                                fontSize = if (isLatest) 8.sp else 5.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confetti animation effect burst
 */
@Composable
private fun ConfettiBurstEffect() {
    val particles = remember {
        List(45) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                color = listOf(NitroGreen, TurboCyan, AccentYellow, HazardRed, ApexPurple, PrimaryNeon).random(),
                size = Random.nextFloat() * 8f + 4f,
                speedY = Random.nextFloat() * 2f + 1f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "confetti_fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val curY = ((p.y + animProgress * p.speedY) % 1f) * size.height
            val curX = p.x * size.width + Math.sin((animProgress * 10f + p.x * 5f).toDouble()).toFloat() * 20f
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(curX, curY)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val speedY: Float
)

/**
 * Centered Modal Popup Game Menu Dialog
 */
@Composable
private fun GameMenuDialog(
    state: GameSessionState,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, Color(0xFF334155), RoundedCornerShape(22.dp)),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = state.pack.manifest.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Turn ${state.turnNumber} • ${state.players.size} Racers",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Active Mutator Chips
                if (state.activeMutators.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.activeMutators.forEach { mutatorId ->
                            val label = when (mutatorId) {
                                MutatorId.CLASSIC_GRAND_PRIX -> "1d6 Classic"
                                MutatorId.NITRO_TARGET_1D60 -> "1d60 Target"
                                MutatorId.NITRO_ASSIST_1D60 -> "1d60 Assist"
                                MutatorId.REVERSE_HAZARD_OVERDRIVE -> "Reverse Slicks"
                                MutatorId.CUSTOM_GRID_DICE_LOADOUT -> "Custom Grid"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF334155))

                // Recent Match Log History
                Text("Match Events", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.logHistory) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDarkCard)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(log.message, fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                Divider(color = Color(0xFF334155))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restart", color = TextPrimary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
