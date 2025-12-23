package com.haaz.data

import com.haaz.BuildConfig
import com.haaz.data.network.VoiceDto
import com.haaz.data.network.VoiceSearchResponse
import com.haaz.domain.TtsModel
import com.haaz.domain.Voice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TextToSpeechRepositoryTest {

    @Test
    fun generateSpeech_success_buildsRequestWithDefaults() = runTest {
        assumeTrue(BuildConfig.ELEVENLABS_API_KEY.isNotBlank())
        val fakeApi = FakeTextToSpeechEndpoint().apply {
            streamResponse = Response.success(
                "audio".toResponseBody("audio/mpeg".toMediaType())
            )
        }
        val repo = TextToSpeechRepository(fakeApi, UnconfinedTestDispatcher())
        val settings = TtsSettings(
            model = TtsModel.MultilingualV2,
            voiceId = null,
            speed = 1.2f,
            stability = 0.7f
        )

        val result = repo.generateSpeech("Hello", settings)

        assertTrue(result.isSuccess)
        assertEquals("audio", String(result.getOrThrow(), Charsets.UTF_8))
        assertEquals("IKne3meq5aSn9XLyUdCD", fakeApi.lastVoiceId)
        assertEquals("Hello", fakeApi.lastRequest?.text)
        assertEquals(settings.model.id, fakeApi.lastRequest?.modelId)
        assertEquals(settings.speed, fakeApi.lastRequest?.voiceSettings?.speed)
        assertEquals(settings.stability, fakeApi.lastRequest?.voiceSettings?.stability)
    }

    @Test
    fun generateSpeech_httpError_returnsFailure() = runTest {
        assumeTrue(BuildConfig.ELEVENLABS_API_KEY.isNotBlank())
        val fakeApi = FakeTextToSpeechEndpoint().apply {
            streamResponse = Response.error(
                500,
                "error".toResponseBody("text/plain".toMediaType())
            )
        }
        val repo = TextToSpeechRepository(fakeApi, UnconfinedTestDispatcher())

        val result = repo.generateSpeech("Hello", TtsSettings())

        assertTrue(result.isFailure)
        assertEquals("Text-to-speech failed (500)", result.exceptionOrNull()?.message)
    }

    @Test
    fun fetchVoices_usesCacheWhenNotForced() = runTest {
        assumeTrue(BuildConfig.ELEVENLABS_API_KEY.isNotBlank())
        val fakeApi = FakeTextToSpeechEndpoint().apply {
            voicesResponse = VoiceSearchResponse(
                voices = listOf(
                    VoiceDto("voice-1", "Alpha", labels = mapOf("descriptive" to "Warm")),
                    VoiceDto("voice-2", "Beta", labels = emptyMap())
                )
            )
        }
        val repo = TextToSpeechRepository(fakeApi, UnconfinedTestDispatcher())

        val first = repo.fetchVoices()
        val second = repo.fetchVoices()

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(1, fakeApi.voiceCalls)
        assertEquals(
            listOf(
                Voice(id = "voice-1", name = "Alpha", descriptor = "Warm"),
                Voice(id = "voice-2", name = "Beta", descriptor = null)
            ),
            first.getOrThrow()
        )
    }

    @Test
    fun fetchVoices_forceRefresh_bypassesCache() = runTest {
        assumeTrue(BuildConfig.ELEVENLABS_API_KEY.isNotBlank())
        val fakeApi = FakeTextToSpeechEndpoint().apply {
            voicesResponse = VoiceSearchResponse(
                voices = listOf(VoiceDto("voice-1", "Alpha", labels = emptyMap()))
            )
        }
        val repo = TextToSpeechRepository(fakeApi, UnconfinedTestDispatcher())

        val first = repo.fetchVoices()
        fakeApi.voicesResponse = VoiceSearchResponse(
            voices = listOf(VoiceDto("voice-2", "Beta", labels = emptyMap()))
        )
        val second = repo.fetchVoices(forceRefresh = true)

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(2, fakeApi.voiceCalls)
        assertEquals("voice-2", second.getOrThrow().first().id)
    }
}
