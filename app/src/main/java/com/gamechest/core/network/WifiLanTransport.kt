package com.gamechest.core.network

import com.gamechest.core.model.CarAvatar
import com.gamechest.core.model.PlayerProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*
import java.util.concurrent.ConcurrentHashMap

class WifiLanTransport(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : GameTransport {
    override val mode: TransportMode = TransportMode.WIFI_LAN

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<NetworkPeer>>(emptyList())
    override val connectedPeers: StateFlow<List<NetworkPeer>> = _connectedPeers.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    override val receivedPackets: Flow<NetworkPacket> = _receivedPackets.asSharedFlow()

    private val _localIpAddress = MutableStateFlow<String?>(null)
    override val localIpAddress: StateFlow<String?> = _localIpAddress.asStateFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    override val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private val clientWriters = ConcurrentHashMap<String, BufferedWriter>()
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var beaconJob: Job? = null
    private var discoveryJob: Job? = null
    private var currentHostProfile: PlayerProfile? = null

    companion object {
        const val TCP_GAME_PORT = 8998
        const val UDP_BEACON_PORT = 8999
    }

    init {
        detectLocalIp()
    }

    fun detectLocalIp() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val hostIp = addr.hostAddress
                        if (hostIp != null && !hostIp.startsWith("127.")) {
                            _localIpAddress.value = hostIp
                            return
                        }
                    }
                }
            }
        } catch (_: Exception) {
            _localIpAddress.value = "127.0.0.1"
        }
    }

    override suspend fun startHosting(port: Int, hostProfile: PlayerProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            stopDiscovery()
            detectLocalIp()
            currentHostProfile = hostProfile

            val hostIp = _localIpAddress.value ?: "127.0.0.1"
            val hostPeer = NetworkPeer(
                peerId = hostProfile.id,
                displayName = hostProfile.name,
                isHost = true,
                ipAddress = hostIp,
                profile = hostProfile,
                isReady = true,
                slotIndex = 0
            )
            _connectedPeers.value = listOf(hostPeer)

            val server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress(port))
            serverSocket = server
            _isConnected.value = true

            // 1. Accept Client TCP Connections
            serverJob = coroutineScope.launch {
                while (isActive && !server.isClosed) {
                    try {
                        val socket = server.accept()
                        launch { handleServerClient(socket) }
                    } catch (_: Exception) {
                        if (server.isClosed) break
                    }
                }
            }

            // 2. Start UDP Broadcast Beacon every 1.0 second
            beaconJob = coroutineScope.launch {
                val socket = try {
                    DatagramSocket().apply { broadcast = true }
                } catch (_: Exception) { null }

                while (isActive && socket != null && !socket.isClosed) {
                    try {
                        val beaconData = DiscoveredRoom(
                            roomName = "${hostProfile.name}'s Room",
                            hostName = hostProfile.name,
                            hostAddress = hostIp,
                            port = port,
                            currentPlayers = _connectedPeers.value.size,
                            maxPlayers = 4,
                            gameTitle = "Rev-Up Racers: Turbo Circuit",
                            lastSeenTimestamp = System.currentTimeMillis()
                        )
                        val bytes = json.encodeToString(beaconData).toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), UDP_BEACON_PORT)
                        socket.send(packet)
                    } catch (_: Exception) {}
                    delay(1000)
                }
                socket?.close()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _isConnected.value = false
            Result.failure(e)
        }
    }

    override suspend fun startDiscovery() = withContext(Dispatchers.IO) {
        if (discoveryJob?.isActive == true) return@withContext
        _discoveredRooms.value = emptyList()

        discoveryJob = coroutineScope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_BEACON_PORT))
                    soTimeout = 2000
                }

                val buffer = ByteArray(2048)
                while (isActive && socket != null && !socket.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val room = json.decodeFromString<DiscoveredRoom>(text).copy(
                            hostAddress = packet.address.hostAddress ?: "127.0.0.1",
                            lastSeenTimestamp = System.currentTimeMillis()
                        )

                        // Update or add discovered room
                        val now = System.currentTimeMillis()
                        val updated = _discoveredRooms.value
                            .filter { now - it.lastSeenTimestamp < 3500 && it.hostAddress != room.hostAddress }
                            .plus(room)
                        _discoveredRooms.value = updated
                    } catch (_: SocketTimeoutException) {
                        // Purge expired rooms
                        val now = System.currentTimeMillis()
                        _discoveredRooms.value = _discoveredRooms.value.filter { now - it.lastSeenTimestamp < 3500 }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
            }
        }
    }

    override suspend fun stopDiscovery() = withContext(Dispatchers.IO) {
        discoveryJob?.cancel()
        discoveryJob = null
        _discoveredRooms.value = emptyList()
    }

    private suspend fun handleServerClient(socket: Socket) = withContext(Dispatchers.IO) {
        var clientPeerId: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

            while (isActive && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val packet = json.decodeFromString<NetworkPacket>(line)
                _receivedPackets.emit(packet)

                when (packet) {
                    is NetworkPacket.JoinLobby -> {
                        clientPeerId = packet.profile.id
                        clientWriters[packet.profile.id] = writer

                        // Allocate next free slot (1..3)
                        val takenSlots = _connectedPeers.value.map { it.slotIndex }.toSet()
                        val nextSlot = (1..3).firstOrNull { !takenSlots.contains(it) } ?: (_connectedPeers.value.size)

                        val newPeer = NetworkPeer(
                            peerId = packet.profile.id,
                            displayName = packet.profile.name,
                            isHost = false,
                            ipAddress = socket.inetAddress.hostAddress,
                            profile = packet.profile,
                            isReady = false,
                            slotIndex = nextSlot
                        )
                        _connectedPeers.value = _connectedPeers.value.filter { it.peerId != packet.profile.id } + newPeer
                        broadcastLobbyState()
                    }
                    is NetworkPacket.PlayerReadyToggle -> {
                        _connectedPeers.value = _connectedPeers.value.map {
                            if (it.peerId == packet.peerId) it.copy(isReady = packet.isReady) else it
                        }
                        broadcastLobbyState()
                    }
                    is NetworkPacket.VehicleSelect -> {
                        _connectedPeers.value = _connectedPeers.value.map {
                            if (it.peerId == packet.peerId) it.copy(profile = it.profile?.copy(carAvatar = packet.avatar)) else it
                        }
                        broadcastLobbyState()
                    }
                    is NetworkPacket.ActionBroadcast, is NetworkPacket.StartGame, is NetworkPacket.StateSync -> {
                        relayPacket(packet, senderId = clientPeerId)
                    }
                    is NetworkPacket.Disconnect -> {
                        break
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) {
        } finally {
            if (clientPeerId != null) {
                clientWriters.remove(clientPeerId)
                _connectedPeers.value = _connectedPeers.value.filter { it.peerId != clientPeerId }
                broadcastLobbyState()
            }
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun broadcastLobbyState() {
        val hostId = currentHostProfile?.id ?: _connectedPeers.value.firstOrNull()?.peerId ?: ""
        val lobbyPacket = NetworkPacket.LobbyUpdated(peers = _connectedPeers.value, hostPlayerId = hostId)
        sendPacket(lobbyPacket)
    }

    private suspend fun relayPacket(packet: NetworkPacket, senderId: String?) {
        val text = json.encodeToString(packet) + "\n"
        clientWriters.forEach { (id, writer) ->
            if (id != senderId) {
                try {
                    writer.write(text)
                    writer.flush()
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun joinHost(hostAddress: String, port: Int, clientProfile: PlayerProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            stopDiscovery()

            // Parse clean IP and port
            var cleanHost = hostAddress.trim()
            var targetPort = port
            if (cleanHost.contains(":")) {
                val parts = cleanHost.split(":")
                cleanHost = parts[0].trim()
                targetPort = parts.getOrNull(1)?.toIntOrNull() ?: port
            }

            val socket = Socket()
            socket.connect(InetSocketAddress(cleanHost, targetPort), 5000)
            clientSocket = socket

            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            clientWriter = writer
            _isConnected.value = true

            // Send Join packet
            val joinPacket = NetworkPacket.JoinLobby(clientProfile)
            val joinJson = json.encodeToString<NetworkPacket>(joinPacket) + "\n"
            writer.write(joinJson)
            writer.flush()

            clientJob = coroutineScope.launch {
                try {
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    while (isActive && !socket.isClosed) {
                        val line = reader.readLine() ?: break
                        val packet = json.decodeFromString<NetworkPacket>(line)
                        _receivedPackets.emit(packet)

                        if (packet is NetworkPacket.LobbyUpdated) {
                            _connectedPeers.value = packet.peers
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    _isConnected.value = false
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _isConnected.value = false
            Result.failure(e)
        }
    }

    override suspend fun sendPacket(packet: NetworkPacket): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val text = json.encodeToString(packet) + "\n"

            // If client, send to host
            clientWriter?.let { writer ->
                writer.write(text)
                writer.flush()
            }

            // If host, send to all connected clients
            clientWriters.values.forEach { writer ->
                try {
                    writer.write(text)
                    writer.flush()
                } catch (_: Exception) {}
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReady(peerId: String, isReady: Boolean) {
        val packet = NetworkPacket.PlayerReadyToggle(peerId, isReady)
        if (serverSocket != null) {
            _connectedPeers.value = _connectedPeers.value.map {
                if (it.peerId == peerId) it.copy(isReady = isReady) else it
            }
            broadcastLobbyState()
        } else {
            sendPacket(packet)
        }
    }

    override suspend fun selectVehicle(peerId: String, avatar: CarAvatar) {
        val packet = NetworkPacket.VehicleSelect(peerId, avatar)
        if (serverSocket != null) {
            _connectedPeers.value = _connectedPeers.value.map {
                if (it.peerId == peerId) it.copy(profile = it.profile?.copy(carAvatar = avatar)) else it
            }
            broadcastLobbyState()
        } else {
            sendPacket(packet)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            beaconJob?.cancel()
            beaconJob = null
            serverJob?.cancel()
            serverJob = null
            clientJob?.cancel()
            clientJob = null

            clientWriter?.let {
                try {
                    val disc = json.encodeToString<NetworkPacket>(NetworkPacket.Disconnect(_connectedPeers.value.firstOrNull()?.peerId ?: "")) + "\n"
                    it.write(disc)
                    it.flush()
                } catch (_: Exception) {}
            }

            try { serverSocket?.close() } catch (_: Exception) {}
            try { clientSocket?.close() } catch (_: Exception) {}
            serverSocket = null
            clientSocket = null
            clientWriter = null
            clientWriters.clear()

            _connectedPeers.value = emptyList()
            _isConnected.value = false
        } catch (_: Exception) {}
    }
}
