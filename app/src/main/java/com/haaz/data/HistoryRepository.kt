package com.haaz.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.haaz.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
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

private val Context.historyDataStore: DataStore<List<String>> by dataStore(
    fileName = "history.json",
    serializer = HistorySerializer
)

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    val history: Flow<List<String>> = context.historyDataStore.data.map { it }

    suspend fun addEntry(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        withContext(dispatcher) {
            context.historyDataStore.updateData { current ->
                (current + trimmed).takeLast(MAX_ENTRIES)
            }
        }
    }

    suspend fun clear() {
        withContext(dispatcher) {
            context.historyDataStore.updateData { emptyList() }
        }
    }

    private companion object {
        const val MAX_ENTRIES = 50
    }
}

private object HistorySerializer : Serializer<List<String>> {
    override val defaultValue: List<String> = emptyList()

    override suspend fun readFrom(input: InputStream): List<String> = runCatching {
        val content = input.bufferedReader().use { it.readText() }
        if (content.isBlank()) return defaultValue
        val jsonArray = JSONArray(content)
        val length = jsonArray.length()
        buildList(length) {
            for (index in 0 until length) {
                add(jsonArray.optString(index))
            }
        }
    }.getOrElse { exception ->
        if (exception is JSONException) throw CorruptionException("Cannot read history JSON", exception)
        else throw exception
    }

    override suspend fun writeTo(t: List<String>, output: OutputStream) {
        val array = JSONArray()
        t.forEach { array.put(it) }
        output.bufferedWriter().use { writer ->
            writer.write(array.toString())
        }
    }
}
