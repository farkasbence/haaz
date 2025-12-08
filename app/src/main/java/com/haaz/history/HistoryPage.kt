package com.haaz.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HistoryPage(
    onBack: () -> Unit,
    onQuerySelected: (String) -> Unit
) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val entries by viewModel.history.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()

    HistoryPageUI(
        entries = entries,
        onBack = onBack,
        onQuerySelected = onQuerySelected,
        onClear = { viewModel.clearHistory() },
        isClearing = isClearing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryPageUI(
    entries: List<String>,
    onBack: () -> Unit,
    onQuerySelected: (String) -> Unit,
    onClear: () -> Unit,
    isClearing: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onClear, enabled = entries.isNotEmpty() && !isClearing) {
                        Text(text = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
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
                items(entries) { entry ->
                    HistoryRow(
                        text = entry,
                        onClick = { onQuerySelected(entry) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null
            )
        }
    )
}
