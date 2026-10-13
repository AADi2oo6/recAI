package com.recai.app.service

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        // Always allow the call through (do NOT reject, silence, or skip call log)
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)

        // Only handle incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        // Extract raw phone number from callDetails.handle (tel: URI)
        val rawHandle = callDetails.handle?.schemeSpecificPart

        serviceScope.launch {
            try {
                val folderPrefs = FolderPreferences(applicationContext)
                val treeUriString = folderPrefs.folderUriFlow.firstOrNull()

                val conversation = if (!treeUriString.isNullOrBlank() && !rawHandle.isNullOrBlank()) {
                    val fileRepo = FileRepository(applicationContext)
                    val treeUri = Uri.parse(treeUriString)
                    fileRepo.findConversationByPhoneNumber(treeUri, rawHandle)
                } else {
                    null
                }

                CallerNotificationHelper.showCallerNotification(
                    context = applicationContext,
                    rawPhoneNumber = rawHandle,
                    conversation = conversation
                )
            } catch (e: Exception) {
                // Safeguard: Never crash in background screening service
                try {
                    CallerNotificationHelper.showCallerNotification(
                        context = applicationContext,
                        rawPhoneNumber = rawHandle,
                        conversation = null
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
