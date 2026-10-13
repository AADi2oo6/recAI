package com.recai.app.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.recai.app.data.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class FileRepository(private val context: Context) {

    companion object {
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            "mp3",
            "m4a",
            "wav",
            "aac",
            "ogg"
        )
    }

    /**
     * Reads the audio files directly from the specified tree URI.
     * Queries DocumentsContract children, filters for supported audio formats,
     * and sorts results newest first (by last modified timestamp).
     */
    suspend fun getAudioFiles(treeUri: Uri): Result<List<AudioFile>> = withContext(Dispatchers.IO) {
        try {
            // Check that we still hold persistable read permission
            val hasPersistedPermission = context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == treeUri && permission.isReadPermission
            }
            if (!hasPersistedPermission) {
                return@withContext Result.failure(SecurityException("Persisted read permission not found for tree URI"))
            }

            // Build the children URI for the root folder of this document tree
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val audioFiles = mutableListOf<AudioFile>()

            context.contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val dateIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val childDocId = if (idIndex >= 0) cursor.getString(idIndex) else continue
                    val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                    val mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    val lastModified = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L

                    // Skip directories
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        continue
                    }

                    // Check if file is supported audio by extension or mimeType
                    if (isSupportedAudioFile(displayName, mimeType)) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val metadata = com.recai.app.data.parser.RecordingFilenameParser.parse(displayName)
                        audioFiles.add(
                            AudioFile(
                                name = displayName,
                                uri = fileUri,
                                mimeType = mimeType,
                                size = size,
                                lastModified = lastModified,
                                metadata = metadata
                            )
                        )
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not query documents for URI"))

            // Sort newest first
            val sortedList = audioFiles.sortedByDescending { it.lastModified }
            Result.success(sortedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isSupportedAudioFile(fileName: String, mimeType: String?): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (SUPPORTED_AUDIO_EXTENSIONS.contains(extension)) {
            return true
        }

        if (mimeType != null && mimeType.startsWith("audio/", ignoreCase = true)) {
            return true
        }

        return false
    }
}
