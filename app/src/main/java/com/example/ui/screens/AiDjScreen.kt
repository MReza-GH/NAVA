package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiPlaylistRecommendation
import com.example.ai.ChatMessage
import com.example.model.AudioTrack
import com.example.ui.components.ArtworkImage
import com.example.ui.components.FormatUtils
import com.example.ui.theme.NavaPrimary
import com.example.ui.theme.NavaSecondary
import com.example.ui.viewmodel.AiDjUiState

@Composable
fun AiDjScreen(
    uiState: AiDjUiState,
    onSendMessage: (String) -> Unit,
    onInputTextChange: (String) -> Unit,
    onPlayTracks: (AudioTrack, List<AudioTrack>) -> Unit,
    onAddAllToQueue: (List<AudioTrack>) -> Unit,
    onSaveAsPlaylist: (String, List<AudioTrack>) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "🎵 یک پلی‌لیست پرانرژی برای ورزش",
        "☕ موسیقی آرامش‌بخش برای مطالعه و تمرکز",
        "🌙 پلی‌لیست ملایم برای رانندگی شبانه",
        "🎻 آهنگ‌های خاطره‌انگیز و اصیل ایرانی",
        "📱 آهنگ‌های پیشنهادی از حافظه گوشی من"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_dj_screen")
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NavaPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "دی‌جی هوشمند (AI DJ)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "پیشنهاد و ساخت پلی‌لیست هوشمند با Gemini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Configuration Warning if key is missing
        if (!uiState.isConfigured) {
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
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = NavaSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تنظیم کلید هوش مصنوعی Gemini",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "برای فعال‌سازی دستیار هوشمند، کلید Gemini API را در پنل Secrets یا تنظیمات وارد کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                    ) {
                        Text("تنظیم")
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                SuggestionChip(
                    onClick = { onSendMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages) { message ->
                MessageBubble(
                    message = message,
                    onPlayAll = { playlist ->
                        if (playlist.recommendedTracks.isNotEmpty()) {
                            onPlayTracks(playlist.recommendedTracks.first(), playlist.recommendedTracks)
                        }
                    },
                    onAddAllToQueue = { playlist ->
                        onAddAllToQueue(playlist.recommendedTracks)
                    },
                    onSavePlaylist = { playlist ->
                        onSaveAsPlaylist(playlist.title, playlist.recommendedTracks)
                    }
                )
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NavaPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "دی‌جی در حال گلچین بهترین آهنگ‌هاست…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 120.dp), // Clear bottom nav & mini-player
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = onInputTextChange,
                    placeholder = { Text("به دی‌جی بگو چه نوع آهنگی می‌خواهی…") },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_dj_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { onSendMessage(uiState.inputText) },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = NavaPrimary),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onPlayAll: (AiPlaylistRecommendation) -> Unit,
    onAddAllToQueue: (AiPlaylistRecommendation) -> Unit,
    onSavePlaylist: (AiPlaylistRecommendation) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) NavaPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // If message has an attached generated playlist
                message.attachedPlaylist?.let { playlist ->
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "قطعات انتخاب شده (${playlist.recommendedTracks.size} آهنگ):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavaPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    playlist.recommendedTracks.take(5).forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                artworkUri = track.artworkUri,
                                modifier = Modifier.size(36.dp),
                                cornerRadius = 6.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (playlist.recommendedTracks.size > 5) {
                        Text(
                            text = "+ ${playlist.recommendedTracks.size - 5} آهنگ دیگر",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(playlist) },
                            colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پخش همه", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { onSavePlaylist(playlist) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        OutlinedButton(
                            onClick = { onAddAllToQueue(playlist) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
