package com.example.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {

    @GET("tracks/")
    suspend fun getPopularTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("order") order: String = "popularity_week",
        @Query("include") include: String = "musicinfo",
        @Query("audioformat") audioFormat: String = "mp32"
    ): Response<JamendoApiResponse>

    @GET("tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("search") search: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("order") order: String = "popularity_total",
        @Query("include") include: String = "musicinfo",
        @Query("audioformat") audioFormat: String = "mp32"
    ): Response<JamendoApiResponse>

    @GET("tracks/")
    suspend fun searchByArtist(
        @Query("client_id") clientId: String,
        @Query("artist_name") artistName: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("include") include: String = "musicinfo",
        @Query("audioformat") audioFormat: String = "mp32"
    ): Response<JamendoApiResponse>
}
