package com.example.model

enum class TrackSourceType {
    LOCAL,
    ONLINE_JAMENDO,
    ONLINE_APPLE_PREVIEW,
    EXTERNAL_CATALOG,
    DOWNLOADED
}

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val mediaUri: String,
    val artworkUri: String? = null,
    val sourceType: TrackSourceType = TrackSourceType.LOCAL,
    val license: String? = null,
    val downloadUrl: String? = null,
    val isDownloadAllowed: Boolean = false,
    val folderName: String? = null,
    val isFavorite: Boolean = false,
    val localFilePath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val externalUrl: String? = null,
    val providerName: String = "Local",
    val isPreviewOnly: Boolean = false
)
