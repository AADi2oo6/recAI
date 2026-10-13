package com.recai.app.data.storage

import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.SortOrder
import com.recai.app.data.model.timestampMillis
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Pure functions for searching, filtering, and sorting Conversations and AudioFiles.
 * These functions never mutate input collections or underlying data objects.
 */
object ConversationFilterHelper {

    /**
     * Filters a list of Conversations by a search query (matching caller name or phone number).
     * If the query is blank, returns the list as-is.
     * Safe with null caller names and null phone numbers.
     */
    fun filterConversations(
        conversations: List<Conversation>,
        query: String
    ): List<Conversation> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return conversations

        return conversations.filter { conversation ->
            val matchesName = conversation.callerName?.contains(trimmed, ignoreCase = true) == true
            val matchesPhone = conversation.phoneNumber?.contains(trimmed, ignoreCase = true) == true
            matchesName || matchesPhone
        }
    }

    /**
     * Sorts a list of Conversations according to the specified SortOrder.
     */
    fun sortConversations(
        conversations: List<Conversation>,
        sortOrder: SortOrder
    ): List<Conversation> {
        return when (sortOrder) {
            SortOrder.NEWEST_FIRST -> conversations.sortedByDescending { it.latestTimestampMillis }
            SortOrder.OLDEST_FIRST -> conversations.sortedBy { it.latestTimestampMillis }
        }
    }

    /**
     * Filters recordings within a Conversation by a search query:
     * - Matches caller name
     * - Matches phone number
     * - Matches formatted date/time text or filename
     * Does NOT mutate the conversation or alter recordingCount.
     */
    fun filterRecordings(
        recordings: List<AudioFile>,
        query: String
    ): List<AudioFile> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return recordings

        return recordings.filter { file ->
            val matchesName = file.metadata?.callerName?.contains(trimmed, ignoreCase = true) == true
            val matchesPhone = file.metadata?.phoneNumber?.contains(trimmed, ignoreCase = true) == true
            val matchesFileName = file.name.contains(trimmed, ignoreCase = true)

            val ldt = file.metadata?.recordingDateTime
            val matchesDate = if (ldt != null) {
                val formatted = ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault()))
                formatted.contains(trimmed, ignoreCase = true)
            } else if (file.lastModified > 0) {
                val formatted = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(file.lastModified))
                formatted.contains(trimmed, ignoreCase = true)
            } else {
                false
            }

            matchesName || matchesPhone || matchesFileName || matchesDate
        }
    }

    /**
     * Sorts a list of AudioFiles according to the specified SortOrder.
     */
    fun sortRecordings(
        recordings: List<AudioFile>,
        sortOrder: SortOrder
    ): List<AudioFile> {
        return when (sortOrder) {
            SortOrder.NEWEST_FIRST -> recordings.sortedByDescending { it.timestampMillis }
            SortOrder.OLDEST_FIRST -> recordings.sortedBy { it.timestampMillis }
        }
    }
}
