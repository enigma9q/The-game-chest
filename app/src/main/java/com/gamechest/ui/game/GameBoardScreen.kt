package com.gamechest.ui.game

import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.engine.*
import com.gamechest.core.model.*
import com.gamechest.ui.components.DiceRollerComponent
import com.gamechest.ui.components.RacetrackCanvas
import com.gamechest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoardScreen(
    engine: GameEngine,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isRollingAnimation by remember { mutableStateOf(false) }
    var selectedTileInfo by remember { mutableStateOf<TileNode?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }
    var isQuickPlayEnabled by remember { mutableStateOf(false) }

    val currentPlayer = state.players.getOrNull(state.currentTurnPlayerIndex)

    // Quick Play auto-advance effect
    LaunchedEffect(state.turnPhase, isQuickPlayEnabled, state.extraRollAwarded) {
        if (isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER && state.winnerPlayerId == null && !state.extraRollAwarded) {
            delay(750)
            engine.nextTurn()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        if (isLandscape) {
            // SIDEWAYS / LANDSCAPE: Board on LEFT, Controls & Dice on RIGHT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left side: Track Board + Overlays
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    BoardWithRollAgainOverlay(
                        state = state,
                        onTileClicked = { selectedTileInfo = it }
                    )
                }

                // Right side: Controls Panel
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp)),
                    color = SurfaceDark
                ) {
                    DiceControlsPanel(
                        state = state,
                        currentPlayer = currentPlayer,
                        isQuickPlayEnabled = isQuickPlayEnabled,
                        onToggleQuickPlay = { isQuickPlayEnabled = !isQuickPlayEnabled },
                        isRollingAnimation = isRollingAnimation,
                        selectedTileInfo = selectedTileInfo,
                        onClearTileInfo = { selectedTileInfo = null },
                        onOpenMenu = { showMenuSheet = true },
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
        } else {
            // PORTRAIT: Board on TOP, Controls & Dice on BOTTOM
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top: Track Board + Overlays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BoardWithRollAgainOverlay(
                        state = state,
                        onTileClicked = { selectedTileInfo = it }
                    )
                }

                // Bottom: Controls Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = SurfaceDark
                ) {
                    DiceControlsPanel(
                        state = state,
                        currentPlayer = currentPlayer,
                        isQuickPlayEnabled = isQuickPlayEnabled,
                        onToggleQuickPlay = { isQuickPlayEnabled = !isQuickPlayEnabled },
                        isRollingAnimation = isRollingAnimation,
                        selectedTileInfo = selectedTileInfo,
                        onClearTileInfo = { selectedTileInfo = null },
                        onOpenMenu = { showMenuSheet = true },
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

    // Bottom Sheet Game Menu
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = state.pack.manifest.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Turn ${state.turnNumber} • ${state.players.size} Racers",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = { showMenuSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

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
                                Text(label, fontSize = 11.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF334155))

                Button(
                    onClick = {
                        showMenuSheet = false
                        showLogsSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = AccentYellow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("View Match History", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        showMenuSheet = false
                        engine.restartGame(keepMutators = true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryNeon)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Restart Race", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        showMenuSheet = false
                        onExitGame()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = HazardRed)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Exit to Main Lobby", color = HazardRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Match Event Log Bottom Sheet
    if (showLogsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogsSheet = false },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Race Event Log",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.logHistory) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDarkCard)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log.message,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardWithRollAgainOverlay(
    state: GameSessionState,
    onTileClicked: (TileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        RacetrackCanvas(
            layout = state.pack.tableLayout,
            players = state.players,
            activeMutators = state.activeMutators,
            onTileClicked = onTileClicked,
            modifier = Modifier.fillMaxSize()
        )

        // Whole-board "ROLL AGAIN!" Notification with GREEN Title
        androidx.compose.animation.AnimatedVisibility(
            visible = state.extraRollAwarded,
            enter = scaleIn(tween(300)) + fadeIn(tween(300)),
            exit = scaleOut(tween(250)) + fadeOut(tween(250))
        ) {
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .border(3.dp, NitroGreen, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xF40F172A)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = null,
                            tint = NitroGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ROLL AGAIN!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = NitroGreen,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.extraRollReason ?: "Bonus roll awarded!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DiceControlsPanel(
    state: GameSessionState,
    currentPlayer: PlayerRuntimeState?,
    isQuickPlayEnabled: Boolean,
    onToggleQuickPlay: () -> Unit,
    isRollingAnimation: Boolean,
    selectedTileInfo: TileNode?,
    onClearTileInfo: () -> Unit,
    onOpenMenu: () -> Unit,
    onNextTurn: () -> Unit,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Player Info on Left, "Next Racer >" on Top-Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPlayer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Color(android.graphics.Color.parseColor(currentPlayer.profile.carAvatar.colorHex))
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${currentPlayer.profile.name}'s Turn",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (currentPlayer.currentTileId == 0) "Starting Grid (Tile 0)" else "Tile ${currentPlayer.currentTileId} / ${state.pack.tableLayout.finishTileId}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Top-Right Position: Next Racer > Button
            if (!isQuickPlayEnabled && state.turnPhase == TurnPhase.TURN_OVER && state.winnerPlayerId == null) {
                Button(
                    onClick = onNextTurn,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Next Racer >", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Tile info banner if tapped
        if (selectedTileInfo != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tile ${selectedTileInfo.index}: ${selectedTileInfo.description}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearTileInfo, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Middle: Fixed Centered Dice Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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

        // Bottom Bar: Menu on Bottom-LEFT, Quick Play on Bottom-RIGHT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Button (Bottom-Left)
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

            // Quick Play Button (Bottom-Right: Green when active, Grey when not, no emoticons, no on/off text)
            Button(
                onClick = onToggleQuickPlay,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isQuickPlayEnabled) NitroGreen else SurfaceDarkCard,
                    contentColor = if (isQuickPlayEnabled) Color.White else TextSecondary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Quick Play",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
