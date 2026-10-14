package com.example.model

enum class RepeatState {
    OFF,
    ALL,
    ONE
}

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleModeEnabled: Boolean = false,
    val repeatState: RepeatState = RepeatState.OFF,
    val queue: List<AudioTrack> = emptyList(),
    val queueIndex: Int = -1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
