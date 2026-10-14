package com.example.data.remote

import com.example.model.AudioTrack
import com.example.model.TrackSourceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JamendoApiResponse(
    @param:Json(name = "headers") val headers: JamendoHeaders?,
    @param:Json(name = "results") val results: List<JamendoTrackDto>?
)

@JsonClass(generateAdapter = true)
data class JamendoHeaders(
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "code") val code: Int?,
    @param:Json(name = "error_message") val errorMessage: String?,
    @param:Json(name = "results_count") val resultsCount: Int?
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "duration") val duration: Int?, // duration in seconds
    @param:Json(name = "artist_id") val artistId: String?,
    @param:Json(name = "artist_name") val artistName: String?,
    @param:Json(name = "album_name") val albumName: String?,
    @param:Json(name = "album_id") val albumId: String?,
    @param:Json(name = "license_ccurl") val licenseCcUrl: String?,
    @param:Json(name = "album_image") val albumImage: String?,
    @param:Json(name = "image") val image: String?,
    @param:Json(name = "audio") val audio: String?,
    @param:Json(name = "audiodownload") val audioDownload: String?,
    @param:Json(name = "audiodownload_allowed") val audioDownloadAllowed: Boolean?
) {
    fun toAudioTrack(): AudioTrack {
        val durationSeconds = duration ?: 0
        val trackTitle = if (!name.isNullOrBlank()) name else "Untitled Track"
        val artist = if (!artistName.isNullOrBlank()) artistName else "Unknown Artist"
        val album = if (!albumName.isNullOrBlank()) albumName else "Single"
        val artwork = image ?: albumImage

        return AudioTrack(
            id = "jamendo_$id",
            title = trackTitle,
            artist = artist,
            album = album,
            durationMs = durationSeconds * 1000L,
            mediaUri = audio ?: "",
            artworkUri = artwork,
            sourceType = TrackSourceType.ONLINE_JAMENDO,
            license = licenseCcUrl ?: "Creative Commons",
            downloadUrl = audioDownload,
            isDownloadAllowed = audioDownloadAllowed == true && !audioDownload.isNullOrBlank()
        )
    }
}
