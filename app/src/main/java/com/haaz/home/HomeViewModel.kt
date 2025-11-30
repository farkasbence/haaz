package com.haaz.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haaz.data.SettingsRepository
import com.haaz.data.TextToSpeechDataSource
import com.haaz.data.TtsSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    context: Context
) : ViewModel() {
    private val textToSpeechDataSource = TextToSpeechDataSource()
    private val settingsRepository = SettingsRepository(context.applicationContext)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onPromptChange(text: String) {
        _uiState.update { it.copy(promptText = text, errorMessage = null) }
    }

    fun onToggleSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsSheetOpen = open) }
    }

    fun generateSpeech() {
        val prompt = _uiState.value.promptText.trim()
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null, playback = null) }

            val result = textToSpeechDataSource.generateSpeech(prompt, _uiState.value.settings)
            _uiState.update {
                result.fold(
                    onSuccess = { audio ->
                        it.copy(
                            isGenerating = false,
                            playback = PlaybackState(audioData = audio, isPlaying = false)
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isGenerating = false,
                            errorMessage = error.message ?: "Something went wrong"
                        )
                    }
                )
            }
        }
    }

    fun onTogglePlayback() {
        _uiState.update { state ->
            val current = state.playback ?: return@update state
            state.copy(playback = current.copy(isPlaying = !current.isPlaying))
        }
    }

    fun onClosePlayback() {
        _uiState.update { it.copy(playback = null) }
    }

    fun onPlaybackFinished() {
        _uiState.update { state ->
            val current = state.playback ?: return@update state
            state.copy(playback = current.copy(isPlaying = false))
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveSettings(settings: TtsSettings) {
        viewModelScope.launch {
            settingsRepository.save(settings)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.reset()
        }
    }

    companion object {
        // TODO: Implement dependency injection via Hilt
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(context.applicationContext) as T
                }
            }
    }
}

data class HomeUiState(
    val promptText: String = "",
    val isGenerating: Boolean = false,
    val playback: PlaybackState? = null,
    val errorMessage: String? = null,
    val isSettingsSheetOpen: Boolean = false,
    val settings: TtsSettings = TtsSettings()
) {
    val canGenerate: Boolean get() = promptText.isNotBlank() && !isGenerating
}

data class PlaybackState(
    val audioData: ByteArray,
    val title: String = "Generated audio",
    val isPlaying: Boolean = false
)
