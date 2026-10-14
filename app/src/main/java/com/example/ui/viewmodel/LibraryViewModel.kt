package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NavaDatabase
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistTrackCrossRef
import com.example.data.media.LocalMusicScanner
import com.example.model.AlbumSummary
import com.example.model.ArtistSummary
import com.example.model.AudioTrack
import com.example.model.FolderSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab {
    TRACKS,
    ARTISTS,
    ALBUMS,
    PLAYLISTS,
    FOLDERS,
    FAVORITES
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.TRACKS,
    val searchQuery: String = "",
    val allTracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val favoriteTracks: List<AudioTrack> = emptyList(),
    val isScanning: Boolean = false,
    val selectedPlaylist: PlaylistEntity? = null,
    val selectedPlaylistTracks: List<AudioTrack> = emptyList(),
    val selectedArtistName: String? = null,
    val selectedAlbumName: String? = null,
    val selectedFolderName: String? = null
)

class LibraryViewModel(
    private val database: NavaDatabase,
    private val scanner: LocalMusicScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var playlistTracksJob: Job? = null

    init {
        observeTracks()
        observePlaylists()
        observeFavorites()
    }

    private fun observeTracks() {
        viewModelScope.launch {
            database.trackDao().getAllTracks()
                .flowOn(Dispatchers.IO)
                .collect { entities ->
                    // Offload grouping, sorting, and transformation strictly to Dispatchers.Default
                    withContext(Dispatchers.Default) {
                        val tracks = entities.map { it.toAudioTrack() }

                        // Group by Artists
                        val artistMap = tracks.groupBy { it.artist }
                        val artistList = artistMap.map { (artist, trackList) ->
                            ArtistSummary(
                                name = artist,
                                trackCount = trackList.size,
                                sampleArtworkUri = trackList.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri
                            )
                        }.sortedBy { it.name.lowercase() }

                        // Group by Albums
                        val albumMap = tracks.groupBy { "${it.album}##${it.artist}" }
                        val albumList = albumMap.map { (_, trackList) ->
                            val first = trackList.first()
                            AlbumSummary(
                                title = first.album,
                                artist = first.artist,
                                trackCount = trackList.size,
                                artworkUri = trackList.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri
                            )
                        }.sortedBy { it.title.lowercase() }

                        // Group by Folders
                        val folderMap = tracks.groupBy { it.folderName ?: "Music" }
                        val folderList = folderMap.map { (folder, trackList) ->
                            FolderSummary(
                                name = folder,
                                trackCount = trackList.size
                            )
                        }.sortedBy { it.name.lowercase() }

                        val query = _uiState.value.searchQuery.trim().lowercase()
                        val filtered = if (query.isEmpty()) {
                            tracks
                        } else {
                            tracks.filter {
                                it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                            }
                        }

                        _uiState.update {
                            it.copy(
                                allTracks = tracks,
                                filteredTracks = filtered,
                                artists = artistList,
                                albums = albumList,
                                folders = folderList
                            )
                        }
                    }
                }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            database.playlistDao().getAllPlaylists()
                .flowOn(Dispatchers.IO)
                .collect { playlists ->
                    _uiState.update { it.copy(playlists = playlists) }
                }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            database.favoriteDao().getAllFavorites()
                .flowOn(Dispatchers.IO)
                .collect { favorites ->
                    withContext(Dispatchers.Default) {
                        val favTracks = favorites.map { it.toAudioTrack() }
                        _uiState.update { it.copy(favoriteTracks = favTracks) }
                    }
                }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectedPlaylist = null,
                selectedArtistName = null,
                selectedAlbumName = null,
                selectedFolderName = null
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val trimmed = query.trim().lowercase()
            val filtered = if (trimmed.isEmpty()) {
                current.allTracks
            } else {
                current.allTracks.filter {
                    it.title.lowercase().contains(trimmed) || it.artist.lowercase().contains(trimmed)
                }
            }
            current.copy(searchQuery = query, filteredTracks = filtered)
        }
    }

    fun scanDeviceStorage() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                scanner.scanDeviceAudio()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun importAudioUri(uri: Uri) {
        viewModelScope.launch {
            scanner.importAudioUri(uri)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.playlistDao().insertPlaylist(PlaylistEntity(name = name))
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.playlistDao().renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.playlistDao().deletePlaylistTracks(playlistId)
            database.playlistDao().deletePlaylist(playlistId)
            if (_uiState.value.selectedPlaylist?.id == playlistId) {
                _uiState.update { it.copy(selectedPlaylist = null, selectedPlaylistTracks = emptyList()) }
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = database.playlistDao().getMaxOrderIndex(playlistId) ?: -1
            database.playlistDao().addTrackToPlaylist(
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = trackId,
                    orderIndex = maxOrder + 1
                )
            )
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.playlistDao().removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun moveTrackInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = _uiState.value.selectedPlaylistTracks.toMutableList()
            if (fromIndex in tracks.indices && toIndex in tracks.indices && fromIndex != toIndex) {
                val item = tracks.removeAt(fromIndex)
                tracks.add(toIndex, item)
                _uiState.update { it.copy(selectedPlaylistTracks = tracks) }
                tracks.forEachIndexed { index, track ->
                    database.playlistDao().updateTrackOrder(playlistId, track.id, index)
                }
            }
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity) {
        playlistTracksJob?.cancel()
        _uiState.update { it.copy(selectedPlaylist = playlist) }
        playlistTracksJob = viewModelScope.launch {
            database.playlistDao().getTracksForPlaylist(playlist.id)
                .flowOn(Dispatchers.IO)
                .collect { entities ->
                    withContext(Dispatchers.Default) {
                        val tracks = entities.map { it.toAudioTrack() }
                        _uiState.update { it.copy(selectedPlaylistTracks = tracks) }
                    }
                }
        }
    }

    fun selectArtist(artistName: String?) {
        _uiState.update { it.copy(selectedArtistName = artistName) }
    }

    fun selectAlbum(albumName: String?) {
        _uiState.update { it.copy(selectedAlbumName = albumName) }
    }

    fun selectFolder(folderName: String?) {
        _uiState.update { it.copy(selectedFolderName = folderName) }
    }

    fun clearSubSelection() {
        playlistTracksJob?.cancel()
        playlistTracksJob = null
        _uiState.update {
            it.copy(
                selectedPlaylist = null,
                selectedPlaylistTracks = emptyList(),
                selectedArtistName = null,
                selectedAlbumName = null,
                selectedFolderName = null
            )
        }
    }
}
