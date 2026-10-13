package com.recai.app.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single extension property for DataStore instance across the app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recai_folder_prefs")

class FolderPreferences(private val context: Context) {

    companion object {
        private val KEY_FOLDER_URI = stringPreferencesKey("selected_folder_uri")
    }

    /**
     * Reads the stored folder URI string from DataStore preferences as a Flow.
     */
    val folderUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_FOLDER_URI]
    }

    /**
     * Persistently saves the selected folder URI string.
     */
    suspend fun saveFolderUri(uriString: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FOLDER_URI] = uriString
        }
    }

    /**
     * Clears the stored folder URI if it becomes invalid or revoked.
     */
    suspend fun clearFolderUri() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_FOLDER_URI)
        }
    }

    /**
     * Checks if we currently have persistent permission and valid access to this tree URI,
     * and returns the folder's human-readable display name using standard Android DocumentsContract.
     * If the folder is inaccessible or permissions were revoked, returns null.
     */
    fun getAccessibleFolderName(uri: Uri): String? {
        // 1. Verify that the system still grants persistent read permission to this URI
        val hasPersistedPermission = context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
        if (!hasPersistedPermission) {
            return null
        }

        // 2. Query document metadata using DocumentsContract and ContentResolver
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )

            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
