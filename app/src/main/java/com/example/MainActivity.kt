package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GameViewModel
import com.example.ui.PlayState
import com.example.ui.ScreenState
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SkinsShopScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var viewModelRef: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()
                viewModelRef = gameViewModel

                val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
                val levels by gameViewModel.allLevels.collectAsStateWithLifecycle()
                val settings by gameViewModel.settings.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        when (uiState.currentScreen) {
                            ScreenState.MAIN_MENU -> {
                                MainMenuScreen(
                                    levels = levels,
                                    settings = settings,
                                    currentSkin = gameViewModel.getCurrentSkin(),
                                    onNavigate = { screen -> gameViewModel.navigateTo(screen) },
                                    onPlayNext = {
                                        val nextPlayable = levels.firstOrNull { it.unlocked && !it.completed }?.levelNumber
                                            ?: (levels.lastOrNull { it.unlocked }?.levelNumber ?: 1)
                                        gameViewModel.startLevel(nextPlayable)
                                    }
                                )
                            }
                            ScreenState.LEVEL_SELECT -> {
                                LevelSelectScreen(
                                    levels = levels,
                                    onSelectLevel = { lvl -> gameViewModel.startLevel(lvl) },
                                    onBack = { gameViewModel.navigateTo(ScreenState.MAIN_MENU) }
                                )
                            }
                            ScreenState.GAMEPLAY -> {
                                GameScreen(
                                    uiState = uiState,
                                    levelData = gameViewModel.currentLevelData,
                                    physics = gameViewModel.physicsEngine,
                                    renderer = gameViewModel.renderer3D,
                                    currentSkin = gameViewModel.getCurrentSkin(),
                                    onDrag = { dx, dy, width -> gameViewModel.handleDrag(dx, dy, width) },
                                    onReleaseTouch = { gameViewModel.releaseTouch() },
                                    onToggleCamera = { gameViewModel.toggleCameraMode() },
                                    onPause = { gameViewModel.pauseGame() },
                                    onResume = { gameViewModel.resumeGame() },
                                    onRestart = { gameViewModel.restartCurrentLevel() },
                                    onNextLevel = { gameViewModel.nextLevel() },
                                    onExit = { gameViewModel.navigateTo(ScreenState.LEVEL_SELECT) }
                                )
                            }
                            ScreenState.SKINS_SHOP -> {
                                SkinsShopScreen(
                                    settings = settings,
                                    onSelectSkin = { skinId -> gameViewModel.selectSkin(skinId) },
                                    onPurchaseSkin = { skinId, cost -> gameViewModel.purchaseSkin(skinId, cost) },
                                    onBack = { gameViewModel.navigateTo(ScreenState.MAIN_MENU) }
                                )
                            }
                            ScreenState.SETTINGS -> {
                                SettingsScreen(
                                    settings = settings,
                                    onUpdateSettings = { sound, music, sfxVol, musicVol, sens ->
                                        gameViewModel.updateSettings(sound, music, sfxVol, musicVol, sens)
                                    },
                                    onBack = { gameViewModel.navigateTo(ScreenState.MAIN_MENU) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModelRef?.soundManager?.stopMusic()
        if (viewModelRef?.uiState?.value?.playState == PlayState.PLAYING) {
            viewModelRef?.pauseGame()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModelRef?.settings?.value?.musicEnabled == true) {
            viewModelRef?.soundManager?.startMusic()
        }
    }
}
