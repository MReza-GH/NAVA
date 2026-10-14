package com.example.data.remote

import com.example.model.AudioTrack
import com.example.model.TrackSourceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    @param:Json(name = "resultCount") val resultCount: Int?,
    @param:Json(name = "results") val results: List<ItunesTrackDto>?
)

@JsonClass(generateAdapter = true)
data class ItunesTrackDto(
    @param:Json(name = "trackId") val trackId: Long?,
    @param:Json(name = "trackName") val trackName: String?,
    @param:Json(name = "artistName") val artistName: String?,
    @param:Json(name = "collectionName") val collectionName: String?,
    @param:Json(name = "previewUrl") val previewUrl: String?,
    @param:Json(name = "artworkUrl100") val artworkUrl100: String?,
    @param:Json(name = "artworkUrl60") val artworkUrl60: String?,
    @param:Json(name = "trackTimeMillis") val trackTimeMillis: Long?,
    @param:Json(name = "trackViewUrl") val trackViewUrl: String?,
    @param:Json(name = "primaryGenreName") val primaryGenreName: String?
) {
    fun toAudioTrack(): AudioTrack? {
        val id = trackId ?: return null
        val title = trackName ?: "Unknown Track"
        val artist = artistName ?: "Unknown Artist"
        val album = collectionName ?: "Single"
        val duration = trackTimeMillis ?: 30000L
        val streamUrl = previewUrl ?: ""

        // High resolution artwork by replacing 100x100 with 600x600
        val artwork = artworkUrl100?.replace("100x100", "600x600") ?: artworkUrl60

        return AudioTrack(
            id = "itunes_$id",
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            mediaUri = streamUrl,
            artworkUri = artwork,
            sourceType = TrackSourceType.ONLINE_APPLE_PREVIEW,
            license = "Apple Music Preview (30s)",
            downloadUrl = null,
            isDownloadAllowed = false,
            externalUrl = trackViewUrl,
            providerName = "Apple Music",
            isPreviewOnly = true
        )
    }
}

interface ItunesApiService {
    @GET("search")
    suspend fun searchSongs(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25
    ): Response<ItunesSearchResponse>
}

class AppleMusicCatalogSource : MusicSource {

    override val providerName: String = "Apple Music"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val apiService: ItunesApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ItunesApiService::class.java)
    }

    override suspend fun isConfigured(): Boolean = true

    override suspend fun getFeaturedTracks(): OnlineResult<List<AudioTrack>> {
        // Query featured popular music
        return searchTracks("Persian Pop")
    }

    override suspend fun searchTracks(query: String): OnlineResult<List<AudioTrack>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext OnlineResult.Success(emptyList())
        }

        try {
            val response = apiService.searchSongs(term = trimmed)
            if (response.isSuccessful) {
                val results = response.body()?.results?.mapNotNull { it.toAudioTrack() } ?: emptyList()
                OnlineResult.Success(results)
            } else {
                OnlineResult.Error("Apple Catalog API error: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            OnlineResult.Error("Apple Catalog search failed: ${e.localizedMessage ?: "Network error"}")
        }
    }
}
