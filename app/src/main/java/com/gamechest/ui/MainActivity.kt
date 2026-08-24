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
import com.gamechest.core.model.MutatorId
import com.gamechest.core.model.PlayerProfile
import com.gamechest.core.network.TransportMode
import com.gamechest.ui.editor.GamePackBrowserScreen
import com.gamechest.ui.game.GameBoardScreen
import com.gamechest.ui.lobby.LobbyScreen
import com.gamechest.ui.theme.DarkBackground
import com.gamechest.ui.theme.GameChestTheme

enum class ScreenState {
    LOBBY,
    GAME_BOARD,
    PACK_BROWSER
}

class MainActivity : ComponentActivity() {

    private val packManager = GamePackManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
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
