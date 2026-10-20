package com.recai.app.ui.util

import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.RecordingMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RecordingNavigationHelperTest {

    private fun createTestAudio(name: String, lastModified: Long): AudioFile {
        return AudioFile(
            name = name,
            uri = null,
            mimeType = "audio/mpeg",
            size = 1000L,
            lastModified = lastModified,
            metadata = RecordingMetadata(
                callerName = "Test Contact",
                phoneNumber = "00918830714901",
                recordingDateTime = LocalDateTime.of(2026, 10, 18, 12, 0),
                originalFileName = name
            )
        )
    }

    @Test
    fun testHasPrevious() {
        assertFalse("First recording has no previous", RecordingNavigationHelper.hasPrevious(0))
        assertTrue("Second recording has previous", RecordingNavigationHelper.hasPrevious(1))
        assertTrue("Third recording has previous", RecordingNavigationHelper.hasPrevious(2))
        assertFalse("Negative index has no previous", RecordingNavigationHelper.hasPrevious(-1))
    }

    @Test
    fun testHasNext() {
        val totalSize = 3
        assertTrue("First recording has next in list of 3", RecordingNavigationHelper.hasNext(0, totalSize))
        assertTrue("Second recording has next in list of 3", RecordingNavigationHelper.hasNext(1, totalSize))
        assertFalse("Last recording has no next in list of 3", RecordingNavigationHelper.hasNext(2, totalSize))

        // Single recording boundary
        assertFalse("Single recording has no next", RecordingNavigationHelper.hasNext(0, 1))

        // Empty list boundary
        assertFalse("Empty list has no next", RecordingNavigationHelper.hasNext(0, 0))
    }

    @Test
    fun testCalculateCurrentIndex() {
        val file1 = createTestAudio("rec1.mp3", 1000L)
        val file2 = createTestAudio("rec2.mp3", 2000L)
        val file3 = createTestAudio("rec3.mp3", 3000L)
        val playlist = listOf(file1, file2, file3)

        assertEquals(0, RecordingNavigationHelper.calculateCurrentIndex(playlist, file1))
        assertEquals(1, RecordingNavigationHelper.calculateCurrentIndex(playlist, file2))
        assertEquals(2, RecordingNavigationHelper.calculateCurrentIndex(playlist, file3))

        val unknownFile = createTestAudio("unknown.mp3", 4000L)
        assertEquals(-1, RecordingNavigationHelper.calculateCurrentIndex(playlist, unknownFile))
        assertEquals(-1, RecordingNavigationHelper.calculateCurrentIndex(playlist, null))
    }

    @Test
    fun testFindTargetAfterDeletion_deleteFirstRecording_navigatesToNext() {
        val file1 = createTestAudio("rec1.mp3", 3000L)
        val file2 = createTestAudio("rec2.mp3", 2000L)
        val file3 = createTestAudio("rec3.mp3", 1000L)
        val playlist = listOf(file1, file2, file3)

        // Deleting first recording (rec1) -> next recording (rec2)
        val target = RecordingNavigationHelper.findTargetAfterDeletion(playlist, file1)
        assertEquals(file2.name, target?.name)
    }

    @Test
    fun testFindTargetAfterDeletion_deleteMiddleRecording_navigatesToNext() {
        val file1 = createTestAudio("rec1.mp3", 3000L)
        val file2 = createTestAudio("rec2.mp3", 2000L)
        val file3 = createTestAudio("rec3.mp3", 1000L)
        val playlist = listOf(file1, file2, file3)

        // Deleting middle recording (rec2) -> next recording (rec3)
        val target = RecordingNavigationHelper.findTargetAfterDeletion(playlist, file2)
        assertEquals(file3.name, target?.name)
    }

    @Test
    fun testFindTargetAfterDeletion_deleteLastRecording_navigatesToPrevious() {
        val file1 = createTestAudio("rec1.mp3", 3000L)
        val file2 = createTestAudio("rec2.mp3", 2000L)
        val file3 = createTestAudio("rec3.mp3", 1000L)
        val playlist = listOf(file1, file2, file3)

        // Deleting last recording (rec3) -> fallback to previous recording (rec2)
        val target = RecordingNavigationHelper.findTargetAfterDeletion(playlist, file3)
        assertEquals(file2.name, target?.name)
    }

    @Test
    fun testFindTargetAfterDeletion_deleteOnlyRecording_returnsNull() {
        val singleFile = createTestAudio("single.mp3", 1000L)
        val playlist = listOf(singleFile)

        // Deleting only recording -> null (signals navigation back to conversation detail)
        val target = RecordingNavigationHelper.findTargetAfterDeletion(playlist, singleFile)
        assertNull(target)
    }

    @Test
    fun testFindTargetAfterDeletion_respectsActiveSortOrder() {
        val oldest = createTestAudio("rec_oldest.mp3", 1000L)
        val middle = createTestAudio("rec_middle.mp3", 2000L)
        val newest = createTestAudio("rec_newest.mp3", 3000L)

        // 1. In Newest First ordering: [newest, middle, oldest]
        val newestFirstPlaylist = listOf(newest, middle, oldest)
        // Deleting newest (index 0) moves to next in that order (middle)
        assertEquals(middle.name, RecordingNavigationHelper.findTargetAfterDeletion(newestFirstPlaylist, newest)?.name)
        // Deleting oldest (last item) moves to previous in that order (middle)
        assertEquals(middle.name, RecordingNavigationHelper.findTargetAfterDeletion(newestFirstPlaylist, oldest)?.name)

        // 2. In Oldest First ordering: [oldest, middle, newest]
        val oldestFirstPlaylist = listOf(oldest, middle, newest)
        // Deleting oldest (index 0) moves to next in that order (middle)
        assertEquals(middle.name, RecordingNavigationHelper.findTargetAfterDeletion(oldestFirstPlaylist, oldest)?.name)
        // Deleting newest (last item) moves to previous in that order (middle)
        assertEquals(middle.name, RecordingNavigationHelper.findTargetAfterDeletion(oldestFirstPlaylist, newest)?.name)
    }
}
