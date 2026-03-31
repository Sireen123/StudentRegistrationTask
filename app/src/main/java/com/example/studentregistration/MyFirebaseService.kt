package com.example.studentregistration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // ✅ Handle both "notification" and "data" payloads
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New Notification"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new message."

        sendNotification(title, body)
    }

    @Suppress("MissingPermission")
    private fun sendNotification(title: String, message: String) {

        // ✅ When tapping notification → open DashboardActivity
        val intent = Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "student_channel"

        // ✅ Notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Student Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for Student Registration App"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // ✅ Notification builder
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // ✅ Samsung A-series needs this check
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_TOKEN", "Token: $token")
    }
}