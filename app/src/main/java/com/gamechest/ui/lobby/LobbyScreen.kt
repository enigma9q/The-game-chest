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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.model.*
import com.gamechest.core.network.NetworkPacket
import com.gamechest.core.network.NetworkPeer
import com.gamechest.core.network.TransportMode
import com.gamechest.core.network.WifiLanTransport
import com.gamechest.ui.components.MutatorSelector
import com.gamechest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LobbyScreen(
    gamePack: GamePack,
    onStartGame: (List<PlayerProfile>, Set<MutatorId>, TransportMode) -> Unit,
    onBrowsePacks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val wifiTransport = remember { WifiLanTransport() }
    val isWifiConnected by wifiTransport.isConnected.collectAsState()
    val connectedPeers by wifiTransport.connectedPeers.collectAsState()
    val localIpAddress by wifiTransport.localIpAddress.collectAsState()

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

    // Wi-Fi Local Co-op Specific State
    var wifiRole by remember { mutableStateOf("HOST") } // "HOST" or "JOIN"
    var hostIpInput by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("Player 2") }
    var clientAvatar by remember { mutableStateOf(CarAvatar.TURBO_BLUE) }
    var isClientReady by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var wifiStatusMessage by remember { mutableStateOf<String?>(null) }

    var showDiceDialogForPlayerIndex by remember { mutableStateOf<Int?>(null) }
    var showColorDialogForPlayerIndex by remember { mutableStateOf<Int?>(null) }

    val isCustomLoadoutActive = selectedMutators.contains(MutatorId.CUSTOM_GRID_DICE_LOADOUT)

    // Handle incoming network packets on Client
    LaunchedEffect(transportMode) {
        if (transportMode == TransportMode.WIFI_LAN) {
            wifiTransport.receivedPackets.collect { packet ->
                when (packet) {
                    is NetworkPacket.StartGame -> {
                        val activeProfiles = packet.initialSessionState.players.map { it.profile }
                        onStartGame(activeProfiles, packet.initialSessionState.activeMutators, TransportMode.WIFI_LAN)
                    }
                    else -> {}
                }
            }
        }
    }

    // Automatically initialize hosting if Host mode selected
    LaunchedEffect(transportMode, wifiRole) {
        if (transportMode == TransportMode.WIFI_LAN && wifiRole == "HOST") {
            val hostProfile = players.firstOrNull() ?: PlayerProfile("p1", "Host Player", CarAvatar.SPEEDSTER_RED)
            wifiTransport.startHosting(8998, hostProfile)
        } else if (transportMode == TransportMode.SAME_DEVICE_LOCAL) {
            wifiTransport.disconnect()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight || maxWidth > 650.dp

        val allPeersReady = connectedPeers.isNotEmpty() && connectedPeers.all { it.isReady || it.isHost }

        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = if (isLandscape) 8.dp else 14.dp),
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
                        val isStartEnabled = if (transportMode == TransportMode.WIFI_LAN) {
                            if (wifiRole == "HOST") allPeersReady else isClientReady
                        } else true

                        Button(
                            onClick = {
                                if (transportMode == TransportMode.WIFI_LAN && wifiRole == "HOST") {
                                    val activeProfiles = connectedPeers.mapNotNull { it.profile }
                                    onStartGame(activeProfiles.ifEmpty { players }, selectedMutators, transportMode)
                                } else {
                                    onStartGame(players, selectedMutators, transportMode)
                                }
                            },
                            enabled = isStartEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStartEnabled) PrimaryNeon else Color(0x3300E5FF),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (transportMode == TransportMode.WIFI_LAN && !allPeersReady && wifiRole == "HOST") "WAITING FOR PLAYERS TO BE READY..." else "START RACE",
                                fontSize = 15.sp,
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

                            if (transportMode == TransportMode.WIFI_LAN) {
                                item {
                                    WifiLanControlCard(
                                        wifiRole = wifiRole,
                                        localIpAddress = localIpAddress,
                                        isHost = wifiRole == "HOST",
                                        isClientConnected = isWifiConnected,
                                        hostIpInput = hostIpInput,
                                        onHostIpInputChange = { hostIpInput = it },
                                        onSelectRole = {
                                            wifiRole = it
                                            wifiStatusMessage = null
                                        },
                                        onConnect = {
                                            if (hostIpInput.isNotBlank()) {
                                                isConnecting = true
                                                wifiStatusMessage = "Connecting to host..."
                                                coroutineScope.launch {
                                                    val clientProfile = PlayerProfile("client_${System.currentTimeMillis() % 1000}", clientName, clientAvatar)
                                                    val res = wifiTransport.joinHost(hostIpInput.trim(), 8998, clientProfile)
                                                    isConnecting = false
                                                    wifiStatusMessage = if (res.isSuccess) "Connected to Host!" else "Connection failed. Check IP & Wi-Fi."
                                                }
                                            }
                                        },
                                        statusMessage = wifiStatusMessage
                                    )
                                }

                                if (wifiRole == "HOST") {
                                    item {
                                        Text(
                                            text = "Connected Players (${connectedPeers.size}):",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    itemsIndexed(connectedPeers) { _, peer ->
                                        ConnectedPeerCard(peer = peer)
                                    }
                                } else {
                                    item {
                                        ClientReadyControlCard(
                                            clientName = clientName,
                                            clientAvatar = clientAvatar,
                                            isReady = isClientReady,
                                            isConnected = isWifiConnected,
                                            onToggleReady = {
                                                isClientReady = !isClientReady
                                                val peerId = connectedPeers.firstOrNull { !it.isHost }?.peerId ?: "client"
                                                coroutineScope.launch {
                                                    wifiTransport.toggleReady(peerId, isClientReady)
                                                }
                                            },
                                            onAvatarClick = { showColorDialogForPlayerIndex = 0 }
                                        )
                                    }
                                }
                            } else {
                                // PASS & PLAY LOCAL
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
                        }

                        // Bottom Start Button (Landscape)
                        val isStartEnabled = if (transportMode == TransportMode.WIFI_LAN) {
                            if (wifiRole == "HOST") allPeersReady else isClientReady
                        } else true

                        Button(
                            onClick = {
                                if (transportMode == TransportMode.WIFI_LAN && wifiRole == "HOST") {
                                    val activeProfiles = connectedPeers.mapNotNull { it.profile }
                                    onStartGame(activeProfiles.ifEmpty { players }, selectedMutators, transportMode)
                                } else {
                                    onStartGame(players, selectedMutators, transportMode)
                                }
                            },
                            enabled = isStartEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStartEnabled) PrimaryNeon else Color(0x3300E5FF),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (transportMode == TransportMode.WIFI_LAN && !allPeersReady && wifiRole == "HOST") "WAITING FOR ALL PLAYERS READY" else "START RACE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Right Column: Mutator Engine Customization Selector
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        MutatorSelector(
                            availableMutators = gamePack.manifest.availableMutators,
                            selectedMutators = selectedMutators,
                            onMutatorToggled = { id ->
                                selectedMutators = if (selectedMutators.contains(id)) {
                                    if (selectedMutators.size > 1) selectedMutators - id else selectedMutators
                                } else {
                                    selectedMutators + id
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // PORTRAIT LAYOUT
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        MultiplayerModeRow(
                            transportMode = transportMode,
                            onSelectMode = { transportMode = it }
                        )
                    }

                    if (transportMode == TransportMode.WIFI_LAN) {
                        item {
                            WifiLanControlCard(
                                wifiRole = wifiRole,
                                localIpAddress = localIpAddress,
                                isHost = wifiRole == "HOST",
                                isClientConnected = isWifiConnected,
                                hostIpInput = hostIpInput,
                                onHostIpInputChange = { hostIpInput = it },
                                onSelectRole = {
                                    wifiRole = it
                                    wifiStatusMessage = null
                                },
                                onConnect = {
                                    if (hostIpInput.isNotBlank()) {
                                        isConnecting = true
                                        wifiStatusMessage = "Connecting to host..."
                                        coroutineScope.launch {
                                            val clientProfile = PlayerProfile("client_${System.currentTimeMillis() % 1000}", clientName, clientAvatar)
                                            val res = wifiTransport.joinHost(hostIpInput.trim(), 8998, clientProfile)
                                            isConnecting = false
                                            wifiStatusMessage = if (res.isSuccess) "Connected to Host!" else "Connection failed. Check IP & Wi-Fi."
                                        }
                                    }
                                },
                                statusMessage = wifiStatusMessage
                            )
                        }

                        if (wifiRole == "HOST") {
                            item {
                                Text(
                                    text = "Connected Players (${connectedPeers.size}):",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            itemsIndexed(connectedPeers) { _, peer ->
                                ConnectedPeerCard(peer = peer)
                            }
                        } else {
                            item {
                                ClientReadyControlCard(
                                    clientName = clientName,
                                    clientAvatar = clientAvatar,
                                    isReady = isClientReady,
                                    isConnected = isWifiConnected,
                                    onToggleReady = {
                                        isClientReady = !isClientReady
                                        val peerId = connectedPeers.firstOrNull { !it.isHost }?.peerId ?: "client"
                                        coroutineScope.launch {
                                            wifiTransport.toggleReady(peerId, isClientReady)
                                        }
                                    },
                                    onAvatarClick = { showColorDialogForPlayerIndex = 0 }
                                )
                            }
                        }
                    } else {
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

                    item {
                        MutatorSelector(
                            availableMutators = gamePack.manifest.availableMutators,
                            selectedMutators = selectedMutators,
                            onMutatorToggled = { id ->
                                selectedMutators = if (selectedMutators.contains(id)) {
                                    if (selectedMutators.size > 1) selectedMutators - id else selectedMutators
                                } else {
                                    selectedMutators + id
                                }
                            }
                        )
                    }
                }
            }
        }

        // Vehicle Selection Dialog
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
                            text = "Choose Vehicle for ${player.name}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Select from available vehicles:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableAvatars.chunked(2).forEach { rowAvatars ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowAvatars.forEach { avatar ->
                                            val color = parseHexColor(avatar.colorHex)
                                            val isCurrent = player.carAvatar == avatar
                                            Button(
                                                onClick = {
                                                    players = players.toMutableList().also {
                                                        it[pIdx] = player.copy(carAvatar = avatar)
                                                    }
                                                    showColorDialogForPlayerIndex = null
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrent) SurfaceDark else DarkBackground,
                                                    contentColor = TextPrimary
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (isCurrent) 2.dp else 1.dp,
                                                    color = if (isCurrent) color else BorderDark
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                    )
                                                    Text(
                                                        text = avatar.displayName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
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

        // Custom Dice Dialog
        showDiceDialogForPlayerIndex?.let { pIdx ->
            val player = players.getOrNull(pIdx)
            if (player != null) {
                var selectedSides by remember { mutableStateOf(player.customDiceSpec?.sides ?: 6) }
                AlertDialog(
                    onDismissRequest = { showDiceDialogForPlayerIndex = null },
                    containerColor = SurfaceDarkCard,
                    title = {
                        Text("Player Dice Loadout: ${player.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Assign a custom die to this player:", fontSize = 13.sp, color = TextSecondary)
                            val diceOptions = listOf(2, 4, 6, 8, 10, 12, 20, 60, 100)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                diceOptions.take(5).forEach { sides ->
                                    FilterChip(selected = selectedSides == sides, onClick = { selectedSides = sides }, label = { Text("d$sides") })
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                diceOptions.drop(5).forEach { sides ->
                                    FilterChip(selected = selectedSides == sides, onClick = { selectedSides = sides }, label = { Text("d$sides") })
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newSpec = DiceSpec.custom(count = 1, sides = selectedSides)
                                players = players.toMutableList().also { it[pIdx] = player.copy(customDiceSpec = newSpec) }
                                showDiceDialogForPlayerIndex = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color(0xFF0F172A))
                        ) {
                            Text("Apply")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiceDialogForPlayerIndex = null }) { Text("Cancel", color = TextMuted) }
                    }
                )
            }
        }
    }
}

@Composable
private fun WifiLanControlCard(
    wifiRole: String,
    localIpAddress: String?,
    isHost: Boolean,
    isClientConnected: Boolean,
    hostIpInput: String,
    onHostIpInputChange: (String) -> Unit,
    onSelectRole: (String) -> Unit,
    onConnect: () -> Unit,
    statusMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Role Selector Tabs: HOST (SERVER) vs JOIN (CLIENT)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSelectRole("HOST") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHost) PrimaryNeon else SurfaceDark,
                        contentColor = if (isHost) Color(0xFF0F172A) else TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Host Room", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { onSelectRole("JOIN") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isHost) PrimaryNeon else SurfaceDark,
                        contentColor = if (!isHost) Color(0xFF0F172A) else TextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Join Room", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (isHost) {
                // Host Information
                Surface(
                    color = Color(0x2200E5FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("HOST WI-FI IP ADDRESS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                            Text(
                                text = "${localIpAddress ?: "127.0.0.1"}:8998",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = Color(0x3310B981),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SERVER ACTIVE", color = NitroGreen, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            } else {
                // Client Join Information
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter Host IP Address:", fontSize = 12.sp, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hostIpInput,
                            onValueChange = onHostIpInputChange,
                            placeholder = { Text("e.g. 192.168.1.100", fontSize = 13.sp, color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onConnect,
                            enabled = hostIpInput.isNotBlank() && !isClientConnected,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text(if (isClientConnected) "Connected" else "Connect", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            statusMessage?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (it.contains("Connected")) NitroGreen else AccentYellow
                )
            }
        }
    }
}

@Composable
private fun ConnectedPeerCard(peer: NetworkPeer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (peer.isReady || peer.isHost) NitroGreen else BorderDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val color = parseHexColor(peer.profile?.carAvatar?.colorHex ?: "#3B82F6")
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(color)
                )
                Column {
                    Text(peer.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(peer.profile?.carAvatar?.displayName ?: "Racer", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Surface(
                color = if (peer.isHost) Color(0x3300E5FF) else if (peer.isReady) Color(0x3310B981) else Color(0x33F59E0B),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (peer.isHost) "HOST 👑" else if (peer.isReady) "READY ✓" else "NOT READY ⏳",
                    color = if (peer.isHost) PrimaryNeon else if (peer.isReady) NitroGreen else AccentYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ClientReadyControlCard(
    clientName: String,
    clientAvatar: CarAvatar,
    isReady: Boolean,
    isConnected: Boolean,
    onToggleReady: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isReady) NitroGreen else BorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your Racer Setup:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable { onAvatarClick() }
                ) {
                    val color = parseHexColor(clientAvatar.colorHex)
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(2.dp, Color.White, CircleShape))
                    Column {
                        Text(clientName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${clientAvatar.displayName} (Tap to change)", fontSize = 12.sp, color = PrimaryNeon)
                    }
                }
            }

            Button(
                onClick = onToggleReady,
                enabled = isConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReady) NitroGreen else Color(0xFFF59E0B),
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(if (isReady) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isReady) "READY FOR RACE! (Click to Unready)" else "CLICK WHEN READY TO RACE", fontWeight = FontWeight.Black)
            }
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
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
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
            TextButton(
                onClick = onAddPlayer,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Add Racer", color = PrimaryNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    val color = parseHexColor(player.carAvatar.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onColorClick() }
            )

            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = player.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryNeon,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
                Text(
                    text = "${player.carAvatar.displayName} (Tap color to change)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (isCustomLoadoutActive) {
                Button(
                    onClick = onDiceClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF), contentColor = PrimaryNeon),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("d${player.customDiceSpec?.sides ?: 6}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
