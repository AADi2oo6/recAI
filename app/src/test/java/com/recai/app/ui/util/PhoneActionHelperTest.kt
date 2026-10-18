package com.recai.app.ui.util

import org.junit.Assert.assertFalse
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
}
