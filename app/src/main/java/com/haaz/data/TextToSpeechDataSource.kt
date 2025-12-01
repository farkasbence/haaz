package com.haaz.data

import com.haaz.BuildConfig
import com.haaz.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechDataSource @Inject constructor(
    private val api: TextToSpeechEndpoint,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    suspend fun generateSpeech(prompt: String, settings: TtsSettings): Result<ByteArray> = withContext(dispatcher) {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Missing ElevenLabs API Key."))
        }

        runCatching {
            val response = api.streamSpeech(
                apiKey = apiKey,
                voiceId = DEFAULT_VOICE_ID,
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

    companion object {
        // TODO: add support for more voice models.
        private const val DEFAULT_VOICE_ID = "IKne3meq5aSn9XLyUdCD"
    }
}
