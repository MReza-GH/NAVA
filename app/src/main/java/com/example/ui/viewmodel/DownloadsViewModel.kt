package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NavaDatabase
import com.example.data.media.NavaDownloadManager
import com.example.model.AudioTrack
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val downloadedTracks: List<AudioTrack> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val isLoading: Boolean = false
)

class DownloadsViewModel(
    private val database: NavaDatabase,
    private val downloadManager: NavaDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            database.downloadDao().getAllDownloads().collect { entities ->
                val tracks = entities.map { it.toAudioTrack() }
                val totalBytes = entities.sumOf { it.fileSizeBytes }
                _uiState.update {
                    it.copy(
                        downloadedTracks = tracks,
                        totalSizeBytes = totalBytes
                    )
                }
            }
        }
    }

    fun deleteDownload(trackId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownload(trackId)
        }
    }
}
