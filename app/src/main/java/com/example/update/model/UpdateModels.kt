package com.example.update.model

import java.util.Date

// API Response from update server
data class UpdateCheckResponse(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: Long,
    val changeLog: String,
    val downloadUrl: String,
    val fileHash: String, // SHA-256 для проверки целостности
    val fileSize: Long,
    val isForced: Boolean = false,
    val minSdkVersion: Int? = null
)

// Local update info
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: Date,
    val changeLog: String,
    val downloadUrl: String,
    val fileHash: String,
    val fileSize: Long,
    val isForced: Boolean,
    val status: UpdateStatus = UpdateStatus.AVAILABLE,
    val downloadProgress: Float = 0f
)

enum class UpdateStatus {
    AVAILABLE,        // Обновление доступно
    DOWNLOADING,      // Загружается
    DOWNLOADED,       // Загружено и готово к установке
    INSTALLING,       // Устанавливается
    INSTALLED,        // Установлено
    FAILED,           // Ошибка
    DISMISSED         // Отклонено пользователем
}

// Update check history for analytics
data class UpdateCheckLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val currentVersion: Int,
    val latestVersion: Int,
    val updateAvailable: Boolean,
    val downloadUrl: String? = null,
    val result: String // SUCCESS, FAILED, NO_UPDATE
)
