package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.NavaDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.model.AudioTrack
import com.example.model.PlaybackState
import com.example.model.RepeatState
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class PlaybackManager(
    private val context: Context,
    private val database: NavaDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val trackCache = mutableMapOf<String, AudioTrack>()

    // Pending track action if controller is still initializing
    private var pendingPlayAction: (() -> Unit)? = null

    init {
        initMediaController()
        observePreferences()
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, NavaMediaPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                restorePreferencesToPlayer()
                pendingPlayAction?.invoke()
                pendingPlayAction = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun observePreferences() {
        scope.launch {
            val shuffle = preferencesRepository.shuffleFlow.first()
            val repeat = preferencesRepository.repeatModeFlow.first()
            _playbackState.value = _playbackState.value.copy(
                shuffleModeEnabled = shuffle,
                repeatState = when (repeat) {
                    1 -> RepeatState.ONE
                    2 -> RepeatState.ALL
                    else -> RepeatState.OFF
                }
            )
        }
    }

    private fun restorePreferencesToPlayer() {
        val controller = mediaController ?: return
        val currentShuffle = _playbackState.value.shuffleModeEnabled
        controller.shuffleModeEnabled = currentShuffle
        controller.repeatMode = when (_playbackState.value.repeatState) {
            RepeatState.ONE -> Player.REPEAT_MODE_ONE
            RepeatState.ALL -> Player.REPEAT_MODE_ALL
            RepeatState.OFF -> Player.REPEAT_MODE_OFF
        }
    }

    private fun setupPlayerListener() {
        val controller = mediaController ?: return

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    isLoading = false
                )
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playbackState.value = _playbackState.value.copy(isLoading = true)
                    }
                    Player.STATE_READY -> {
                        val duration = if (controller.duration > 0) controller.duration else 0L
                        _playbackState.value = _playbackState.value.copy(
                            isLoading = false,
                            durationMs = duration,
                            bufferedPositionMs = controller.bufferedPosition
                        )
                    }
                    Player.STATE_ENDED -> {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isLoading = false
                        )
                    }
                    Player.STATE_IDLE -> {
                        _playbackState.value = _playbackState.value.copy(isLoading = false)
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val trackId = mediaItem?.mediaId
                val track = trackId?.let { trackCache[it] }
                val currentIndex = controller.currentMediaItemIndex

                _playbackState.value = _playbackState.value.copy(
                    currentTrack = track,
                    queueIndex = currentIndex,
                    currentPositionMs = 0L,
                    durationMs = if (controller.duration > 0) controller.duration else (track?.durationMs ?: 0L)
                )

                // Log into history
                track?.let { currentTrack ->
                    scope.launch(Dispatchers.IO) {
                        database.historyDao().insertHistory(
                            PlaybackHistoryEntity.fromAudioTrack(currentTrack)
                        )
                        preferencesRepository.setLastPlaybackState(currentTrack.id, 0L)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = _playbackState.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = error.localizedMessage ?: "Playback error"
                )
            }
        })
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    val pos = controller.currentPosition
                    val duration = if (controller.duration > 0) controller.duration else _playbackState.value.durationMs
                    val buffered = controller.bufferedPosition
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = duration,
                        bufferedPositionMs = buffered
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun playTrack(track: AudioTrack, newQueue: List<AudioTrack> = listOf(track)) {
        val action = {
            val controller = mediaController
            if (controller != null) {
                // Cache tracks
                newQueue.forEach { trackCache[it.id] = it }
                trackCache[track.id] = track

                val targetIndex = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

                // Safe queue windowing: To avoid android.os.TransactionTooLargeException over Binder IPC,
                // limit maximum media items sent in one transaction to 60 items around the target track.
                val maxIpcWindow = 60
                val (windowQueue, windowIndex) = if (newQueue.size > maxIpcWindow) {
                    val half = maxIpcWindow / 2
                    val start = (targetIndex - half).coerceAtLeast(0)
                    val end = (start + maxIpcWindow).coerceAtMost(newQueue.size)
                    val adjustedStart = (end - maxIpcWindow).coerceAtLeast(0)
                    val subList = newQueue.subList(adjustedStart, end)
                    Pair(subList, targetIndex - adjustedStart)
                } else {
                    Pair(newQueue, targetIndex)
                }

                val mediaItems = windowQueue.map { audioTrackToMediaItem(it) }

                try {
                    controller.setMediaItems(mediaItems, windowIndex, 0L)
                    controller.prepare()
                    controller.play()

                    _playbackState.value = _playbackState.value.copy(
                        queue = newQueue,
                        queueIndex = targetIndex,
                        currentTrack = track,
                        isPlaying = true,
                        errorMessage = null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to playing single item if batch transaction fails
                    try {
                        controller.setMediaItem(audioTrackToMediaItem(track))
                        controller.prepare()
                        controller.play()
                        _playbackState.value = _playbackState.value.copy(
                            queue = listOf(track),
                            queueIndex = 0,
                            currentTrack = track,
                            isPlaying = true,
                            errorMessage = null
                        )
                    } catch (fallbackEx: Exception) {
                        fallbackEx.printStackTrace()
                    }
                }
            }
        }

        if (mediaController != null) {
            action()
        } else {
            pendingPlayAction = action
            initMediaController()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0, 0L)
            }
            controller.play()
        }
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else {
            // Check if our virtual queue has next track
            val currentQueue = _playbackState.value.queue
            val currentIndex = _playbackState.value.queueIndex
            if (currentIndex + 1 < currentQueue.size) {
                playTrack(currentQueue[currentIndex + 1], currentQueue)
            }
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            val currentQueue = _playbackState.value.queue
            val currentIndex = _playbackState.value.queueIndex
            if (currentIndex - 1 >= 0) {
                playTrack(currentQueue[currentIndex - 1], currentQueue)
            } else {
                controller.seekTo(0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !_playbackState.value.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _playbackState.value = _playbackState.value.copy(shuffleModeEnabled = newShuffle)
        scope.launch { preferencesRepository.setShuffleEnabled(newShuffle) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val (nextState, playerRepeat) = when (_playbackState.value.repeatState) {
            RepeatState.OFF -> Pair(RepeatState.ALL, Player.REPEAT_MODE_ALL)
            RepeatState.ALL -> Pair(RepeatState.ONE, Player.REPEAT_MODE_ONE)
            RepeatState.ONE -> Pair(RepeatState.OFF, Player.REPEAT_MODE_OFF)
        }
        controller.repeatMode = playerRepeat
        _playbackState.value = _playbackState.value.copy(repeatState = nextState)
        scope.launch {
            val modeInt = when (nextState) {
                RepeatState.OFF -> 0
                RepeatState.ONE -> 1
                RepeatState.ALL -> 2
            }
            preferencesRepository.setRepeatMode(modeInt)
        }
    }

    fun playNext(track: AudioTrack) {
        val controller = mediaController ?: return
        trackCache[track.id] = track
        val currentQueue = _playbackState.value.queue.toMutableList()
        val currentIndex = _playbackState.value.queueIndex
        val insertIndex = (currentIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(insertIndex, track)
        try {
            val playerInsertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
            controller.addMediaItem(playerInsertIndex, audioTrackToMediaItem(track))
        } catch (_: Exception) {}
        _playbackState.value = _playbackState.value.copy(queue = currentQueue)
    }

    fun addToQueue(track: AudioTrack) {
        val controller = mediaController ?: return
        trackCache[track.id] = track
        try {
            controller.addMediaItem(audioTrackToMediaItem(track))
        } catch (_: Exception) {}
        val currentQueue = _playbackState.value.queue.toMutableList()
        currentQueue.add(track)
        _playbackState.value = _playbackState.value.copy(queue = currentQueue)
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (index in currentQueue.indices) {
            try {
                if (index < controller.mediaItemCount) {
                    controller.removeMediaItem(index)
                }
            } catch (_: Exception) {}
            currentQueue.removeAt(index)
            val newIndex = if (index < _playbackState.value.queueIndex) {
                _playbackState.value.queueIndex - 1
            } else _playbackState.value.queueIndex
            _playbackState.value = _playbackState.value.copy(queue = currentQueue, queueIndex = newIndex)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices && fromIndex != toIndex) {
            val track = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, track)
            try {
                if (fromIndex < controller.mediaItemCount && toIndex < controller.mediaItemCount) {
                    controller.moveMediaItem(fromIndex, toIndex)
                }
            } catch (_: Exception) {}
            _playbackState.value = _playbackState.value.copy(queue = currentQueue)
        }
    }

    fun clearQueue() {
        val controller = mediaController ?: return
        try {
            controller.clearMediaItems()
        } catch (_: Exception) {}
        _playbackState.value = _playbackState.value.copy(
            queue = emptyList(),
            currentTrack = null,
            isPlaying = false,
            queueIndex = -1
        )
    }

    // --- Sleep Timer ---
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return

        val totalMs = minutes * 60 * 1000L
        _sleepTimerRemainingMs.value = totalMs

        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1000L
                _sleepTimerRemainingMs.value = remaining
            }
            if (isActive) {
                mediaController?.pause()
                _sleepTimerRemainingMs.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
    }

    private fun audioTrackToMediaItem(track: AudioTrack): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.mediaUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun release() {
        stopPositionUpdates()
        cancelSleepTimer()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        scope.cancel()
    }
}
