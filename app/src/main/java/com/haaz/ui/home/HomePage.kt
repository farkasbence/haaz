@file:OptIn(ExperimentalMaterial3Api::class)

package com.haaz.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CameraAlt
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.haaz.R
import com.haaz.domain.player.PlaybackController
import com.haaz.domain.scanner.createImageUri
import com.haaz.ui.settings.SettingsSheetUI
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onOpenHistory: () -> Unit,
    selectedHistoryPrompt: String?,
    selectedClipFileName: String?,
    onHistoryConsumed: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val playbackController = rememberPlaybackController(
        onEnded = viewModel::onPlaybackFinished,
        onPlayerStateChanged = viewModel::onPlayerStateChanged
    )
    val context = LocalContext.current
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingImageUri
        if (success && uri != null) {
            viewModel.onImageCaptured(uri)
        } else {
            viewModel.onScanCancelled()
        }
        pendingImageUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingImageUri
        if (granted && uri != null) {
            takePictureLauncher.launch(uri)
        } else {
            pendingImageUri = null
            viewModel.onScanError("Camera permission denied")
        }
    }

    LaunchedEffect(selectedHistoryPrompt, selectedClipFileName) {
        if (selectedClipFileName != null) {
            viewModel.onClipSelected(selectedHistoryPrompt.orEmpty(), selectedClipFileName)
            onHistoryConsumed()
            return@LaunchedEffect
        }
        val history = selectedHistoryPrompt ?: return@LaunchedEffect
        viewModel.onHistorySelected(history)
        onHistoryConsumed()
    }

    LaunchedEffect(uiState.playback?.audioData, uiState.playback?.audioFilePath) {
        val playback = uiState.playback
        if (playback == null) {
            playbackController.clear()
            return@LaunchedEffect
        }

        runCatching {
            when {
                playback.audioData != null -> playbackController.setAudioData(playback.audioData, playback.isPlaying)
                playback.audioFilePath != null -> playbackController.setAudioFile(File(playback.audioFilePath), playback.isPlaying)
            }
        }.onFailure { throwable ->
            snackbarHostState.showSnackbar(throwable.message ?: "Unable to start playback")
            viewModel.onClosePlayback()
        }
    }

    LaunchedEffect(uiState.playback?.isPlaying) {
        val playback = uiState.playback ?: return@LaunchedEffect
        playbackController.setPlaying(playback.isPlaying)
    }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    HomePageUI(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        viewModel = viewModel,
        onOpenHistory = onOpenHistory,
        onOpenCamera = {
            val uri = createImageUri(context) ?: run {
                viewModel.onScanError("Unable to open camera")
                return@HomePageUI
            }
            pendingImageUri = uri
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePictureLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    )
}

@Composable
private fun HomePageUI(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel,
    onOpenHistory: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val backgroundColor = Color(0xFFF6F7F9)
    val elevatedSurfaceColor = Color.White

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.Top
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "History",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenCamera,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = "Scan text",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.haaz_icon),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Haaz", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), clip = false),
                        value = uiState.promptText,
                        onValueChange = viewModel::onPromptChange,
                        placeholder = { Text("Enter text-to-speech prompt…") },
                        enabled = !uiState.isGenerating && !uiState.isScanning,
                        minLines = 6,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = elevatedSurfaceColor,
                            unfocusedContainerColor = elevatedSurfaceColor,
                            disabledContainerColor = elevatedSurfaceColor,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(visible = uiState.isScanning) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning image…")
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                visible = uiState.playback != null,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 200),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 200),
                    targetOffsetY = { fullHeight -> fullHeight }
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Column {
                    PlaybackBar(
                        playback = uiState.playback,
                        onTogglePlayback = { viewModel.onTogglePlayback() },
                        onClose = { viewModel.onClosePlayback() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), clip = false),
                color = elevatedSurfaceColor,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
            }

            if (uiState.isSettingsSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.onToggleSettings(false) },
                    sheetState = sheetState,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    containerColor = backgroundColor
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
    val barColor = MaterialTheme.colorScheme.primary
    val buttonColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f).compositeOver(barColor)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), clip = false),
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
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { if (enabled) onTogglePlayback() }, enabled = enabled) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = buttonColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (playback?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow
                    val description = if (playback?.isPlaying == true) "Pause" else "Play"
                    Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            IconButton(onClick = { if (enabled) onClose() }, enabled = enabled) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = buttonColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun rememberPlaybackController(
    onEnded: () -> Unit,
    onPlayerStateChanged: (Boolean) -> Unit
): PlaybackController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { PlaybackController(context, onEnded, onPlayerStateChanged) }
    DisposableEffect(controller) {
        onDispose { scope.launch { controller.release() } }
    }
    return controller
}
