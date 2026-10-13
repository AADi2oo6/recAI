package com.recai.app.data.storage

import android.net.Uri
import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.RecordingMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ConversationGroupingTest {

    private val repository = FileRepository()

    private fun createAudioFile(
        name: String,
        callerName: String?,
        phoneNumber: String?,
        dateTime: LocalDateTime?,
        lastModified: Long = 1000L
    ): AudioFile {
        return AudioFile(
            name = name,
            uri = null,
            mimeType = "audio/mpeg",
            size = 1024L,
            lastModified = lastModified,
            metadata = RecordingMetadata(
                callerName = callerName,
                phoneNumber = phoneNumber,
                recordingDateTime = dateTime,
                originalFileName = name
            )
        )
    }

    @Test
    fun test1_multipleRecordingsWithSamePhoneNumberBecomeOneConversation() {
        val files = listOf(
            createAudioFile(
                name = "Darshan(00919699177117)_20261013200200.mp3",
                callerName = "Darshan Roommate",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 20, 2),
                lastModified = 1000L
            ),
            createAudioFile(
                name = "Darshan(00919699177117)_20261013210000.mp3",
                callerName = "Darshan Roommate",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 21, 0),
                lastModified = 2000L
            ),
            createAudioFile(
                name = "Darshan(00919699177117)_20261013230000.mp3",
                callerName = "Darshan Roommate",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 23, 0),
                lastModified = 3000L
            )
        )

        val conversations = repository.groupIntoConversations(files)

        assertEquals(1, conversations.size)
        val conversation = conversations.first()
        assertEquals("00919699177117", conversation.phoneNumber)
        assertEquals("Darshan Roommate", conversation.callerName)
        assertEquals(3, conversation.recordingCount)
    }

    @Test
    fun test2_differentPhoneNumbersBecomeDifferentConversations() {
        val files = listOf(
            createAudioFile(
                name = "Darshan(00919699177117)_20261013200200.mp3",
                callerName = "Darshan Roommate",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 20, 2)
            ),
            createAudioFile(
                name = "Lab(00919529485776)_20261013210000.mp3",
                callerName = "Lab hade",
                phoneNumber = "00919529485776",
                dateTime = LocalDateTime.of(2026, 10, 13, 21, 0)
            )
        )

        val conversations = repository.groupIntoConversations(files)
        assertEquals(2, conversations.size)
    }

    @Test
    fun test3_samePhoneNumberWithDifferentCallerNamesBecomesOneConversation() {
        val files = listOf(
            createAudioFile(
                name = "Darshan(00919699177117)_20261013200200.mp3",
                callerName = "Darshan",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 20, 2),
                lastModified = 1000L
            ),
            createAudioFile(
                name = "Darshan Roommate(00919699177117)_20261013210000.mp3",
                callerName = "Darshan Roommate",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 21, 0),
                lastModified = 2000L
            )
        )

        val conversations = repository.groupIntoConversations(files)
        assertEquals(1, conversations.size)
        assertEquals(2, conversations.first().recordingCount)
        assertNotNull(conversations.first().callerName)
    }

    @Test
    fun test4_conversationsAreSortedByLatestRecording() {
        val files = listOf(
            createAudioFile(
                name = "Baba(00918830714901)_20260415100000.mp3",
                callerName = "Baba Tatkal",
                phoneNumber = "00918830714901",
                dateTime = LocalDateTime.of(2026, 4, 15, 10, 0),
                lastModified = 1000L
            ),
            createAudioFile(
                name = "Darshan(00919699177117)_20261013205500.mp3",
                callerName = "Darshan Roommate",
                phoneNumber = "00919699177117",
                dateTime = LocalDateTime.of(2026, 10, 13, 20, 55),
                lastModified = 3000L
            ),
            createAudioFile(
                name = "Ishwar(00919529485776)_20261012150000.mp3",
                callerName = "Ishwar Tatkal",
                phoneNumber = "00919529485776",
                dateTime = LocalDateTime.of(2026, 10, 12, 15, 0),
                lastModified = 2000L
            )
        )

        val conversations = repository.groupIntoConversations(files)
        assertEquals(3, conversations.size)
        assertEquals("00919699177117", conversations[0].phoneNumber) // Oct 13
        assertEquals("00919529485776", conversations[1].phoneNumber) // Oct 12
        assertEquals("00918830714901", conversations[2].phoneNumber) // Apr 15
    }

    @Test
    fun test5_recordingCountIsCorrect() {
        val files = listOf(
            createAudioFile("rec1.mp3", "Alice", "123", LocalDateTime.now()),
            createAudioFile("rec2.mp3", "Alice", "123", LocalDateTime.now()),
            createAudioFile("rec3.mp3", "Alice", "123", LocalDateTime.now()),
            createAudioFile("rec4.mp3", "Alice", "123", LocalDateTime.now())
        )

        val conversations = repository.groupIntoConversations(files)
        assertEquals(1, conversations.size)
        assertEquals(4, conversations[0].recordingCount)
    }

    @Test
    fun test6_missingPhoneNumbersDoNotCrash() {
        val files = listOf(
            createAudioFile(
                name = "UnknownRecording.mp3",
                callerName = null,
                phoneNumber = null,
                dateTime = null
            )
        )

        val conversations = repository.groupIntoConversations(files)
        assertEquals(1, conversations.size)
        assertNull(conversations.first().phoneNumber)
        assertEquals(1, conversations.first().recordingCount)
    }

    @Test
    fun test7_multipleRecordingsWithMissingPhoneNumbersAreNotMerged() {
        val files = listOf(
            createAudioFile(
                name = "VoiceNote1.mp3",
                callerName = null,
                phoneNumber = null,
                dateTime = null
            ),
            createAudioFile(
                name = "VoiceNote2.mp3",
                callerName = null,
                phoneNumber = null,
                dateTime = null
            )
        )

        val conversations = repository.groupIntoConversations(files)
        // Must NOT be merged into one single conversation
        assertEquals(2, conversations.size)
    }

    @Test
    fun test8_emptyRecordingListIsHandledSafely() {
        val conversations = repository.groupIntoConversations(emptyList())
        assertTrue(conversations.isEmpty())
    }
}
