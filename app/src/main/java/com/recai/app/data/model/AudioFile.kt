package com.recai.app.data.model

import android.net.Uri

/**
 * Represents a scanned audio file in the selected directory.
 */
data class AudioFile(
    val name: String,
    val uri: Uri,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long
)
