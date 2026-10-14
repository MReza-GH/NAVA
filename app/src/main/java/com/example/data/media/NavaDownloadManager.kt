package com.example.data.media

import android.content.Context
import android.os.Environment
import com.example.data.local.NavaDatabase
import com.example.data.local.entity.DownloadedTrackEntity
import com.example.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadProgress(
    val trackId: String,
    val progressPercent: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val error: String? = null
)

class NavaDownloadManager(
    private val context: Context,
    private val database: NavaDatabase
) {
    private val okHttpClient = OkHttpClient()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Boolean>()

    private val downloadsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Nava_Downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    suspend fun downloadTrack(track: AudioTrack): Boolean = withContext(Dispatchers.IO) {
        if (!track.isDownloadAllowed || track.downloadUrl.isNullOrBlank()) {
            return@withContext false
        }

        val trackId = track.id
        activeJobs[trackId] = true

        updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.DOWNLOADING))

        val cleanTitle = track.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetFile = File(downloadsDir, "${trackId}_$cleanTitle.mp3")

        try {
            val request = Request.Builder().url(track.downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.FAILED, "HTTP ${response.code}"))
                return@withContext false
            }

            val body = response.body ?: run {
                updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.FAILED, "Empty body"))
                return@withContext false
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (activeJobs[trackId] != true) {
                            // Cancelled
                            targetFile.delete()
                            updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.IDLE))
                            return@withContext false
                        }
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                            updateState(trackId, DownloadProgress(trackId, percent, DownloadStatus.DOWNLOADING))
                        }
                    }
                    output.flush()
                }
            }

            // Verify file was written
            if (targetFile.exists() && targetFile.length() > 0) {
                val entity = DownloadedTrackEntity(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    localFilePath = targetFile.absolutePath,
                    artworkUri = track.artworkUri,
                    license = track.license,
                    downloadUrl = track.downloadUrl,
                    downloadedAt = System.currentTimeMillis(),
                    fileSizeBytes = targetFile.length()
                )
                database.downloadDao().insertDownload(entity)
                updateState(trackId, DownloadProgress(trackId, 100, DownloadStatus.COMPLETED))
                true
            } else {
                updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.FAILED, "File verification failed"))
                false
            }
        } catch (e: Exception) {
            targetFile.delete()
            updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.FAILED, e.localizedMessage))
            false
        } finally {
            activeJobs.remove(trackId)
        }
    }

    fun cancelDownload(trackId: String) {
        activeJobs[trackId] = false
        updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.IDLE))
    }

    suspend fun deleteDownload(trackId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = database.downloadDao().getDownloadById(trackId)
            if (record != null) {
                val file = File(record.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                database.downloadDao().deleteDownload(trackId)
                updateState(trackId, DownloadProgress(trackId, 0, DownloadStatus.IDLE))
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun updateState(trackId: String, progress: DownloadProgress) {
        val current = _downloadStates.value.toMutableMap()
        current[trackId] = progress
        _downloadStates.value = current
    }
}
