package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NavaDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.media.NavaDownloadManager
import com.example.model.AudioTrack
import com.example.model.PlaybackState
import com.example.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainDestination {
    HOME,
    ONLINE,
    LIBRARY,
    DOWNLOADS,
    SETTINGS,
    AI_DJ
}

data class MainUiState(
    val currentDestination: MainDestination = MainDestination.HOME,
    val isFullPlayerVisible: Boolean = false,
    val isQueueVisible: Boolean = false,
    val trackToAddToPlaylist: AudioTrack? = null,
    val isCreatePlaylistVisible: Boolean = false,
    val playlists: List<PlaylistEntity> = emptyList(),
    val favoriteTrackIds: Set<String> = emptySet(),
    val language: String = "fa",
    val theme: String = "dark",
    val snackbarMessage: String? = null
)

class MainViewModel(
    private val playbackManager: PlaybackManager,
    private val database: NavaDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val downloadManager: NavaDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    init {
        observeFavorites()
        observePlaylists()
        observePreferences()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            database.favoriteDao().getAllFavorites().collect { favorites ->
                val favIds = favorites.map { it.trackId }.toSet()
                _uiState.update { it.copy(favoriteTrackIds = favIds) }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            database.playlistDao().getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.languageFlow.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.themeFlow.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
    }

    fun navigateTo(destination: MainDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun openFullPlayer() {
        _uiState.update { it.copy(isFullPlayerVisible = true) }
    }

    fun closeFullPlayer() {
        _uiState.update { it.copy(isFullPlayerVisible = false) }
    }

    fun openQueue() {
        _uiState.update { it.copy(isQueueVisible = true) }
    }

    fun closeQueue() {
        _uiState.update { it.copy(isQueueVisible = false) }
    }

    fun playTrack(track: AudioTrack, queue: List<AudioTrack> = listOf(track)) {
        playbackManager.playTrack(track, queue)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun skipNext() {
        playbackManager.skipNext()
    }

    fun skipPrevious() {
        playbackManager.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playbackManager.cycleRepeatMode()
    }

    fun addToQueue(track: AudioTrack) {
        playbackManager.addToQueue(track)
        showSnackbar("به صف پخش اضافه شد")
    }

    fun removeFromQueue(index: Int) {
        playbackManager.removeFromQueue(index)
    }

    fun clearQueue() {
        playbackManager.clearQueue()
    }

    fun toggleFavorite(track: AudioTrack) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = database.favoriteDao().isFavoriteDirect(track.id)
            if (isFav) {
                database.favoriteDao().deleteFavorite(track.id)
            } else {
                database.favoriteDao().insertFavorite(FavoriteEntity.fromAudioTrack(track))
            }
        }
    }

    fun isTrackFavorite(trackId: String): Boolean {
        return _uiState.value.favoriteTrackIds.contains(trackId)
    }

    fun showAddToPlaylist(track: AudioTrack) {
        _uiState.update { it.copy(trackToAddToPlaylist = track) }
    }

    fun dismissAddToPlaylist() {
        _uiState.update { it.copy(trackToAddToPlaylist = null) }
    }

    fun showCreatePlaylist() {
        _uiState.update { it.copy(isCreatePlaylistVisible = true) }
    }

    fun dismissCreatePlaylist() {
        _uiState.update { it.copy(isCreatePlaylistVisible = false) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.playlistDao().insertPlaylist(PlaylistEntity(name = name))
            dismissCreatePlaylist()
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: AudioTrack) {
        viewModelScope.launch(Dispatchers.IO) {
            database.playlistDao().addTrackToPlaylist(
                com.example.data.local.entity.PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = track.id
                )
            )
            dismissAddToPlaylist()
            showSnackbar("به پلی‌لیست اضافه شد")
        }
    }

    fun downloadTrack(track: AudioTrack) {
        viewModelScope.launch {
            val started = downloadManager.downloadTrack(track)
            if (started) {
                showSnackbar("دانلود آغاز شد")
            }
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
