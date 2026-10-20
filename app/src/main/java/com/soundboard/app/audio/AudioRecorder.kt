package com.soundboard.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(buttonId: Int): File? {
        stopRecording() // safety

        val dir = File(context.filesDir, "sounds").apply { mkdirs() }
        outputFile = File(dir, "sound_$buttonId.m4a")

        return try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            recorder?.release()
            recorder = null
            outputFile = null
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            recorder?.release()
            recorder = null
            // Delete incomplete file
            outputFile?.delete()
            null
        } finally {
            val result = outputFile
            outputFile = null
            result
        }
    }

    fun isRecording(): Boolean = recorder != null
}
