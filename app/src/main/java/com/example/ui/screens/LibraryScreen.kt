package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.AudioTrack
import com.example.model.PlaybackState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.FormatUtils
import com.example.ui.components.TrackListItem
import com.example.ui.theme.NavaPrimary
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playbackState: PlaybackState,
    isFavorite: (String) -> Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onScanDeviceStorage: () -> Unit,
    onImportAudioUri: (Uri) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRenamePlaylistClick: (Long, String) -> Unit,
    onDeletePlaylistClick: (Long) -> Unit,
    onRemoveFromPlaylist: (Long, String) -> Unit = { _, _ -> },
    onMoveTrackInPlaylist: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onSelectPlaylist: (com.example.data.local.entity.PlaylistEntity) -> Unit = {},
    onSelectArtist: (String) -> Unit = {},
    onSelectAlbum: (String) -> Unit = {},
    onSelectFolder: (String) -> Unit = {},
    onClearSubSelection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            onScanDeviceStorage()
        }
    }

    // Auto-scan once on initial mount if permission is already granted and no tracks exist
    var hasAutoScanned by remember { mutableStateOf(false) }
    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState.allTracks.isEmpty() && !uiState.isScanning && !hasAutoScanned) {
            hasAutoScanned = true
            onScanDeviceStorage()
        }
    }

    // Manual File Picker (OpenMultipleDocuments)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onImportAudioUri(uri)
        }
    }

    val isInSubView = uiState.selectedPlaylist != null ||
            uiState.selectedArtistName != null ||
            uiState.selectedAlbumName != null ||
            uiState.selectedFolderName != null

    if (isInSubView) {
        BackHandler { onClearSubSelection() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        if (isInSubView) {
            val subTitle = uiState.selectedPlaylist?.name
                ?: uiState.selectedArtistName
                ?: uiState.selectedAlbumName
                ?: uiState.selectedFolderName
                ?: ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClearSubSelection) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.selectedPlaylist != null) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    var showRenameDialog by remember { mutableStateOf(false) }

                    if (showRenameDialog) {
                        var newName by remember { mutableStateOf(uiState.selectedPlaylist.name) }
                        AlertDialog(
                            onDismissRequest = { showRenameDialog = false },
                            title = { Text(stringResource(R.string.rename)) },
                            text = {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newName.isNotBlank()) {
                                            onRenamePlaylistClick(uiState.selectedPlaylist.id, newName.trim())
                                            showRenameDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                                ) {
                                    Text(stringResource(R.string.confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRenameDialog = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_playlist)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDeletePlaylistClick(uiState.selectedPlaylist.id)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Main Library Header & Search
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nav_library),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(
                            onClick = {
                                if (hasPermission) onScanDeviceStorage() else permissionLauncher.launch(audioPermission)
                            },
                            modifier = Modifier.testTag("rescan_library_button")
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = NavaPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.rescan_library),
                                    tint = NavaPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                            modifier = Modifier.testTag("pick_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = stringResource(R.string.pick_files_manually),
                                tint = NavaPrimary
                            )
                        }
                    }
                }

                // Search field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                // Safe RTL-friendly Sub-Tabs Row (eliminates ScrollableTabRow RTL freeze bug)
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LibraryTab.entries.forEach { tab ->
                        val isSelected = uiState.selectedTab == tab
                        val labelRes = when (tab) {
                            LibraryTab.TRACKS -> R.string.tab_tracks
                            LibraryTab.ARTISTS -> R.string.tab_artists
                            LibraryTab.ALBUMS -> R.string.tab_albums
                            LibraryTab.PLAYLISTS -> R.string.tab_playlists
                            LibraryTab.FOLDERS -> R.string.tab_folders
                            LibraryTab.FAVORITES -> R.string.favorite_tracks
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onTabSelected(tab) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavaPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Permission Banner if not granted
        if (!hasPermission && !isInSubView) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NavaPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_needed_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.permission_needed_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { permissionLauncher.launch(audioPermission) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        }

        // Body Content
        if (isInSubView) {
            val tracksToShow = when {
                uiState.selectedPlaylist != null -> uiState.selectedPlaylistTracks
                uiState.selectedArtistName != null -> uiState.allTracks.filter { it.artist == uiState.selectedArtistName }
                uiState.selectedAlbumName != null -> uiState.allTracks.filter { it.album == uiState.selectedAlbumName }
                uiState.selectedFolderName != null -> uiState.allTracks.filter { (it.folderName ?: "Music") == uiState.selectedFolderName }
                else -> emptyList()
            }

            if (tracksToShow.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val totalDuration = tracksToShow.sumOf { it.durationMs }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    // Header summary and Quick Actions
                    item(key = "subview_header") {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.songs_count, tracksToShow.size)} • ${FormatUtils.formatDuration(totalDuration)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onTrackClick(tracksToShow.first(), tracksToShow) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.play))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val shuffled = tracksToShow.shuffled()
                                        onTrackClick(shuffled.first(), shuffled)
                                    }
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.shuffle))
                                }
                            }
                        }
                    }

                    if (uiState.selectedPlaylist != null) {
                        itemsIndexed(tracksToShow, key = { index, track -> "pl_${track.id}_$index" }) { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackListItem(
                                    track = track.copy(isFavorite = isFavorite(track.id)),
                                    isPlaying = playbackState.isPlaying,
                                    isCurrentTrack = playbackState.currentTrack?.id == track.id,
                                    onTrackClick = { onTrackClick(track, tracksToShow) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onAddToQueue = { onAddToQueue(track) },
                                    onAddToPlaylist = { onAddToPlaylist(track) },
                                    modifier = Modifier.weight(1f)
                                )

                                // Reorder actions: Move Up / Down
                                Column {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { onMoveTrackInPlaylist(uiState.selectedPlaylist.id, index, index - 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    if (index < tracksToShow.size - 1) {
                                        IconButton(
                                            onClick = { onMoveTrackInPlaylist(uiState.selectedPlaylist.id, index, index + 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveFromPlaylist(uiState.selectedPlaylist.id, track.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(tracksToShow, key = { index, track -> "${track.id}_$index" }) { _, track ->
                            TrackListItem(
                                track = track.copy(isFavorite = isFavorite(track.id)),
                                isPlaying = playbackState.isPlaying,
                                isCurrentTrack = playbackState.currentTrack?.id == track.id,
                                onTrackClick = { onTrackClick(track, tracksToShow) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = { onAddToPlaylist(track) },
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        } else {
            when (uiState.selectedTab) {
                LibraryTab.TRACKS -> {
                    val filtered = uiState.filteredTracks

                    if (filtered.isEmpty()) {
                        EmptyLibraryPlaceholder(
                            onScanClick = {
                                if (hasPermission) onScanDeviceStorage() else permissionLauncher.launch(audioPermission)
                            },
                            onPickFilesClick = { filePickerLauncher.launch(arrayOf("audio/*")) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                        ) {
                            itemsIndexed(filtered, key = { index, track -> "${track.id}_$index" }) { _, track ->
                                TrackListItem(
                                    track = track.copy(isFavorite = isFavorite(track.id)),
                                    isPlaying = playbackState.isPlaying,
                                    isCurrentTrack = playbackState.currentTrack?.id == track.id,
                                    onTrackClick = { onTrackClick(track, filtered) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onAddToQueue = { onAddToQueue(track) },
                                    onAddToPlaylist = { onAddToPlaylist(track) },
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    val query = uiState.searchQuery.trim().lowercase()
                    val filteredArtists = if (query.isEmpty()) uiState.artists else uiState.artists.filter {
                        it.name.lowercase().contains(query)
                    }
                    if (filteredArtists.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                        ) {
                            itemsIndexed(filteredArtists, key = { index, artist -> "${artist.name}_$index" }) { _, artist ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectArtist(artist.name) },
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ArtworkImage(
                                            artworkUri = artist.sampleArtworkUri,
                                            modifier = Modifier.size(50.dp),
                                            cornerRadius = 25.dp
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = stringResource(R.string.songs_count, artist.trackCount),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    val query = uiState.searchQuery.trim().lowercase()
                    val filteredAlbums = if (query.isEmpty()) uiState.albums else uiState.albums.filter {
                        it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                    }
                    if (filteredAlbums.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                        ) {
                            itemsIndexed(filteredAlbums, key = { index, album -> "${album.title}_${album.artist}_$index" }) { _, album ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectAlbum(album.title) },
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ArtworkImage(
                                            artworkUri = album.artworkUri,
                                            modifier = Modifier.size(50.dp),
                                            cornerRadius = 8.dp
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${album.artist} • ${stringResource(R.string.songs_count, album.trackCount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                    ) {
                        item(key = "create_pl_btn") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onCreatePlaylistClick)
                                    .testTag("create_playlist_button"),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = NavaPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = stringResource(R.string.create_playlist),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = NavaPrimary
                                    )
                                }
                            }
                        }

                        if (uiState.playlists.isEmpty()) {
                            item(key = "empty_playlists") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_playlists),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(uiState.playlists, key = { index, pl -> "${pl.id}_$index" }) { _, playlist ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectPlaylist(playlist) },
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            tint = NavaPrimary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.FOLDERS -> {
                    if (uiState.folders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                        ) {
                            itemsIndexed(uiState.folders, key = { index, folder -> "${folder.name}_$index" }) { _, folder ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectFolder(folder.name) },
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = NavaPrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = stringResource(R.string.songs_count, folder.trackCount),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.FAVORITES -> {
                    val query = uiState.searchQuery.trim().lowercase()
                    val filteredFavorites = if (query.isEmpty()) uiState.favoriteTracks else uiState.favoriteTracks.filter {
                        it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                    }

                    if (filteredFavorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_favorites),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                        ) {
                            itemsIndexed(filteredFavorites, key = { index, track -> "fav_${track.id}_$index" }) { _, track ->
                                TrackListItem(
                                    track = track.copy(isFavorite = true),
                                    isPlaying = playbackState.isPlaying,
                                    isCurrentTrack = playbackState.currentTrack?.id == track.id,
                                    onTrackClick = { onTrackClick(track, filteredFavorites) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onAddToQueue = { onAddToQueue(track) },
                                    onAddToPlaylist = { onAddToPlaylist(track) },
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryPlaceholder(
    onScanClick: () -> Unit,
    onPickFilesClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_library),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_library_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                ) {
                    Text(stringResource(R.string.scan_device_library))
                }
                OutlinedButton(onClick = onPickFilesClick) {
                    Text(stringResource(R.string.pick_files_manually))
                }
            }
        }
    }
}
