package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.media.LocalMusicScanner
import com.example.data.remote.JamendoMusicSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: String = "fa",
    val theme: String = "dark",
    val jamendoClientId: String = "",
    val effectiveJamendoClientId: String = "",
    val isScanning: Boolean = false,
    val scanSuccessMessage: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val scanner: LocalMusicScanner,
    private val musicSource: JamendoMusicSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.languageFlow,
                preferencesRepository.themeFlow,
                preferencesRepository.jamendoClientIdFlow
            ) { lang, theme, clientId ->
                Triple(lang, theme, clientId)
            }.collect { (lang, theme, clientId) ->
                val effective = musicSource.getEffectiveClientId()
                _uiState.update {
                    it.copy(
                        language = lang,
                        theme = theme,
                        jamendoClientId = clientId,
                        effectiveJamendoClientId = effective
                    )
                }
            }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }

    fun saveJamendoClientId(clientId: String) {
        viewModelScope.launch {
            preferencesRepository.setJamendoClientId(clientId.trim())
            val effective = musicSource.getEffectiveClientId()
            _uiState.update { it.copy(effectiveJamendoClientId = effective) }
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanSuccessMessage = false) }
            scanner.scanDeviceAudio()
            _uiState.update { it.copy(isScanning = false, scanSuccessMessage = true) }
        }
    }

    fun clearScanSuccess() {
        _uiState.update { it.copy(scanSuccessMessage = false) }
    }
}
