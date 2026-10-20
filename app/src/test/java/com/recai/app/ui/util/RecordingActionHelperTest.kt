package com.recai.app.ui.util

import com.recai.app.data.model.AudioFile
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingActionHelperTest {

    @Test
    fun testGetMimeType_byExtension() {
        val mp3File = AudioFile("rec.mp3", null, null, 100L, 100L)
        assertEquals("audio/mpeg", RecordingActionHelper.getMimeType(mp3File))

        val m4aFile = AudioFile("rec.m4a", null, null, 100L, 100L)
        assertEquals("audio/mp4", RecordingActionHelper.getMimeType(m4aFile))

        val wavFile = AudioFile("rec.wav", null, null, 100L, 100L)
        assertEquals("audio/wav", RecordingActionHelper.getMimeType(wavFile))

        val aacFile = AudioFile("rec.aac", null, null, 100L, 100L)
        assertEquals("audio/aac", RecordingActionHelper.getMimeType(aacFile))

        val oggFile = AudioFile("rec.ogg", null, null, 100L, 100L)
        assertEquals("audio/ogg", RecordingActionHelper.getMimeType(oggFile))

        val unknownFile = AudioFile("rec.xyz", null, null, 100L, 100L)
        assertEquals("audio/*", RecordingActionHelper.getMimeType(unknownFile))
    }

    @Test
    fun testGetMimeType_preservesExplicitAudioMime() {
        val customFile = AudioFile("rec.custom", null, "audio/3gpp", 100L, 100L)
        assertEquals("audio/3gpp", RecordingActionHelper.getMimeType(customFile))
    }
}
