package com.recai.app.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.recai.app.data.parser.PhoneNumberMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.trackingDataStore: DataStore<Preferences> by preferencesDataStore(name = "recai_tracking_prefs")

/**
 * Manages persistent tracking state for incoming callers using Jetpack DataStore Preferences.
 *
 * Rules:
 * - New callers are tracked by default.
 * - Callers explicitly removed from tracking are persisted in [KEY_UNTRACKED_NUMBERS].
 * - Stable keys are ALWAYS normalized numbers via [PhoneNumberMatcher.normalize].
 * - Never modifies or deletes audio recording files.
 */
class TrackingRepository(
    private val dataStore: DataStore<Preferences>
) {
    // Secondary constructor taking Android Context for standard usage
    constructor(context: Context) : this(context.trackingDataStore)

    companion object {
        private val KEY_UNTRACKED_NUMBERS = stringSetPreferencesKey("untracked_phone_numbers")
        // Stores custom caller names for untracked callers formatted as "normalizedNumber:::callerName"
        private val KEY_UNTRACKED_CALLER_NAMES = stringSetPreferencesKey("untracked_caller_names")
        private const val DELIMITER = ":::"

        /**
         * Pure decision function for checking tracking state against a set of untracked numbers.
         * Uses [PhoneNumberMatcher.matches] so that all formatting variations of a number
         * (e.g. 00918830714901, +918830714901, 8830714901) consistently resolve to the same tracking state.
         */
        fun isNumberTracked(untrackedSet: Set<String>, phoneNumber: String?): Boolean {
            if (phoneNumber.isNullOrBlank()) return true
            val norm = PhoneNumberMatcher.normalize(phoneNumber)
            if (norm.isEmpty()) return true
            // Check direct normalized match or suffix/prefix match via PhoneNumberMatcher
            return !untrackedSet.any { untrackedKey ->
                PhoneNumberMatcher.matches(untrackedKey, phoneNumber)
            }
        }
    }

    /**
     * Flow of all normalized phone numbers that have tracking disabled.
     */
    val untrackedNumbersFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_UNTRACKED_NUMBERS] ?: emptySet()
    }

    /**
     * Flow of custom caller name snapshots for untracked callers (normalized number -> caller name).
     */
    val untrackedCallerNamesFlow: Flow<Map<String, String>> = dataStore.data.map { preferences ->
        val rawSet = preferences[KEY_UNTRACKED_CALLER_NAMES] ?: emptySet()
        rawSet.mapNotNull { entry ->
            val parts = entry.split(DELIMITER, limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    /**
     * Observes whether tracking is enabled for [phoneNumber].
     */
    fun isTrackingEnabledFlow(phoneNumber: String?): Flow<Boolean> {
        return untrackedNumbersFlow.map { untrackedSet ->
            isNumberTracked(untrackedSet, phoneNumber)
        }
    }

    /**
     * Synchronously/suspendingly checks if tracking is currently enabled for [phoneNumber].
     */
    suspend fun isTrackingEnabled(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) return true
        val preferences = dataStore.data.first()
        val untrackedSet = preferences[KEY_UNTRACKED_NUMBERS] ?: emptySet()
        return isNumberTracked(untrackedSet, phoneNumber)
    }

    /**
     * Persistently sets tracking enabled or disabled for [phoneNumber].
     * Uses normalized representation as stable key.
     */
    suspend fun setTrackingEnabled(phoneNumber: String, enabled: Boolean, callerName: String? = null) {
        val normalized = PhoneNumberMatcher.normalize(phoneNumber)
        if (normalized.isEmpty()) return

        dataStore.edit { preferences ->
            val currentUntracked = preferences[KEY_UNTRACKED_NUMBERS]?.toMutableSet() ?: mutableSetOf()
            val currentNames = preferences[KEY_UNTRACKED_CALLER_NAMES]?.toMutableSet() ?: mutableSetOf()

            if (enabled) {
                // Remove from untracked set
                currentUntracked.remove(normalized)
                currentNames.removeAll { it.startsWith("$normalized$DELIMITER") }
            } else {
                // Add to untracked set
                currentUntracked.add(normalized)
                if (!callerName.isNullOrBlank() && !callerName.equals("Unknown caller", ignoreCase = true)) {
                    currentNames.removeAll { it.startsWith("$normalized$DELIMITER") }
                    currentNames.add("$normalized$DELIMITER$callerName")
                }
            }

            preferences[KEY_UNTRACKED_NUMBERS] = currentUntracked
            preferences[KEY_UNTRACKED_CALLER_NAMES] = currentNames
        }
    }
}
