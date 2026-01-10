package com.haaz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haaz.data.HistoryRepository
import com.haaz.data.SettingsRepository
import com.haaz.data.TextToSpeechRepository
import com.haaz.data.TtsSettings
import com.haaz.domain.Voice
import com.haaz.domain.scanner.TextScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.net.Uri
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val textToSpeechRepository: TextToSpeechRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val textScanner: TextScanner
) : ViewModel() {
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
        if (open) loadVoicesIfNeeded()
    }

    fun generateSpeech() {
        val prompt = _uiState.value.promptText.trim()
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null, playback = null) }

            val result = textToSpeechRepository.generateSpeech(prompt, _uiState.value.settings)
            if (result.isSuccess) {
                historyRepository.addEntry(prompt)
            }
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

    fun onPlayerStateChanged(isPlaying: Boolean) {
        _uiState.update { state ->
            val current = state.playback ?: return@update state
            if (current.isPlaying == isPlaying) state else state.copy(
                playback = current.copy(
                    isPlaying = isPlaying
                )
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onScanError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isScanning = false) }
    }

    fun onHistorySelected(query: String) {
        _uiState.update { it.copy(promptText = query, errorMessage = null) }
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

    fun refreshVoices() {
        viewModelScope.launch { loadVoices(force = true) }
    }

    private fun loadVoicesIfNeeded() {
        val state = _uiState.value
        if (state.voices.isNotEmpty() || state.isVoicesLoading) return
        viewModelScope.launch { loadVoices(force = false) }
    }

    private suspend fun loadVoices(force: Boolean) {
        _uiState.update { it.copy(isVoicesLoading = true, voicesError = null) }
        val result = textToSpeechRepository.fetchVoices(force)
        _uiState.update { state ->
            result.fold(
                onSuccess = { voices -> state.copy(voices = voices, isVoicesLoading = false) },
                onFailure = { error ->
                    state.copy(
                        isVoicesLoading = false,
                        voicesError = error.message ?: "Unable to load voices"
                    )
                }
            )
        }
    }

    fun onImageCaptured(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            val result = textScanner.scanText(uri)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { text ->
                        state.copy(
                            isScanning = false,
                            promptText = text,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isScanning = false,
                            errorMessage = error.message ?: "Unable to read text from image"
                        )
                    }
                )
            }
        }
    }

    fun onScanCancelled() {
        _uiState.update { it.copy(isScanning = false) }
    }
}

data class HomeUiState(
    val promptText: String = "",
    val isGenerating: Boolean = false,
    val playback: PlaybackState? = null,
    val errorMessage: String? = null,
    val isSettingsSheetOpen: Boolean = false,
    val settings: TtsSettings = TtsSettings(),
    val voices: List<Voice> = emptyList(),
    val isVoicesLoading: Boolean = false,
    val voicesError: String? = null,
    val isScanning: Boolean = false
) {
    val canGenerate: Boolean get() = promptText.isNotBlank() && !isGenerating && !isScanning
}

data class PlaybackState(
    val audioData: ByteArray,
    val title: String = "Generated audio",
    val isPlaying: Boolean = false
)
