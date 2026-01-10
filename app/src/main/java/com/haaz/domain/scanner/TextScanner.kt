package com.haaz.domain.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.coroutines.resume
import com.haaz.di.IoDispatcher

/**
 * Scans text from an image Uri using ML Kit on-device recognizer.
 */
class TextScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun scanText(imageUri: Uri): Result<String> = withContext(ioDispatcher) {
        val image = runCatching { InputImage.fromFilePath(context, imageUri) }
            .getOrElse { return@withContext Result.failure(it) }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    if (text.isBlank()) {
                        cont.resume(Result.failure(IllegalStateException("No text found in image")))
                    } else {
                        cont.resume(Result.success(text))
                    }
                }
                .addOnFailureListener { exception ->
                    if (cont.isCancelled) return@addOnFailureListener
                    cont.resume(Result.failure(exception))
                }
                .addOnCanceledListener {
                    cont.cancel(CancellationException("Text recognition cancelled"))
                }
        }
    }
}
