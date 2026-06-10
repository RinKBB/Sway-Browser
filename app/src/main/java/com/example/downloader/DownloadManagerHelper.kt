package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadManagerHelper(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun checkUrlExists(url: String, userAgent: String?, referer: String?): Boolean {
        try {
            val builder = Request.Builder().url(url).head()
            if (!userAgent.isNullOrEmpty()) builder.addHeader("User-Agent", userAgent)
            if (!referer.isNullOrEmpty()) builder.addHeader("Referer", referer)
            client.newCall(builder.build()).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun resolveUrlWithFallback(url: String, userAgent: String?, referer: String?): String {
        // Pinterest Video HLS (.m3u8) to MP4 upgrade
        if (url.contains("v1.pinimg.com") || url.contains("pinimg.com/videos")) {
            if (url.contains(".m3u8") || url.contains("/hls/")) {
                val base = url.replace("/hls/", "/720p/").replace(".m3u8", ".mp4")
                if (checkUrlExists(base, userAgent, referer)) return base
                val expOption = url.replace("/hls/", "/exp/").replace(".m3u8", ".mp4")
                if (checkUrlExists(expOption, userAgent, referer)) return expOption
                val v2Option = url.replace("/hls/", "/v2/").replace(".m3u8", ".mp4")
                if (checkUrlExists(v2Option, userAgent, referer)) return v2Option
            }
        }
        // Pinterest Photo original quality fallback & high-resolution upgrade
        if (url.contains("pinimg.com")) {
            var targetUrl = url
            val resPatterns = listOf("/736x/", "/564x/", "/236x/", "/474x/", "/170x/")
            for (pattern in resPatterns) {
                if (url.contains(pattern)) {
                    targetUrl = url.replace(pattern, "/originals/")
                    break
                }
            }
            if (targetUrl.contains("/originals/")) {
                if (checkUrlExists(targetUrl, userAgent, referer)) {
                    return targetUrl
                }
                val baseWithoutExt = targetUrl.substringBeforeLast(".")
                val originalExt = targetUrl.substringAfterLast(".", "")
                val fallbackExts = listOf("png", "gif", "jpeg", "jpg").filter { it != originalExt }
                for (ext in fallbackExts) {
                    val candidate = "$baseWithoutExt.$ext"
                    if (checkUrlExists(candidate, userAgent, referer)) {
                        return candidate
                    }
                }
                // Fallback back to original URL if originals completely 404
                if (checkUrlExists(url, userAgent, referer)) {
                    return url
                }
            }
        }
        return url
    }

    /**
     * Query size of a remote asset using HTTP HEAD or GET (if HEAD is forbidden)
     */
    suspend fun queryContentSize(url: String, userAgent: String? = null, referer: String? = null): Long = withContext(Dispatchers.IO) {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            Log.e("DownloadManagerHelper", "Security Violation: Insecure or unsupported protocol for size query: $url")
            return@withContext -1L
        }
        try {
            val resolvedUrl = resolveUrlWithFallback(url, userAgent, referer)
            val builder = Request.Builder().url(resolvedUrl).head()
            if (!userAgent.isNullOrEmpty()) {
                builder.addHeader("User-Agent", userAgent)
            }
            if (!referer.isNullOrEmpty()) {
                builder.addHeader("Referer", referer)
            }
            val response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) {
                val len = response.header("Content-Length")?.toLongOrNull()
                if (len != null && len > 0) {
                    response.close()
                    return@withContext len
                }
            }
            response.close()

            // If HEAD fails or gives no length, try a brief GET range request or simple GET and fetch headers
            val getBuilder = Request.Builder().url(resolvedUrl).get()
                .addHeader("Range", "bytes=0-0")
            if (!userAgent.isNullOrEmpty()) {
                getBuilder.addHeader("User-Agent", userAgent)
            }
            if (!referer.isNullOrEmpty()) {
                getBuilder.addHeader("Referer", referer)
            }
            val getResponse = client.newCall(getBuilder.build()).execute()
            if (getResponse.isSuccessful) {
                val rangeHeader = getResponse.header("Content-Range")
                if (rangeHeader != null) {
                    val actualLength = rangeHeader.substringAfterLast("/").toLongOrNull()
                    if (actualLength != null && actualLength > 0) {
                        getResponse.close()
                        return@withContext actualLength
                    }
                }
                val rawLength = getResponse.header("Content-Length")?.toLongOrNull()
                if (rawLength != null) {
                    getResponse.close()
                    return@withContext rawLength
                }
            }
            getResponse.close()
        } catch (e: Exception) {
            Log.e("DownloadManagerHelper", "Error querying size for $url: ${e.message}")
        }
        return@withContext -1L
    }

    /**
     * Save downloaded InputStream into system directories via MediaStore or Legacy File APIs
     */
    private fun saveToPublicDownloads(
        filename: String,
        mimeType: String,
        writeBlock: (OutputStream) -> Unit
    ): Uri? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val isImage = mimeType.startsWith("image/")
            val isVideo = mimeType.startsWith("video/")
            
            val collection = when {
                isImage -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                isVideo -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            
            val relativePath = when {
                isImage -> Environment.DIRECTORY_PICTURES + "/MediaDownloader"
                isVideo -> Environment.DIRECTORY_MOVIES + "/MediaDownloader"
                else -> Environment.DIRECTORY_DOWNLOADS + "/MediaDownloader"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            
            val uri = resolver.insert(collection, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        writeBlock(outStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return uri
                } catch (e: Exception) {
                    try {
                        resolver.delete(uri, null, null)
                    } catch (ex: Exception) {
                        Log.e("DownloadManagerHelper", "Failed to clean up pending uri", ex)
                    }
                    Log.e("DownloadManagerHelper", "Failed saving stream to MediaStore Q+", e)
                    throw e
                }
            }
        } else {
            // Legacy Storage (Android 9 or below)
            val isImage = mimeType.startsWith("image/")
            val isVideo = mimeType.startsWith("video/")
            val baseDir = File(
                Environment.getExternalStoragePublicDirectory(
                    when {
                        isImage -> Environment.DIRECTORY_PICTURES
                        isVideo -> Environment.DIRECTORY_MOVIES
                        else -> Environment.DIRECTORY_DOWNLOADS
                    }
                ),
                "MediaDownloader"
            )
            if (!baseDir.exists()) baseDir.mkdirs()
            
            var file = File(baseDir, filename)
            if (file.exists()) {
                val dotIndex = filename.lastIndexOf('.')
                val nameBase = if (dotIndex > 0) filename.substring(0, dotIndex) else filename
                val nameExt = if (dotIndex > 0) filename.substring(dotIndex) else ""
                var count = 1
                while (file.exists()) {
                    file = File(baseDir, "$nameBase ($count)$nameExt")
                    count++
                }
            }
            try {
                FileOutputStream(file).use { outStream ->
                    writeBlock(outStream)
                }
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                return Uri.fromFile(file)
            } catch (e: Exception) {
                if (file.exists()) {
                    try {
                        file.delete()
                    } catch (ex: Exception) {}
                }
                Log.e("DownloadManagerHelper", "Failed legacy file writing", e)
                throw e
            }
        }
        return null
    }

    /**
     * Downloads an individual media file
     */
    suspend fun downloadFile(
        item: MediaItem,
        userAgent: String? = null,
        referer: String? = null,
        onProgress: (currentBytes: Long, totalBytes: Long) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        if (!item.url.startsWith("http://", ignoreCase = true) && !item.url.startsWith("https://", ignoreCase = true)) {
            Log.e("DownloadManagerHelper", "Security Violation: Insecure or unsupported protocol for file download: ${item.url}")
            return@withContext null
        }
        try {
            val resolvedUrl = resolveUrlWithFallback(item.url, userAgent, referer)
            val resolvedExt = resolvedUrl.substringAfterLast(".", "")
            val finalFilename = if (resolvedExt.isNotEmpty() && resolvedExt != item.ext) {
                val baseName = item.filename.substringBeforeLast(".")
                "$baseName.$resolvedExt"
            } else {
                item.filename
            }
            val finalItem = item.copy(url = resolvedUrl, filename = finalFilename, ext = resolvedExt)

            val builder = Request.Builder().url(finalItem.url).get()
            if (!userAgent.isNullOrEmpty()) {
                builder.addHeader("User-Agent", userAgent)
            }
            if (!referer.isNullOrEmpty()) {
                builder.addHeader("Referer", referer)
            }
            
            val response = client.newCall(builder.build()).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }
            
            val body = response.body
            if (body == null) {
                response.close()
                return@withContext null
            }
            
            val totalBytes = if (finalItem.sizeBytes > 0) finalItem.sizeBytes else body.contentLength()
            
            // Sanitize MIME type (remove parameters like charset, and fallback to ext-based MIME type if generic)
            var mimeType = response.header("Content-Type")?.substringBefore(";")?.trim() ?: ""
            if (mimeType.contains("html", ignoreCase = true) || mimeType.contains("xml", ignoreCase = true)) {
                if (finalItem.type == "image" || finalItem.type == "video") {
                    Log.e("DownloadManagerHelper", "Aborting download: Expected media, but server returned HTML ($mimeType)")
                    response.close()
                    return@withContext null
                }
            }
            
            if (mimeType.isBlank() || mimeType == "application/octet-stream" || mimeType == "binary/octet-stream") {
                mimeType = getMimeTypeFromExt(finalItem.ext)
            }
            
            var savedUri: Uri? = null
            
            body.byteStream().use { inputStream ->
                savedUri = saveToPublicDownloads(finalItem.filename, mimeType) { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead, totalBytes)
                    }
                    outputStream.flush()
                    if (totalRead == 0L) {
                        throw java.io.IOException("Downloaded 0 bytes - corrupt file")
                    }
                }
            }
            response.close()
            return@withContext savedUri
        } catch (e: Exception) {
            Log.e("DownloadManagerHelper", "Error downloading single product: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Downloads multiple media files and compresses them into a single ZIP archive.
     */
    suspend fun downloadZip(
        items: List<MediaItem>,
        zipFilename: String,
        userAgent: String? = null,
        referer: String? = null,
        onItemProgress: (currentIndex: Int, totalItems: Int, progressFraction: Float, fileName: String) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val totalCount = items.size
        
        try {
            val savedUri = saveToPublicDownloads(zipFilename, "application/zip") { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    items.forEachIndexed { index, mediaItem ->
                        try {
                            if (!mediaItem.url.startsWith("http://", ignoreCase = true) && !mediaItem.url.startsWith("https://", ignoreCase = true)) {
                                Log.e("DownloadManagerHelper", "Security Violation: Insecure or unsupported protocol for ZIP item: ${mediaItem.url}")
                                return@forEachIndexed
                            }
                            
                            val resolvedUrl = resolveUrlWithFallback(mediaItem.url, userAgent, referer)
                            val resolvedExt = resolvedUrl.substringAfterLast(".", "")
                            val finalFilename = if (resolvedExt.isNotEmpty() && resolvedExt != mediaItem.ext) {
                                val baseName = mediaItem.filename.substringBeforeLast(".")
                                "$baseName.$resolvedExt"
                            } else {
                                mediaItem.filename
                            }
                            val finalItem = mediaItem.copy(url = resolvedUrl, filename = finalFilename, ext = resolvedExt)
                            
                            onItemProgress(index, totalCount, 0.0f, finalItem.filename)
                            
                            val builder = Request.Builder().url(finalItem.url).get()
                            if (!userAgent.isNullOrEmpty()) {
                                builder.addHeader("User-Agent", userAgent)
                            }
                            if (!referer.isNullOrEmpty()) {
                                builder.addHeader("Referer", referer)
                            }
                            
                            val response = client.newCall(builder.build()).execute()
                            if (response.isSuccessful && response.body != null) {
                                val body = response.body!!
                                val cleanedZipName = "${index}_${finalItem.filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
                                val entry = ZipEntry(cleanedZipName)
                                zipOut.putNextEntry(entry)
                                
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(8192)
                                    var readBytes: Int
                                    while (input.read(buffer).also { readBytes = it } != -1) {
                                        zipOut.write(buffer, 0, readBytes)
                                    }
                                }
                                
                                response.close()
                                onItemProgress(index, totalCount, 1.0f, finalItem.filename)
                                zipOut.closeEntry()
                            } else {
                                response.close()
                            }
                        } catch (e: Exception) {
                            Log.e("DownloadManagerHelper", "Skip item in Zip: ${mediaItem.filename} due to ${e.message}")
                        }
                    }
                }
            }
            return@withContext savedUri
        } catch (e: Exception) {
            Log.e("DownloadManagerHelper", "Zip compression failed: ${e.message}")
            return@withContext null
        }
    }

    private fun getMimeTypeFromExt(ext: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "*/*"
    }
}
