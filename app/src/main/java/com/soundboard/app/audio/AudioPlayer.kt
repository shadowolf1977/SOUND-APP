package com.soundboard.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(filePath: String) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "Error playing: what=$what extra=$extra")
                    release()
                    mediaPlayer = null
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play $filePath", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping", e)
        }
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
