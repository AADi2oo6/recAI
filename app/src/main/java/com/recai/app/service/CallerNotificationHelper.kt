package com.recai.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.recai.app.MainActivity
import com.recai.app.R
import com.recai.app.data.model.Conversation
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object CallerNotificationHelper {

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
     * Shows a heads-up notification for an incoming call.
     * If [conversation] is non-null, shows the known caller info.
     * Otherwise, shows an unknown caller info card.
     */
    fun showCallerNotification(
        context: Context,
        rawPhoneNumber: String?,
        conversation: Conversation?
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (conversation != null) {
                putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversation.id)
                putExtra(MainActivity.EXTRA_PHONE_NUMBER, conversation.phoneNumber)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val contentText: String
        val bigTextBuilder = StringBuilder()

        if (conversation != null) {
            val callerName = conversation.callerName ?: "Unknown caller"
            val phone = conversation.phoneNumber ?: (rawPhoneNumber ?: "")
            val count = conversation.recordingCount
            val countStr = if (count == 1) "1 recording" else "$count recordings"

            val latestRec = conversation.latestRecording
            val latestDate = formatLatestDate(
                latestRec?.metadata?.recordingDateTime,
                latestRec?.lastModified ?: 0L
            )

            title = "Incoming call: $callerName"
            contentText = "$phone • $countStr"

            bigTextBuilder.append(callerName).append("\n")
            if (phone.isNotBlank()) {
                bigTextBuilder.append(phone).append("\n")
            }
            bigTextBuilder.append(countStr)
            if (latestDate.isNotBlank()) {
                bigTextBuilder.append("\nLatest: ").append(latestDate)
            }
        } else {
            val phone = if (!rawPhoneNumber.isNullOrBlank()) rawPhoneNumber else "Private / Unknown number"
            title = "Incoming call: Unknown caller"
            contentText = "$phone • No previous recordings"

            bigTextBuilder.append("Unknown caller\n")
            bigTextBuilder.append(phone).append("\n")
            bigTextBuilder.append("No previous recordings")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_audio_file)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextBuilder.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_audio_file,
                "Open recAI",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatLatestDate(dateTime: LocalDateTime?, lastModifiedFallback: Long): String {
        if (dateTime != null) {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.getDefault())
            return dateTime.format(formatter)
        }
        if (lastModifiedFallback > 0) {
            val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            return formatter.format(Date(lastModifiedFallback))
        }
        return ""
    }
}
