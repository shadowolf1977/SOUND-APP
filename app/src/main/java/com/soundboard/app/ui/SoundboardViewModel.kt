package com.soundboard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundboard.app.audio.AudioPlayer
import com.soundboard.app.audio.AudioRecorder
import com.soundboard.app.data.SettingsRepository
import com.soundboard.app.data.SoundButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SoundboardUiState(
    val buttons: List<SoundButton> = emptyList(),
    val buttonCount: Int = 12,
    val gridColumns: Int = 3,
    val isRecording: Boolean = false,
    val recordingButtonId: Int? = null
)

class SoundboardViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val audioPlayer = AudioPlayer(application)
    private val audioRecorder = AudioRecorder(application)

    private val _isRecording = MutableStateFlow(false)
    private val _recordingButtonId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SoundboardUiState> = combine(
        settingsRepo.buttons,
        settingsRepo.buttonCount,
        settingsRepo.gridColumns,
        _isRecording,
        _recordingButtonId
    ) { buttons, count, columns, recording, recId ->
        SoundboardUiState(
            buttons = buttons,
            buttonCount = count,
            gridColumns = columns,
            isRecording = recording,
            recordingButtonId = recId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SoundboardUiState()
    )

    fun playSound(button: SoundButton) {
        if (_isRecording.value) return // don't play while recording
        button.filePath?.let { path ->
            audioPlayer.play(path)
        }
    }

    fun startRecording(buttonId: Int) {
        if (_isRecording.value) return
        val file = audioRecorder.startRecording(buttonId)
        if (file != null) {
            _isRecording.value = true
            _recordingButtonId.value = buttonId
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        val buttonId = _recordingButtonId.value ?: return
        val file = audioRecorder.stopRecording()
        _isRecording.value = false
        _recordingButtonId.value = null

        if (file != null && file.exists() && file.length() > 0) {
            viewModelScope.launch {
                val currentButtons = uiState.value.buttons
                val button = currentButtons.find { it.id == buttonId }
                if (button != null) {
                    val updated = button.copy(
                        filePath = file.absolutePath,
                        title = "Sound ${buttonId + 1}"
                    )
                    settingsRepo.updateButton(updated)
                }
            }
        }
    }

    fun setButtonCount(count: Int) {
        viewModelScope.launch {
            settingsRepo.setButtonCount(count.coerceIn(1, 24))
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsRepo.setGridColumns(columns.coerceIn(2, 5))
        }
    }

    fun updateButtonColor(buttonId: Int, color: Long) {
        viewModelScope.launch {
            val button = uiState.value.buttons.find { it.id == buttonId } ?: return@launch
            settingsRepo.updateButton(button.copy(color = color))
        }
    }

    fun updateButtonTitle(buttonId: Int, title: String) {
        viewModelScope.launch {
            val button = uiState.value.buttons.find { it.id == buttonId } ?: return@launch
            settingsRepo.updateButton(button.copy(title = title.ifBlank { "Empty" }))
        }
    }

    fun clearButton(buttonId: Int) {
        viewModelScope.launch {
            val button = uiState.value.buttons.find { it.id == buttonId } ?: return@launch
            // Delete the file if it exists
            button.filePath?.let { path ->
                try {
                    java.io.File(path).delete()
                } catch (_: Exception) {}
            }
            settingsRepo.updateButton(button.copy(filePath = null, title = "Empty"))
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            // Delete all sound files
            val dir = java.io.File(getApplication<Application>().filesDir, "sounds")
            dir.listFiles()?.forEach { it.delete() }
            settingsRepo.resetAllButtons()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        if (audioRecorder.isRecording()) {
            audioRecorder.stopRecording()
        }
    }
}
