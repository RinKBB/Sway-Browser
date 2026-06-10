package com.example.update.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDao {
    @Query("SELECT * FROM updates ORDER BY versionCode DESC LIMIT 1")
    fun getLatestUpdate(): Flow<UpdateEntity?>

    @Query("SELECT * FROM updates WHERE versionCode = :versionCode")
    suspend fun getUpdateByVersion(versionCode: Int): UpdateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: UpdateEntity)

    @Update
    suspend fun updateUpdate(update: UpdateEntity)

    @Delete
    suspend fun deleteUpdate(update: UpdateEntity)

    @Query("DELETE FROM updates")
    suspend fun deleteAllUpdates()

    // History
    @Query("SELECT * FROM update_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<UpdateHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: UpdateHistoryEntity)

    @Query("DELETE FROM update_history WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldHistory(beforeTimestamp: Long)
}
