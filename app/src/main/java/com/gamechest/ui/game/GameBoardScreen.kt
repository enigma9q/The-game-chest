package com.gamechest.ui.game

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun GameBoardScreen(
    engine: GameEngine,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()

    // 1. Lock Screen Orientation to Landscape for Game Board
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = prevOrientation
        }
    }

    // 2. Persistent Quick Play Setting
    val prefs = remember { context.getSharedPreferences("game_chest_prefs", Context.MODE_PRIVATE) }
    var isQuickPlayEnabled by remember {
        mutableStateOf(prefs.getBoolean("quick_play_enabled", false))
    }

    var isRollingAnimation by remember { mutableStateOf(false) }
    var selectedTileInfo by remember { mutableStateOf<TileNode?>(null) }
    var showMenuDialog by remember { mutableStateOf(false) }

    val currentPlayer = state.players.getOrNull(state.currentTurnPlayerIndex)

    // Quick Play auto-advance effect
    LaunchedEffect(state.turnPhase, isQuickPlayEnabled, state.extraRollAwarded) {
        if (isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER && state.winnerPlayerId == null && !state.extraRollAwarded) {
            delay(700)
            engine.nextTurn()
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
                // ==================== LEFT SIDEBAR: MENU + PLAYER TURN QUEUE + QUICK PLAY ====================
                LeftSidebar(
                    state = state,
                    isQuickPlayEnabled = isQuickPlayEnabled,
                    onToggleQuickPlay = {
                        val newValue = !isQuickPlayEnabled
                        isQuickPlayEnabled = newValue
                        prefs.edit().putBoolean("quick_play_enabled", newValue).apply()
                    },
                    onOpenMenu = { showMenuDialog = true }
                )

                // ==================== CENTER: RACETRACK CANVAS BOARD ====================
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

                // ==================== RIGHT: DICE AREA WITH ROLL HISTORY SIDEBAR ====================
                Surface(
                    modifier = Modifier
                        .width(235.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp)),
                    color = SurfaceDark
                ) {
                    RightDiceArea(
                        state = state,
                        currentPlayer = currentPlayer,
                        isQuickPlayEnabled = isQuickPlayEnabled,
                        isRollingAnimation = isRollingAnimation,
                        onNextTurn = { engine.nextTurn() },
                        onRollDice = {
                            scope.launch {
                                if (isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER) {
                                    engine.nextTurn()
                                    delay(100)
                                }
                                val activePlayer = engine.getCurrentPlayer() ?: return@launch
                                isRollingAnimation = true
                                delay(500)
                                engine.rollDice(activePlayer.profile.id)
                                isRollingAnimation = false
                            }
                        }
                    )
                }
            }

            // ==================== ROLL AGAIN! CENTER-TO-SIDE ANIMATED OVERLAY ====================
            AnimatedRollAgainBanner(
                extraRollAwarded = state.extraRollAwarded,
                reason = state.extraRollReason
            )
        }
    }

    // Win Celebration Dialog
    if (state.winnerPlayerId != null) {
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
                    onClick = { engine.restartGame(keepMutators = true) },
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

    // Modern Centered Popup Menu Dialog
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
 * Left Sidebar: Menu Button (Top), Player Turn Queue (Middle), Thunder Quick Play (Bottom)
 */
@Composable
private fun LeftSidebar(
    state: GameSessionState,
    isQuickPlayEnabled: Boolean,
    onToggleQuickPlay: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val players = state.players
    val currentIdx = state.currentTurnPlayerIndex
    val prevIdx = if (players.isNotEmpty()) (currentIdx - 1 + players.size) % players.size else 0
    val nextIdx = if (players.isNotEmpty()) (currentIdx + 1) % players.size else 0

    val prevPlayer = players.getOrNull(prevIdx)
    val currPlayer = players.getOrNull(currentIdx)
    val nextPlayer = players.getOrNull(nextIdx)

    Surface(
        modifier = modifier
            .width(72.dp)
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
                    .size(42.dp)
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

            // MIDDLE: Player Turn Queue (Previous, Current Turn, Next)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Previous Player (Smaller by 30%)
                if (prevPlayer != null && players.size > 1) {
                    PlayerQueueItem(
                        player = prevPlayer,
                        isCurrentTurn = false,
                        subLabel = "PREV"
                    )
                }

                // 2. Current Player (Active, Full Size)
                if (currPlayer != null) {
                    PlayerQueueItem(
                        player = currPlayer,
                        isCurrentTurn = true,
                        subLabel = "TURN"
                    )
                }

                // 3. Next Player (Smaller by 30%)
                if (nextPlayer != null && players.size > 1 && nextPlayer != prevPlayer) {
                    PlayerQueueItem(
                        player = nextPlayer,
                        isCurrentTurn = false,
                        subLabel = "NEXT"
                    )
                }
            }

            // BOTTOM: Quick Play Thunder Icon Toggle Button
            IconButton(
                onClick = onToggleQuickPlay,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isQuickPlayEnabled) NitroGreen else SurfaceDarkCard)
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = "Quick Play",
                    tint = if (isQuickPlayEnabled) Color(0xFF0F172A) else TextSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerQueueItem(
    player: PlayerRuntimeState,
    isCurrentTurn: Boolean,
    subLabel: String
) {
    val carColor = Color(android.graphics.Color.parseColor(player.profile.carAvatar.colorHex))
    val avatarSize = if (isCurrentTurn) 40.dp else 28.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (isCurrentTurn) 1f else 0.55f)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(carColor)
                .border(
                    width = if (isCurrentTurn) 2.5.dp else 1.dp,
                    color = if (isCurrentTurn) Color.White else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isCurrentTurn) 22.dp else 16.dp)
            )
        }

        Text(
            text = player.profile.name.take(6),
            fontSize = if (isCurrentTurn) 11.sp else 8.5.sp,
            fontWeight = if (isCurrentTurn) FontWeight.Black else FontWeight.Normal,
            color = if (isCurrentTurn) carColor else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = subLabel,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentTurn) NitroGreen else TextMuted
        )
    }
}

/**
 * Right Dice Area: Scrollable Roll History Sidebar on Left + Centered Dice Roller
 */
@Composable
private fun RightDiceArea(
    state: GameSessionState,
    currentPlayer: PlayerRuntimeState?,
    isQuickPlayEnabled: Boolean,
    isRollingAnimation: Boolean,
    onNextTurn: () -> Unit,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Roll History Vertical Sidebar
        RollHistorySidebar(
            state = state,
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Main Dice & Controls Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Next Racer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER && state.winnerPlayerId == null) {
                    Button(
                        onClick = onNextTurn,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Next >", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Fixed Middle: Dice Box with "ROLL ?"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (state.winnerPlayerId == null) {
                    val isDiceActive = (state.turnPhase == TurnPhase.WAITING_FOR_ROLL) || (isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER)
                    DiceRollerComponent(
                        diceSpec = state.currentActiveDiceSpec,
                        lastRoll = state.currentRollResult,
                        isRolling = isRollingAnimation,
                        isCurrentPlayerTurn = isDiceActive,
                        onRollClick = onRollDice
                    )
                }
            }

            // Bottom Player Tile position info
            if (currentPlayer != null) {
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
 * Scrollable list of recent rolls with numbers in the player's avatar color
 */
@Composable
private fun RollHistorySidebar(
    state: GameSessionState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        color = SurfaceDarkCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ROLLS",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            val rollLogs = state.logHistory.filter { it.icon == "casino" }

            if (rollLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("-", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rollLogs) { log ->
                        val player = state.players.find { it.profile.id == log.playerId }
                        val playerColor = player?.let {
                            Color(android.graphics.Color.parseColor(it.profile.carAvatar.colorHex))
                        } ?: PrimaryNeon

                        val rollNumber = log.message.substringAfter("rolled ").substringBefore(" on").trim()

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(playerColor.copy(alpha = 0.2f))
                                .border(1.5.dp, playerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rollNumber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = playerColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated "ROLL AGAIN!" banner that zooms in center and slides to the side
 */
@Composable
private fun AnimatedRollAgainBanner(
    extraRollAwarded: Boolean,
    reason: String?
) {
    AnimatedVisibility(
        visible = extraRollAwarded,
        enter = scaleIn(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
        exit = scaleOut(tween(200)) + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
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
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = NitroGreen,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROLL AGAIN!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = NitroGreen,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = reason ?: "Bonus roll awarded!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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
