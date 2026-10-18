package com.recai.app.data.model

import com.recai.app.data.parser.PhoneNumberFormatter
import com.recai.app.data.parser.PhoneNumberMatcher

/**
 * Represents a caller entry in the tracking management screen.
 */
data class TrackingEntry(
    val phoneNumber: String,
    val displayName: String?,
    val isTracked: Boolean,
    val recordingCount: Int = 0
) {
    /**
     * Normalized phone number used as the stable internal key for tracking persistence.
     */
    val normalizedPhoneNumber: String
        get() = PhoneNumberMatcher.normalize(phoneNumber)

    /**
     * Cleanly formatted phone number for UI display.
     */
    val displayPhoneNumber: String
        get() = PhoneNumberFormatter.format(phoneNumber)
}
