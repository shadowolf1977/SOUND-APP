package com.soundboard.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()
    private val BUTTON_COUNT = intPreferencesKey("button_count")
    private val GRID_COLUMNS = intPreferencesKey("grid_columns")
    private val BUTTONS_JSON = stringPreferencesKey("buttons_json")

    val buttonCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BUTTON_COUNT] ?: 12
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[GRID_COLUMNS] ?: 3
    }

    val buttons: Flow<List<SoundButton>> = context.dataStore.data.map { prefs ->
        val json = prefs[BUTTONS_JSON]
        if (json.isNullOrBlank()) {
            createDefaultButtons(prefs[BUTTON_COUNT] ?: 12)
        } else {
            try {
                val type = object : TypeToken<List<SoundButton>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                createDefaultButtons(prefs[BUTTON_COUNT] ?: 12)
            }
        }
    }

    suspend fun setButtonCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[BUTTON_COUNT] = count
            // Resize the button list
            val currentJson = prefs[BUTTONS_JSON]
            val currentButtons: List<SoundButton> = if (currentJson.isNullOrBlank()) {
                createDefaultButtons(count)
            } else {
                try {
                    val type = object : TypeToken<List<SoundButton>>() {}.type
                    gson.fromJson(currentJson, type)
                } catch (e: Exception) {
                    createDefaultButtons(count)
                }
            }
            val newButtons = if (currentButtons.size >= count) {
                currentButtons.take(count)
            } else {
                currentButtons + (currentButtons.size until count).map { id ->
                    SoundButton(id = id)
                }
            }
            prefs[BUTTONS_JSON] = gson.toJson(newButtons)
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { prefs ->
            prefs[GRID_COLUMNS] = columns
        }
    }

    suspend fun updateButton(button: SoundButton) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[BUTTONS_JSON]
            val currentButtons: MutableList<SoundButton> = if (currentJson.isNullOrBlank()) {
                createDefaultButtons(prefs[BUTTON_COUNT] ?: 12).toMutableList()
            } else {
                try {
                    val type = object : TypeToken<List<SoundButton>>() {}.type
                    gson.fromJson<List<SoundButton>>(currentJson, type).toMutableList()
                } catch (e: Exception) {
                    createDefaultButtons(prefs[BUTTON_COUNT] ?: 12).toMutableList()
                }
            }
            val index = currentButtons.indexOfFirst { it.id == button.id }
            if (index >= 0) {
                currentButtons[index] = button
            }
            prefs[BUTTONS_JSON] = gson.toJson(currentButtons)
        }
    }

    suspend fun resetAllButtons() {
        context.dataStore.edit { prefs ->
            val count = prefs[BUTTON_COUNT] ?: 12
            prefs[BUTTONS_JSON] = gson.toJson(createDefaultButtons(count))
        }
    }

    private fun createDefaultButtons(count: Int): List<SoundButton> {
        val colors = listOf(
            0xFF6200EE, 0xFF03DAC5, 0xFF018786, 0xFF3700B3,
            0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
            0xFF2196F3, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
            0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107,
            0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF607D8B,
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7
        )
        return (0 until count).map { id ->
            SoundButton(
                id = id,
                title = "Empty",
                color = colors[id % colors.size]
            )
        }
    }
}
