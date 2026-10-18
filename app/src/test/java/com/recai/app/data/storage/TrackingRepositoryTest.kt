package com.recai.app.data.storage

import com.recai.app.data.parser.PhoneNumberMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingRepositoryTest {

    @Test
    fun testNewNumberDefaultsToTracked() {
        val untrackedSet = emptySet<String>()
        val incoming = "+91 88307 14901"
        assertTrue(TrackingRepository.isNumberTracked(untrackedSet, incoming))
    }

    @Test
    fun testDisabledTrackingIsRecognized() {
        val untrackedSet = setOf(PhoneNumberMatcher.normalize("00918830714901"))
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "00918830714901"))
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "+918830714901"))
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "8830714901"))
    }

    @Test
    fun testNormalizedVariantsReferToSameCaller() {
        val key1 = PhoneNumberMatcher.normalize("00919699177117")
        val key2 = PhoneNumberMatcher.normalize("+919699177117")
        val key3 = PhoneNumberMatcher.normalize("+91 96991 77117")

        assertEquals(key1, key2)
        assertEquals(key2, key3)

        val untrackedSet = setOf(key1)
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "+919699177117"))
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "00919699177117"))
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "+91 96991 77117"))
    }

    @Test
    fun testDifferentPhoneNumbersRemainIndependent() {
        val untrackedSet = setOf(PhoneNumberMatcher.normalize("00918830714901"))

        // Number 1 is untracked
        assertFalse(TrackingRepository.isNumberTracked(untrackedSet, "00918830714901"))

        // Number 2 is NOT untracked (so it remains tracked)
        assertTrue(TrackingRepository.isNumberTracked(untrackedSet, "00919699177117"))
    }

    @Test
    fun testReEnabledNumberIsTrackedAgain() {
        val mutableSet = mutableSetOf(PhoneNumberMatcher.normalize("00918830714901"))
        assertFalse(TrackingRepository.isNumberTracked(mutableSet, "00918830714901"))

        // User enables tracking
        mutableSet.remove(PhoneNumberMatcher.normalize("00918830714901"))
        assertTrue(TrackingRepository.isNumberTracked(mutableSet, "00918830714901"))
    }

    @Test
    fun testPrivateOrNullNumberHandledSafely() {
        val untrackedSet = setOf(PhoneNumberMatcher.normalize("00918830714901"))
        // Empty/null number defaults to true without crashing
        assertTrue(TrackingRepository.isNumberTracked(untrackedSet, null))
        assertTrue(TrackingRepository.isNumberTracked(untrackedSet, ""))
    }
}
