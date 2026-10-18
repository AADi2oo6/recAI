package com.recai.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPlaybackHelperTest {

    @Test
    fun testRewind10Seconds_normalPosition() {
        // 25 seconds -> rewinds to 15 seconds
        val result = AudioPlaybackHelper.calculateRewindPosition(25_000L)
        assertEquals(15_000L, result)
    }

    @Test
    fun testRewind10Seconds_nearStart_stopsAtZero() {
        // 7 seconds -> rewinds to 0
        val result = AudioPlaybackHelper.calculateRewindPosition(7_000L)
        assertEquals(0L, result)

        // 0 seconds -> remains at 0
        assertEquals(0L, AudioPlaybackHelper.calculateRewindPosition(0L))
    }

    @Test
    fun testForward10Seconds_normalPosition() {
        // 10 seconds with 60 second duration -> forwards to 20 seconds
        val result = AudioPlaybackHelper.calculateForwardPosition(10_000L, 60_000L)
        assertEquals(20_000L, result)
    }

    @Test
    fun testForward10Seconds_nearEnd_stopsAtDuration() {
        // 55 seconds with 60 second duration -> stops at 60 seconds
        val result = AudioPlaybackHelper.calculateForwardPosition(55_000L, 60_000L)
        assertEquals(60_000L, result)

        // At end -> remains at duration
        assertEquals(60_000L, AudioPlaybackHelper.calculateForwardPosition(60_000L, 60_000L))
    }

    @Test
    fun testAvailableSpeedsContainsRequiredValues() {
        val speeds = AudioPlaybackHelper.AVAILABLE_SPEEDS
        assertEquals(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f), speeds)
    }

    @Test
    fun testSpeedLabels() {
        assertEquals("0.5×", AudioPlaybackHelper.formatSpeedLabel(0.5f))
        assertEquals("0.75×", AudioPlaybackHelper.formatSpeedLabel(0.75f))
        assertEquals("1.0×", AudioPlaybackHelper.formatSpeedLabel(1.0f))
        assertEquals("1.25×", AudioPlaybackHelper.formatSpeedLabel(1.25f))
        assertEquals("1.5×", AudioPlaybackHelper.formatSpeedLabel(1.5f))
        assertEquals("1.75×", AudioPlaybackHelper.formatSpeedLabel(1.75f))
        assertEquals("2.0×", AudioPlaybackHelper.formatSpeedLabel(2.0f))
    }
}
