package com.example.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.QueueBottomSheet
import com.example.ui.theme.NavaPrimary
import com.example.ui.viewmodel.*
import java.util.Locale

@Composable
fun NavaMainScreen(
    mainViewModel: MainViewModel,
    homeViewModel: HomeViewModel,
    onlineViewModel: OnlineViewModel,
    libraryViewModel: LibraryViewModel,
    downloadsViewModel: DownloadsViewModel,
    settingsViewModel: SettingsViewModel,
    aiDjViewModel: AiDjViewModel? = null,
    modifier: Modifier = Modifier
) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val onlineUiState by onlineViewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val downloadsUiState by downloadsViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val playbackState by mainViewModel.playbackState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mainUiState.snackbarMessage) {
        mainUiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearSnackbar()
        }
    }

    if (mainUiState.currentDestination == MainDestination.AI_DJ) {
        BackHandler { mainViewModel.navigateTo(MainDestination.HOME) }
    }

    // Dynamic Locale and RTL layout direction
    val layoutDirection = if (mainUiState.language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr
    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocale(if (mainUiState.language == "fa") Locale.forLanguageTag("fa") else Locale.forLanguageTag("en"))
    }
    val localizedContext = LocalContext.current.createConfigurationContext(configuration)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalContext provides localizedContext
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Persistent Mini Player above Navigation Bar
                        MiniPlayer(
                            playbackState = playbackState,
                            onMiniPlayerClick = { mainViewModel.openFullPlayer() },
                            onTogglePlayPause = { mainViewModel.togglePlayPause() },
                            onSkipNext = { mainViewModel.skipNext() }
                        )

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = mainUiState.currentDestination == MainDestination.HOME,
                                onClick = { mainViewModel.navigateTo(MainDestination.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = if (mainUiState.currentDestination == MainDestination.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = stringResource(R.string.nav_home)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_home)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavaPrimary,
                                    selectedTextColor = NavaPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_home")
                            )

                            NavigationBarItem(
                                selected = mainUiState.currentDestination == MainDestination.ONLINE,
                                onClick = { mainViewModel.navigateTo(MainDestination.ONLINE) },
                                icon = {
                                    Icon(
                                        imageVector = if (mainUiState.currentDestination == MainDestination.ONLINE) Icons.Filled.Language else Icons.Outlined.Language,
                                        contentDescription = stringResource(R.string.nav_online)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_online)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavaPrimary,
                                    selectedTextColor = NavaPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_online")
                            )

                            NavigationBarItem(
                                selected = mainUiState.currentDestination == MainDestination.LIBRARY,
                                onClick = { mainViewModel.navigateTo(MainDestination.LIBRARY) },
                                icon = {
                                    Icon(
                                        imageVector = if (mainUiState.currentDestination == MainDestination.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                        contentDescription = stringResource(R.string.nav_library)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_library)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavaPrimary,
                                    selectedTextColor = NavaPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_library")
                            )

                            NavigationBarItem(
                                selected = mainUiState.currentDestination == MainDestination.DOWNLOADS,
                                onClick = { mainViewModel.navigateTo(MainDestination.DOWNLOADS) },
                                icon = {
                                    Icon(
                                        imageVector = if (mainUiState.currentDestination == MainDestination.DOWNLOADS) Icons.Filled.Download else Icons.Outlined.Download,
                                        contentDescription = stringResource(R.string.nav_downloads)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_downloads)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavaPrimary,
                                    selectedTextColor = NavaPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_downloads")
                            )

                            NavigationBarItem(
                                selected = mainUiState.currentDestination == MainDestination.SETTINGS,
                                onClick = { mainViewModel.navigateTo(MainDestination.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (mainUiState.currentDestination == MainDestination.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.nav_settings)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_settings)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavaPrimary,
                                    selectedTextColor = NavaPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_settings")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (mainUiState.currentDestination) {
                        MainDestination.HOME -> {
                            HomeScreen(
                                uiState = homeUiState,
                                playbackState = playbackState,
                                isFavorite = { mainViewModel.isTrackFavorite(it) },
                                onTrackClick = { track, queue -> mainViewModel.playTrack(track, queue) },
                                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                onAddToQueue = { mainViewModel.addToQueue(it) },
                                onAddToPlaylist = { mainViewModel.showAddToPlaylist(it) },
                                onDownloadTrack = { mainViewModel.downloadTrack(it) },
                                onNavigateToOnline = { mainViewModel.navigateTo(MainDestination.ONLINE) },
                                onNavigateToLibrary = { mainViewModel.navigateTo(MainDestination.LIBRARY) }
                            )
                        }

                        MainDestination.ONLINE -> {
                            OnlineScreen(
                                uiState = onlineUiState,
                                playbackState = playbackState,
                                isFavorite = { mainViewModel.isTrackFavorite(it) },
                                onSearchQueryChange = { onlineViewModel.onSearchQueryChange(it) },
                                onTrackClick = { track, queue -> mainViewModel.playTrack(track, queue) },
                                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                onAddToQueue = { mainViewModel.addToQueue(it) },
                                onAddToPlaylist = { mainViewModel.showAddToPlaylist(it) },
                                onDownloadTrack = { onlineViewModel.downloadTrack(it) },
                                onRetry = { onlineViewModel.retry() },
                                onNavigateToSettings = { mainViewModel.navigateTo(MainDestination.SETTINGS) },
                                onSelectFilter = { onlineViewModel.setFilter(it) },
                                onExpandWithAi = { onlineViewModel.expandWithGemini() }
                            )
                        }

                        MainDestination.LIBRARY -> {
                            LibraryScreen(
                                uiState = libraryUiState,
                                playbackState = playbackState,
                                isFavorite = { mainViewModel.isTrackFavorite(it) },
                                onTabSelected = { libraryViewModel.selectTab(it) },
                                onSearchQueryChange = { libraryViewModel.setSearchQuery(it) },
                                onTrackClick = { track, queue -> mainViewModel.playTrack(track, queue) },
                                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                onAddToQueue = { mainViewModel.addToQueue(it) },
                                onAddToPlaylist = { mainViewModel.showAddToPlaylist(it) },
                                onScanDeviceStorage = { libraryViewModel.scanDeviceStorage() },
                                onImportAudioUri = { libraryViewModel.importAudioUri(it) },
                                onCreatePlaylistClick = { mainViewModel.showCreatePlaylist() },
                                onRenamePlaylistClick = { id, name -> libraryViewModel.renamePlaylist(id, name) },
                                onDeletePlaylistClick = { libraryViewModel.deletePlaylist(it) },
                                onRemoveFromPlaylist = { playlistId, trackId ->
                                    libraryViewModel.removeTrackFromPlaylist(playlistId, trackId)
                                },
                                onMoveTrackInPlaylist = { playlistId, from, to ->
                                    libraryViewModel.moveTrackInPlaylist(playlistId, from, to)
                                },
                                onSelectPlaylist = { libraryViewModel.selectPlaylist(it) },
                                onSelectArtist = { libraryViewModel.selectArtist(it) },
                                onSelectAlbum = { libraryViewModel.selectAlbum(it) },
                                onSelectFolder = { libraryViewModel.selectFolder(it) },
                                onClearSubSelection = { libraryViewModel.clearSubSelection() }
                            )
                        }

                        MainDestination.DOWNLOADS -> {
                            DownloadsScreen(
                                uiState = downloadsUiState,
                                playbackState = playbackState,
                                isFavorite = { mainViewModel.isTrackFavorite(it) },
                                onTrackClick = { track, queue -> mainViewModel.playTrack(track, queue) },
                                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                onAddToQueue = { mainViewModel.addToQueue(it) },
                                onAddToPlaylist = { mainViewModel.showAddToPlaylist(it) },
                                onDeleteDownload = { downloadsViewModel.deleteDownload(it) }
                            )
                        }

                        MainDestination.SETTINGS -> {
                            SettingsScreen(
                                uiState = settingsUiState,
                                onLanguageChange = { settingsViewModel.setLanguage(it) },
                                onThemeChange = { settingsViewModel.setTheme(it) },
                                onSaveJamendoClientId = { settingsViewModel.saveJamendoClientId(it) },
                                onRescanLibrary = { settingsViewModel.rescanLibrary() }
                            )
                        }

                        MainDestination.AI_DJ -> {
                            if (aiDjViewModel != null) {
                                val aiDjUiState by aiDjViewModel.uiState.collectAsState()
                                AiDjScreen(
                                    uiState = aiDjUiState,
                                    onSendMessage = { aiDjViewModel.sendMessage(it) },
                                    onInputTextChange = { aiDjViewModel.onInputTextChange(it) },
                                    onPlayTracks = { track, queue -> mainViewModel.playTrack(track, queue) },
                                    onAddAllToQueue = { tracks ->
                                        tracks.forEach { mainViewModel.addToQueue(it) }
                                    },
                                    onSaveAsPlaylist = { name, tracks ->
                                        mainViewModel.createPlaylist(name)
                                        // Once created, tracks will be added
                                    },
                                    onNavigateToSettings = { mainViewModel.navigateTo(MainDestination.SETTINGS) }
                                )
                            }
                        }
                    }
                }
            }

            // Full Player Screen (Overlaid with vertical slide animation)
            AnimatedVisibility(
                visible = mainUiState.isFullPlayerVisible && playbackState.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                playbackState.currentTrack?.let { track ->
                    FullPlayerScreen(
                        playbackState = playbackState,
                        isFavorite = mainViewModel.isTrackFavorite(track.id),
                        onCollapse = { mainViewModel.closeFullPlayer() },
                        onTogglePlayPause = { mainViewModel.togglePlayPause() },
                        onSkipNext = { mainViewModel.skipNext() },
                        onSkipPrevious = { mainViewModel.skipPrevious() },
                        onSeekTo = { mainViewModel.seekTo(it) },
                        onToggleShuffle = { mainViewModel.toggleShuffle() },
                        onCycleRepeat = { mainViewModel.cycleRepeatMode() },
                        onToggleFavorite = { mainViewModel.toggleFavorite(track) },
                        onOpenQueue = { mainViewModel.openQueue() }
                    )
                }
            }

            // Queue Bottom Sheet
            if (mainUiState.isQueueVisible) {
                QueueBottomSheet(
                    playbackState = playbackState,
                    onDismiss = { mainViewModel.closeQueue() },
                    onTrackSelect = { track -> mainViewModel.playTrack(track, playbackState.queue) },
                    onRemoveFromQueue = { index -> mainViewModel.removeFromQueue(index) },
                    onClearQueue = { mainViewModel.clearQueue() }
                )
            }

            // Add To Playlist Dialog
            mainUiState.trackToAddToPlaylist?.let { track ->
                AddToPlaylistDialog(
                    track = track,
                    playlists = mainUiState.playlists,
                    onDismiss = { mainViewModel.dismissAddToPlaylist() },
                    onSelectPlaylist = { playlistId ->
                        mainViewModel.addTrackToPlaylist(playlistId, track)
                    },
                    onCreateNewPlaylist = {
                        mainViewModel.showCreatePlaylist()
                    }
                )
            }

            // Create Playlist Dialog
            if (mainUiState.isCreatePlaylistVisible) {
                CreatePlaylistDialog(
                    onDismiss = { mainViewModel.dismissCreatePlaylist() },
                    onConfirm = { name -> mainViewModel.createPlaylist(name) }
                )
            }
        }
    }
}
