package com.example.data.remote

import android.net.Uri
import com.example.model.AudioTrack
import com.example.model.TrackSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

data class ExternalServiceLink(
    val serviceName: String,
    val query: String,
    val webUrl: String,
    val appIntentUri: String
)

data class MultiSourceSearchResult(
    val tracks: List<AudioTrack>,
    val externalLinks: List<ExternalServiceLink>,
    val activeProviders: List<String>,
    val errors: List<String>
)

class MultiSourceMusicRepository(
    val jamendoSource: JamendoMusicSource,
    val appleMusicSource: AppleMusicCatalogSource
) {

    suspend fun searchAllSources(query: String): MultiSourceSearchResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext MultiSourceSearchResult(
                tracks = emptyList(),
                externalLinks = emptyList(),
                activeProviders = emptyList(),
                errors = emptyList()
            )
        }

        // Generate normalized & transliterated queries (Persian <-> Latin)
        val queryVariants = PersianSearchNormalizer.generateSearchQueries(trimmed)
        val primaryQuery = queryVariants.firstOrNull() ?: trimmed
        val alternativeQuery = queryVariants.getOrNull(1)

        val errors = mutableListOf<String>()
        val allTracks = mutableListOf<AudioTrack>()
        val activeProviders = mutableListOf<String>()

        // Execute in parallel
        val jamendoDeferred = async {
            val res1 = jamendoSource.searchTracks(primaryQuery)
            if (res1 is OnlineResult.Success && res1.data.isNotEmpty()) {
                res1
            } else if (alternativeQuery != null) {
                jamendoSource.searchTracks(alternativeQuery)
            } else res1
        }

        val appleDeferred = async {
            val res1 = appleMusicSource.searchTracks(primaryQuery)
            if (res1 is OnlineResult.Success && res1.data.isNotEmpty()) {
                res1
            } else if (alternativeQuery != null) {
                // If primary query returned few results, try alternative query
                val res2 = appleMusicSource.searchTracks(alternativeQuery)
                if (res2 is OnlineResult.Success && res1 is OnlineResult.Success) {
                    OnlineResult.Success((res1.data + res2.data).distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" })
                } else res2
            } else res1
        }

        // Collect Jamendo result
        when (val jamendoRes = jamendoDeferred.await()) {
            is OnlineResult.Success -> {
                allTracks.addAll(jamendoRes.data)
                activeProviders.add(jamendoSource.providerName)
            }
            is OnlineResult.Error -> {
                errors.add("${jamendoSource.providerName}: ${jamendoRes.message}")
            }
            else -> {}
        }

        // Collect Apple Music result
        when (val appleRes = appleDeferred.await()) {
            is OnlineResult.Success -> {
                allTracks.addAll(appleRes.data)
                activeProviders.add(appleMusicSource.providerName)
            }
            is OnlineResult.Error -> {
                errors.add("${appleMusicSource.providerName}: ${appleRes.message}")
            }
            else -> {}
        }

        // Deduplicate tracks by normalized title + artist
        val deduplicatedTracks = allTracks.distinctBy {
            val normTitle = PersianSearchNormalizer.normalize(it.title).lowercase()
            val normArtist = PersianSearchNormalizer.normalize(it.artist).lowercase()
            "$normTitle##$normArtist"
        }

        // Generate official external service links
        val encodedQuery = Uri.encode(trimmed)
        val externalLinks = listOf(
            ExternalServiceLink(
                serviceName = "Spotify",
                query = trimmed,
                webUrl = "https://open.spotify.com/search/$encodedQuery",
                appIntentUri = "spotify:search:$encodedQuery"
            ),
            ExternalServiceLink(
                serviceName = "YouTube Music",
                query = trimmed,
                webUrl = "https://music.youtube.com/search?q=$encodedQuery",
                appIntentUri = "https://music.youtube.com/search?q=$encodedQuery"
            ),
            ExternalServiceLink(
                serviceName = "Apple Music",
                query = trimmed,
                webUrl = "https://music.apple.com/search?term=$encodedQuery",
                appIntentUri = "https://music.apple.com/search?term=$encodedQuery"
            )
        )

        MultiSourceSearchResult(
            tracks = deduplicatedTracks,
            externalLinks = externalLinks,
            activeProviders = activeProviders,
            errors = errors
        )
    }

    suspend fun getPopularTracks(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val all = mutableListOf<AudioTrack>()

        val jamendoDeferred = async { jamendoSource.getFeaturedTracks() }
        val appleDeferred = async { appleMusicSource.searchTracks("Popular Iranian Pop") }

        when (val res = jamendoDeferred.await()) {
            is OnlineResult.Success -> all.addAll(res.data)
            else -> {}
        }

        when (val res = appleDeferred.await()) {
            is OnlineResult.Success -> all.addAll(res.data)
            else -> {}
        }

        all.distinctBy {
            val normTitle = PersianSearchNormalizer.normalize(it.title).lowercase()
            val normArtist = PersianSearchNormalizer.normalize(it.artist).lowercase()
            "$normTitle##$normArtist"
        }
    }
}
