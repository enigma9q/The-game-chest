package com.gamechest.ui.lobby

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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THE GAME CHEST",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = gamePack.manifest.title,
                        fontSize = 20.sp,
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
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Mode Selector
            item {
                Text(
                    text = "Multiplayer Mode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                            onClick = { transportMode = mode },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryNeon else SurfaceDarkCard,
                                contentColor = if (isSelected) Color(0xFF0F172A) else TextSecondary
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Player Seats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Racers (${players.size}/4)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (players.size < 4) {
                        TextButton(
                            onClick = {
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
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Racer", color = PrimaryNeon)
                        }
                    }
                }
            }

            itemsIndexed(players) { index, player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Car Avatar Badge - opens available colors picker
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(player.carAvatar.colorHex)))
                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    showColorDialogForPlayerIndex = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = "Pick Color",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${player.carAvatar.displayName}${if (isCustomLoadoutActive) " • Start: Tile ${player.customStartTile ?: 1} • Dice: ${player.customDiceSpec?.label ?: "Default"}" else ""}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        if (isCustomLoadoutActive) {
                            IconButton(onClick = { showDiceDialogForPlayerIndex = index }) {
                                Icon(Icons.Default.Casino, contentDescription = "Pick Dice", tint = AccentYellow)
                            }
                        }

                        if (players.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = players.toMutableList()
                                    updated.removeAt(index)
                                    players = updated
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // Mutators Panel (5 Presets)
            item {
                MutatorSelector(
                    availableMutators = gamePack.manifest.availableMutators,
                    selectedMutators = selectedMutators,
                    onMutatorToggled = { mutatorId ->
                        selectedMutators = if (selectedMutators.contains(mutatorId)) {
                            selectedMutators - mutatorId
                        } else {
                            // If toggling 1d60 modes, ensure exclusivity between classic 1d6 and target 1d60
                            if (mutatorId == MutatorId.NITRO_TARGET_1D60) {
                                (selectedMutators - MutatorId.NITRO_ASSIST_1D60 - MutatorId.CLASSIC_GRAND_PRIX) + mutatorId
                            } else if (mutatorId == MutatorId.NITRO_ASSIST_1D60) {
                                (selectedMutators - MutatorId.NITRO_TARGET_1D60 - MutatorId.CLASSIC_GRAND_PRIX) + mutatorId
                            } else if (mutatorId == MutatorId.CLASSIC_GRAND_PRIX) {
                                (selectedMutators - MutatorId.NITRO_TARGET_1D60 - MutatorId.NITRO_ASSIST_1D60) + mutatorId
                            } else {
                                selectedMutators + mutatorId
                            }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dice Picker Dialog for Mutator 5
    if (showDiceDialogForPlayerIndex != null) {
        val targetIdx = showDiceDialogForPlayerIndex!!
        val targetPlayer = players.getOrNull(targetIdx)
        if (targetPlayer != null) {
            AlertDialog(
                onDismissRequest = { showDiceDialogForPlayerIndex = null },
                title = {
                    Text("Select Dice for ${targetPlayer.name}", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StandardDiceType.entries.size) { i ->
                            val diceType = StandardDiceType.entries[i]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = players.toMutableList()
                                        updated[targetIdx] = targetPlayer.copy(
                                            customDiceSpec = DiceSpec.standard(diceType)
                                        )
                                        players = updated
                                        showDiceDialogForPlayerIndex = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, tint = PrimaryNeon)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(diceType.displayName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDiceDialogForPlayerIndex = null }) {
                        Text("Close", color = PrimaryNeon)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }

    // Available Car Color Picker Dialog (Filters only remaining untaken colors)
    if (showColorDialogForPlayerIndex != null) {
        val targetIdx = showColorDialogForPlayerIndex!!
        val targetPlayer = players.getOrNull(targetIdx)
        if (targetPlayer != null) {
            val takenColors = players.mapIndexedNotNull { i, p -> if (i != targetIdx) p.carAvatar else null }.toSet()
            val availableColors = CarAvatar.entries.filter { !takenColors.contains(it) }

            AlertDialog(
                onDismissRequest = { showColorDialogForPlayerIndex = null },
                title = {
                    Text("Select Color for ${targetPlayer.name}", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "${availableColors.size} color${if (availableColors.size != 1) "s" else ""} available",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableColors.size) { i ->
                                val avatar = availableColors[i]
                                val isCurrent = targetPlayer.carAvatar == avatar
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updated = players.toMutableList()
                                            updated[targetIdx] = targetPlayer.copy(carAvatar = avatar)
                                            players = updated
                                            showColorDialogForPlayerIndex = null
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) SurfaceDarkCard else Color(0xFF0F172A)
                                    ),
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, PrimaryNeon) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(avatar.colorHex))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = avatar.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = PrimaryNeon,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorDialogForPlayerIndex = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
