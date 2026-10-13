package com.recai.app.data.parser

import com.recai.app.data.model.RecordingMetadata
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object RecordingFilenameParser {

    // Pattern: <callerName>(<phoneNumber>)_<YYYYMMDDHHMMSS>
    // Caller name is captured lazily before '(', phone number inside '()', and 14 digits after '_'
    private val STANDARD_PATTERN = Regex("""^(.*?)\(([^)]+)\)_(\d{14})$""")

    // Fallback pattern when there are no parentheses, e.g. "Recording_20261013153000" or "9876543210_20261013153000"
    private val NO_PARENTHESES_PATTERN = Regex("""^(.*)_(\d{14})$""")

    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    /**
     * Parses a recording filename defensively into structured RecordingMetadata.
     * Never throws exceptions; malformed portions return null.
     */
    fun parse(fileName: String): RecordingMetadata {
        val baseName = fileName.substringBeforeLast('.')

        // 1. Try standard pattern: Name(Number)_Timestamp
        val standardMatch = STANDARD_PATTERN.matchEntire(baseName)
        if (standardMatch != null) {
            val rawName = standardMatch.groupValues[1].trim()
            val rawPhone = standardMatch.groupValues[2].trim()
            val rawTimestamp = standardMatch.groupValues[3]

            val callerName = rawName.ifBlank { null }
            val phoneNumber = rawPhone.ifBlank { null }
            val recordingDateTime = parseDateTime(rawTimestamp)

            return RecordingMetadata(
                callerName = callerName,
                phoneNumber = phoneNumber,
                recordingDateTime = recordingDateTime,
                originalFileName = fileName
            )
        }

        // 2. Try no-parentheses pattern: Prefix_Timestamp
        val noParenMatch = NO_PARENTHESES_PATTERN.matchEntire(baseName)
        if (noParenMatch != null) {
            val prefix = noParenMatch.groupValues[1].trim()
            val rawTimestamp = noParenMatch.groupValues[2]
            val recordingDateTime = parseDateTime(rawTimestamp)

            // Check if prefix looks like a pure phone number (only digits, spaces, hyphens, leading '+')
            val isPhoneOnly = prefix.isNotBlank() && prefix.matches(Regex("""^\+?[\d\s\-]+$"""))

            val callerName = if (isPhoneOnly) null else prefix.ifBlank { null }
            val phoneNumber = if (isPhoneOnly) prefix else null

            return RecordingMetadata(
                callerName = callerName,
                phoneNumber = phoneNumber,
                recordingDateTime = recordingDateTime,
                originalFileName = fileName
            )
        }

        // 3. Fallback for arbitrary/unknown filenames (e.g. "Some random recording.mp3")
        return RecordingMetadata(
            callerName = null,
            phoneNumber = null,
            recordingDateTime = null,
            originalFileName = fileName
        )
    }

    private fun parseDateTime(rawTimestamp: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(rawTimestamp, DATE_TIME_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
