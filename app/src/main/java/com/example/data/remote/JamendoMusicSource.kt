package com.example.data.remote

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.UserPreferencesRepository
import com.example.model.AudioTrack
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class JamendoMusicSource(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : MusicSource {

    override val providerName: String = "Jamendo Music"

    // Official Jamendo API Base URL
    private val baseUrl = "https://api.jamendo.com/v3.0/"

    // Standard public sandbox Client ID for Jamendo open catalog access
    private val defaultPublicClientId = "c45a68d0"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val apiService: JamendoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JamendoApiService::class.java)
    }

    suspend fun getEffectiveClientId(): String {
        // 1. Check user configured ID in DataStore
        val userSavedId = preferencesRepository.jamendoClientIdFlow.first().trim()
        if (userSavedId.isNotBlank()) return userSavedId

        // 2. Check BuildConfig.JAMENDO_CLIENT_ID from secrets/env if available
        try {
            val buildConfigField = BuildConfig::class.java.getField("JAMENDO_CLIENT_ID")
            val buildConfigId = buildConfigField.get(null) as? String
            if (!buildConfigId.isNullOrBlank()) return buildConfigId.trim()
        } catch (_: Exception) {
            // Field not present in BuildConfig
        }

        // 3. Fallback to default public open client ID
        return defaultPublicClientId
    }

    override suspend fun isConfigured(): Boolean {
        val key = getEffectiveClientId()
        return key.isNotBlank()
    }

    override suspend fun getFeaturedTracks(): OnlineResult<List<AudioTrack>> {
        val clientId = getEffectiveClientId()
        if (clientId.isBlank()) {
            return OnlineResult.Error("Jamendo Client ID is not configured.", isApiKeyMissing = true)
        }

        return try {
            val response = apiService.getPopularTracks(clientId = clientId)
            if (response.isSuccessful) {
                val body = response.body()
                val tracks = body?.results?.map { it.toAudioTrack() } ?: emptyList()
                if (tracks.isNotEmpty()) {
                    OnlineResult.Success(tracks)
                } else {
                    val errorMsg = body?.headers?.errorMessage
                    if (!errorMsg.isNullOrBlank() && errorMsg.contains("client", ignoreCase = true)) {
                        OnlineResult.Error("Invalid Jamendo Client ID: $errorMsg", isApiKeyMissing = true)
                    } else {
                        OnlineResult.Success(emptyList())
                    }
                }
            } else {
                val code = response.code()
                if (code == 401 || code == 403) {
                    OnlineResult.Error("Authentication failed with Jamendo API ($code)", isApiKeyMissing = true)
                } else {
                    OnlineResult.Error("Server error from Jamendo: HTTP $code")
                }
            }
        } catch (e: Exception) {
            OnlineResult.Error("Network error: ${e.localizedMessage ?: "Failed to reach Jamendo"}")
        }
    }

    override suspend fun searchTracks(query: String): OnlineResult<List<AudioTrack>> {
        val clientId = getEffectiveClientId()
        if (clientId.isBlank()) {
            return OnlineResult.Error("Jamendo Client ID is not configured.", isApiKeyMissing = true)
        }
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return getFeaturedTracks()
        }

        return try {
            val response = apiService.searchTracks(clientId = clientId, search = trimmed)
            if (response.isSuccessful) {
                val body = response.body()
                val tracks = body?.results?.map { it.toAudioTrack() } ?: emptyList()
                OnlineResult.Success(tracks)
            } else {
                val code = response.code()
                if (code == 401 || code == 403) {
                    OnlineResult.Error("Authentication failed ($code)", isApiKeyMissing = true)
                } else {
                    OnlineResult.Error("Search error: HTTP $code")
                }
            }
        } catch (e: Exception) {
            OnlineResult.Error("Network error: ${e.localizedMessage ?: "Search failed"}")
        }
    }
}
