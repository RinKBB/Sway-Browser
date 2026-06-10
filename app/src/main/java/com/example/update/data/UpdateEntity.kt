package com.example.update.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "updates")
data class UpdateEntity(
    @PrimaryKey
    val versionCode: Int,
    val versionName: String,
    val releaseDate: Long,
    val changeLog: String,
    val downloadUrl: String,
    val fileHash: String,
    val fileSize: Long,
    val isForced: Boolean = false,
    val status: String = "AVAILABLE",
    val downloadedPath: String? = null,
    val downloadProgress: Float = 0f,
    val lastCheckTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "update_history")
data class UpdateHistoryEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val versionCode: Int,
    val updateResult: String // INSTALLED, FAILED, SKIPPED
)
