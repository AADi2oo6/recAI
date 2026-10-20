package com.recai.app.data.parser

/**
 * Reusable formatter for phone numbers intended ONLY for display presentation in the UI.
 * Does NOT modify stored phone numbers, filename parsing, or PhoneNumberMatcher logic.
 */
object PhoneNumberFormatter {

    private const val DEFAULT_FALLBACK = "Not available"

    /**
     * Formats a phone number for UI display.
     * Recognized Indian formats (0091, +91, 91, 0-trunk, 10-digit) are formatted to:
     * "+91 XXXXX XXXXX".
     * Other recognizable formats (e.g. US +1) are cleanly formatted.
     * Unrecognized/malformed numbers fall back safely to the cleaned original string without corruption.
     * Null or blank inputs return [fallback].
     */
    fun format(rawNumber: String?, fallback: String = DEFAULT_FALLBACK): String {
        if (rawNumber.isNullOrBlank() || rawNumber.trim().equals("null", ignoreCase = true)) {
            return fallback
        }

        val trimmed = rawNumber.trim()

        // Strip common formatting symbols: spaces, dashes, parentheses, dots
        val stripped = trimmed.replace(Regex("""[\s\-\(\)\.]"""), "")

        // Check if digits-only (or starting with '+')
        val hasPlus = stripped.startsWith("+")
        val digitsOnly = stripped.filter { it.isDigit() }

        // 1. Indian format: starts with "+91" and followed by 10 digits (total 12 digits)
        if (hasPlus && stripped.startsWith("+91") && digitsOnly.length == 12) {
            val local10 = digitsOnly.takeLast(10)
            return "+91 ${local10.substring(0, 5)} ${local10.substring(5)}"
        }

        // 2. Indian format: starts with "0091" exit code followed by 10 digits (total 14 digits)
        if (stripped.startsWith("0091") && digitsOnly.length == 14) {
            val local10 = digitsOnly.takeLast(10)
            return "+91 ${local10.substring(0, 5)} ${local10.substring(5)}"
        }

        // 3. Indian format: 12 digits starting with "91" (e.g. "919699177117")
        if (digitsOnly.length == 12 && digitsOnly.startsWith("91") && !hasPlus) {
            val local10 = digitsOnly.takeLast(10)
            return "+91 ${local10.substring(0, 5)} ${local10.substring(5)}"
        }

        // 4. Indian format: 11 digits starting with "0" trunk prefix (e.g. "08830714901")
        if (digitsOnly.length == 11 && digitsOnly.startsWith("0")) {
            val local10 = digitsOnly.takeLast(10)
            return "+91 ${local10.substring(0, 5)} ${local10.substring(5)}"
        }

        // 5. Indian format: raw 10 digits (e.g. "8830714901")
        if (digitsOnly.length == 10 && !hasPlus) {
            return "+91 ${digitsOnly.substring(0, 5)} ${digitsOnly.substring(5)}"
        }

        // 6. US / North American format: starts with "+1" followed by 10 digits (total 11 digits)
        if (hasPlus && stripped.startsWith("+1") && digitsOnly.length == 11) {
            val local10 = digitsOnly.takeLast(10)
            return "+1 ${local10.substring(0, 3)} ${local10.substring(3, 6)} ${local10.substring(6)}"
        }

        // If it already has some custom spacing/masking (e.g. "+91 98765 XXXXX"), return trimmed
        if (trimmed.contains(' ') || trimmed.contains('X', ignoreCase = true)) {
            return trimmed
        }

        // Fallback: return the trimmed original input to avoid corrupting unknown numbers
        return trimmed
    }

    /**
     * Formats phone number for display, returning empty string if null or blank.
     */
    fun formatOrEmpty(rawNumber: String?): String {
        return format(rawNumber, fallback = "")
    }
}
