package com.haaz.data

import com.haaz.BuildConfig
import com.haaz.di.IoDispatcher
import com.haaz.domain.Voice
import com.haaz.network.TextToSpeechEndpoint
import com.haaz.network.TextToSpeechRequest
import com.haaz.network.VoiceSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechDataSource @Inject constructor(
    private val api: TextToSpeechEndpoint,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val cachedVoices = MutableStateFlow<List<Voice>>(emptyList())

    suspend fun generateSpeech(prompt: String, settings: TtsSettings): Result<ByteArray> = withContext(dispatcher) {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Missing ElevenLabs API Key."))
        }

        runCatching {
            val response = api.streamSpeech(
                apiKey = apiKey,
                voiceId = settings.voiceId ?: DEFAULT_VOICE_ID,
                request = TextToSpeechRequest(
                    text = prompt,
                    modelId = settings.model.id,
                    voiceSettings = VoiceSettings(
                        stability = settings.stability,
                        speed = settings.speed
                    )
                )
            )

            if (!response.isSuccessful) {
                val message = "Text-to-speech failed (${response.code()})"
                throw IllegalStateException(message)
            }

            val body = response.body() ?: error("Empty audio response")
            body.bytes()
        }
    }

    suspend fun fetchVoices(forceRefresh: Boolean = false): Result<List<Voice>> = withContext(dispatcher) {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Missing ElevenLabs API Key."))
        }

        if (!forceRefresh && cachedVoices.value.isNotEmpty()) {
            return@withContext Result.success(cachedVoices.value)
        }

        runCatching {
            val response = api.searchVoices(pageSize = 10, apiKey = apiKey)
            val voices = response.voices.map { dto ->
                Voice(
                    id = dto.voiceId,
                    name = dto.name,
                    descriptor = dto.labels?.get("descriptive"),
                )
            }
            cachedVoices.value = voices
            voices
        }
    }

    companion object {
        private const val DEFAULT_VOICE_ID = "IKne3meq5aSn9XLyUdCD"
    }
}
