package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AudioTrack
import com.example.model.PlaybackState
import com.example.ui.components.TrackListItem
import com.example.ui.theme.NavaPrimary
import com.example.ui.theme.NavaSecondary
import com.example.ui.viewmodel.OnlineCatalogFilter
import com.example.ui.viewmodel.OnlineUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScreen(
    uiState: OnlineUiState,
    playbackState: PlaybackState,
    isFavorite: (String) -> Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onDownloadTrack: (AudioTrack) -> Unit,
    onRetry: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectFilter: (OnlineCatalogFilter) -> Unit = {},
    onExpandWithAi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("online_screen")
    ) {
        // Search Bar Top
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NavaPrimary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.close)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavaPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("online_search_input")
                )

                // Source Catalog Filter Chips
                val filterScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(filterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnlineCatalogFilter.entries.forEach { filter ->
                        val isSelected = uiState.activeFilter == filter
                        val filterLabel = when (filter) {
                            OnlineCatalogFilter.ALL -> "همه منابع"
                            OnlineCatalogFilter.JAMENDO -> "Jamendo (رایگان)"
                            OnlineCatalogFilter.APPLE_PREVIEWS -> "Apple Music (پیش‌نمایش)"
                            OnlineCatalogFilter.EXTERNAL_SERVICES -> "سرویس‌های آنلاین"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFilter(filter) },
                            label = { Text(filterLabel, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavaPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // AI Query Expansion button
                    if (uiState.searchQuery.isNotBlank()) {
                        AssistChip(
                            onClick = onExpandWithAi,
                            label = {
                                Text(
                                    if (uiState.isAiExpanding) "در حال بررسی..." else "✨ بهبود با هوش مصنوعی",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                if (uiState.isAiExpanding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = NavaPrimary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = NavaPrimary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // AI Suggested Alternative Keywords
                if (uiState.suggestedKeywords.isNotEmpty()) {
                    val kwScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(kwScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.suggestedKeywords.forEach { keyword ->
                            SuggestionChip(
                                onClick = { onSearchQueryChange(keyword) },
                                label = { Text(keyword, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Body Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NavaPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "در حال بارگذاری کاتالوگ آنلاین...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.errorMessage != null && uiState.filteredTracks.isEmpty() -> {
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
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.online_error),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            uiState.filteredTracks.isEmpty() && uiState.externalLinks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    item {
                        Text(
                            text = if (uiState.isSearching) {
                                stringResource(R.string.search_results)
                            } else {
                                stringResource(R.string.popular_tracks)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // External Service Links Card
                    if (uiState.externalLinks.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "جستجوی این قطعه در سرویس‌های آنلاین:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        uiState.externalLinks.forEach { link ->
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.webUrl))
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = link.serviceName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(uiState.filteredTracks, key = { index, track -> "${track.id}_$index" }) { _, track ->
                        val downloadState = uiState.downloadStates[track.id]
                        val progress = downloadState?.progressPercent

                        TrackListItem(
                            track = track.copy(isFavorite = isFavorite(track.id)),
                            isPlaying = playbackState.isPlaying,
                            isCurrentTrack = playbackState.currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, uiState.filteredTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onDownloadTrack = if (track.isDownloadAllowed) {
                                { onDownloadTrack(track) }
                            } else null,
                            downloadProgress = progress,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
