package com.recai.app.data.model

import java.time.LocalDateTime

/**
 * Structured metadata extracted from a call recording filename.
 */
data class RecordingMetadata(
    val callerName: String?,
    val phoneNumber: String?,
    val recordingDateTime: LocalDateTime?,
    val originalFileName: String
)
