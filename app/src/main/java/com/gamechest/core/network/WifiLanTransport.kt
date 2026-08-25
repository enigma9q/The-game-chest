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
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
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

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private val clientWriters = ConcurrentHashMap<String, BufferedWriter>()
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var currentHostProfile: PlayerProfile? = null

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
            detectLocalIp()
            currentHostProfile = hostProfile

            val hostIp = _localIpAddress.value ?: "127.0.0.1"
            val hostPeer = NetworkPeer(
                peerId = hostProfile.id,
                displayName = hostProfile.name,
                isHost = true,
                ipAddress = hostIp,
                profile = hostProfile,
                isReady = true
            )
            _connectedPeers.value = listOf(hostPeer)

            val server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress(port))
            serverSocket = server
            _isConnected.value = true

            serverJob = coroutineScope.launch {
                while (isActive && !server.isClosed) {
                    try {
                        val socket = server.accept()
                        launch { handleServerClient(socket) }
                    } catch (e: Exception) {
                        if (server.isClosed) break
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _isConnected.value = false
            Result.failure(e)
        }
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

                        val newPeer = NetworkPeer(
                            peerId = packet.profile.id,
                            displayName = packet.profile.name,
                            isHost = false,
                            ipAddress = socket.inetAddress.hostAddress,
                            profile = packet.profile,
                            isReady = false
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
                        // Relay to other clients
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
            val socket = Socket()
            socket.connect(InetSocketAddress(hostAddress, port), 4000)
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
            clientWriter?.let {
                try {
                    val disc = json.encodeToString<NetworkPacket>(NetworkPacket.Disconnect(_connectedPeers.value.firstOrNull()?.peerId ?: "")) + "\n"
                    it.write(disc)
                    it.flush()
                } catch (_: Exception) {}
            }

            serverJob?.cancel()
            clientJob?.cancel()
            serverJob = null
            clientJob = null

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
