package com.haaz.data

import com.haaz.data.network.TextToSpeechEndpoint
import com.haaz.data.network.TextToSpeechRequest
import com.haaz.data.network.VoiceSearchResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeTextToSpeechEndpoint : TextToSpeechEndpoint {
    var streamResponse: Response<ResponseBody> = Response.success(
        "audio".toResponseBody("audio/mpeg".toMediaType())
    )
    var voicesResponse: VoiceSearchResponse = VoiceSearchResponse()
    var lastVoiceId: String? = null
    var lastRequest: TextToSpeechRequest? = null
    var voiceCalls: Int = 0

    override suspend fun streamSpeech(
        apiKey: String,
        voiceId: String,
        request: TextToSpeechRequest
    ): Response<ResponseBody> {
        lastVoiceId = voiceId
        lastRequest = request
        return streamResponse
    }

    override suspend fun searchVoices(apiKey: String, pageSize: Int): VoiceSearchResponse {
        voiceCalls += 1
        return voicesResponse
    }
}
