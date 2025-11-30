package com.haaz.player

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Small wrapper around ExoPlayer to manage temp audio files and playback lifecycle.
 */
class AudioPlayer(
    private val context: Context,
    private val onEnded: () -> Unit
) {
    private val player = ExoPlayer.Builder(context).build()
    private var audioFile: File? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onEnded()
            }
        })
    }

    suspend fun setAudioData(audioData: ByteArray, playWhenReady: Boolean) {
        val file = withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("haaz_tts_", ".mp3", context.cacheDir)
            tempFile.outputStream().use { output ->
                output.write(audioData)
            }
            audioFile?.delete()
            tempFile
        }

        audioFile = file
        player.setMediaItem(MediaItem.fromUri(file.toUri()))
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun setPlaying(isPlaying: Boolean) {
        if (isPlaying) {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.playWhenReady = true
        } else {
            player.pause()
        }
    }

    fun clear() {
        player.stop()
        player.clearMediaItems()
        audioFile?.delete()
        audioFile = null
    }

    fun release() {
        clear()
        player.release()
    }
}