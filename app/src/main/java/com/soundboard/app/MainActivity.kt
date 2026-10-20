package com.soundboard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.soundboard.app.ui.SettingsScreen
import com.soundboard.app.ui.SoundboardScreen
import com.soundboard.app.ui.SoundboardViewModel
import com.soundboard.app.ui.theme.SoundboardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SoundboardViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Microphone permission is required to record sounds",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request mic permission early
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            SoundboardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by viewModel.uiState.collectAsState()
                    var showSettings by remember { mutableStateOf(false) }

                    if (showSettings) {
                        SettingsScreen(
                            uiState = uiState,
                            onBack = { showSettings = false },
                            onSetButtonCount = viewModel::setButtonCount,
                            onSetGridColumns = viewModel::setGridColumns,
                            onUpdateColor = viewModel::updateButtonColor,
                            onUpdateTitle = viewModel::updateButtonTitle,
                            onClearButton = viewModel::clearButton,
                            onResetAll = viewModel::resetAll
                        )
                    } else {
                        SoundboardScreen(
                            uiState = uiState,
                            onPlay = viewModel::playSound,
                            onRecordStart = { buttonId ->
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.startRecording(buttonId)
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Please grant microphone permission",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onRecordStop = viewModel::stopRecording,
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}
