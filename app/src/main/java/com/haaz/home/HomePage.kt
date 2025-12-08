@file:OptIn(ExperimentalMaterial3Api::class)

package com.haaz.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haaz.R
import com.haaz.player.AudioPlayer
import com.haaz.settings.SettingsSheetUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onOpenHistory: () -> Unit,
    selectedHistory: String?,
    onHistoryConsumed: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val audioPlayer = rememberAudioPlayer(onEnded = viewModel::onPlaybackFinished)

    LaunchedEffect(selectedHistory) {
        val history = selectedHistory ?: return@LaunchedEffect
        viewModel.onHistorySelected(history)
        onHistoryConsumed()
    }

    LaunchedEffect(uiState.playback) {
        val playback = uiState.playback
        if (playback == null) {
            audioPlayer.clear()
            return@LaunchedEffect
        }

        runCatching {
            audioPlayer.setAudioData(playback.audioData, playback.isPlaying)
        }.onFailure { throwable ->
            snackbarHostState.showSnackbar(throwable.message ?: "Unable to start playback")
            viewModel.onClosePlayback()
        }
    }

    LaunchedEffect(uiState.playback?.isPlaying) {
        val playback = uiState.playback ?: return@LaunchedEffect
        audioPlayer.setPlaying(playback.isPlaying)
    }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    HomePageUI(uiState, snackbarHostState, viewModel, onOpenHistory)
}

@Composable
private fun HomePageUI(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel,
    onOpenHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "History",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.speech_bubble),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Haaz", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    value = uiState.promptText,
                    onValueChange = viewModel::onPromptChange,
                    placeholder = { Text("Enter text-to-speech prompt…") },
                    enabled = !uiState.isGenerating,
                    minLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            PlaybackBar(
                playback = uiState.playback,
                onTogglePlayback = { viewModel.onTogglePlayback() },
                onClose = { viewModel.onClosePlayback() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.onToggleSettings(true) },
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.generateSpeech() },
                    enabled = uiState.canGenerate,
                    colors = ButtonDefaults.buttonColors()
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Generate")
                    }
                }
            }

            if (uiState.isSettingsSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.onToggleSettings(false) },
                    sheetState = sheetState,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    SettingsSheetUI(
                        initial = uiState.settings,
                        voices = uiState.voices,
                        isVoicesLoading = uiState.isVoicesLoading,
                        voicesError = uiState.voicesError,
                        onRetryVoices = { viewModel.refreshVoices() },
                        onSave = { newSettings ->
                            viewModel.saveSettings(newSettings)
                            viewModel.onToggleSettings(false)
                        },
                        onReset = { viewModel.resetSettings() },
                        onClose = { viewModel.onToggleSettings(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackBar(
    playback: PlaybackState?,
    onTogglePlayback: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = playback != null
    val title = playback?.title ?: "No audio yet"
    val statusText = when {
        playback == null -> "Generate an audio"
        playback.isPlaying -> "Playing"
        else -> "Ready to play"
    }
    val barColor = MaterialTheme.colorScheme.surfaceVariant
    val buttonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f).compositeOver(barColor)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = barColor,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (enabled) MaterialTheme.colorScheme.primary else Color.Unspecified)
                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { if (enabled) onTogglePlayback() }, enabled = enabled) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = buttonColor, shape = RoundedCornerShape(percent = 50)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (playback?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow
                    val description = if (playback?.isPlaying == true) "Pause" else "Play"
                    Icon(imageVector = icon, contentDescription = description)
                }
            }
            IconButton(onClick = { if (enabled) onClose() }, enabled = enabled) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = buttonColor, shape = RoundedCornerShape(percent = 50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}

@Composable
private fun rememberAudioPlayer(onEnded: () -> Unit): AudioPlayer {
    val context = LocalContext.current
    val player = remember { AudioPlayer(context, onEnded) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
