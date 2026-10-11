package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.GameSettingsEntity
import com.example.data.LevelProgressEntity
import com.example.game.BallSkin
import com.example.game.CameraMode
import com.example.game.LevelData
import com.example.game.LevelGenerator
import com.example.game.PhysicsEngine
import com.example.game.Renderer3D
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenState {
    MAIN_MENU,
    LEVEL_SELECT,
    GAMEPLAY,
    SKINS_SHOP,
    SETTINGS
}

enum class PlayState {
    PLAYING,
    PAUSED,
    GAME_OVER,
    LEVEL_COMPLETE
}

data class GameUiState(
    val currentScreen: ScreenState = ScreenState.MAIN_MENU,
    val playState: PlayState = PlayState.PLAYING,
    val cameraMode: CameraMode = CameraMode.THIRD_PERSON,
    val selectedLevelNumber: Int = 1,
    val currentScore: Int = 0,
    val currentCoins: Int = 0,
    val currentProgress: Float = 0f,
    val earnedStars: Int = 0,
    val deathReason: String = "",
    val speedBoostActive: Boolean = false,
    val shieldActive: Boolean = false,
    val magnetActive: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val soundManager: SoundManager = SoundManager(application)
    val renderer3D = Renderer3D()

    val allLevels: StateFlow<List<LevelProgressEntity>>
    val settings: StateFlow<GameSettingsEntity>

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var physicsEngine: PhysicsEngine? = null
        private set
    var currentLevelData: LevelData? = null
        private set

    private var gameLoopJob: Job? = null
    private var levelStartTime: Long = 0L

    init {
        val dao = GameDatabase.getDatabase(application).gameDao()
        repository = GameRepository(dao)

        allLevels = repository.allLevels.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        settings = repository.settings.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GameSettingsEntity()
        )

        viewModelScope.launch {
            repository.initializeDefaults()
        }

        viewModelScope.launch {
            settings.collect { s ->
                if (s != null) {
                    soundManager.sfxEnabled = s.soundEnabled
                    soundManager.musicEnabled = s.musicEnabled
                    soundManager.sfxVolume = s.sfxVolume
                    soundManager.musicVolume = s.musicVolume
                    physicsEngine?.sensitivity = s.sensitivity
                }
            }
        }

        soundManager.startMusic()
    }

    fun navigateTo(screen: ScreenState) {
        soundManager.playButtonClick()
        if (screen != ScreenState.GAMEPLAY) {
            stopGameLoop()
        }
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun startLevel(levelNumber: Int) {
        val level = LevelGenerator.getLevel(levelNumber)
        currentLevelData = level
        val currentSensitivity = settings.value.sensitivity
        val physics = PhysicsEngine(level, soundManager, currentSensitivity)
        physicsEngine = physics
        levelStartTime = System.currentTimeMillis()

        _uiState.value = _uiState.value.copy(
            currentScreen = ScreenState.GAMEPLAY,
            playState = PlayState.PLAYING,
            selectedLevelNumber = levelNumber,
            currentScore = 0,
            currentCoins = 0,
            currentProgress = 0f,
            earnedStars = 0,
            deathReason = "",
            speedBoostActive = false,
            shieldActive = false,
            magnetActive = false
        )

        startGameLoop()
    }

    fun restartCurrentLevel() {
        startLevel(_uiState.value.selectedLevelNumber)
    }

    fun nextLevel() {
        val next = (_uiState.value.selectedLevelNumber + 1).coerceAtMost(20)
        startLevel(next)
    }

    fun pauseGame() {
        soundManager.playButtonClick()
        _uiState.value = _uiState.value.copy(playState = PlayState.PAUSED)
    }

    fun resumeGame() {
        soundManager.playButtonClick()
        _uiState.value = _uiState.value.copy(playState = PlayState.PLAYING)
    }

    fun toggleCameraMode() {
        soundManager.playButtonClick()
        val next = when (_uiState.value.cameraMode) {
            CameraMode.THIRD_PERSON -> CameraMode.FIRST_PERSON
            CameraMode.FIRST_PERSON -> CameraMode.FRONT_PERSON
            CameraMode.FRONT_PERSON -> CameraMode.THIRD_PERSON
        }
        _uiState.value = _uiState.value.copy(cameraMode = next)
    }

    fun handleDrag(deltaX: Float, deltaY: Float, screenWidth: Float) {
        if (_uiState.value.playState == PlayState.PLAYING) {
            val isFront = (_uiState.value.cameraMode == CameraMode.FRONT_PERSON)
            physicsEngine?.handleDrag(deltaX, deltaY, screenWidth, isFront)
        }
    }

    fun releaseTouch() {
        physicsEngine?.releaseTouch()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()

            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTimeNanos = now

                val physics = physicsEngine
                val level = currentLevelData

                if (_uiState.value.playState == PlayState.PLAYING && physics != null && level != null) {
                    physics.update(dt)

                    val progress = (physics.ballPos.z / level.totalLengthZ).coerceIn(0f, 1f)

                    _uiState.value = _uiState.value.copy(
                        currentScore = physics.score,
                        currentCoins = physics.coinsCollected,
                        currentProgress = progress,
                        speedBoostActive = physics.speedMultiplier > 1.1f,
                        shieldActive = physics.shieldActive,
                        magnetActive = physics.magnetActive
                    )

                    if (physics.isDead && _uiState.value.playState == PlayState.PLAYING) {
                        _uiState.value = _uiState.value.copy(
                            playState = PlayState.GAME_OVER,
                            deathReason = physics.deathReason
                        )
                    } else if (physics.isVictory && _uiState.value.playState == PlayState.PLAYING) {
                        val durationMs = System.currentTimeMillis() - levelStartTime
                        val totalLevelCoins = level.collectibles.count { it.type == com.example.game.CollectibleType.COIN }
                        val coinRatio = if (totalLevelCoins > 0) physics.coinsCollected.toFloat() / totalLevelCoins else 1f
                        val stars = when {
                            coinRatio >= 0.85f -> 3
                            coinRatio >= 0.45f -> 2
                            else -> 1
                        }

                        _uiState.value = _uiState.value.copy(
                            playState = PlayState.LEVEL_COMPLETE,
                            earnedStars = stars
                        )

                        // Save progress to database
                        viewModelScope.launch {
                            repository.completeLevel(
                                levelNumber = level.number,
                                score = physics.score,
                                coinsEarned = physics.coinsCollected,
                                stars = stars,
                                timeMs = durationMs
                            )
                        }
                    }
                }
                delay(16) // ~60fps target
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun getCurrentSkin(): BallSkin {
        val skinId = settings.value.selectedSkinId
        return LevelGenerator.SKINS.firstOrNull { it.id == skinId } ?: LevelGenerator.SKINS.first()
    }

    fun selectSkin(skinId: String) {
        viewModelScope.launch {
            repository.selectSkin(skinId)
        }
    }

    fun purchaseSkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.purchaseSkin(skinId, cost)
            if (success) {
                soundManager.playCoin()
            }
        }
    }

    fun updateSettings(
        soundEnabled: Boolean? = null,
        musicEnabled: Boolean? = null,
        sfxVolume: Float? = null,
        musicVolume: Float? = null,
        sensitivity: Float? = null
    ) {
        viewModelScope.launch {
            repository.updateSettings(soundEnabled, musicEnabled, sfxVolume, musicVolume, sensitivity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        soundManager.release()
    }
}
