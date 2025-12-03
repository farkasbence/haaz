package com.haaz.network

import com.squareup.moshi.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Query
import retrofit2.http.Header

interface TextToSpeechEndpoint {
    @Streaming
    @Headers("Accept: audio/mpeg")
    @POST("v1/text-to-speech/{voice_id}/stream")
    suspend fun streamSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String,
        @Body request: TextToSpeechRequest
    ): Response<ResponseBody>

    @Headers("Accept: application/json")
    @GET("v2/voices")
    suspend fun searchVoices(
        @Header("xi-api-key") apiKey: String,
        @Query("page_size") pageSize: Int = 10
    ): VoiceSearchResponse
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

data class VoiceSearchResponse(
    @Json(name = "voices") val voices: List<VoiceDto> = emptyList(),
)

data class VoiceDto(
    @Json(name = "voice_id") val voiceId: String,
    @Json(name = "name") val name: String,
    @Json(name = "labels") val labels: Map<String, String>? = null,
)
