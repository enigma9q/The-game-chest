package com.gamechest.core.network

import com.gamechest.core.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modular transport adapter placeholder for Bluetooth RFCOMM / Nearby Connections.
 * Plugs directly into GameTransport without requiring any changes to GameEngine or UI.
 */
class BluetoothTransportPlaceholder : GameTransport {
    override val mode: TransportMode = TransportMode.BLUETOOTH

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<NetworkPeer>>(emptyList())
    override val connectedPeers: StateFlow<List<NetworkPeer>> = _connectedPeers.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    override val receivedPackets: Flow<NetworkPacket> = _receivedPackets.asSharedFlow()

    override suspend fun startHosting(port: Int, hostProfile: PlayerProfile): Result<Unit> {
        // Ready for BluetoothAdapter.listenUsingRfcommWithServiceRecord(...)
        _connectedPeers.value = listOf(
            NetworkPeer(peerId = hostProfile.id, displayName = "${hostProfile.name} (BT Host)", isHost = true)
        )
        _isConnected.value = true
        return Result.success(Unit)
    }

    override suspend fun joinHost(hostAddress: String, port: Int, clientProfile: PlayerProfile): Result<Unit> {
        // Ready for BluetoothDevice.createRfcommSocketToServiceRecord(...)
        _isConnected.value = true
        return Result.success(Unit)
    }

    override suspend fun sendPacket(packet: NetworkPacket): Result<Unit> {
        _receivedPackets.emit(packet)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _isConnected.value = false
        _connectedPeers.value = emptyList()
    }
}
