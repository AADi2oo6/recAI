package com.recai.app.service

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.recai.app.data.storage.FileRepository
import com.recai.app.data.storage.FolderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Modern Android CallScreeningService for detecting incoming calls.
 * Never blocks or alters call behavior; allows the call and looks up conversation history.
 */
class IncomingCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "IncomingCallScreeningService"

        /**
         * Safely masks phone number for logging privacy (e.g., "98****3210").
         */
        fun maskPhoneNumber(number: String?): String {
            if (number.isNullOrBlank()) return "null"
            if (number.length <= 4) return "****"
            val prefix = number.take(2)
            val suffix = number.takeLast(2)
            return "$prefix****$suffix"
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "onScreenCall received for call ID: ${callDetails.hashCode()}")

        // Always allow the call through immediately (do NOT reject, silence, or skip call log)
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
        Log.d(TAG, "response sent (allowed call to continue normally)")

        // Only handle incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            Log.d(TAG, "Call is not incoming (direction=${callDetails.callDirection}), ignoring.")
            return
        }

        // Extract raw phone number from callDetails.handle (tel: URI)
        val rawHandle = callDetails.handle?.schemeSpecificPart
        Log.d(TAG, "extracted phone number: ${maskPhoneNumber(rawHandle)}")

        serviceScope.launch {
            try {
                Log.d(TAG, "starting recording lookup...")
                val folderPrefs = FolderPreferences(applicationContext)
                val treeUriString = folderPrefs.folderUriFlow.firstOrNull()

                val conversation = if (!treeUriString.isNullOrBlank()) {
                    Log.d(TAG, "recording folder URI found")
                    if (!rawHandle.isNullOrBlank()) {
                        val fileRepo = FileRepository(applicationContext)
                        val treeUri = Uri.parse(treeUriString)
                        fileRepo.findConversationByPhoneNumber(treeUri, rawHandle)
                    } else {
                        Log.d(TAG, "raw phone number is blank, skipping file matching")
                        null
                    }
                } else {
                    Log.d(TAG, "recording folder URI not found (no folder selected or persisted)")
                    null
                }

                if (conversation != null) {
                    Log.d(TAG, "conversation found: callerName='${conversation.callerName}', count=${conversation.recordingCount}")
                } else {
                    Log.d(TAG, "conversation not found for incoming number")
                }

                CallerNotificationHelper.showCallerNotification(
                    context = applicationContext,
                    rawPhoneNumber = rawHandle,
                    conversation = conversation
                )
                Log.d(TAG, "notification posted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "exception during call screening lookup: ${e.message}", e)
                // Safeguard: Never crash in background screening service
                try {
                    CallerNotificationHelper.showCallerNotification(
                        context = applicationContext,
                        rawPhoneNumber = rawHandle,
                        conversation = null
                    )
                    Log.d(TAG, "fallback notification posted")
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "failed to post fallback notification: ${fallbackEx.message}", fallbackEx)
                }
            }
        }
    }
}
