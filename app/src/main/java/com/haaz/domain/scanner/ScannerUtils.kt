package com.haaz.domain.scanner

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal fun createImageUri(context: Context): Uri? {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("scan_", ".jpg", imagesDir)
    val authority = "${context.packageName}.fileprovider"
    return runCatching {
        FileProvider.getUriForFile(context, authority, imageFile)
    }.getOrNull()
}