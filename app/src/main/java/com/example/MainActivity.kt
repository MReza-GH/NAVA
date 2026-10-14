package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.NavaMainScreen
import com.example.ui.theme.NavaTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NavaApplication

        setContent {
            val mainViewModel: MainViewModel = viewModel {
                MainViewModel(
                    playbackManager = app.playbackManager,
                    database = app.database,
                    preferencesRepository = app.preferencesRepository,
                    downloadManager = app.downloadManager
                )
            }
            val homeViewModel: HomeViewModel = viewModel {
                HomeViewModel(
                    database = app.database,
                    musicSource = app.musicSource
                )
            }
            val onlineViewModel: OnlineViewModel = viewModel {
                OnlineViewModel(
                    multiSourceRepository = app.multiSourceRepository,
                    downloadManager = app.downloadManager,
                    geminiAssistant = app.geminiAssistant
                )
            }
            val libraryViewModel: LibraryViewModel = viewModel {
                LibraryViewModel(
                    database = app.database,
                    scanner = app.localMusicScanner
                )
            }
            val downloadsViewModel: DownloadsViewModel = viewModel {
                DownloadsViewModel(
                    database = app.database,
                    downloadManager = app.downloadManager
                )
            }
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(
                    preferencesRepository = app.preferencesRepository,
                    scanner = app.localMusicScanner,
                    musicSource = app.musicSource
                )
            }
            val aiDjViewModel: AiDjViewModel = viewModel {
                AiDjViewModel(
                    geminiAssistant = app.geminiAssistant,
                    database = app.database,
                    multiSourceRepository = app.multiSourceRepository
                )
            }

            val mainUiState by mainViewModel.uiState.collectAsState()

            NavaTheme(themePreference = mainUiState.theme) {
                NavaMainScreen(
                    mainViewModel = mainViewModel,
                    homeViewModel = homeViewModel,
                    onlineViewModel = onlineViewModel,
                    libraryViewModel = libraryViewModel,
                    downloadsViewModel = downloadsViewModel,
                    settingsViewModel = settingsViewModel,
                    aiDjViewModel = aiDjViewModel
                )
            }
        }
    }
}
