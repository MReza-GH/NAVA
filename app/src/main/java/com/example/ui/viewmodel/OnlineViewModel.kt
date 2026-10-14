package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiMusicAssistant
import com.example.data.media.DownloadProgress
import com.example.data.media.NavaDownloadManager
import com.example.data.remote.ExternalServiceLink
import com.example.data.remote.MultiSourceMusicRepository
import com.example.model.AudioTrack
import com.example.model.TrackSourceType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class OnlineCatalogFilter {
    ALL,
    JAMENDO,
    APPLE_PREVIEWS,
    EXTERNAL_SERVICES
}

data class OnlineUiState(
    val searchQuery: String = "",
    val allTracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val externalLinks: List<ExternalServiceLink> = emptyList(),
    val activeFilter: OnlineCatalogFilter = OnlineCatalogFilter.ALL,
    val suggestedKeywords: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSearching: Boolean = false,
    val downloadStates: Map<String, DownloadProgress> = emptyMap(),
    val isAiExpanding: Boolean = false
)

class OnlineViewModel(
    private val multiSourceRepository: MultiSourceMusicRepository,
    private val downloadManager: NavaDownloadManager,
    private val geminiAssistant: GeminiMusicAssistant
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineUiState())
    val uiState: StateFlow<OnlineUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPopularTracks()
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadManager.downloadStates.collect { states ->
                _uiState.update { it.copy(downloadStates = states) }
            }
        }
    }

    fun loadPopularTracks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val tracks = multiSourceRepository.getPopularTracks()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allTracks = tracks,
                        filteredTracks = applyFilter(tracks, it.activeFilter),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    fun setFilter(filter: OnlineCatalogFilter) {
        _uiState.update {
            it.copy(
                activeFilter = filter,
                filteredTracks = applyFilter(it.allTracks, filter)
            )
        }
    }

    private fun applyFilter(tracks: List<AudioTrack>, filter: OnlineCatalogFilter): List<AudioTrack> {
        return when (filter) {
            OnlineCatalogFilter.ALL -> tracks
            OnlineCatalogFilter.JAMENDO -> tracks.filter { it.sourceType == TrackSourceType.ONLINE_JAMENDO }
            OnlineCatalogFilter.APPLE_PREVIEWS -> tracks.filter { it.sourceType == TrackSourceType.ONLINE_APPLE_PREVIEW }
            OnlineCatalogFilter.EXTERNAL_SERVICES -> emptyList()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    suggestedKeywords = emptyList(),
                    externalLinks = emptyList()
                )
            }
            loadPopularTracks()
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce typing
            _uiState.update { it.copy(isLoading = true, isSearching = true, errorMessage = null) }
            try {
                val result = multiSourceRepository.searchAllSources(newQuery)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allTracks = result.tracks,
                        filteredTracks = applyFilter(result.tracks, it.activeFilter),
                        externalLinks = result.externalLinks,
                        errorMessage = if (result.tracks.isEmpty() && result.errors.isNotEmpty()) {
                            result.errors.joinToString("\n")
                        } else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    fun expandWithGemini() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiExpanding = true) }
            val expansions = geminiAssistant.expandQuery(query)
            _uiState.update {
                it.copy(
                    isAiExpanding = false,
                    suggestedKeywords = expansions
                )
            }
        }
    }

    fun downloadTrack(track: AudioTrack) {
        viewModelScope.launch {
            downloadManager.downloadTrack(track)
        }
    }

    fun retry() {
        if (_uiState.value.searchQuery.isNotBlank()) {
            onSearchQueryChange(_uiState.value.searchQuery)
        } else {
            loadPopularTracks()
        }
    }
}
