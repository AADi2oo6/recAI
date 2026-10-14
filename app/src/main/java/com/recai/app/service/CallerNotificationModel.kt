package com.recai.app.service

import com.recai.app.data.model.Conversation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Variants of incoming caller notifications.
 */
enum class NotificationVariant {
    KNOWN_CALLER_WITH_RECORDINGS,
    KNOWN_CALLER_WITHOUT_RECORDINGS,
    UNKNOWN_NUMBER_WITH_RECORDINGS,
    UNKNOWN_NUMBER_WITHOUT_RECORDINGS,
    PRIVATE_UNKNOWN_NUMBER
}

/**
 * Pure data model representing the formatted content of an incoming call notification.
 * Decoupled from Android NotificationCompat APIs so it can be thoroughly unit-tested.
 */
data class CallerNotificationData(
    val variant: NotificationVariant,
    val title: String,
    val contentText: String,
    val bigText: String,
    val actionLabel: String,
    val callerName: String?,
    val displayPhoneNumber: String,
    val recordingCountText: String,
    val latestRecordingText: String?
)

/**
 * Pure decision and formatting logic for incoming call notifications.
 */
object CallerNotificationFormatter {

    private const val DEFAULT_TITLE = "Incoming call"
    private const val PRIVATE_NUMBER_DISPLAY = "Private / Unknown number"
    private const val NO_RECORDINGS_TEXT = "No previous recordings"

    /**
     * Formats phone numbers cleanly for notification display only.
     * E.g., "+919322477239" -> "+91 93224 77239", "9322477239" -> "93224 77239".
     * If formatting cannot be safely determined, returns the cleaned original number.
     * Does NOT alter the underlying stored phone number or matching logic.
     */
    fun formatPhoneNumberForDisplay(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) {
            return PRIVATE_NUMBER_DISPLAY
        }

        val trimmed = rawNumber.trim()

        // If already formatted with spaces or contains masked characters (e.g. 'X'), preserve as-is
        if (trimmed.contains(' ') || trimmed.contains('X', ignoreCase = true)) {
            return trimmed
        }

        val digitsOnly = trimmed.filter { it.isDigit() }

        // International India: +91 or 0091 followed by 10 digits
        if ((trimmed.startsWith("+91") && digitsOnly.length == 12) ||
            (trimmed.startsWith("0091") && digitsOnly.length == 14) ||
            (trimmed.startsWith("91") && digitsOnly.length == 12)
        ) {
            val local10 = digitsOnly.takeLast(10)
            return "+91 ${local10.substring(0, 5)} ${local10.substring(5)}"
        }

        // Local 10 digits (e.g. 9322477239)
        if (digitsOnly.length == 10 && !trimmed.startsWith("+")) {
            return "${digitsOnly.substring(0, 5)} ${digitsOnly.substring(5)}"
        }

        // National 11 digits with leading 0 trunk prefix (e.g. 09322477239)
        if (digitsOnly.length == 11 && trimmed.startsWith("0")) {
            return "0${digitsOnly.substring(1, 6)} ${digitsOnly.substring(6)}"
        }

        // US / North American numbers: +1 followed by 10 digits
        if (trimmed.startsWith("+1") && digitsOnly.length == 11) {
            val local10 = digitsOnly.takeLast(10)
            return "+1 ${local10.substring(0, 3)} ${local10.substring(3, 6)} ${local10.substring(6)}"
        }

        // Fallback: Return original trimmed number
        return trimmed
    }

    /**
     * Formats recording count with correct singular/plural wording:
     * - "No previous recordings"
     * - "1 previous recording"
     * - "$count previous recordings"
     */
    fun formatRecordingCount(count: Int): String {
        return when {
            count <= 0 -> NO_RECORDINGS_TEXT
            count == 1 -> "1 previous recording"
            else -> "$count previous recordings"
        }
    }

    /**
     * Formats the latest recording date/time with relative and friendly patterns:
     * - "Latest: Today, 6:42 PM"
     * - "Latest: Yesterday, 11:30 AM"
     * - "Latest: MMM d, h:mm a"
     */
    fun formatLatestRecordingDate(
        dateTime: LocalDateTime?,
        lastModifiedFallback: Long,
        now: LocalDateTime = LocalDateTime.now()
    ): String? {
        val ldt = dateTime ?: if (lastModifiedFallback > 0L) {
            Instant.ofEpochMilli(lastModifiedFallback)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        } else {
            null
        } ?: return null

        val recordDate = ldt.toLocalDate()
        val nowDate = now.toLocalDate()

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val timeStr = ldt.format(timeFormatter)

        val dateStr = when (recordDate) {
            nowDate -> "Today, $timeStr"
            nowDate.minusDays(1) -> "Yesterday, $timeStr"
            else -> {
                if (recordDate.year == nowDate.year) {
                    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
                    ldt.format(formatter)
                } else {
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US)
                    ldt.format(formatter)
                }
            }
        }
        return "Latest: $dateStr"
    }

    /**
     * Builds the complete [CallerNotificationData] for an incoming call.
     */
    fun buildNotificationData(
        rawPhoneNumber: String?,
        conversation: Conversation?,
        now: LocalDateTime = LocalDateTime.now()
    ): CallerNotificationData {
        val hasPhoneNumber = !rawPhoneNumber.isNullOrBlank() || !conversation?.phoneNumber.isNullOrBlank()

        // Handle private or completely unknown number
        if (!hasPhoneNumber) {
            val bigText = "$PRIVATE_NUMBER_DISPLAY\n$NO_RECORDINGS_TEXT"
            val contentText = "$PRIVATE_NUMBER_DISPLAY • $NO_RECORDINGS_TEXT"
            return CallerNotificationData(
                variant = NotificationVariant.PRIVATE_UNKNOWN_NUMBER,
                title = DEFAULT_TITLE,
                contentText = contentText,
                bigText = bigText,
                actionLabel = "Open recAI",
                callerName = null,
                displayPhoneNumber = PRIVATE_NUMBER_DISPLAY,
                recordingCountText = NO_RECORDINGS_TEXT,
                latestRecordingText = null
            )
        }

        // Determine if there is a known caller name
        val rawCallerName = conversation?.callerName?.trim()
        val isKnownCaller = !rawCallerName.isNullOrBlank() &&
                !rawCallerName.equals("Unknown caller", ignoreCase = true) &&
                rawCallerName != conversation?.phoneNumber &&
                rawCallerName != rawPhoneNumber

        val callerName = if (isKnownCaller) rawCallerName else null
        val effectivePhone = conversation?.phoneNumber ?: rawPhoneNumber
        val displayPhone = formatPhoneNumberForDisplay(effectivePhone)

        val count = conversation?.recordingCount ?: 0
        val countText = formatRecordingCount(count)

        val latestText = if (count > 0 && conversation != null) {
            val latestRec = conversation.latestRecording
            formatLatestRecordingDate(
                dateTime = latestRec?.metadata?.recordingDateTime,
                lastModifiedFallback = latestRec?.lastModified ?: 0L,
                now = now
            )
        } else {
            null
        }

        return when {
            // CASE 3: Known caller WITH previous recordings
            isKnownCaller && count > 0 -> {
                val bigTextLines = mutableListOf<String>().apply {
                    add(callerName!!)
                    add(displayPhone)
                    add(countText)
                    if (latestText != null) add(latestText)
                }
                CallerNotificationData(
                    variant = NotificationVariant.KNOWN_CALLER_WITH_RECORDINGS,
                    title = DEFAULT_TITLE,
                    contentText = "$callerName • $countText",
                    bigText = bigTextLines.joinToString("\n"),
                    actionLabel = "Open recordings",
                    callerName = callerName,
                    displayPhoneNumber = displayPhone,
                    recordingCountText = countText,
                    latestRecordingText = latestText
                )
            }

            // CASE 4: Known caller with NO previous recordings
            isKnownCaller && count == 0 -> {
                val bigText = "$callerName\n$displayPhone\n$NO_RECORDINGS_TEXT"
                CallerNotificationData(
                    variant = NotificationVariant.KNOWN_CALLER_WITHOUT_RECORDINGS,
                    title = DEFAULT_TITLE,
                    contentText = "$callerName • $NO_RECORDINGS_TEXT",
                    bigText = bigText,
                    actionLabel = "Open conversation",
                    callerName = callerName,
                    displayPhoneNumber = displayPhone,
                    recordingCountText = countText,
                    latestRecordingText = null
                )
            }

            // CASE 2: Unknown number WITH previous recordings
            !isKnownCaller && count > 0 -> {
                val bigTextLines = mutableListOf<String>().apply {
                    add(displayPhone)
                    add(countText)
                    if (latestText != null) add(latestText)
                }
                CallerNotificationData(
                    variant = NotificationVariant.UNKNOWN_NUMBER_WITH_RECORDINGS,
                    title = DEFAULT_TITLE,
                    contentText = "$displayPhone • $countText",
                    bigText = bigTextLines.joinToString("\n"),
                    actionLabel = "Open recordings",
                    callerName = null,
                    displayPhoneNumber = displayPhone,
                    recordingCountText = countText,
                    latestRecordingText = latestText
                )
            }

            // CASE 1: Unknown number with NO previous recordings
            else -> {
                val bigText = "$displayPhone\n$NO_RECORDINGS_TEXT"
                CallerNotificationData(
                    variant = NotificationVariant.UNKNOWN_NUMBER_WITHOUT_RECORDINGS,
                    title = DEFAULT_TITLE,
                    contentText = "$displayPhone • $NO_RECORDINGS_TEXT",
                    bigText = bigText,
                    actionLabel = "Open recAI",
                    callerName = null,
                    displayPhoneNumber = displayPhone,
                    recordingCountText = countText,
                    latestRecordingText = null
                )
            }
        }
    }
}
