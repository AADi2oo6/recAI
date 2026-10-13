package com.recai.app.data.parser

import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.RecordingMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PhoneNumberMatcherTest {

    private fun createAudioFile(
        name: String,
        callerName: String?,
        phoneNumber: String?,
        dateTime: LocalDateTime?
    ): AudioFile {
        return AudioFile(
            name = name,
            uri = null,
            mimeType = "audio/mpeg",
            size = 1024L,
            lastModified = 1000L,
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
    fun test1_exactPhoneNumberMatch() {
        assertTrue(PhoneNumberMatcher.matches("00918830714901", "00918830714901"))
        assertTrue(PhoneNumberMatcher.matches("9876543210", "9876543210"))
    }

    @Test
    fun test2_matchWithPlusCountryCodeFormatting() {
        assertTrue(PhoneNumberMatcher.matches("00918830714901", "+918830714901"))
        assertTrue(PhoneNumberMatcher.matches("+14155552671", "0014155552671"))
    }

    @Test
    fun test3_matchWithSpaces() {
        assertTrue(PhoneNumberMatcher.matches("00918830714901", "+91 88307 14901"))
        assertTrue(PhoneNumberMatcher.matches("98765 43210", "9876543210"))
    }

    @Test
    fun test4_matchWithHyphens() {
        assertTrue(PhoneNumberMatcher.matches("00918830714901", "+91-88307-14901"))
        assertTrue(PhoneNumberMatcher.matches("123-456-7890", "1234567890"))
    }

    @Test
    fun test5_partialLocalFormattingMatch() {
        // Local 10 digits vs country-code prefixed stored number
        assertTrue(PhoneNumberMatcher.matches("00918830714901", "8830714901"))
        assertTrue(PhoneNumberMatcher.matches("8830714901", "+918830714901"))
    }

    @Test
    fun test6_differentPhoneNumbersDoNotMatch() {
        assertFalse(PhoneNumberMatcher.matches("00918830714901", "00919699177117"))
        assertFalse(PhoneNumberMatcher.matches("1234567890", "9876543210"))
    }

    @Test
    fun test7_nullIncomingNumberIsHandledSafely() {
        assertFalse(PhoneNumberMatcher.matches("00918830714901", null))
        assertEquals("", PhoneNumberMatcher.normalize(null))
    }

    @Test
    fun test8_emptyIncomingNumberIsHandledSafely() {
        assertFalse(PhoneNumberMatcher.matches("00918830714901", ""))
        assertFalse(PhoneNumberMatcher.matches("00918830714901", "   "))
        assertEquals("", PhoneNumberMatcher.normalize(""))
    }

    @Test
    fun test9_nullConversationPhoneNumberDoesNotCrash() {
        assertFalse(PhoneNumberMatcher.matches(null, "00918830714901"))
        assertFalse(PhoneNumberMatcher.matches(null, null))
    }

    @Test
    fun test10_unknownCallerProducesNoMatchingConversation() {
        val conversations = listOf(
            createConversation("1", "Ishwar", "00918830714901", emptyList()),
            createConversation("2", "Darshan", "00919699177117", emptyList())
        )
        val incoming = "+91 99999 88888"
        val match = conversations.firstOrNull { PhoneNumberMatcher.matches(it.phoneNumber, incoming) }
        assertNull(match)
    }

    @Test
    fun test11_samePhoneNumberWithDifferentCallerNamesStillMatchesConversation() {
        val rec1 = createAudioFile("rec1.mp3", "Ishwar", "00918830714901", LocalDateTime.now())
        val rec2 = createAudioFile("rec2.mp3", "Ishwar Tatkal", "00918830714901", LocalDateTime.now())
        val conversation = createConversation("1", "Ishwar Tatkal Ta", "00918830714901", listOf(rec1, rec2))

        val incoming = "8830714901"
        assertTrue(PhoneNumberMatcher.matches(conversation.phoneNumber, incoming))
    }

    @Test
    fun test12_internationalStyleNumbersDoNotGetIncorrectlyMerged() {
        // Different numbers with different prefixes but different local digits
        assertFalse(PhoneNumberMatcher.matches("+14155552671", "+44155552672"))
        assertFalse(PhoneNumberMatcher.matches("00919876543210", "00449876543211"))
    }

    @Test
    fun test13_knownCallerReturnsCorrectRecordingCount() {
        val rec1 = createAudioFile("rec1.mp3", "Ishwar", "00918830714901", LocalDateTime.now())
        val rec2 = createAudioFile("rec2.mp3", "Ishwar", "00918830714901", LocalDateTime.now())
        val rec3 = createAudioFile("rec3.mp3", "Ishwar", "00918830714901", LocalDateTime.now())
        val conversation = createConversation("1", "Ishwar", "00918830714901", listOf(rec1, rec2, rec3))

        val incoming = "+91 88307 14901"
        assertTrue(PhoneNumberMatcher.matches(conversation.phoneNumber, incoming))
        assertEquals(3, conversation.recordingCount)
    }

    @Test
    fun test14_knownCallerReturnsCorrectLatestRecording() {
        val recOld = createAudioFile("old.mp3", "Ishwar", "00918830714901", LocalDateTime.of(2026, 4, 15, 10, 0))
        val recNew = createAudioFile("new.mp3", "Ishwar", "00918830714901", LocalDateTime.of(2026, 10, 13, 21, 40))
        val conversation = createConversation("1", "Ishwar", "00918830714901", listOf(recNew, recOld))

        val incoming = "00918830714901"
        assertTrue(PhoneNumberMatcher.matches(conversation.phoneNumber, incoming))
        assertNotNull(conversation.latestRecording)
        assertEquals("new.mp3", conversation.latestRecording?.name)
    }

    @Test
    fun test15_missingCallerNameFallsBackSafely() {
        val rec = createAudioFile("anon.mp3", null, "00918830714901", LocalDateTime.now())
        val conversation = createConversation("1", null, "00918830714901", listOf(rec))

        val incoming = "8830714901"
        assertTrue(PhoneNumberMatcher.matches(conversation.phoneNumber, incoming))
        assertNull(conversation.callerName)
        val fallbackDisplay = conversation.callerName ?: "Unknown caller"
        assertEquals("Unknown caller", fallbackDisplay)
    }

    @Test
    fun test16_knownContactMatchingExistingRecording() {
        val rec = createAudioFile("rec.mp3", "Darshan Roommate", "00919699177117", LocalDateTime.now())
        val conversations = listOf(
            createConversation("conv-1", "Darshan Roommate", "00919699177117", listOf(rec))
        )
        val incoming = "+91 96991 77117"
        val match = conversations.firstOrNull { PhoneNumberMatcher.matches(it.phoneNumber, incoming) }
        assertNotNull(match)
        assertEquals("Darshan Roommate", match?.callerName)
        assertEquals(1, match?.recordingCount)
    }

    @Test
    fun test17_knownContactWithNoExistingRecording() {
        // Contact is known to user or contacts list, but no conversation/recording exists in recAI folder
        val conversations = listOf(
            createConversation("conv-1", "Darshan Roommate", "00919699177117", emptyList())
        )
        val incomingOtherKnown = "+91 98220 12345"
        val match = conversations.firstOrNull { PhoneNumberMatcher.matches(it.phoneNumber, incomingOtherKnown) }
        assertNull(match)
    }

    @Test
    fun test18_unknownNumberMatchingExistingRecording() {
        // Caller has no name in contacts/recordings (unknown number), but recordings exist from past calls
        val rec = createAudioFile("rec_raw.mp3", null, "00919529485776", LocalDateTime.now())
        val conversations = listOf(
            createConversation("conv-1", null, "00919529485776", listOf(rec))
        )
        val incoming = "9529485776"
        val match = conversations.firstOrNull { PhoneNumberMatcher.matches(it.phoneNumber, incoming) }
        assertNotNull(match)
        assertNull(match?.callerName)
        assertEquals(1, match?.recordingCount)
    }

    @Test
    fun test19_unknownNumberWithNoExistingRecording() {
        // Unknown incoming caller with zero past recordings
        val conversations = listOf(
            createConversation("conv-1", "Lab Hade", "00919529485776", listOf(
                createAudioFile("rec.mp3", "Lab Hade", "00919529485776", LocalDateTime.now())
            ))
        )
        val incoming = "+91 91111 22222"
        val match = conversations.firstOrNull { PhoneNumberMatcher.matches(it.phoneNumber, incoming) }
        assertNull(match)
    }

    @Test
    fun test20_formattedIncomingNumberMatchingStoredRecording() {
        val storedNumber = "00918830714901"
        // Various formatted representations of incoming call numbers
        assertTrue(PhoneNumberMatcher.matches(storedNumber, "+91 88307-14901"))
        assertTrue(PhoneNumberMatcher.matches(storedNumber, "(+91) 88307 14901"))
        assertTrue(PhoneNumberMatcher.matches(storedNumber, "+918830714901"))
        assertTrue(PhoneNumberMatcher.matches(storedNumber, "8830714901"))
        assertTrue(PhoneNumberMatcher.matches(storedNumber, "08830714901"))
    }

    @Test
    fun test21_privateOrMissingIncomingNumberHandling() {
        val storedNumber = "00918830714901"
        assertFalse(PhoneNumberMatcher.matches(storedNumber, null))
        assertFalse(PhoneNumberMatcher.matches(storedNumber, ""))
        assertFalse(PhoneNumberMatcher.matches(storedNumber, "   "))
        assertFalse(PhoneNumberMatcher.matches(storedNumber, "Private Number"))
        assertFalse(PhoneNumberMatcher.matches(storedNumber, "UNKNOWN"))
        assertFalse(PhoneNumberMatcher.matches(storedNumber, "RESTRICTED"))
    }
}

