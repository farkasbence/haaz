package com.haaz.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haaz.data.VoiceClipEntry

@Composable
fun HistoryPage(
    onBack: () -> Unit,
    onPromptSelected: (String) -> Unit,
    onClipSelected: (VoiceClipEntry) -> Unit
) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val historyState by viewModel.history.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()

    HistoryPageUI(
        voiceClips = historyState.voiceClips,
        prompts = historyState.prompts,
        onBack = onBack,
        onPromptSelected = onPromptSelected,
        onClipSelected = onClipSelected,
        onClear = { viewModel.clearHistory() },
        isClearing = isClearing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryPageUI(
    voiceClips: List<VoiceClipEntry>,
    prompts: List<String>,
    onBack: () -> Unit,
    onPromptSelected: (String) -> Unit,
    onClipSelected: (VoiceClipEntry) -> Unit,
    onClear: () -> Unit,
    isClearing: Boolean
) {
    val backgroundColor = Color(0xFFF6F7F9)
    val hasHistory = voiceClips.isNotEmpty() || prompts.isNotEmpty()
    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(text = "History") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onClear, enabled = hasHistory && !isClearing) {
                        Text(text = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        if (!hasHistory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    SectionHeader(text = "Voice Clips")
                }
                if (voiceClips.isEmpty()) {
                    item {
                        SectionEmpty(text = "No voice clips yet")
                    }
                } else {
                    items(voiceClips) { clip ->
                        HistoryRow(
                            text = clip.prompt,
                            leadingIcon = Icons.Outlined.RecordVoiceOver,
                            onClick = { onClipSelected(clip) }
                        )
                    }
                }
                item {
                    SectionHeader(
                        text = "Prompts",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (prompts.isEmpty()) {
                    item {
                        SectionEmpty(text = "No prompts yet")
                    }
                } else {
                    items(prompts) { entry ->
                        HistoryRow(
                            text = entry,
                            leadingIcon = Icons.Outlined.EditNote,
                            onClick = { onPromptSelected(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SectionEmpty(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun HistoryRow(
    text: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp), clip = false),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null
            )
        }
    }
}
