package com.haaz.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haaz.data.TtsSettings
import com.haaz.domain.TtsModel
import com.haaz.domain.Voice

@Composable
fun SettingsSheetUI(
    initial: TtsSettings,
    voices: List<Voice>,
    isVoicesLoading: Boolean,
    voicesError: String?,
    onRetryVoices: () -> Unit,
    onSave: (TtsSettings) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    var draft by rememberSaveable(stateSaver = TtsSettingsSaver) { mutableStateOf(initial) }

    LaunchedEffect(initial) { draft = initial }

    val modelOptions = remember { TtsModel.entries.toList() }
    val speedTargets = remember { listOf(0.8f, 1.0f, 1.2f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close settings")
            }
        }

        Text(text = "Model", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(modelOptions) { option ->
                val selected = option == draft.model
                OutlinedCard(
                    modifier = Modifier.size(width = 200.dp, height = 140.dp),
                    onClick = { draft = draft.copy(model = option) },
                    border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = option.badge, style = MaterialTheme.typography.labelSmall)
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = option.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = option.subtitle, style = MaterialTheme.typography.bodyMedium)
                            Text(text = option.quality, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Text(text = "Voice", style = MaterialTheme.typography.titleMedium)
        when {
            isVoicesLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            voicesError != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = voicesError, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onRetryVoices) {
                        Text("Retry")
                    }
                }
            }
            voices.isEmpty() -> {
                Text(text = "No voices available. Try again later.", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(voices) { voice ->
                        val selected = voice.id == draft.voiceId
                        OutlinedCard(
                            modifier = Modifier.size(width = 200.dp, height = 96.dp),
                            onClick = { draft = draft.copy(voiceId = voice.id) },
                            border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = voice.name, style = MaterialTheme.typography.titleMedium)
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                voice.descriptor?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create speed slider
        SettingsSlider(
            title = "Speed",
            value = draft.speed,
            onValueChange = { draft = draft.copy(speed = it) },
            valueRange = speedTargets.first()..speedTargets.last(),
            steps = 1,
            startLabel = "Slow",
            endLabel = "Fast"
        )

        // Create stability slider
        SettingsSlider(
            title = "Stability",
            value = draft.stability,
            onValueChange = { draft = draft.copy(stability = it) },
            valueRange = 0f..1f,
            steps = 5,
            startLabel = "More variable",
            endLabel = "More stable"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    onReset()
                    draft = TtsSettings()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Reset")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onSave(draft) }
            ) {
                Text("Save")
            }
        }
    }
}

private val TtsSettingsSaver = Saver<TtsSettings, Map<String, Any?>>(save = {
    mapOf(
        "model" to it.model.id,
        "voiceId" to (it.voiceId ?: ""),
        "speed" to it.speed,
        "stability" to it.stability
    )
}, restore = {
    TtsSettings(
        model = TtsModel.fromId(it["model"] as? String),
        voiceId = (it["voiceId"] as? String)?.takeIf { id -> id.isNotEmpty() },
        speed = (it["speed"] as? Float) ?: 1.0f,
        stability = (it["stability"] as? Float) ?: 0.5f
    )
})

@Composable
private fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    startLabel: String,
    endLabel: String,
    markerValue: Float? = null,
    markerLabel: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        if (markerValue != null && markerLabel != null) {
            val fraction = ((markerValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(fraction))
                Text(markerLabel, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.weight(1f - fraction))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(startLabel, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(endLabel, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
