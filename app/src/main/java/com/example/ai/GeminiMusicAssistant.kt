package com.example.ai

import com.example.BuildConfig
import com.example.data.local.UserPreferencesRepository
import com.example.model.AudioTrack
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class AiPlaylistRecommendation(
    val title: String,
    val explanation: String,
    val recommendedTracks: List<AudioTrack>
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedPlaylist: AiPlaylistRecommendation? = null
)

class GeminiMusicAssistant(
    private val preferencesRepository: UserPreferencesRepository
) {

    // 60-second timeouts as required by gemini-api skill
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getEffectiveApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_GEMINI_API_KEY") key.trim() else ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun isConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    suspend fun generateRecommendation(
        userPrompt: String,
        candidatePool: List<AudioTrack>
    ): Result<AiPlaylistRecommendation> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("کلید API جمینای تنظیم نشده است. لطفاً کلید خود را در پنل Secrets یا تنظیمات برنامه وارد کنید.")
            )
        }

        if (candidatePool.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("هیچ آهنگ کاندیدی در کتابخانه یا کاتالوگ آنلاین برای انتخاب وجود ندارد.")
            )
        }

        val candidatesListText = candidatePool.take(50).joinToString("\n") {
            "ID: ${it.id} | Title: ${it.title} | Artist: ${it.artist} | Source: ${it.sourceType}"
        }

        val systemPrompt = """
            You are the "AI DJ" and music curator for Nava (نوا), an Android music player app.
            Your role is to select the most suitable tracks from the provided CANDIDATE POOL to fulfill the user's mood or request.
            CRITICAL RULES:
            1. ONLY choose track IDs that exist in the CANDIDATE POOL provided below. NEVER invent or fabricate track IDs or titles.
            2. Choose between 3 and 15 matching tracks.
            3. Provide a creative Persian playlist title and a friendly 1-2 sentence Persian introduction/explanation of why these songs match the mood.
            
            Format your response strictly as follows:
            TITLE: <Playlist Title in Persian>
            COMMENT: <Curator introduction in Persian>
            TRACKS: <comma-separated list of selected track IDs exactly as listed in the candidates>
        """.trimIndent()

        val userMessage = """
            User Request: "$userPrompt"
            
            CANDIDATE POOL:
            $candidatesListText
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userMessage)), role = "user")
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.5f)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return@withContext Result.failure(Exception("پاسخی از هوش مصنوعی دریافت نشد."))

                val candidateMap = candidatePool.associateBy { it.id }

                var title = "پلی‌لیست پیشنهادی دی‌جی"
                var comment = "این پلی‌لیست متناسب با درخواست شما انتخاب شده است."
                val selectedTracks = mutableListOf<AudioTrack>()

                text.lines().forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("TITLE:", ignoreCase = true) -> {
                            val t = trimmed.substringAfter(":").trim()
                            if (t.isNotBlank()) title = t
                        }
                        trimmed.startsWith("COMMENT:", ignoreCase = true) -> {
                            val c = trimmed.substringAfter(":").trim()
                            if (c.isNotBlank()) comment = c
                        }
                        trimmed.startsWith("TRACKS:", ignoreCase = true) -> {
                            val ids = trimmed.substringAfter(":").split(",")
                            ids.map { it.trim() }.forEach { id ->
                                candidateMap[id]?.let { selectedTracks.add(it) }
                            }
                        }
                    }
                }

                // If no tracks were parsed via tags, try regex matching candidate IDs
                if (selectedTracks.isEmpty()) {
                    candidateMap.forEach { (id, track) ->
                        if (text.contains(id)) {
                            selectedTracks.add(track)
                        }
                    }
                }

                if (selectedTracks.isEmpty()) {
                    // Fallback to top candidates if parsing failed to extract IDs
                    selectedTracks.addAll(candidatePool.take(5))
                }

                Result.success(
                    AiPlaylistRecommendation(
                        title = title,
                        explanation = comment,
                        recommendedTracks = selectedTracks.distinctBy { it.id }
                    )
                )
            } else {
                Result.failure(Exception("خطا در ارتباط با جمینای: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(
        conversation: List<ChatMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("کلید API جمینای تنظیم نشده است.")
            )
        }

        val systemPrompt = """
            You are the friendly and knowledgeable AI DJ music assistant in the Nava music player app.
            You speak natural, warm Persian (and English if spoken to in English).
            You help users discover songs, understand music genres, explore Persian and international music, and create listening vibes.
            Keep responses concise, polite, and helpful (max 3-4 sentences unless in-depth explanation is requested).
        """.trimIndent()

        val contents = mutableListOf<GeminiContent>()
        conversation.takeLast(6).forEach { msg ->
            contents.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = msg.text)),
                    role = if (msg.isUser) "user" else "model"
                )
            )
        }
        contents.add(
            GeminiContent(
                parts = listOf(GeminiPart(text = userMessage)),
                role = "user"
            )
        )

        val request = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val reply = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "پاسخی دریافت نشد."
                Result.success(reply.trim())
            } else {
                Result.failure(Exception("خطا: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun expandQuery(query: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) return@withContext emptyList()

        val prompt = "Give up to 3 alternative search keywords or Persian/English transliterations for music search: \"$query\". Output only comma-separated terms without formatting."
        val request = GeminiGenerateRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                text.split(",").map { it.trim().trim('"', '\'') }.filter { it.isNotBlank() }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
