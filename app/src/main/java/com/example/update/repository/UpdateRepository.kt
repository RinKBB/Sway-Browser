package com.example.update.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.update.data.UpdateDao
import com.example.update.data.UpdateEntity
import com.example.update.model.UpdateCheckResponse
import com.example.update.model.UpdateInfo
import com.example.update.model.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.MessageDigest

interface UpdateApiService {
    @GET("check-update")
    suspend fun checkUpdate(
        @Query("version_code") versionCode: Int,
        @Query("package_name") packageName: String
    ): UpdateCheckResponse
}

class UpdateRepository(
    private val context: Context,
    private val dao: UpdateDao,
    private val serverUrl: String = "https://your-update-server.com/api/"
) {
    private val apiService: UpdateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(serverUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApiService::class.java)
    }

    companion object {
        private const val TAG = "UpdateRepository"
        private const val PREFERENCES_NAME = "update_preferences"
        private const val LAST_CHECK_TIME_KEY = "last_check_time"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Get current app version code
     */
    fun getCurrentVersionCode(): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not get current version", e)
            0
        }
    }

    /**
     * Get current app version name
     */
    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not get current version name", e)
            "1.0"
        }
    }

    /**
     * Check for updates from server
     */
    suspend fun checkForUpdates(forceCheck: Boolean = false): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if we should skip this check based on interval
            if (!forceCheck && !shouldCheckForUpdates()) {
                Log.d(TAG, "Skipping update check - interval not met")
                return@withContext Result.success(null)
            }

            val currentVersion = getCurrentVersionCode()
            val response = apiService.checkUpdate(currentVersion, context.packageName)

            // Save last check time
            saveLastCheckTime()

            // Validate response
            if (response.versionCode <= currentVersion) {
                Log.d(TAG, "Already on latest version")
                return@withContext Result.success(null)
            }

            val updateEntity = UpdateEntity(
                versionCode = response.versionCode,
                versionName = response.versionName,
                releaseDate = response.releaseDate,
                changeLog = response.changeLog,
                downloadUrl = response.downloadUrl,
                fileHash = response.fileHash,
                fileSize = response.fileSize,
                isForced = response.isForced,
                status = UpdateStatus.AVAILABLE.name
            )

            dao.insertUpdate(updateEntity)

            val updateInfo = UpdateInfo(
                versionCode = response.versionCode,
                versionName = response.versionName,
                releaseDate = java.util.Date(response.releaseDate),
                changeLog = response.changeLog,
                downloadUrl = response.downloadUrl,
                fileHash = response.fileHash,
                fileSize = response.fileSize,
                isForced = response.isForced,
                status = UpdateStatus.AVAILABLE
            )

            Log.d(TAG, "Update found: ${response.versionName}")
            Result.success(updateInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            Result.failure(e)
        }
    }

    /**
     * Verify file integrity using SHA-256
     */
    suspend fun verifyFileHash(filePath: String, expectedHash: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val fileBytes = java.io.File(filePath).readBytes()
            val hashBytes = messageDigest.digest(fileBytes)
            val calculatedHash = hashBytes.joinToString("") { "%02x".format(it) }
            
            val isValid = calculatedHash.equals(expectedHash, ignoreCase = true)
            Log.d(TAG, "File hash verification: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying file hash", e)
            false
        }
    }

    /**
     * Get the latest available update
     */
    fun getLatestUpdate() = dao.getLatestUpdate()

    /**
     * Update the update status
     */
    suspend fun updateStatus(versionCode: Int, status: UpdateStatus) {
        withContext(Dispatchers.IO) {
            val update = dao.getUpdateByVersion(versionCode)
            if (update != null) {
                dao.updateUpdate(update.copy(status = status.name))
            }
        }
    }

    /**
     * Update download progress
     */
    suspend fun updateDownloadProgress(versionCode: Int, progress: Float) {
        withContext(Dispatchers.IO) {
            val update = dao.getUpdateByVersion(versionCode)
            if (update != null) {
                dao.updateUpdate(update.copy(downloadProgress = progress))
            }
        }
    }

    /**
     * Save downloaded file path
     */
    suspend fun saveDownloadedPath(versionCode: Int, path: String) {
        withContext(Dispatchers.IO) {
            val update = dao.getUpdateByVersion(versionCode)
            if (update != null) {
                dao.updateUpdate(update.copy(downloadedPath = path))
            }
        }
    }

    private fun shouldCheckForUpdates(): Boolean {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong(LAST_CHECK_TIME_KEY, 0L)
        return System.currentTimeMillis() - lastCheckTime >= CHECK_INTERVAL_MS
    }

    private fun saveLastCheckTime() {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_CHECK_TIME_KEY, System.currentTimeMillis()).apply()
    }
}
