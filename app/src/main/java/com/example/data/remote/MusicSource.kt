package com.example.data.remote

import com.example.model.AudioTrack

sealed class OnlineResult<out T> {
    data class Success<out T>(val data: T) : OnlineResult<T>()
    data class Error(val message: String, val isApiKeyMissing: Boolean = false) : OnlineResult<Nothing>()
    data object Loading : OnlineResult<Nothing>()
}

interface MusicSource {
    val providerName: String
    suspend fun getFeaturedTracks(): OnlineResult<List<AudioTrack>>
    suspend fun searchTracks(query: String): OnlineResult<List<AudioTrack>>
    suspend fun isConfigured(): Boolean
}
