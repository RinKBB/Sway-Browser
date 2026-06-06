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

    /**
     * Query size of a remote asset using HTTP HEAD or GET (if HEAD is forbidden)
     */
    suspend fun queryContentSize(url: String, userAgent: String? = null, referer: String? = null): Long = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder().url(url).head()
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
            val getBuilder = Request.Builder().url(url).get()
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
        try {
            val builder = Request.Builder().url(item.url).get()
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
            
            val totalBytes = if (item.sizeBytes > 0) item.sizeBytes else body.contentLength()
            
            // Sanitize MIME type (remove parameters like charset, and fallback to ext-based MIME type if generic)
            var mimeType = response.header("Content-Type")?.substringBefore(";")?.trim() ?: ""
            if (mimeType.contains("html", ignoreCase = true) || mimeType.contains("xml", ignoreCase = true)) {
                if (item.type == "image" || item.type == "video") {
                    Log.e("DownloadManagerHelper", "Aborting download: Expected media, but server returned HTML ($mimeType)")
                    response.close()
                    return@withContext null
                }
            }
            
            if (mimeType.isBlank() || mimeType == "application/octet-stream" || mimeType == "binary/octet-stream") {
                mimeType = getMimeTypeFromExt(item.ext)
            }
            
            var savedUri: Uri? = null
            
            body.byteStream().use { inputStream ->
                savedUri = saveToPublicDownloads(item.filename, mimeType) { outputStream ->
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
                            onItemProgress(index, totalCount, 0.0f, mediaItem.filename)
                            
                            val builder = Request.Builder().url(mediaItem.url).get()
                            if (!userAgent.isNullOrEmpty()) {
                                builder.addHeader("User-Agent", userAgent)
                            }
                            if (!referer.isNullOrEmpty()) {
                                builder.addHeader("Referer", referer)
                            }
                            
                            val response = client.newCall(builder.build()).execute()
                            if (response.isSuccessful && response.body != null) {
                                val body = response.body!!
                                val rawBytes = body.bytes()
                                response.close()
                                
                                val cleanedZipName = "${index}_${mediaItem.filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
                                val entry = ZipEntry(cleanedZipName)
                                zipOut.putNextEntry(entry)
                                
                                zipOut.write(rawBytes)
                                onItemProgress(index, totalCount, 1.0f, mediaItem.filename)
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
