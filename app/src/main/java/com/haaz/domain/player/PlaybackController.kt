package com.haaz.domain.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * MediaController-backed playback wrapper with temp file management.
 */
class PlaybackController(
    context: Context,
    private val onEnded: () -> Unit,
    private val onPlayerStateChanged: (Boolean) -> Unit
) {
    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    private val controllerMutex = Mutex()
    private var controller: MediaController? = null
    private var tempAudioFile: File? = null
    private var pendingPlayWhenReady: Boolean = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                pendingPlayWhenReady = false
                onPlayerStateChanged(true)
                return
            }
            if (pendingPlayWhenReady) return
            onPlayerStateChanged(false)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) onEnded()
        }
    }

    suspend fun setAudioData(audioData: ByteArray, playWhenReady: Boolean) {
        val file = withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("haaz_tts_", ".mp3", appContext.cacheDir)
            tempFile.outputStream().use { output -> output.write(audioData) }
            tempAudioFile?.delete()
            tempFile
        }

        tempAudioFile = file
        val mediaController = getController()
        mediaController.setMediaItem(MediaItem.fromUri(file.toUri()))
        mediaController.prepare()
        if (playWhenReady || pendingPlayWhenReady) {
            pendingPlayWhenReady = true
            mediaController.play()
        } else {
            mediaController.playWhenReady = false
        }
    }

    suspend fun setAudioFile(file: File, playWhenReady: Boolean) {
        tempAudioFile?.delete()
        tempAudioFile = null
        val mediaController = getController()
        mediaController.setMediaItem(MediaItem.fromUri(file.toUri()))
        mediaController.prepare()
        if (playWhenReady || pendingPlayWhenReady) {
            pendingPlayWhenReady = true
            mediaController.play()
        } else {
            mediaController.playWhenReady = false
        }
    }

    suspend fun setPlaying(isPlaying: Boolean) {
        val mediaController = getController()
        if (isPlaying) {
            pendingPlayWhenReady = true
            if (mediaController.mediaItemCount == 0) return
            if (mediaController.playbackState == Player.STATE_IDLE) mediaController.prepare()
            mediaController.play()
        } else {
            pendingPlayWhenReady = false
            mediaController.pause()
        }
    }

    fun clear() {
        controller?.let { mediaController ->
            mediaController.stop()
            mediaController.clearMediaItems()
        }
        tempAudioFile?.delete()
        tempAudioFile = null
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    private suspend fun getController(): MediaController = controllerMutex.withLock {
        controller ?: buildController().also { mediaController ->
            mediaController.addListener(listener)
            controller = mediaController
        }
    }

    private suspend fun buildController(): MediaController {
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        return future.await(ContextCompat.getMainExecutor(appContext))
    }
}

private suspend fun <T> ListenableFuture<T>.await(executor: Executor): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            },
            executor
        )
        continuation.invokeOnCancellation { cancel(true) }
    }
