package com.gamechest.core.network

import com.gamechest.core.engine.GameAction
import com.gamechest.core.engine.GameSessionState
import com.gamechest.core.model.CarAvatar
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
data class DiscoveredRoom(
    val roomName: String,
    val hostName: String,
    val hostAddress: String,
    val port: Int = 8998,
    val currentPlayers: Int = 1,
    val maxPlayers: Int = 4,
    val gameTitle: String = "Rev-Up Racers",
    val lastSeenTimestamp: Long = 0L
)

@Serializable
data class NetworkPeer(
    val peerId: String,
    val displayName: String,
    val isHost: Boolean,
    val ipAddress: String? = null,
    val profile: PlayerProfile? = null,
    val isReady: Boolean = false,
    val slotIndex: Int = 0
)

@Serializable
sealed interface NetworkPacket {
    @Serializable
    data class JoinLobby(val profile: PlayerProfile) : NetworkPacket

    @Serializable
    data class PlayerReadyToggle(val peerId: String, val isReady: Boolean) : NetworkPacket

    @Serializable
    data class VehicleSelect(val peerId: String, val avatar: CarAvatar) : NetworkPacket

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

    @Serializable
    data class Disconnect(val peerId: String) : NetworkPacket
}

interface GameTransport {
    val mode: TransportMode
    val isConnected: StateFlow<Boolean>
    val connectedPeers: StateFlow<List<NetworkPeer>>
    val receivedPackets: Flow<NetworkPacket>
    val localIpAddress: StateFlow<String?>
    val discoveredRooms: StateFlow<List<DiscoveredRoom>>

    suspend fun startHosting(port: Int = 8998, hostProfile: PlayerProfile): Result<Unit>
    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun joinHost(hostAddress: String, port: Int = 8998, clientProfile: PlayerProfile): Result<Unit>
    suspend fun sendPacket(packet: NetworkPacket): Result<Unit>
    suspend fun toggleReady(peerId: String, isReady: Boolean)
    suspend fun selectVehicle(peerId: String, avatar: CarAvatar)
    suspend fun disconnect()
}
