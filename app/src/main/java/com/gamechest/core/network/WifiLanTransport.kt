package com.gamechest.core.network

import com.gamechest.core.model.PlayerProfile
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.server.websocket.webSocket as serverWebSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class WifiLanTransport(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : GameTransport {
    override val mode: TransportMode = TransportMode.WIFI_LAN

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<NetworkPeer>>(emptyList())
    override val connectedPeers: StateFlow<List<NetworkPeer>> = _connectedPeers.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    override val receivedPackets: Flow<NetworkPacket> = _receivedPackets.asSharedFlow()

    private var serverEngine: ApplicationEngine? = null
    private var clientHttpClient: HttpClient? = null
    private var activeClientSession: WebSocketSession? = null
    private val serverClientSessions = ConcurrentHashMap<String, WebSocketSession>()

    override suspend fun startHosting(port: Int, hostProfile: PlayerProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val hostPeer = NetworkPeer(hostProfile.id, hostProfile.name, isHost = true, ipAddress = "0.0.0.0")
            _connectedPeers.value = listOf(hostPeer)

            serverEngine = embeddedServer(ServerCIO, port = port) {
                install(ServerWebSockets)
                routing {
                    serverWebSocket("/game") {
                        val sessionId = java.util.UUID.randomUUID().toString()
                        serverClientSessions[sessionId] = this
                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val packet = json.decodeFromString<NetworkPacket>(text)
                                    _receivedPackets.emit(packet)
                                    
                                    // Relay to other connected clients
                                    serverClientSessions.forEach { (id, session) ->
                                        if (id != sessionId) {
                                            try {
                                                session.send(Frame.Text(text))
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                            }
                        } finally {
                            serverClientSessions.remove(sessionId)
                        }
                    }
                }
            }.start(wait = false)

            _isConnected.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinHost(hostAddress: String, port: Int, clientProfile: PlayerProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val client = HttpClient(CIO) {
                install(WebSockets)
            }
            clientHttpClient = client

            coroutineScope.launch {
                try {
                    client.webSocket(host = hostAddress, port = port, path = "/game") {
                        activeClientSession = this
                        _isConnected.value = true
                        
                        // Send initial join packet
                        val joinPacket = NetworkPacket.JoinLobby(clientProfile)
                        send(Frame.Text(json.encodeToString<NetworkPacket>(joinPacket)))

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val packet = json.decodeFromString<NetworkPacket>(text)
                                _receivedPackets.emit(packet)
                            }
                        }
                    }
                } catch (e: Exception) {
                    _isConnected.value = false
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPacket(packet: NetworkPacket): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val text = json.encodeToString<NetworkPacket>(packet)
            val clientSession = activeClientSession
            if (clientSession != null) {
                clientSession.send(Frame.Text(text))
            }

            serverClientSessions.values.forEach { session ->
                try {
                    session.send(Frame.Text(text))
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            serverEngine?.stop(100, 500)
            serverEngine = null
            clientHttpClient?.close()
            clientHttpClient = null
            activeClientSession = null
            serverClientSessions.clear()
            _isConnected.value = false
            _connectedPeers.value = emptyList()
        } catch (_: Exception) {}
    }
}
