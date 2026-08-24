package com.gamechest.ui.lobby

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.model.*
import com.gamechest.core.network.TransportMode
import com.gamechest.ui.components.MutatorSelector
import com.gamechest.ui.theme.*

@Composable
fun LobbyScreen(
    gamePack: GamePack,
    onStartGame: (List<PlayerProfile>, Set<MutatorId>, TransportMode) -> Unit,
    onBrowsePacks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var transportMode by remember { mutableStateOf(TransportMode.SAME_DEVICE_LOCAL) }
    var selectedMutators by remember {
        mutableStateOf(setOf(gamePack.manifest.defaultMutatorId))
    }

    var players by remember {
        mutableStateOf(
            listOf(
                PlayerProfile("p1", "Player 1", CarAvatar.SPEEDSTER_RED, customStartTile = 0),
                PlayerProfile("p2", "Player 2", CarAvatar.TURBO_BLUE, customStartTile = 0)
            )
        )
    }

    var showDiceDialogForPlayerIndex by remember { mutableStateOf<Int?>(null) }
    var showColorDialogForPlayerIndex by remember { mutableStateOf<Int?>(null) }

    val isCustomLoadoutActive = selectedMutators.contains(MutatorId.CUSTOM_GRID_DICE_LOADOUT)

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = if (isLandscape) 8.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THE GAME CHEST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = gamePack.manifest.title,
                        fontSize = if (isLandscape) 18.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
                IconButton(
                    onClick = onBrowsePacks,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceDarkCard)
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = "Game Packs", tint = AccentYellow)
                }
            }
        },
        bottomBar = {
            if (!isLandscape) {
                Surface(
                    color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            onStartGame(players, selectedMutators, transportMode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START RACE",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (isLandscape) {
            // LANDSCAPE: 2-COLUMN RESPONSIVE LAYOUT
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Mode Selector, Racers & Start Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            MultiplayerModeRow(
                                transportMode = transportMode,
                                onSelectMode = { transportMode = it }
                            )
                        }
                        item {
                            RacersHeader(
                                playerCount = players.size,
                                onAddPlayer = {
                                    if (players.size < 4) {
                                        val takenAvatars = players.map { it.carAvatar }.toSet()
                                        val nextAvatar = CarAvatar.entries.find { !takenAvatars.contains(it) } ?: CarAvatar.SPEEDSTER_RED
                                        val nextIdx = players.size + 1
                                        players = players + PlayerProfile(
                                            id = "p$nextIdx",
                                            name = "Player $nextIdx",
                                            carAvatar = nextAvatar,
                                            customStartTile = 0
                                        )
                                    }
                                }
                            )
                        }
                        itemsIndexed(players) { index, player ->
                            RacerCard(
                                player = player,
                                index = index,
                                canRemove = players.size > 1,
                                isCustomLoadoutActive = isCustomLoadoutActive,
                                onColorClick = { showColorDialogForPlayerIndex = index },
                                onDiceClick = { showDiceDialogForPlayerIndex = index },
                                onNameChange = { newName ->
                                    players = players.toMutableList().also {
                                        it[index] = player.copy(name = newName)
                                    }
                                },
                                onRemove = {
                                    if (players.size > 1) {
                                        players = players.filterIndexed { i, _ -> i != index }
                                    }
                                }
                            )
                        }
                    }

                    // Start Race Button
                    Button(
                        onClick = {
                            onStartGame(players, selectedMutators, transportMode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START RACE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Right Column: Mutator Selector
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp)),
                    color = SurfaceDark
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Race Mutators & Rules",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        item {
                            MutatorSelector(
                                availableMutators = gamePack.manifest.availableMutators,
                                selectedMutators = selectedMutators,
                                onMutatorToggled = { mutatorId ->
                                    selectedMutators = setOf(mutatorId)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // PORTRAIT: SINGLE COLUMN SCROLLABLE LAYOUT
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    MultiplayerModeRow(
                        transportMode = transportMode,
                        onSelectMode = { transportMode = it }
                    )
                }

                item {
                    RacersHeader(
                        playerCount = players.size,
                        onAddPlayer = {
                            if (players.size < 4) {
                                val takenAvatars = players.map { it.carAvatar }.toSet()
                                val nextAvatar = CarAvatar.entries.find { !takenAvatars.contains(it) } ?: CarAvatar.SPEEDSTER_RED
                                val nextIdx = players.size + 1
                                players = players + PlayerProfile(
                                    id = "p$nextIdx",
                                    name = "Player $nextIdx",
                                    carAvatar = nextAvatar,
                                    customStartTile = 0
                                )
                            }
                        }
                    )
                }

                itemsIndexed(players) { index, player ->
                    RacerCard(
                        player = player,
                        index = index,
                        canRemove = players.size > 1,
                        isCustomLoadoutActive = isCustomLoadoutActive,
                        onColorClick = { showColorDialogForPlayerIndex = index },
                        onDiceClick = { showDiceDialogForPlayerIndex = index },
                        onNameChange = { newName ->
                            players = players.toMutableList().also {
                                it[index] = player.copy(name = newName)
                            }
                        },
                        onRemove = {
                            if (players.size > 1) {
                                players = players.filterIndexed { i, _ -> i != index }
                            }
                        }
                    )
                }

                item {
                    Text(
                        text = "Game Mutators",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MutatorSelector(
                        availableMutators = gamePack.manifest.availableMutators,
                        selectedMutators = selectedMutators,
                        onMutatorToggled = { mutatorId ->
                            selectedMutators = setOf(mutatorId)
                        }
                    )
                }
            }
        }
    }

    // Color Selection Dialog
    showColorDialogForPlayerIndex?.let { pIdx ->
        val player = players.getOrNull(pIdx)
        if (player != null) {
            val takenAvatars = players.filterIndexed { i, _ -> i != pIdx }.map { it.carAvatar }.toSet()
            val availableAvatars = CarAvatar.entries.filter { !takenAvatars.contains(it) }

            AlertDialog(
                onDismissRequest = { showColorDialogForPlayerIndex = null },
                containerColor = SurfaceDarkCard,
                title = {
                    Text(
                        text = "Choose Color for ${player.name}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select from available racer colors:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableAvatars.forEach { avatar ->
                                val color = Color(android.graphics.Color.parseColor(avatar.colorHex))
                                val isCurrent = player.carAvatar == avatar
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isCurrent) 3.dp else 1.dp,
                                            color = if (isCurrent) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            players = players.toMutableList().also {
                                                it[pIdx] = player.copy(carAvatar = avatar)
                                            }
                                            showColorDialogForPlayerIndex = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorDialogForPlayerIndex = null }) {
                        Text("Close", color = PrimaryNeon)
                    }
                }
            )
        }
    }

    // Custom Dice Spec Dialog for Player
    showDiceDialogForPlayerIndex?.let { pIdx ->
        val player = players.getOrNull(pIdx)
        if (player != null) {
            var selectedSides by remember { mutableStateOf(player.customDiceSpec?.sides ?: 6) }
            AlertDialog(
                onDismissRequest = { showDiceDialogForPlayerIndex = null },
                containerColor = SurfaceDarkCard,
                title = {
                    Text(
                        text = "Custom Dice for ${player.name}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Assign a custom die to this player:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        val diceOptions = listOf(2, 4, 6, 8, 10, 12, 20, 60, 100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            diceOptions.take(5).forEach { sides ->
                                FilterChip(
                                    selected = selectedSides == sides,
                                    onClick = { selectedSides = sides },
                                    label = { Text("d$sides") }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            diceOptions.drop(5).forEach { sides ->
                                FilterChip(
                                    selected = selectedSides == sides,
                                    onClick = { selectedSides = sides },
                                    label = { Text("d$sides") }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newSpec = DiceSpec.custom(count = 1, sides = selectedSides)
                            players = players.toMutableList().also {
                                it[pIdx] = player.copy(customDiceSpec = newSpec)
                            }
                            showDiceDialogForPlayerIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color(0xFF0F172A))
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiceDialogForPlayerIndex = null }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
private fun MultiplayerModeRow(
    transportMode: TransportMode,
    onSelectMode: (TransportMode) -> Unit
) {
    Text(
        text = "Multiplayer Mode",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple(TransportMode.SAME_DEVICE_LOCAL, "Same Device", Icons.Default.PhoneAndroid),
            Triple(TransportMode.WIFI_LAN, "Wi-Fi Coop", Icons.Default.Wifi),
            Triple(TransportMode.BLUETOOTH, "Bluetooth", Icons.Default.Bluetooth)
        ).forEach { (mode, label, icon) ->
            val isSelected = transportMode == mode
            Button(
                onClick = { onSelectMode(mode) },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) PrimaryNeon else SurfaceDarkCard,
                    contentColor = if (isSelected) Color(0xFF0F172A) else TextSecondary
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RacersHeader(
    playerCount: Int,
    onAddPlayer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Racers ($playerCount/4)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (playerCount < 4) {
            TextButton(onClick = onAddPlayer) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Racer", color = PrimaryNeon, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RacerCard(
    player: PlayerProfile,
    index: Int,
    canRemove: Boolean,
    isCustomLoadoutActive: Boolean,
    onColorClick: () -> Unit,
    onDiceClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Car Avatar Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(player.carAvatar.colorHex)))
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onColorClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = "Change Color",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = player.carAvatar.displayName,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (isCustomLoadoutActive) {
                val diceLabel = player.customDiceSpec?.label ?: "1d6"
                OutlinedButton(
                    onClick = onDiceClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(diceLabel, fontSize = 11.sp, color = AccentYellow)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
