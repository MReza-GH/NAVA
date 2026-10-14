package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NavaDatabase
import com.example.data.remote.JamendoMusicSource
import com.example.data.remote.OnlineResult
import com.example.model.AudioTrack
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentlyPlayed: List<AudioTrack> = emptyList(),
    val favoriteTracks: List<AudioTrack> = emptyList(),
    val recentlyAdded: List<AudioTrack> = emptyList(),
    val featuredOnline: List<AudioTrack> = emptyList(),
    val isOnlineLoading: Boolean = false,
    val onlineError: String? = null
)

class HomeViewModel(
    private val database: NavaDatabase,
    private val musicSource: JamendoMusicSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeLocalData()
        loadOnlineFeatured()
    }

    private fun observeLocalData() {
        // Observe Recently Played
        viewModelScope.launch {
            database.historyDao().getRecentlyPlayed().collect { historyList ->
                // Deduplicate consecutive or repeating tracks by ID
                val tracks = historyList.distinctBy { it.trackId }.map { it.toAudioTrack() }
                _uiState.update { it.copy(recentlyPlayed = tracks) }
            }
        }

        // Observe Favorites
        viewModelScope.launch {
            database.favoriteDao().getAllFavorites().collect { favorites ->
                val tracks = favorites.map { it.toAudioTrack() }
                _uiState.update { it.copy(favoriteTracks = tracks) }
            }
        }

        // Observe Recently Added Local Tracks
        viewModelScope.launch {
            database.trackDao().getAllTracks().collect { localTracks ->
                val tracks = localTracks.take(20).map { it.toAudioTrack() }
                _uiState.update { it.copy(recentlyAdded = tracks) }
            }
        }
    }

    fun loadOnlineFeatured() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOnlineLoading = true, onlineError = null) }
            when (val result = musicSource.getFeaturedTracks()) {
                is OnlineResult.Success -> {
                    _uiState.update { it.copy(isOnlineLoading = false, featuredOnline = result.data) }
                }
                is OnlineResult.Error -> {
                    _uiState.update { it.copy(isOnlineLoading = false, onlineError = result.message) }
                }
                OnlineResult.Loading -> {
                    _uiState.update { it.copy(isOnlineLoading = true) }
                }
            }
        }
    }
}
