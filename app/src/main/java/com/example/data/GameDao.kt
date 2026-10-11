package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun getAllLevelProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber")
    fun getLevelProgress(levelNumber: Int): Flow<LevelProgressEntity?>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber")
    suspend fun getLevelProgressSync(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevelProgress(level: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllLevels(levels: List<LevelProgressEntity>)

    @Query("SELECT * FROM game_settings WHERE id = 1")
    fun getSettings(): Flow<GameSettingsEntity?>

    @Query("SELECT * FROM game_settings WHERE id = 1")
    suspend fun getSettingsSync(): GameSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: GameSettingsEntity)
}
