package com.recai.app.data.storage

import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.RecordingMetadata
import com.recai.app.data.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ConversationFilterHelperTest {

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

    private fun createConversation(
        id: String,
        callerName: String?,
        phoneNumber: String?,
        recordings: List<AudioFile>
    ): Conversation {
        return Conversation(
            id = id,
            phoneNumber = phoneNumber,
            callerName = callerName,
            recordings = recordings
        )
    }

    @Test
    fun test1_searchByCallerName() {
        val convs = listOf(
            createConversation("1", "Ishwar Tatkal Ta", "00919529485776", emptyList()),
            createConversation("2", "Darshan Roommate", "00919699177117", emptyList())
        )

        val result = ConversationFilterHelper.filterConversations(convs, "Ishwar")
        assertEquals(1, result.size)
        assertEquals("Ishwar Tatkal Ta", result[0].callerName)
    }

    @Test
    fun test2_searchByPhoneNumber() {
        val convs = listOf(
            createConversation("1", "Ishwar Tatkal", "00919529485776", emptyList()),
            createConversation("2", "Baba Tatkal", "00918830714901", emptyList())
        )

        val result = ConversationFilterHelper.filterConversations(convs, "00918830714901")
        assertEquals(1, result.size)
        assertEquals("Baba Tatkal", result[0].callerName)
    }

    @Test
    fun test3_partialPhoneNumberSearch() {
        val convs = listOf(
            createConversation("1", "Baba Tatkal", "00918830714901", emptyList()),
            createConversation("2", "Darshan", "00919699177117", emptyList())
        )

        val result = ConversationFilterHelper.filterConversations(convs, "18830714901")
        assertEquals(1, result.size)
        assertEquals("Baba Tatkal", result[0].callerName)
    }

    @Test
    fun test4_caseInsensitiveCallerNameSearch() {
        val convs = listOf(
            createConversation("1", "Ishwar Tatkal Ta", "00919529485776", emptyList()),
            createConversation("2", "Darshan Roommate", "00919699177117", emptyList())
        )

        val result = ConversationFilterHelper.filterConversations(convs, "ishwar")
        assertEquals(1, result.size)
        assertEquals("Ishwar Tatkal Ta", result[0].callerName)
    }

    @Test
    fun test5_searchWithNullCallerName() {
        val convs = listOf(
            createConversation("1", null, "00919529485776", emptyList()),
            createConversation("2", "Darshan", "00919699177117", emptyList())
        )

        // Searching by name does not match the null callerName conversation and does not crash
        val result = ConversationFilterHelper.filterConversations(convs, "Darshan")
        assertEquals(1, result.size)
        assertEquals("Darshan", result[0].callerName)

        // Searching by phone matches even when callerName is null
        val phoneResult = ConversationFilterHelper.filterConversations(convs, "9529485776")
        assertEquals(1, phoneResult.size)
        assertEquals("1", phoneResult[0].id)
    }

    @Test
    fun test6_searchWithNullPhoneNumber() {
        val convs = listOf(
            createConversation("1", "Unknown Voice", null, emptyList()),
            createConversation("2", "Darshan", "00919699177117", emptyList())
        )

        // Searching by phone does not crash on null phone number
        val result = ConversationFilterHelper.filterConversations(convs, "9699177117")
        assertEquals(1, result.size)
        assertEquals("Darshan", result[0].callerName)

        // Searching by name matches the null phone conversation
        val nameResult = ConversationFilterHelper.filterConversations(convs, "Unknown")
        assertEquals(1, nameResult.size)
        assertEquals("Unknown Voice", nameResult[0].callerName)
    }

    @Test
    fun test7_searchReturningZeroResults() {
        val convs = listOf(
            createConversation("1", "Ishwar", "00919529485776", emptyList()),
            createConversation("2", "Darshan", "00919699177117", emptyList())
        )

        val result = ConversationFilterHelper.filterConversations(convs, "NonExistentName")
        assertTrue(result.isEmpty())
    }

    @Test
    fun test8_newestFirstConversationSorting() {
        val recOld = createAudioFile("old.mp3", "A", "1", LocalDateTime.of(2026, 4, 15, 10, 0))
        val recMid = createAudioFile("mid.mp3", "B", "2", LocalDateTime.of(2026, 10, 12, 10, 0))
        val recNew = createAudioFile("new.mp3", "C", "3", LocalDateTime.of(2026, 10, 13, 10, 0))

        val convOld = createConversation("1", "Old", "1", listOf(recOld))
        val convMid = createConversation("2", "Mid", "2", listOf(recMid))
        val convNew = createConversation("3", "New", "3", listOf(recNew))

        val sorted = ConversationFilterHelper.sortConversations(
            listOf(convOld, convNew, convMid),
            SortOrder.NEWEST_FIRST
        )

        assertEquals("New", sorted[0].callerName)
        assertEquals("Mid", sorted[1].callerName)
        assertEquals("Old", sorted[2].callerName)
    }

    @Test
    fun test9_oldestFirstConversationSorting() {
        val recOld = createAudioFile("old.mp3", "A", "1", LocalDateTime.of(2026, 4, 15, 10, 0))
        val recMid = createAudioFile("mid.mp3", "B", "2", LocalDateTime.of(2026, 10, 12, 10, 0))
        val recNew = createAudioFile("new.mp3", "C", "3", LocalDateTime.of(2026, 10, 13, 10, 0))

        val convOld = createConversation("1", "Old", "1", listOf(recOld))
        val convMid = createConversation("2", "Mid", "2", listOf(recMid))
        val convNew = createConversation("3", "New", "3", listOf(recNew))

        val sorted = ConversationFilterHelper.sortConversations(
            listOf(convNew, convOld, convMid),
            SortOrder.OLDEST_FIRST
        )

        assertEquals("Old", sorted[0].callerName)
        assertEquals("Mid", sorted[1].callerName)
        assertEquals("New", sorted[2].callerName)
    }

    @Test
    fun test10_newestFirstRecordingSorting() {
        val rec1 = createAudioFile("rec1.mp3", "A", "1", LocalDateTime.of(2026, 10, 13, 10, 0))
        val rec2 = createAudioFile("rec2.mp3", "A", "1", LocalDateTime.of(2026, 10, 13, 14, 0))
        val rec3 = createAudioFile("rec3.mp3", "A", "1", LocalDateTime.of(2026, 10, 13, 8, 0))

        val sorted = ConversationFilterHelper.sortRecordings(
            listOf(rec1, rec2, rec3),
            SortOrder.NEWEST_FIRST
        )

        assertEquals("rec2.mp3", sorted[0].name) // 14:00
        assertEquals("rec1.mp3", sorted[1].name) // 10:00
        assertEquals("rec3.mp3", sorted[2].name) // 08:00
    }

    @Test
    fun test11_oldestFirstRecordingSorting() {
        val rec1 = createAudioFile("rec1.mp3", "A", "1", LocalDateTime.of(2026, 10, 13, 10, 0))
        val rec2 = createAudioFile("rec2.mp3", "A", "1", LocalDateTime.of(2026, 10, 13, 14, 0))
        val rec3 = createAudioFile("rec3.mp3", "A", "1", LocalDateTime.of(2026, 10, 13, 8, 0))

        val sorted = ConversationFilterHelper.sortRecordings(
            listOf(rec1, rec2, rec3),
            SortOrder.OLDEST_FIRST
        )

        assertEquals("rec3.mp3", sorted[0].name) // 08:00
        assertEquals("rec1.mp3", sorted[1].name) // 10:00
        assertEquals("rec2.mp3", sorted[2].name) // 14:00
    }

    @Test
    fun test12_filteringDoesNotMutateOriginalConversation() {
        val rec1 = createAudioFile("call_001.mp3", "Ishwar", "00919529485776", LocalDateTime.of(2026, 10, 12, 10, 0))
        val rec2 = createAudioFile("call_002.mp3", "Ishwar", "00919529485776", LocalDateTime.of(2026, 10, 13, 10, 0))

        val conversation = createConversation("1", "Ishwar", "00919529485776", listOf(rec1, rec2))

        val filteredRecordings = ConversationFilterHelper.filterRecordings(conversation.recordings, "call_001")
        assertEquals(1, filteredRecordings.size)

        // Original conversation recordings list remains untouched with 2 elements
        assertEquals(2, conversation.recordings.size)
    }

    @Test
    fun test13_conversationRecordingCountRemainsCorrectAfterFiltering() {
        val rec1 = createAudioFile("call_001.mp3", "Ishwar", "00919529485776", LocalDateTime.of(2026, 10, 12, 10, 0))
        val rec2 = createAudioFile("call_002.mp3", "Ishwar", "00919529485776", LocalDateTime.of(2026, 10, 13, 10, 0))
        val rec3 = createAudioFile("call_003.mp3", "Ishwar", "00919529485776", LocalDateTime.of(2026, 10, 14, 10, 0))

        val conversation = createConversation("1", "Ishwar", "00919529485776", listOf(rec1, rec2, rec3))

        // Even if filter matches only 1 item
        val filtered = ConversationFilterHelper.filterRecordings(conversation.recordings, "call_001")
        assertEquals(1, filtered.size)

        // The conversation property itself still reports 3
        assertEquals(3, conversation.recordingCount)
    }

    @Test
    fun test14_emptyConversationList() {
        val filtered = ConversationFilterHelper.filterConversations(emptyList(), "query")
        assertTrue(filtered.isEmpty())

        val sorted = ConversationFilterHelper.sortConversations(emptyList(), SortOrder.NEWEST_FIRST)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun test15_emptyRecordingList() {
        val filtered = ConversationFilterHelper.filterRecordings(emptyList(), "query")
        assertTrue(filtered.isEmpty())

        val sorted = ConversationFilterHelper.sortRecordings(emptyList(), SortOrder.NEWEST_FIRST)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun test16_unknownAndMissingMetadataDoesNotCrash() {
        val malformedFile = createAudioFile("", null, null, null, lastModified = 0L)
        val conv = createConversation("anon", null, null, listOf(malformedFile))

        val filteredConv = ConversationFilterHelper.filterConversations(listOf(conv), "test")
        assertFalse(filteredConv.contains(conv))

        val filteredRecs = ConversationFilterHelper.filterRecordings(listOf(malformedFile), "test")
        assertTrue(filteredRecs.isEmpty())

        val sortedConv = ConversationFilterHelper.sortConversations(listOf(conv), SortOrder.NEWEST_FIRST)
        assertEquals(1, sortedConv.size)

        val sortedRecs = ConversationFilterHelper.sortRecordings(listOf(malformedFile), SortOrder.OLDEST_FIRST)
        assertEquals(1, sortedRecs.size)
    }
}
