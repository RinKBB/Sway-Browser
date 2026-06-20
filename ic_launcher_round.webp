package com.example.model

data class MediaItem(
    val id: String,
    val url: String,
    val type: String, // "image", "video", "audio", "other"
    val ext: String,
    val filename: String,
    val tagName: String,
    val width: Int = -1,
    val height: Int = -1,
    val sizeBytes: Long = -1L // Loaded asynchronously via HTTP HEAD
) {
    val sizeFormatted: String
        get() {
            if (sizeBytes < 0) return "Unknown size"
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format("%.1f MB", mb)
        }
        
    val isImage: Boolean get() = type == "image"
    val isVideo: Boolean get() = type == "video"
}
