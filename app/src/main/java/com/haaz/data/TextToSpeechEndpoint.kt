package com.haaz.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ElevenLabsApi {
    @Streaming
    @Headers("Accept: audio/mpeg")
    @POST("v1/text-to-speech/{voice_id}/stream")
    suspend fun streamSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String,
        @Body request: TextToSpeechRequest
    ): Response<ResponseBody>

    companion object {
        fun create(): ElevenLabsApi {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.elevenlabs.io/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ElevenLabsApi::class.java)
        }
    }
}

data class TextToSpeechRequest(
    @Json(name = "text") val text: String,
    @Json(name = "model_id") val modelId: String = "eleven_turbo_v2",
    @Json(name = "output_format") val outputFormat: String = "mp3_44100_128",
    @Json(name = "voice_settings") val voiceSettings: VoiceSettings,
)

data class VoiceSettings(
    @Json(name = "stability") val stability: Float,
    @Json(name = "speed") val speed: Float,
)
