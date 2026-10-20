package com.recai.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneActionHelperTest {

    @Test
    fun testIsActionablePhoneNumber_validNumbers() {
        assertTrue(PhoneActionHelper.isActionablePhoneNumber("+918830714901"))
        assertTrue(PhoneActionHelper.isActionablePhoneNumber("00918830714901"))
        assertTrue(PhoneActionHelper.isActionablePhoneNumber("8830714901"))
        assertTrue(PhoneActionHelper.isActionablePhoneNumber("+91 88307 14901"))
        assertTrue(PhoneActionHelper.isActionablePhoneNumber("1234567"))
    }

    @Test
    fun testIsActionablePhoneNumber_invalidNumbers() {
        assertFalse(PhoneActionHelper.isActionablePhoneNumber(null))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber(""))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("   "))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("null"))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("NULL"))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("Private / Unknown number"))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("Unknown caller"))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("Not available"))
        assertFalse(PhoneActionHelper.isActionablePhoneNumber("12345")) // Short codes < 7 digits
    }

    @Test
    fun testIsKnownCaller() {
        assertTrue(PhoneActionHelper.isKnownCaller("Darshan Roommate", "00919699177117"))
        assertTrue(PhoneActionHelper.isKnownCaller("Ishwar Tatkal Ta", "8830714901"))

        assertFalse(PhoneActionHelper.isKnownCaller(null, "8830714901"))
        assertFalse(PhoneActionHelper.isKnownCaller("", "8830714901"))
        assertFalse(PhoneActionHelper.isKnownCaller("Unknown caller", "8830714901"))
        assertFalse(PhoneActionHelper.isKnownCaller("unknown caller", "8830714901"))
        // Caller name identical to phone number is not a known contact name
        assertFalse(PhoneActionHelper.isKnownCaller("8830714901", "8830714901"))
        assertFalse(PhoneActionHelper.isKnownCaller("+918830714901", "+918830714901"))
    }

    @Test
    fun testBuildSmsUriString() {
        assertEquals("smsto:+918830714901", PhoneActionHelper.buildSmsUriString("+918830714901"))
        assertEquals("smsto:8830714901", PhoneActionHelper.buildSmsUriString(" 8830714901 "))
        assertNull(PhoneActionHelper.buildSmsUriString(null))
        assertNull(PhoneActionHelper.buildSmsUriString(""))
        assertNull(PhoneActionHelper.buildSmsUriString("Private / Unknown number"))
        assertNull(PhoneActionHelper.buildSmsUriString("12345"))
    }

    @Test
    fun testFormatWhatsAppNumber_indianFormats() {
        // "+91 88307 14901" -> "918830714901"
        assertEquals("918830714901", PhoneActionHelper.formatWhatsAppNumber("+91 88307 14901"))

        // "+918830714901" -> "918830714901"
        assertEquals("918830714901", PhoneActionHelper.formatWhatsAppNumber("+918830714901"))

        // "00918830714901" -> "918830714901"
        assertEquals("918830714901", PhoneActionHelper.formatWhatsAppNumber("00918830714901"))

        // "918830714901" -> "918830714901"
        assertEquals("918830714901", PhoneActionHelper.formatWhatsAppNumber("918830714901"))

        // "08830714901" (11-digit with trunk 0) -> "918830714901"
        assertEquals("918830714901", PhoneActionHelper.formatWhatsAppNumber("08830714901"))

        // "8830714901" (10-digit Indian without country code) -> "918830714901"
        assertEquals("918830714901", PhoneActionHelper.formatWhatsAppNumber("8830714901"))
    }

    @Test
    fun testFormatWhatsAppNumber_internationalFormats() {
        // US number +1 202 555 0123 -> "12025550123"
        assertEquals("12025550123", PhoneActionHelper.formatWhatsAppNumber("+1 202 555 0123"))

        // UK number +44 7911 123456 -> "447911123456"
        assertEquals("447911123456", PhoneActionHelper.formatWhatsAppNumber("+44 7911 123456"))
    }

    @Test
    fun testFormatWhatsAppNumber_invalidNumbers() {
        assertNull(PhoneActionHelper.formatWhatsAppNumber(null))
        assertNull(PhoneActionHelper.formatWhatsAppNumber(""))
        assertNull(PhoneActionHelper.formatWhatsAppNumber("   "))
        assertNull(PhoneActionHelper.formatWhatsAppNumber("Private number"))
        assertNull(PhoneActionHelper.formatWhatsAppNumber("Unknown caller"))
        assertNull(PhoneActionHelper.formatWhatsAppNumber("12345"))
    }

    @Test
    fun testBuildWhatsAppUriString() {
        assertEquals(
            "https://api.whatsapp.com/send?phone=918830714901",
            PhoneActionHelper.buildWhatsAppUriString("+91 88307 14901")
        )
        assertNull(PhoneActionHelper.buildWhatsAppUriString(null))
        assertNull(PhoneActionHelper.buildWhatsAppUriString("Private number"))
    }
}
