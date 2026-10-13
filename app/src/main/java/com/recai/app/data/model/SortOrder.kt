package com.recai.app.data.model

import java.time.ZoneId

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

/**
 * Extension property returning the approximate epoch timestamp for sorting an AudioFile.
 * Prefers parsed recordingDateTime, falling back to file lastModified.
 */
val AudioFile.timestampMillis: Long
    get() {
        val ldt = metadata?.recordingDateTime
        return if (ldt != null) {
            try {
                ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                lastModified
            }
        } else {
            lastModified
        }
    }
