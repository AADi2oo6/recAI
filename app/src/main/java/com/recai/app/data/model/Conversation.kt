package com.recai.app.data.model

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * In-memory representation of a conversation grouping all audio recordings
 * associated with a specific phone number or unique caller identity.
 */
data class Conversation(
    val id: String,
    val phoneNumber: String?,
    val callerName: String?,
    val recordings: List<AudioFile>
) {
    val recordingCount: Int
        get() = recordings.size

    /**
     * The most recent recording in this conversation.
     * Recordings are expected to be ordered newest first.
     */
    val latestRecording: AudioFile?
        get() = recordings.firstOrNull()

    /**
     * Approximate timestamp in epoch milliseconds for sorting conversations.
     * Prefers parsed recordingDateTime, falling back to file lastModified.
     */
    val latestTimestampMillis: Long
        get() {
            val latest = latestRecording ?: return 0L
            val ldt = latest.metadata?.recordingDateTime
            return if (ldt != null) {
                try {
                    ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    latest.lastModified
                }
            } else {
                latest.lastModified
            }
        }
}
