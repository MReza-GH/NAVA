package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AudioTrack
import com.example.model.PlaybackState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.TrackListItem
import com.example.ui.theme.NavaPrimary
import com.example.ui.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    playbackState: PlaybackState,
    isFavorite: (String) -> Boolean,
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onDownloadTrack: (AudioTrack) -> Unit,
    onNavigateToOnline: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Hero Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.greeting_welcome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.about_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Recently Played Section
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.recently_played),
                    actionText = null,
                    onActionClick = null
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recentlyPlayed.take(10)) { track ->
                        RecentlyPlayedCard(
                            track = track,
                            isCurrentTrack = playbackState.currentTrack?.id == track.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = { onTrackClick(track, uiState.recentlyPlayed) }
                        )
                    }
                }
            }
        }

        // Favorites Section
        if (uiState.favoriteTracks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.favorite_tracks),
                    actionText = stringResource(R.string.view_all),
                    onActionClick = onNavigateToLibrary
                )
            }
            items(uiState.favoriteTracks.take(4)) { track ->
                TrackListItem(
                    track = track.copy(isFavorite = true),
                    isPlaying = playbackState.isPlaying,
                    isCurrentTrack = playbackState.currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, uiState.favoriteTracks) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onDownloadTrack = { onDownloadTrack(track) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                )
            }
        }

        // Recently Added Local Tracks
        if (uiState.recentlyAdded.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.recently_added),
                    actionText = stringResource(R.string.view_all),
                    onActionClick = onNavigateToLibrary
                )
            }
            items(uiState.recentlyAdded.take(5)) { track ->
                TrackListItem(
                    track = track.copy(isFavorite = isFavorite(track.id)),
                    isPlaying = playbackState.isPlaying,
                    isCurrentTrack = playbackState.currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, uiState.recentlyAdded) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onDownloadTrack = { onDownloadTrack(track) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                )
            }
        }

        // Featured Online Section
        item {
            SectionHeader(
                title = stringResource(R.string.featured_online),
                actionText = stringResource(R.string.view_all),
                onActionClick = onNavigateToOnline
            )
        }

        if (uiState.isOnlineLoading && uiState.featuredOnline.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NavaPrimary)
                }
            }
        } else if (uiState.featuredOnline.isNotEmpty()) {
            items(uiState.featuredOnline.take(6)) { track ->
                TrackListItem(
                    track = track.copy(isFavorite = isFavorite(track.id)),
                    isPlaying = playbackState.isPlaying,
                    isCurrentTrack = playbackState.currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, uiState.featuredOnline) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onDownloadTrack = { onDownloadTrack(track) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                )
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToOnline),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = NavaPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.nav_online),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String?,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    color = NavaPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedCard(
    track: AudioTrack,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                ArtworkImage(
                    artworkUri = track.artworkUri,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 8.dp
                )
                if (isCurrentTrack) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NavaPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
