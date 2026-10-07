package com.mark.moodlogger.audio

import android.media.MediaPlayer

/** One-at-a-time playback wrapper. */
class AudioPlayer {

    private var player: MediaPlayer? = null
    var playingKey: Any? = null
        private set

    fun play(key: Any, path: String, onComplete: () -> Unit) {
        if (playingKey == key && player?.isPlaying == false) {
            player?.start(); return
        }
        stop()
        player = MediaPlayer().apply {
            setDataSource(path)
            setOnCompletionListener {
                this@AudioPlayer.stop()
                onComplete()
            }
            prepare()
            start()
        }
        playingKey = key
    }

    fun pause() { player?.takeIf { it.isPlaying }?.pause() }

    fun isPlaying(key: Any) = playingKey == key && player?.isPlaying == true

    fun positionMs(): Int = player?.currentPosition ?: 0
    fun durationMs(): Int = player?.duration ?: 0
    fun seekTo(ms: Int) { player?.seekTo(ms) }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playingKey = null
    }
}
