package com.recai.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberFormatterTest {

    @Test
    fun testIndianNumber_0091Prefix() {
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("00918830714901"))
    }

    @Test
    fun testIndianNumber_plus91Prefix() {
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("+918830714901"))
        assertEquals("+91 96991 77117", PhoneNumberFormatter.format("+919699177117"))
    }

    @Test
    fun testIndianNumber_91PrefixWithoutPlus() {
        assertEquals("+91 96991 77117", PhoneNumberFormatter.format("919699177117"))
    }

    @Test
    fun testIndianNumber_alreadyFormattedWithSpaces() {
        assertEquals("+91 96991 77117", PhoneNumberFormatter.format("+91 96991 77117"))
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("+91 88307 14901"))
    }

    @Test
    fun testIndianNumber_withHyphensAndParentheses() {
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("+91-88307-14901"))
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("+91 (88307) 14901"))
    }

    @Test
    fun testIndianNumber_raw10Digits() {
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("8830714901"))
        assertEquals("+91 98765 43210", PhoneNumberFormatter.format("9876543210"))
    }

    @Test
    fun testIndianNumber_0TrunkPrefix11Digits() {
        assertEquals("+91 88307 14901", PhoneNumberFormatter.format("08830714901"))
    }

    @Test
    fun testUsNumber() {
        assertEquals("+1 415 555 2671", PhoneNumberFormatter.format("+14155552671"))
    }

    @Test
    fun testNullAndBlank() {
        assertEquals("Not available", PhoneNumberFormatter.format(null))
        assertEquals("Not available", PhoneNumberFormatter.format(""))
        assertEquals("Not available", PhoneNumberFormatter.format("   "))
        assertEquals("", PhoneNumberFormatter.formatOrEmpty(null))
        assertEquals("", PhoneNumberFormatter.formatOrEmpty(""))
    }

    @Test
    fun testCustomFallback() {
        assertEquals("Private number", PhoneNumberFormatter.format(null, fallback = "Private number"))
    }

    @Test
    fun testMalformedOrUnknownNumber_safeFallback() {
        assertEquals("12345", PhoneNumberFormatter.format("12345"))
        assertEquals("+442071838750", PhoneNumberFormatter.format("+442071838750"))
        assertEquals("+91 98765 XXXXX", PhoneNumberFormatter.format("+91 98765 XXXXX"))
    }
}
