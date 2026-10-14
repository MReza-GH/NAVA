package com.example

import android.app.Application
import com.example.ai.GeminiMusicAssistant
import com.example.data.local.NavaDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.media.LocalMusicScanner
import com.example.data.media.NavaDownloadManager
import com.example.data.remote.AppleMusicCatalogSource
import com.example.data.remote.JamendoMusicSource
import com.example.data.remote.MultiSourceMusicRepository
import com.example.playback.PlaybackManager

class NavaApplication : Application() {

    val database by lazy { NavaDatabase.getInstance(this) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val localMusicScanner by lazy { LocalMusicScanner(this, database) }
    val downloadManager by lazy { NavaDownloadManager(this, database) }
    val musicSource by lazy { JamendoMusicSource(this, preferencesRepository) }
    val appleMusicSource by lazy { AppleMusicCatalogSource() }
    val multiSourceRepository by lazy { MultiSourceMusicRepository(musicSource, appleMusicSource) }
    val playbackManager by lazy { PlaybackManager(this, database, preferencesRepository) }
    val geminiAssistant by lazy { GeminiMusicAssistant(preferencesRepository) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NavaApplication
            private set
    }
}
