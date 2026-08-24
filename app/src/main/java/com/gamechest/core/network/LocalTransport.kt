package com.gamechest.core.network

import com.gamechest.core.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalTransport : GameTransport {
    override val mode: TransportMode = TransportMode.SAME_DEVICE_LOCAL

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<NetworkPeer>>(emptyList())
    override val connectedPeers: StateFlow<List<NetworkPeer>> = _connectedPeers.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    override val receivedPackets: Flow<NetworkPacket> = _receivedPackets.asSharedFlow()

    override suspend fun startHosting(port: Int, hostProfile: PlayerProfile): Result<Unit> {
        _connectedPeers.value = listOf(
            NetworkPeer(peerId = hostProfile.id, displayName = hostProfile.name, isHost = true, ipAddress = "127.0.0.1")
        )
        _isConnected.value = true
        return Result.success(Unit)
    }

    override suspend fun joinHost(hostAddress: String, port: Int, clientProfile: PlayerProfile): Result<Unit> {
        return startHosting(port, clientProfile)
    }

    override suspend fun sendPacket(packet: NetworkPacket): Result<Unit> {
        _receivedPackets.emit(packet)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _connectedPeers.value = emptyList()
        _isConnected.value = false
    }
}
