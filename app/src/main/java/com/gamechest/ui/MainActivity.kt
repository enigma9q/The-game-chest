package com.gamechest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gamechest.core.engine.GameEngine
import com.gamechest.core.loader.GamePackManager
import com.gamechest.core.model.GamePack
import com.gamechest.core.model.PlayerProfile
import com.gamechest.core.network.TransportMode
import com.gamechest.core.network.WifiLanTransport
import com.gamechest.ui.components.AndroidAssetProvider
import com.gamechest.ui.components.LocalAssetProvider
import com.gamechest.ui.editor.GamePackBrowserScreen
import com.gamechest.ui.game.GameBoardScreen
import com.gamechest.ui.lobby.LobbyScreen
import com.gamechest.ui.theme.DarkBackground
import com.gamechest.ui.theme.GameChestTheme

class MainActivity : ComponentActivity() {

    private val packManager = GamePackManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalAssetProvider provides AndroidAssetProvider) {
                GameChestTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DarkBackground
                    ) {
                            val wifiTransport = remember { WifiLanTransport() }
                        var currentScreen by remember { mutableStateOf(ScreenState.LOBBY) }
                        var selectedPack by remember {
                            mutableStateOf(packManager.getAllPacks().first())
                        }
                        var activeEngine by remember { mutableStateOf<GameEngine?>(null) }
                        var activeTransportMode by remember { mutableStateOf(TransportMode.SAME_DEVICE_LOCAL) }
                        var localPlayerId by remember { mutableStateOf<String?>(null) }
                        var isHost by remember { mutableStateOf(true) }

                        when (currentScreen) {
                            ScreenState.LOBBY -> {
                                LobbyScreen(
                                    gamePack = selectedPack,
                                    wifiTransport = wifiTransport,
                                    onStartGame = { players, mutators, transportMode, myId, hostFlag ->
                                        val engine = GameEngine(
                                            initialPack = selectedPack,
                                            initialProfiles = players,
                                            activeMutators = mutators
                                        )
                                        activeEngine = engine
                                        activeTransportMode = transportMode
                                        localPlayerId = myId
                                        isHost = hostFlag
                                        currentScreen = ScreenState.GAME_BOARD
                                    },
                                    onBrowsePacks = {
                                        currentScreen = ScreenState.PACK_BROWSER
                                    }
                                )
                            }
                            ScreenState.GAME_BOARD -> {
                                activeEngine?.let { engine ->
                                    GameBoardScreen(
                                        engine = engine,
                                        wifiTransport = wifiTransport,
                                        localPlayerId = localPlayerId,
                                        isHost = isHost,
                                        isWifiCoop = activeTransportMode == TransportMode.WIFI_LAN,
                                        onExitGame = {
                                            activeEngine = null
                                            currentScreen = ScreenState.LOBBY
                                        }
                                    )
                                }
                            }
                            ScreenState.PACK_BROWSER -> {
                                GamePackBrowserScreen(
                                    packManager = packManager,
                                    onSelectPack = { pack ->
                                        selectedPack = pack
                                        currentScreen = ScreenState.LOBBY
                                    },
                                    onBack = {
                                        currentScreen = ScreenState.LOBBY
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
