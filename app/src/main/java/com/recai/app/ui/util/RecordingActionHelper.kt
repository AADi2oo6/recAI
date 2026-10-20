package com.recai.app.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.recai.app.data.model.AudioFile
import java.util.Locale

/**
 * Reusable helper for recording-related SAF actions:
 * - Sharing SAF audio recording via Android Sharesheet
 * - Permanently deleting SAF audio recording via ContentResolver / DocumentsContract
 */
object RecordingActionHelper {

    /**
     * Resolves the MIME type for an audio file, defaulting based on file extension.
     */
    fun getMimeType(audioFile: AudioFile): String {
        val existingMime = audioFile.mimeType
        if (!existingMime.isNullOrBlank() && existingMime.startsWith("audio/", ignoreCase = true)) {
            return existingMime
        }
        val ext = audioFile.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            else -> "audio/*"
        }
    }

    /**
     * Shares the audio file via the native Android Sharesheet using the existing SAF content URI.
     * Sets Intent.ACTION_SEND, EXTRA_STREAM, ClipData with the URI, and FLAG_GRANT_READ_URI_PERMISSION.
     *
     * @return true if the chooser was successfully opened, false otherwise.
     */
    fun shareRecording(context: Context, audioFile: AudioFile): Boolean {
        val uri = audioFile.uri ?: return false
        return try {
            val mime = getMimeType(audioFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, audioFile.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share recording").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Permanently deletes a recording from the SAF-selected folder using its content URI.
     * Uses [DocumentsContract.deleteDocument] and falls back to [android.content.ContentResolver.delete].
     *
     * @return true if the document was deleted, false otherwise.
     */
    fun deleteRecording(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            if (DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                true
            } else {
                val count = context.contentResolver.delete(uri, null, null)
                count > 0
            }
        } catch (e: Exception) {
            try {
                val count = context.contentResolver.delete(uri, null, null)
                count > 0
            } catch (ex: Exception) {
                false
            }
        }
    }
}
