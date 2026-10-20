package com.recai.app.ui.util

import com.recai.app.data.model.AudioFile

/**
 * Pure helper for recording playlist navigation and deletion fallback calculations.
 */
object RecordingNavigationHelper {

    /**
     * Determines whether the current recording has a previous recording available.
     */
    fun hasPrevious(currentIndex: Int): Boolean = currentIndex > 0

    /**
     * Determines whether the current recording has a next recording available.
     */
    fun hasNext(currentIndex: Int, totalSize: Int): Boolean = currentIndex in 0 until (totalSize - 1)

    /**
     * Finds the index of [currentFile] in [recordings] by matching URI (if present) or filename.
     * Returns -1 if not found.
     */
    fun calculateCurrentIndex(recordings: List<AudioFile>, currentFile: AudioFile?): Int {
        if (currentFile == null) return -1
        return recordings.indexOfFirst { file ->
            (file.uri != null && currentFile.uri != null && file.uri == currentFile.uri) ||
                    file.name == currentFile.name
        }
    }

    /**
     * Determines the next target recording when [deletedFile] is removed from [recordings].
     *
     * Rules:
     * 1. If another recording is available after the deleted recording in active order -> returns that next recording.
     * 2. If no recording is available after it, but a previous recording exists -> returns that previous recording.
     * 3. If no recordings remain in the conversation -> returns null (user navigates back).
     */
    fun findTargetAfterDeletion(
        recordings: List<AudioFile>,
        deletedFile: AudioFile
    ): AudioFile? {
        val currentIndex = calculateCurrentIndex(recordings, deletedFile)
        val remaining = recordings.filterNot { file ->
            (file.uri != null && deletedFile.uri != null && file.uri == deletedFile.uri) ||
                    file.name == deletedFile.name
        }

        if (remaining.isEmpty()) {
            return null
        }

        // If the deleted index is within the bounds of remaining, remaining[currentIndex] was the next item
        return if (currentIndex in remaining.indices) {
            remaining[currentIndex]
        } else {
            // Deleted recording was the last item; fallback to the previous recording (new last item)
            remaining.last()
        }
    }
}
