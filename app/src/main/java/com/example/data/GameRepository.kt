package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.max

class GameRepository(private val gameDao: GameDao) {

    val allLevels: Flow<List<LevelProgressEntity>> = gameDao.getAllLevelProgress()
    val settings: Flow<GameSettingsEntity> = gameDao.getSettings().filterNotNull()

    suspend fun initializeDefaults() {
        val currentSettings = gameDao.getSettingsSync()
        if (currentSettings == null) {
            gameDao.insertOrUpdateSettings(GameSettingsEntity())
        }

        val defaultLevels = (1..20).map { lvl ->
            LevelProgressEntity(
                levelNumber = lvl,
                unlocked = (lvl == 1),
                stars = 0,
                highScore = 0,
                coinsCollected = 0,
                completed = false,
                bestTimeMs = 0L
            )
        }
        gameDao.insertAllLevels(defaultLevels)
    }

    suspend fun completeLevel(
        levelNumber: Int,
        score: Int,
        coinsEarned: Int,
        stars: Int,
        timeMs: Long
    ) {
        val existing = gameDao.getLevelProgressSync(levelNumber) ?: LevelProgressEntity(
            levelNumber = levelNumber,
            unlocked = true
        )

        val updatedCurrent = existing.copy(
            completed = true,
            stars = max(existing.stars, stars),
            highScore = max(existing.highScore, score),
            coinsCollected = max(existing.coinsCollected, coinsEarned),
            bestTimeMs = if (existing.bestTimeMs == 0L) timeMs else minOf(existing.bestTimeMs, timeMs)
        )
        gameDao.insertLevelProgress(updatedCurrent)

        // Unlock next level if available
        if (levelNumber < 20) {
            val nextLevel = gameDao.getLevelProgressSync(levelNumber + 1) ?: LevelProgressEntity(
                levelNumber = levelNumber + 1,
                unlocked = true
            )
            gameDao.insertLevelProgress(nextLevel.copy(unlocked = true))
        }

        // Add coins to wallet
        val currentSettings = gameDao.getSettingsSync() ?: GameSettingsEntity()
        gameDao.insertOrUpdateSettings(
            currentSettings.copy(
                totalCoins = currentSettings.totalCoins + coinsEarned
            )
        )
    }

    suspend fun selectSkin(skinId: String) {
        val currentSettings = gameDao.getSettingsSync() ?: GameSettingsEntity()
        val unlockedList = currentSettings.unlockedSkins.split(",").map { it.trim() }
        if (skinId in unlockedList) {
            gameDao.insertOrUpdateSettings(currentSettings.copy(selectedSkinId = skinId))
        }
    }

    suspend fun purchaseSkin(skinId: String, cost: Int): Boolean {
        val currentSettings = gameDao.getSettingsSync() ?: GameSettingsEntity()
        val unlockedList = currentSettings.unlockedSkins.split(",").map { it.trim() }.toMutableList()
        if (skinId in unlockedList) {
            selectSkin(skinId)
            return true
        }
        if (currentSettings.totalCoins >= cost) {
            unlockedList.add(skinId)
            gameDao.insertOrUpdateSettings(
                currentSettings.copy(
                    totalCoins = currentSettings.totalCoins - cost,
                    unlockedSkins = unlockedList.joinToString(","),
                    selectedSkinId = skinId
                )
            )
            return true
        }
        return false
    }

    suspend fun updateSettings(
        soundEnabled: Boolean? = null,
        musicEnabled: Boolean? = null,
        sfxVolume: Float? = null,
        musicVolume: Float? = null,
        sensitivity: Float? = null
    ) {
        val current = gameDao.getSettingsSync() ?: GameSettingsEntity()
        val updated = current.copy(
            soundEnabled = soundEnabled ?: current.soundEnabled,
            musicEnabled = musicEnabled ?: current.musicEnabled,
            sfxVolume = sfxVolume ?: current.sfxVolume,
            musicVolume = musicVolume ?: current.musicVolume,
            sensitivity = sensitivity ?: current.sensitivity
        )
        gameDao.insertOrUpdateSettings(updated)
    }
}
