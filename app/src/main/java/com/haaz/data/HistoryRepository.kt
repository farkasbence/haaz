package com.haaz.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.haaz.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

private val Context.historyDataStore: DataStore<HistoryState> by dataStore(
    fileName = "history.json",
    serializer = HistorySerializer
)

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    val history: Flow<HistoryState> = context.historyDataStore.data.map { it }

    suspend fun addGeneratedClip(prompt: String, audioData: ByteArray) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        withContext(dispatcher) {
            val clipsDir = ensureClipsDir()
            val timestamp = System.currentTimeMillis()
            val fileName = "clip_${timestamp}.mp3"
            val file = File(clipsDir, fileName)
            file.outputStream().use { output -> output.write(audioData) }

            val newEntry = VoiceClipEntry(
                prompt = trimmed,
                fileName = fileName,
                createdAt = timestamp
            )

            var removedEntries: List<VoiceClipEntry> = emptyList()
            try {
                context.historyDataStore.updateData { current ->
                    val updatedPrompts = (current.prompts + trimmed).takeLast(MAX_PROMPTS)
                    val updatedClips = (current.voiceClips + newEntry).takeLast(MAX_CLIPS)
                    removedEntries = current.voiceClips.filter { existing ->
                        updatedClips.none { it.fileName == existing.fileName }
                    }
                    current.copy(prompts = updatedPrompts, voiceClips = updatedClips)
                }
            } catch (error: Exception) {
                file.delete()
                throw error
            }

            removedEntries.forEach { entry ->
                File(clipsDir, entry.fileName).delete()
            }
        }
    }

    suspend fun clear() {
        withContext(dispatcher) {
            val clipsDir = ensureClipsDir()
            context.historyDataStore.updateData { HistoryState() }
            clipsDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun getClipFile(fileName: String): File = File(ensureClipsDir(), fileName)

    private fun ensureClipsDir(): File {
        val dir = File(context.filesDir, CLIPS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private companion object {
        const val MAX_PROMPTS = 20
        const val MAX_CLIPS = 5
        const val CLIPS_DIR_NAME = "voice_clips"
    }
}

data class HistoryState(
    val prompts: List<String> = emptyList(),
    val voiceClips: List<VoiceClipEntry> = emptyList()
)

data class VoiceClipEntry(
    val prompt: String,
    val fileName: String,
    val createdAt: Long
)

private object HistorySerializer : Serializer<HistoryState> {
    override val defaultValue: HistoryState = HistoryState()

    override suspend fun readFrom(input: InputStream): HistoryState = runCatching {
        val content = input.bufferedReader().use { it.readText() }
        if (content.isBlank()) return defaultValue
        val json = JSONTokener(content).nextValue()
        when (json) {
            is JSONArray -> {
                val length = json.length()
                val prompts = buildList(length) {
                    for (index in 0 until length) {
                        add(json.optString(index))
                    }
                }
                HistoryState(prompts = prompts)
            }
            is JSONObject -> {
                val promptsArray = json.optJSONArray("prompts") ?: JSONArray()
                val clipsArray = json.optJSONArray("voice_clips") ?: JSONArray()
                val prompts = buildList(promptsArray.length()) {
                    for (index in 0 until promptsArray.length()) {
                        add(promptsArray.optString(index))
                    }
                }
                val clips = buildList(clipsArray.length()) {
                    for (index in 0 until clipsArray.length()) {
                        val clipJson = clipsArray.optJSONObject(index) ?: continue
                        add(
                            VoiceClipEntry(
                                prompt = clipJson.optString("prompt"),
                                fileName = clipJson.optString("file_name"),
                                createdAt = clipJson.optLong("created_at")
                            )
                        )
                    }
                }
                HistoryState(prompts = prompts, voiceClips = clips)
            }
            else -> defaultValue
        }
    }.getOrElse { exception ->
        if (exception is JSONException) throw CorruptionException("Cannot read history JSON", exception)
        else throw exception
    }

    override suspend fun writeTo(t: HistoryState, output: OutputStream) {
        val json = JSONObject()
        val promptsArray = JSONArray()
        t.prompts.forEach { promptsArray.put(it) }
        val clipsArray = JSONArray()
        t.voiceClips.forEach { clip ->
            val clipJson = JSONObject()
            clipJson.put("prompt", clip.prompt)
            clipJson.put("file_name", clip.fileName)
            clipJson.put("created_at", clip.createdAt)
            clipsArray.put(clipJson)
        }
        json.put("prompts", promptsArray)
        json.put("voice_clips", clipsArray)
        output.bufferedWriter().use { writer ->
            writer.write(json.toString())
        }
    }
}
