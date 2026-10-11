package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val unlocked: Boolean = false,
    val stars: Int = 0,
    val highScore: Int = 0,
    val coinsCollected: Int = 0,
    val completed: Boolean = false,
    val bestTimeMs: Long = 0L
)

@Entity(tableName = "game_settings")
data class GameSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val totalCoins: Int = 50,
    val selectedSkinId: String = "cyber_blue",
    val unlockedSkins: String = "cyber_blue",
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val sfxVolume: Float = 0.8f,
    val musicVolume: Float = 0.6f,
    val sensitivity: Float = 1.0f
)
