package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.NavaDatabase
import com.example.data.local.entity.TrackEntity
import com.example.model.AudioTrack
import com.example.model.TrackSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMusicScanner(
    private val context: Context,
    private val database: NavaDatabase
) {

    suspend fun scanDeviceAudio(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()

        // Safe projection compatible with Android 10+ (Scoped Storage) and older versions
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Only request RELATIVE_PATH on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }
        // Include DATA column for legacy path reading
        projection.add(MediaStore.Audio.Media.DATA)
        // Request ALBUM_ID if available
        projection.add(MediaStore.Audio.Media.ALBUM_ID)

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 3000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    if (idCol == -1 || c.isNull(idCol)) continue
                    val id = c.getLong(idCol)
                    if (id <= 0) continue

                    val rawTitle = if (titleCol != -1 && !c.isNull(titleCol)) c.getString(titleCol) else null
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle.trim() else "Unknown Track"

                    val rawArtist = if (artistCol != -1 && !c.isNull(artistCol)) c.getString(artistCol) else null
                    val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist.trim() else "Unknown Artist"

                    val rawAlbum = if (albumCol != -1 && !c.isNull(albumCol)) c.getString(albumCol) else null
                    val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum.trim() else "Unknown Album"

                    val duration = if (durationCol != -1 && !c.isNull(durationCol)) c.getLong(durationCol) else 0L
                    val dateAddedSec = if (dateAddedCol != -1 && !c.isNull(dateAddedCol)) c.getLong(dateAddedCol) else 0L
                    val dateAdded = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()

                    val albumId = if (albumIdCol != -1 && !c.isNull(albumIdCol)) c.getLong(albumIdCol) else 0L

                    // Extract folder name purely through fast string operations (NO filesystem I/O inside cursor loop)
                    var folderName = "Music"
                    if (relativePathCol != -1 && !c.isNull(relativePathCol)) {
                        val relPath = c.getString(relativePathCol)
                        if (!relPath.isNullOrBlank()) {
                            val parts = relPath.trim('/').split('/')
                            val lastPart = parts.lastOrNull { it.isNotBlank() }
                            if (!lastPart.isNullOrBlank()) {
                                folderName = lastPart
                            }
                        }
                    } else if (dataCol != -1 && !c.isNull(dataCol)) {
                        try {
                            val filePath = c.getString(dataCol)
                            if (!filePath.isNullOrBlank()) {
                                val cleanPath = filePath.replace('\\', '/')
                                val folder = cleanPath.substringBeforeLast('/').substringAfterLast('/')
                                if (folder.isNotBlank()) {
                                    folderName = folder
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // Content URI is guaranteed to work across all Android versions
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    // Artwork URI: Use standard albumart content URI if albumId exists
                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    tracks.add(
                        AudioTrack(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            mediaUri = contentUri,
                            artworkUri = artworkUri,
                            sourceType = TrackSourceType.LOCAL,
                            folderName = folderName,
                            localFilePath = null,
                            dateAdded = dateAdded,
                            providerName = "Device"
                        )
                    )
                }
            }

            // Persist into database to cache and prevent duplication
            if (tracks.isNotEmpty()) {
                val entities = tracks.map { TrackEntity.fromAudioTrack(it) }
                // Insert in batches of 100 to avoid SQLite binder limit
                entities.chunked(100).forEach { batch ->
                    database.trackDao().insertTracks(batch)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    suspend fun importAudioUri(uri: Uri): AudioTrack? = withContext(Dispatchers.IO) {
        try {
            var title = "Audio Track"
            var artist = "Unknown Artist"
            var album = "Imported"
            var duration = 0L

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                    if (it.isNotBlank()) title = it.trim()
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) artist = it.trim()
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                    if (it.isNotBlank()) album = it.trim()
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                    if (it > 0) duration = it
                }
                retriever.release()
            } catch (metaEx: Exception) {
                // Fallback to Uri path segment if metadata extraction fails
                uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.let {
                    if (it.isNotBlank()) title = it
                }
            }

            val track = AudioTrack(
                id = "manual_${uri.toString().hashCode()}",
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                mediaUri = uri.toString(),
                artworkUri = null,
                sourceType = TrackSourceType.LOCAL,
                folderName = "Imported",
                localFilePath = null,
                dateAdded = System.currentTimeMillis(),
                providerName = "Imported"
            )

            database.trackDao().insertTrack(TrackEntity.fromAudioTrack(track))
            track
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
