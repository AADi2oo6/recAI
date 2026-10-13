package com.recai.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class RecordingFilenameParserTest {

    @Test
    fun test1_normalPattern() {
        val fileName = "Darshan(00919876543210)_20261013200200.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertEquals("Darshan", metadata.callerName)
        assertEquals("00919876543210", metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 20, 2, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test2_callerNameContainingSpaces() {
        val fileName = "Darshan Roommate(00919876543210)_20261013200200.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertEquals("Darshan Roommate", metadata.callerName)
        assertEquals("00919876543210", metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 20, 2, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test3_callerNameContainingEmojiAndSpecialCharacters() {
        val fileName = "Darshan 🎉 & Team(00919876543210)_20261013200200.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertEquals("Darshan 🎉 & Team", metadata.callerName)
        assertEquals("00919876543210", metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 20, 2, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test4_missingPhoneNumberWithTimestamp() {
        val fileName = "Recording_20261013153000.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertEquals("Recording", metadata.callerName)
        assertNull(metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 15, 30, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test5_missingTimestamp() {
        val fileName = "Darshan Roommate(00919876543210).mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        // Does not match standard pattern because timestamp is missing; returns safe fallback
        assertNull(metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test6_completelyUnknownFilename() {
        val fileName = "Some random recording.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertNull(metadata.callerName)
        assertNull(metadata.phoneNumber)
        assertNull(metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test7_differentAudioExtensionsM4aAndWav() {
        val fileNameM4a = "Alice(9876543210)_20261013101500.m4a"
        val metaM4a = RecordingFilenameParser.parse(fileNameM4a)
        assertEquals("Alice", metaM4a.callerName)
        assertEquals("9876543210", metaM4a.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 10, 15, 0), metaM4a.recordingDateTime)
        assertEquals(fileNameM4a, metaM4a.originalFileName)

        val fileNameWav = "Bob(1234567890)_20261013112000.wav"
        val metaWav = RecordingFilenameParser.parse(fileNameWav)
        assertEquals("Bob", metaWav.callerName)
        assertEquals("1234567890", metaWav.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 11, 20, 0), metaWav.recordingDateTime)
        assertEquals(fileNameWav, metaWav.originalFileName)
    }

    @Test
    fun test8_veryLongFilename() {
        val longCallerName = "A".repeat(150)
        val fileName = "${longCallerName}(00919876543210)_20261013200200.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertEquals(longCallerName, metadata.callerName)
        assertEquals("00919876543210", metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 20, 2, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test9_phoneNumberContainingPlusAndFormatting() {
        val fileName = "Work Office(+91 98765-43210)_20261013200200.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertEquals("Work Office", metadata.callerName)
        assertEquals("+91 98765-43210", metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 20, 2, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test10_purePhoneNumberPrefixWithoutParentheses() {
        val fileName = "9876543210_20261013153000.mp3"
        val metadata = RecordingFilenameParser.parse(fileName)

        assertNull(metadata.callerName)
        assertEquals("9876543210", metadata.phoneNumber)
        assertEquals(LocalDateTime.of(2026, 10, 13, 15, 30, 0), metadata.recordingDateTime)
        assertEquals(fileName, metadata.originalFileName)
    }

    @Test
    fun test11_malformedFilenameMustNeverCrash() {
        val malformedCases = listOf(
            "",
            "___",
            "()_()",
            "(98765)_20261013200200.mp3",
            "Darshan()_20261013200200.mp3",
            "Darshan(98765)_99999999999999.mp3", // invalid timestamp date
            "a.b.c.d.e",
            "(()))___20261013",
            "Darshan(123)_20261013200200" // no extension
        )

        for (malformed in malformedCases) {
            val result = RecordingFilenameParser.parse(malformed)
            assertNotNull(result)
            assertEquals(malformed, result.originalFileName)
        }
    }
}
