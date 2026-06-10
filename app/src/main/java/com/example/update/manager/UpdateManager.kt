package com.example.update.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.update.model.UpdateInfo
import com.example.update.model.UpdateStatus
import com.example.update.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class UpdateManager(
    private val context: Context,
    private val repository: UpdateRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "UpdateManager"
        private const val UPDATES_DIR = "app_updates"
        private const val APK_FILENAME = "sway_browser_update.apk"
    }

    /**
     * Download APK file with progress tracking
     */
    suspend fun downloadUpdate(
        updateInfo: UpdateInfo,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            repository.updateStatus(updateInfo.versionCode, UpdateStatus.DOWNLOADING)

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                repository.updateStatus(updateInfo.versionCode, UpdateStatus.FAILED)
                return@withContext Result.failure(Exception("Download failed with code: ${response.code}"))
            }

            val updateDir = File(context.getExternalFilesDir(null), UPDATES_DIR)
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }

            val apkFile = File(updateDir, APK_FILENAME)
            val totalBytes = response.body?.contentLength() ?: -1L
            var downloadedBytes = 0L

            response.body?.use { body ->
                apkFile.outputStream().use { fileOut ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            fileOut.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            onProgress(downloadedBytes, totalBytes)
                            repository.updateDownloadProgress(
                                updateInfo.versionCode,
                                if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            )
                        }
                    }
                }
            }

            // Verify file hash
            val hashValid = repository.verifyFileHash(apkFile.absolutePath, updateInfo.fileHash)
            if (!hashValid) {
                apkFile.delete()
                repository.updateStatus(updateInfo.versionCode, UpdateStatus.FAILED)
                return@withContext Result.failure(Exception("File hash verification failed"))
            }

            repository.updateStatus(updateInfo.versionCode, UpdateStatus.DOWNLOADED)
            repository.saveDownloadedPath(updateInfo.versionCode, apkFile.absolutePath)

            Log.d(TAG, "APK downloaded successfully: ${apkFile.absolutePath}")
            Result.success(apkFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            repository.updateStatus(updateInfo.versionCode, UpdateStatus.FAILED)
            Result.failure(e)
        }
    }

    /**
     * Install APK using PackageInstaller
     */
    suspend fun installUpdate(apkPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return@withContext Result.failure(Exception("APK file not found: $apkPath"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Use PackageInstaller for Android 11+
                installUsingPackageInstaller(apkFile)
            } else {
                // Use intent for older versions
                installUsingIntent(apkFile)
            }

            Log.d(TAG, "Installation started")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            Result.failure(e)
        }
    }

    /**
     * Install using PackageInstaller (Android 11+)
     */
    private fun installUsingPackageInstaller(apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        params.setSize(apkFile.length())

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        apkFile.inputStream().use { input ->
            session.openWrite(APK_FILENAME, 0, apkFile.length()).use { output ->
                input.copyTo(output)
            }
        }

        val intent = Intent(context, UpdateInstallReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        session.commit(pendingIntent.intentSender)
        session.close()
    }

    /**
     * Install using Intent (Android 10 and below)
     */
    private fun installUsingIntent(apkFile: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            android.net.Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /**
     * Clean up old update files
     */
    suspend fun cleanupOldUpdates() = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.getExternalFilesDir(null), UPDATES_DIR)
            if (updateDir.exists()) {
                updateDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension == "apk") {
                        val deleted = file.delete()
                        Log.d(TAG, "Cleanup - deleted ${file.name}: $deleted")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up updates", e)
        }
    }
}
