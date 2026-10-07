package com.mark.moodlogger.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Thin wrapper around MediaRecorder. Records AAC into an .m4a in filesDir/journal_audio/. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAt: Long = 0L

    val isRecording get() = recorder != null

    fun start(): File {
        stop()
        val dir = File(context.filesDir, "journal_audio").apply { mkdirs() }
        val file = File(dir, "reflection_${System.currentTimeMillis()}.m4a")

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()

        recorder = r
        currentFile = file
        startedAt = System.currentTimeMillis()
        return file
    }

    /** Returns (file, durationMs) if a recording was in progress, else null. */
    fun stop(): Pair<File, Long>? {
        val r = recorder ?: return null
        val file = currentFile
        val durationMs = System.currentTimeMillis() - startedAt
        recorder = null
        currentFile = null
        return try {
            r.stop()
            r.release()
            if (file != null && file.exists() && file.length() > 0) file to durationMs else null
        } catch (e: RuntimeException) {
            r.release()
            file?.delete()
            null
        }
    }
}
