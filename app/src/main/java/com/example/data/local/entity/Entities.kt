package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.AudioTrack
import com.example.model.TrackSourceType

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUri: String?,
    val sourceType: String, // "LOCAL", "ONLINE_JAMENDO", "DOWNLOADED"
    val folderName: String?,
    val localFilePath: String?,
    val dateAdded: Long
) {
    fun toAudioTrack(isFavorite: Boolean = false): AudioTrack {
        return AudioTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mediaUri = mediaUri,
            artworkUri = artworkUri,
            sourceType = try {
                TrackSourceType.valueOf(sourceType)
            } catch (e: Exception) {
                TrackSourceType.LOCAL
            },
            folderName = folderName,
            localFilePath = localFilePath,
            dateAdded = dateAdded,
            isFavorite = isFavorite
        )
    }

    companion object {
        fun fromAudioTrack(track: AudioTrack): TrackEntity {
            return TrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                mediaUri = track.mediaUri,
                artworkUri = track.artworkUri,
                sourceType = track.sourceType.name,
                folderName = track.folderName,
                localFilePath = track.localFilePath,
                dateAdded = track.dateAdded
            )
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["trackId"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUri: String?,
    val sourceType: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toAudioTrack(): AudioTrack {
        return AudioTrack(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mediaUri = mediaUri,
            artworkUri = artworkUri,
            sourceType = try {
                TrackSourceType.valueOf(sourceType)
            } catch (e: Exception) {
                TrackSourceType.LOCAL
            },
            isFavorite = true,
            dateAdded = addedAt
        )
    }

    companion object {
        fun fromAudioTrack(track: AudioTrack): FavoriteEntity {
            return FavoriteEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                mediaUri = track.mediaUri,
                artworkUri = track.artworkUri,
                sourceType = track.sourceType.name,
                addedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["playedAt"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUri: String?,
    val sourceType: String,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toAudioTrack(): AudioTrack {
        return AudioTrack(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mediaUri = mediaUri,
            artworkUri = artworkUri,
            sourceType = try {
                TrackSourceType.valueOf(sourceType)
            } catch (e: Exception) {
                TrackSourceType.LOCAL
            },
            dateAdded = playedAt
        )
    }

    companion object {
        fun fromAudioTrack(track: AudioTrack): PlaybackHistoryEntity {
            return PlaybackHistoryEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                mediaUri = track.mediaUri,
                artworkUri = track.artworkUri,
                sourceType = track.sourceType.name,
                playedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val localFilePath: String,
    val artworkUri: String?,
    val license: String?,
    val downloadUrl: String?,
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
) {
    fun toAudioTrack(isFavorite: Boolean = false): AudioTrack {
        return AudioTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mediaUri = localFilePath,
            artworkUri = artworkUri,
            sourceType = TrackSourceType.DOWNLOADED,
            license = license,
            downloadUrl = downloadUrl,
            isDownloadAllowed = true,
            localFilePath = localFilePath,
            isFavorite = isFavorite,
            dateAdded = downloadedAt
        )
    }
}
