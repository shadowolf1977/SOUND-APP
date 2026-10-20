package com.soundboard.app.data

data class SoundButton(
    val id: Int,
    val title: String = "Empty",
    val filePath: String? = null,
    val color: Long = 0xFF6200EE
)
