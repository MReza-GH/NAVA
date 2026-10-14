package com.example.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0
)

data class ArtistSummary(
    val name: String,
    val trackCount: Int,
    val sampleArtworkUri: String? = null
)

data class AlbumSummary(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artworkUri: String? = null
)

data class FolderSummary(
    val name: String,
    val trackCount: Int
)
