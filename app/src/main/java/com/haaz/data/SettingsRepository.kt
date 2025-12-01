package com.haaz.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.haaz.domain.TtsModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "tts_settings")

data class TtsSettings(
    val model: TtsModel = TtsModel.MultilingualV2,
    val speed: Float = 1.0f,
    val stability: Float = 0.5f
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val modelKey = stringPreferencesKey("model_id")
    private val speedKey = floatPreferencesKey("speed")
    private val stabilityKey = floatPreferencesKey("stability")

    val settings: Flow<TtsSettings> = context.dataStore.data.map { prefs ->
        TtsSettings(
            model = TtsModel.fromId(prefs[modelKey]),
            speed = prefs[speedKey] ?: 1.0f,
            stability = prefs[stabilityKey] ?: 0.5f,
        )
    }

    suspend fun save(settings: TtsSettings) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[modelKey] = settings.model.id
            prefs[speedKey] = settings.speed
            prefs[stabilityKey] = settings.stability
        }
    }

    suspend fun reset() {
        save(TtsSettings())
    }
}
