package com.example.model

/**
 * Data class representing an image search result item.
 *
 * @property imageUrl The direct link to the image file.
 * @property title The title or description of the image.
 * @property siteUrl The URL of the source website.
 * @property siteName The friendly name of the source website.
 */
data class SearchImage(
    val imageUrl: String,
    val title: String,
    val siteUrl: String,
    val siteName: String
)
