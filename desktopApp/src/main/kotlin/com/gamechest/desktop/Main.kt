package com.gamechest.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gamechest.core.engine.GameEngine
import com.gamechest.core.loader.GamePackManager
import com.gamechest.core.network.TransportMode
import com.gamechest.ui.ScreenState
import com.gamechest.ui.components.LocalAssetProvider
import com.gamechest.ui.editor.GamePackBrowserScreen
import com.gamechest.ui.game.GameBoardScreen
import com.gamechest.ui.lobby.LobbyScreen
import com.gamechest.ui.theme.DarkBackground
import com.gamechest.ui.theme.GameChestTheme

fun main() = application {
    val packManager = remember { GamePackManager() }
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "The Game Chest - Digital Board Games",
        state = windowState
    ) {
        CompositionLocalProvider(LocalAssetProvider provides DesktopAssetProvider) {
            GameChestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    var currentScreen by remember { mutableStateOf(ScreenState.LOBBY) }
                    var selectedPack by remember {
                        mutableStateOf(packManager.getAllPacks().first())
                    }
                    var activeEngine by remember { mutableStateOf<GameEngine?>(null) }
                    var activeTransportMode by remember { mutableStateOf(TransportMode.SAME_DEVICE_LOCAL) }

                    when (currentScreen) {
                        ScreenState.LOBBY -> {
                            LobbyScreen(
                                gamePack = selectedPack,
                                onStartGame = { players, mutators, transportMode ->
                                    val engine = GameEngine(
                                        initialPack = selectedPack,
                                        initialProfiles = players,
                                        activeMutators = mutators
                                    )
                                    activeEngine = engine
                                    activeTransportMode = transportMode
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
