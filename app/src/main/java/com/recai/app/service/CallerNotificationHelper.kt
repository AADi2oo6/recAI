package com.recai.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.recai.app.MainActivity
import com.recai.app.R
import com.recai.app.data.model.Conversation

object CallerNotificationHelper {

    private const val TAG = "CallerNotificationHelper"
    const val CHANNEL_ID = "recai_incoming_calls"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val name = "Incoming Calls"
        val descriptionText = "Displays recording details for incoming callers"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(true)
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Shows a heads-up notification for an incoming call using [CallerNotificationFormatter].
     */
    fun showCallerNotification(
        context: Context,
        rawPhoneNumber: String?,
        conversation: Conversation?
    ): CallerNotificationData {
        createNotificationChannel(context)

        val data = CallerNotificationFormatter.buildNotificationData(
            rawPhoneNumber = rawPhoneNumber,
            conversation = conversation
        )

        Log.d(TAG, "Notification prepared: variant=${data.variant}, action=${data.actionLabel}")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (conversation != null && conversation.recordingCount > 0) {
                putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversation.id)
                putExtra(MainActivity.EXTRA_PHONE_NUMBER, conversation.phoneNumber ?: rawPhoneNumber)
                putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_TARGET_CONVERSATION_DETAIL)
            } else {
                putExtra(MainActivity.EXTRA_PHONE_NUMBER, rawPhoneNumber)
                putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_TARGET_CONVERSATIONS)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_audio_file)
            .setContentTitle(data.title)
            .setContentText(data.contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(data.bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_audio_file,
                data.actionLabel,
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        return data
    }
}

