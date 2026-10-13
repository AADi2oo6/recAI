package com.recai.app.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.recai.app.data.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class FileRepository(private val context: Context? = null) {

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
        val nonNullContext = context ?: return@withContext Result.failure(IllegalStateException("Context is required to query documents"))
        try {
            // Check that we still hold persistable read permission
            val hasPersistedPermission = nonNullContext.contentResolver.persistedUriPermissions.any { permission ->
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

            nonNullContext.contentResolver.query(
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

    /**
     * Reads audio recordings from the tree URI and groups them into Conversations
     * by phone number, sorted by latest recording date (newest first).
     */
    suspend fun getConversations(treeUri: Uri): Result<List<com.recai.app.data.model.Conversation>> {
        val audioFilesResult = getAudioFiles(treeUri)
        return audioFilesResult.map { files ->
            groupIntoConversations(files)
        }
    }

    /**
     * Groups a list of AudioFiles into Conversations:
     * - Recordings with the same phone number are merged into one Conversation.
     * - The best available non-empty caller name is chosen for the Conversation.
     * - Recordings with no phone number get their own unique conversation (or unique key)
     *   so unrelated unknown recordings are not incorrectly merged.
     * - Conversations are sorted newest first based on their latest recording.
     */
    fun groupIntoConversations(files: List<AudioFile>): List<com.recai.app.data.model.Conversation> {
        if (files.isEmpty()) return emptyList()

        // Map key -> MutableList of AudioFiles
        // For files with a phone number, the key is "phone:<phoneNumber>".
        // For files without a phone number, the key is unique per file ("file:<uri>").
        val groups = linkedMapOf<String, MutableList<AudioFile>>()

        for (file in files) {
            val phone = file.metadata?.phoneNumber?.trim()?.ifBlank { null }
            val groupKey = if (phone != null) {
                "phone:$phone"
            } else {
                // Unique group per file when phone is missing
                "file:${file.name}_${file.lastModified}_${System.identityHashCode(file)}"
            }
            groups.getOrPut(groupKey) { mutableListOf() }.add(file)
        }

        val conversations = groups.map { (key, groupRecordings) ->
            // Sort recordings within the conversation newest first
            val sortedRecordings = groupRecordings.sortedByDescending { it.lastModified }

            val phoneNumber = if (key.startsWith("phone:")) {
                key.removePrefix("phone:")
            } else {
                null
            }

            // Find best available caller name from recordings in this group
            val callerName = sortedRecordings.mapNotNull { it.metadata?.callerName?.trim() }
                .firstOrNull { it.isNotBlank() }

            com.recai.app.data.model.Conversation(
                id = key,
                phoneNumber = phoneNumber,
                callerName = callerName,
                recordings = sortedRecordings
            )
        }

        // Sort conversations newest first
        return conversations.sortedByDescending { it.latestTimestampMillis }
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
