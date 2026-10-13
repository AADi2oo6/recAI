package com.recai.app.data.parser

object PhoneNumberMatcher {

    /**
     * Normalizes a phone number for matching:
     * - Strips spaces, dashes, parentheses, dots, commas.
     * - Strips leading "+" if present.
     * - Strips leading international exit code "00" (e.g. 0091 -> 91).
     * - Preserves digits only.
     */
    fun normalize(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""

        var digitsOnly = rawNumber.replace(Regex("""[\s\-\(\)\.\,\+]"""), "")

        // Normalize leading 00 exit code prefix if followed by international code
        if (digitsOnly.startsWith("00") && digitsOnly.length > 4) {
            digitsOnly = digitsOnly.substring(2)
        }

        return digitsOnly
    }

    /**
     * Determines whether two phone numbers match.
     * Compares their normalized representations:
     * - Exact normalized match
     * - Suffix match if both numbers have at least 7 digits (e.g., local 10 digits matching +91 country prefix)
     */
    fun matches(numberA: String?, numberB: String?): Boolean {
        val normA = normalize(numberA)
        val normB = normalize(numberB)

        if (normA.isEmpty() || normB.isEmpty()) {
            return false
        }

        // Exact normalized match
        if (normA == normB) {
            return true
        }

        // Suffix match for local vs international formatting (e.g. "8830714901" vs "918830714901")
        // Require at least 7 matching digits to avoid false positives with short codes
        val minLen = minOf(normA.length, normB.length)
        if (minLen >= 7) {
            val suffixA = normA.takeLast(minLen)
            val suffixB = normB.takeLast(minLen)
            if (suffixA == suffixB) {
                return true
            }
        }

        return false
    }
}
