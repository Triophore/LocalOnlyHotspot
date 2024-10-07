package com.triophore.localonlyhotspot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.triophore.weaver.WeaverImpl

class NotificationManager(private val context: Context) {
    private var notificationTitle: String? = null
    private var notificationMessage: String? = null
    private var notificationLogoResId: Int? = null

    val CHANNEL_ID = "17"
    val NOTIFICATION_ID = 12345

    fun createNotification() {

        // Check if the notification channel exists already before creating a new one.
        if (!notificationChannelExists(context, CHANNEL_ID)) {
            val name = "Weaver"
            val descriptionText = "LocalOnlyHotspot is Active"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }


        // Set up PendingIntent
        val intent = Intent(context, WeaverImpl::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Build Notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            notificationLogoResId?.let {
                setSmallIcon(it)
            }
            notificationTitle?.let {
                setContentTitle(it)
            }
            notificationMessage?.let {
                setContentText(it)
            }
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setContentIntent(pendingIntent)
            setOngoing(true) // Keep notification visible, can't be swiped away
        }


        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notificationChannelExists(context: Context, channelId: String): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.getNotificationChannel(channelId) != null
    }

    fun dismissNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun setNotification(title: String? = null, message: String? = null, logoResId: Int) {
        this.notificationTitle = title
        this.notificationMessage = message
        this.notificationLogoResId = logoResId
    }
}