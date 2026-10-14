package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiPlaylistRecommendation
import com.example.ai.ChatMessage
import com.example.ai.GeminiMusicAssistant
import com.example.data.local.NavaDatabase
import com.example.data.remote.MultiSourceMusicRepository
import com.example.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiDjUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConfigured: Boolean = false,
    val apiKeyError: String? = null,
    val lastGeneratedPlaylist: AiPlaylistRecommendation? = null
)

class AiDjViewModel(
    private val geminiAssistant: GeminiMusicAssistant,
    private val database: NavaDatabase,
    private val multiSourceRepository: MultiSourceMusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiDjUiState())
    val uiState: StateFlow<AiDjUiState> = _uiState.asStateFlow()

    init {
        checkConfiguration()
        addWelcomeMessage()
    }

    private fun checkConfiguration() {
        viewModelScope.launch {
            val configured = geminiAssistant.isConfigured()
            _uiState.update { it.copy(isConfigured = configured) }
        }
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            text = "سلام! من دی‌جی هوشمند نوا (AI DJ) هستم. می‌توانید از من بخواهید برای هر حال و هوا یا موقعیتی (ورزش، مطالعه، رانندگی شبانه، یا آهنگ‌های خاطره‌انگیز) با آهنگ‌های واقعی گوشی شما یا کاتالوگ آنلاین یک پلی‌لیست ویژه گلچین کنم.",
            isUser = false
        )
        _uiState.update { it.copy(messages = listOf(welcome)) }
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage(userText: String? = null) {
        val text = (userText ?: _uiState.value.inputText).trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(text = text, isUser = true)
        val currentMessages = _uiState.value.messages + userMessage
        _uiState.update { it.copy(messages = currentMessages, inputText = "", isLoading = true, apiKeyError = null) }

        viewModelScope.launch {
            try {
                // Determine if this is a playlist request or a chat query
                val isPlaylistRequest = text.contains("پلی‌لیست", ignoreCase = true) ||
                        text.contains("آهنگ", ignoreCase = true) ||
                        text.contains("playlist", ignoreCase = true) ||
                        text.contains("music", ignoreCase = true) ||
                        text.contains("آهنگ‌های", ignoreCase = true) ||
                        text.contains("پیشنهاد", ignoreCase = true) ||
                        text.contains("ورزش", ignoreCase = true) ||
                        text.contains("مطالعه", ignoreCase = true)

                if (isPlaylistRequest) {
                    // Gather candidate pool of REAL tracks from local database & popular online
                    val candidatePool = withContext(Dispatchers.IO) {
                        val localTracks = database.trackDao().getAllTracks().first().map { it.toAudioTrack() }
                        val onlineTracks = multiSourceRepository.getPopularTracks()
                        (localTracks + onlineTracks).distinctBy { it.id }
                    }

                    val recResult = geminiAssistant.generateRecommendation(text, candidatePool)
                    recResult.onSuccess { rec ->
                        val aiReply = ChatMessage(
                            text = "${rec.title}\n\n${rec.explanation}",
                            isUser = false,
                            attachedPlaylist = rec
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + aiReply,
                                isLoading = false,
                                lastGeneratedPlaylist = rec
                            )
                        }
                    }.onFailure { err ->
                        val errMsg = ChatMessage(
                            text = "متأسفانه نتوانستم پلی‌لیست را تولید کنم: ${err.localizedMessage}",
                            isUser = false
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + errMsg,
                                isLoading = false,
                                apiKeyError = err.localizedMessage
                            )
                        }
                    }
                } else {
                    // General conversation
                    val chatResult = geminiAssistant.chat(_uiState.value.messages, text)
                    chatResult.onSuccess { reply ->
                        val aiReply = ChatMessage(text = reply, isUser = false)
                        _uiState.update { it.copy(messages = it.messages + aiReply, isLoading = false) }
                    }.onFailure { err ->
                        val errMsg = ChatMessage(
                            text = "خطا در دریافت پاسخ: ${err.localizedMessage}",
                            isUser = false
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + errMsg,
                                isLoading = false,
                                apiKeyError = err.localizedMessage
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val errMsg = ChatMessage(
                    text = "خطای غیرمنتظره: ${e.localizedMessage}",
                    isUser = false
                )
                _uiState.update { it.copy(messages = it.messages + errMsg, isLoading = false) }
            }
        }
    }
}
