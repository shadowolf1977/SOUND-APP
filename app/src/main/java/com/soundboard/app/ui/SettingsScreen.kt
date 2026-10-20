package com.soundboard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soundboard.app.data.SoundButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SoundboardUiState,
    onBack: () -> Unit,
    onSetButtonCount: (Int) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onUpdateColor: (Int, Long) -> Unit,
    onUpdateTitle: (Int, String) -> Unit,
    onClearButton: (Int) -> Unit,
    onResetAll: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<SoundButton?>(null) }
    var editTitle by remember { mutableStateOf("") }

    val presetColors = listOf(
        0xFF6200EE, 0xFF03DAC5, 0xFFE91E63, 0xFF9C27B0,
        0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50,
        0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF607D8B
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Number of buttons: ${uiState.buttonCount}", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = uiState.buttonCount.toFloat(),
                onValueChange = { onSetButtonCount(it.toInt()) },
                valueRange = 1f..24f,
                steps = 22
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Grid columns: ${uiState.gridColumns}", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = uiState.gridColumns.toFloat(),
                onValueChange = { onSetGridColumns(it.toInt()) },
                valueRange = 2f..5f,
                steps = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Edit buttons", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            uiState.buttons.forEach { button ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            editingButton = button
                            editTitle = button.title
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(button.color))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(button.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (button.filePath != null) "Has sound" else "Empty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onClearButton(button.id) }) {
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset all sounds")
            }
        }
    }

    // Edit dialog
    editingButton?.let { button ->
        AlertDialog(
            onDismissRequest = { editingButton = null },
            title = { Text("Edit Button ${button.id + 1}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Color")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable {
                                        onUpdateColor(button.id, color)
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateTitle(button.id, editTitle)
                    editingButton = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingButton = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all?") },
            text = { Text("This will delete every recorded sound and reset titles. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAll()
                    showResetDialog = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
