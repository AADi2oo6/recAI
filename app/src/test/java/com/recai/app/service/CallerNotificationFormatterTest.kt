package com.recai.app.service

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

class CallerNotificationFormatterTest {

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
    fun test1_knownCallerWithRecordings() {
        val now = LocalDateTime.of(2026, 10, 14, 20, 0)
        val recTime = LocalDateTime.of(2026, 10, 14, 18, 42) // Today 6:42 PM
        val rec1 = createAudioFile("rec1.mp3", "Darshan", "00919876543210", recTime)
        val rec2 = createAudioFile("rec2.mp3", "Darshan", "00919876543210", recTime.minusHours(2))
        val rec3 = createAudioFile("rec3.mp3", "Darshan", "00919876543210", recTime.minusHours(5))
        val conversation = createConversation("conv-1", "Darshan", "00919876543210", listOf(rec1, rec2, rec3))

        val data = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = "+919876543210",
            conversation = conversation,
            now = now
        )

        assertEquals(NotificationVariant.KNOWN_CALLER_WITH_RECORDINGS, data.variant)
        assertEquals("Incoming call", data.title)
        assertEquals("Open recordings", data.actionLabel)
        assertEquals("Darshan", data.callerName)
        assertEquals("Darshan • 3 previous recordings", data.contentText)
        assertTrue(data.bigText.contains("Darshan"))
        assertTrue(data.bigText.contains("+91 98765 43210"))
        assertTrue(data.bigText.contains("3 previous recordings"))
        assertTrue(data.bigText.contains("Latest: Today, 6:42 PM"))
    }

    @Test
    fun test2_knownCallerWithNoRecordings() {
        val conversation = createConversation("conv-1", "Darshan", "+91 98765 43210", emptyList())

        val data = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = "+91 98765 43210",
            conversation = conversation
        )

        assertEquals(NotificationVariant.KNOWN_CALLER_WITHOUT_RECORDINGS, data.variant)
        assertEquals("Incoming call", data.title)
        assertEquals("Open conversation", data.actionLabel)
        assertEquals("Darshan", data.callerName)
        assertEquals("Darshan • No previous recordings", data.contentText)
        assertTrue(data.bigText.contains("Darshan"))
        assertTrue(data.bigText.contains("+91 98765 43210"))
        assertTrue(data.bigText.contains("No previous recordings"))
        assertNull(data.latestRecordingText)
    }

    @Test
    fun test3_unknownNumberWithRecordings() {
        val now = LocalDateTime.of(2026, 10, 14, 20, 0)
        val recTime = LocalDateTime.of(2026, 10, 14, 18, 42)
        val rec1 = createAudioFile("rec1.mp3", null, "+919322477239", recTime)
        val rec2 = createAudioFile("rec2.mp3", null, "+919322477239", recTime.minusHours(1))
        val rec3 = createAudioFile("rec3.mp3", null, "+919322477239", recTime.minusHours(2))
        val conversation = createConversation("conv-2", null, "+919322477239", listOf(rec1, rec2, rec3))

        val data = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = "+919322477239",
            conversation = conversation,
            now = now
        )

        assertEquals(NotificationVariant.UNKNOWN_NUMBER_WITH_RECORDINGS, data.variant)
        assertEquals("Incoming call", data.title)
        assertEquals("Open recordings", data.actionLabel)
        assertNull(data.callerName)
        assertFalse(data.bigText.contains("Unknown caller"))
        assertFalse(data.contentText.contains("Unknown caller"))
        assertEquals("+91 93224 77239 • 3 previous recordings", data.contentText)
        assertTrue(data.bigText.contains("+91 93224 77239"))
        assertTrue(data.bigText.contains("3 previous recordings"))
        assertTrue(data.bigText.contains("Latest: Today, 6:42 PM"))
    }

    @Test
    fun test4_unknownNumberWithNoRecordings() {
        val data = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = "+919322477239",
            conversation = null
        )

        assertEquals(NotificationVariant.UNKNOWN_NUMBER_WITHOUT_RECORDINGS, data.variant)
        assertEquals("Incoming call", data.title)
        assertEquals("Open recAI", data.actionLabel)
        assertNull(data.callerName)
        assertFalse(data.bigText.contains("Unknown caller"))
        assertFalse(data.contentText.contains("Unknown caller"))
        assertEquals("+91 93224 77239 • No previous recordings", data.contentText)
        assertEquals("+91 93224 77239\nNo previous recordings", data.bigText)
        assertNull(data.latestRecordingText)
    }

    @Test
    fun test5_privateOrMissingNumber() {
        val dataNull = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = null,
            conversation = null
        )

        assertEquals(NotificationVariant.PRIVATE_UNKNOWN_NUMBER, dataNull.variant)
        assertEquals("Incoming call", dataNull.title)
        assertEquals("Open recAI", dataNull.actionLabel)
        assertEquals("Private / Unknown number • No previous recordings", dataNull.contentText)
        assertEquals("Private / Unknown number\nNo previous recordings", dataNull.bigText)
        assertEquals("Private / Unknown number", dataNull.displayPhoneNumber)

        val dataBlank = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = "   ",
            conversation = null
        )
        assertEquals(NotificationVariant.PRIVATE_UNKNOWN_NUMBER, dataBlank.variant)
    }

    @Test
    fun test6_singularRecordingCount() {
        assertEquals("1 previous recording", CallerNotificationFormatter.formatRecordingCount(1))
    }

    @Test
    fun test7_pluralRecordingCount() {
        assertEquals("No previous recordings", CallerNotificationFormatter.formatRecordingCount(0))
        assertEquals("No previous recordings", CallerNotificationFormatter.formatRecordingCount(-1))
        assertEquals("2 previous recordings", CallerNotificationFormatter.formatRecordingCount(2))
        assertEquals("10 previous recordings", CallerNotificationFormatter.formatRecordingCount(10))
    }

    @Test
    fun test8_latestRecordingInformation_relativeDates() {
        val now = LocalDateTime.of(2026, 10, 14, 20, 0)

        // Today
        val todayTime = LocalDateTime.of(2026, 10, 14, 18, 42)
        val todayResult = CallerNotificationFormatter.formatLatestRecordingDate(todayTime, 0L, now)
        assertEquals("Latest: Today, 6:42 PM", todayResult)

        // Yesterday
        val yesterdayTime = LocalDateTime.of(2026, 10, 13, 11, 30)
        val yesterdayResult = CallerNotificationFormatter.formatLatestRecordingDate(yesterdayTime, 0L, now)
        assertEquals("Latest: Yesterday, 11:30 AM", yesterdayResult)

        // Older date in same year
        val olderTime = LocalDateTime.of(2026, 4, 15, 10, 0)
        val olderResult = CallerNotificationFormatter.formatLatestRecordingDate(olderTime, 0L, now)
        assertNotNull(olderResult)
        assertTrue(olderResult!!.contains("Apr 15"))

        // Null dateTime with no fallback
        assertNull(CallerNotificationFormatter.formatLatestRecordingDate(null, 0L, now))
    }

    @Test
    fun test9_phoneNumberDisplayFormatting() {
        // India +91 10 digits
        assertEquals("+91 93224 77239", CallerNotificationFormatter.formatPhoneNumberForDisplay("+919322477239"))
        // India 0091 exit code
        assertEquals("+91 93224 77239", CallerNotificationFormatter.formatPhoneNumberForDisplay("00919322477239"))
        // India 91 prefix without plus
        assertEquals("+91 93224 77239", CallerNotificationFormatter.formatPhoneNumberForDisplay("919322477239"))
        // Local 10 digits
        assertEquals("93224 77239", CallerNotificationFormatter.formatPhoneNumberForDisplay("9322477239"))
        // National 11 digits with 0 trunk
        assertEquals("093224 77239", CallerNotificationFormatter.formatPhoneNumberForDisplay("09322477239"))
        // US +1
        assertEquals("+1 415 555 2671", CallerNotificationFormatter.formatPhoneNumberForDisplay("+14155552671"))
        // Already formatted / masked
        assertEquals("+91 98765 XXXXX", CallerNotificationFormatter.formatPhoneNumberForDisplay("+91 98765 XXXXX"))
        // Null / blank
        assertEquals("Private / Unknown number", CallerNotificationFormatter.formatPhoneNumberForDisplay(null))
        assertEquals("Private / Unknown number", CallerNotificationFormatter.formatPhoneNumberForDisplay(""))
    }

    @Test
    fun test10_callerNameEqualsPhoneNumberDoesNotDuplicateAsName() {
        val rec = createAudioFile("rec.mp3", "+919322477239", "+919322477239", LocalDateTime.now())
        val conversation = createConversation("1", "+919322477239", "+919322477239", listOf(rec))

        val data = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = "+919322477239",
            conversation = conversation
        )

        // When callerName equals the phone number, callerName should be treated as null
        assertNull(data.callerName)
        assertEquals(NotificationVariant.UNKNOWN_NUMBER_WITH_RECORDINGS, data.variant)
        assertFalse(data.bigText.contains("Unknown caller"))
    }
}
