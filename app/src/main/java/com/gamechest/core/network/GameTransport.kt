package com.gamechest.core.network

import com.gamechest.core.engine.GameAction
import com.gamechest.core.engine.GameSessionState
import com.gamechest.core.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
enum class TransportMode {
    SAME_DEVICE_LOCAL,
    WIFI_LAN,
    BLUETOOTH
}

@Serializable
data class NetworkPeer(
    val peerId: String,
    val displayName: String,
    val isHost: Boolean,
    val ipAddress: String? = null
)

@Serializable
sealed interface NetworkPacket {
    @Serializable
    data class JoinLobby(val profile: PlayerProfile) : NetworkPacket

    @Serializable
    data class LobbyUpdated(val peers: List<NetworkPeer>, val hostPlayerId: String) : NetworkPacket

    @Serializable
    data class StartGame(val initialSessionState: GameSessionState) : NetworkPacket

    @Serializable
    data class ActionBroadcast(val action: GameAction) : NetworkPacket

    @Serializable
    data class StateSync(val sessionState: GameSessionState) : NetworkPacket

    @Serializable
    data class Ping(val timestamp: Long) : NetworkPacket
}

interface GameTransport {
    val mode: TransportMode
    val isConnected: StateFlow<Boolean>
    val connectedPeers: StateFlow<List<NetworkPeer>>
    val receivedPackets: Flow<NetworkPacket>

    suspend fun startHosting(port: Int = 8998, hostProfile: PlayerProfile): Result<Unit>
    suspend fun joinHost(hostAddress: String, port: Int = 8998, clientProfile: PlayerProfile): Result<Unit>
    suspend fun sendPacket(packet: NetworkPacket): Result<Unit>
    suspend fun disconnect()
}
