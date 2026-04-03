package com.example.gpstick.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.gpstick.R
import com.example.gpstick.data.preset.LocationPreset

class SimulationNotificationFactory(
    private val context: Context,
) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildActiveNotification(preset: LocationPreset): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text_template, preset.name))
            .setOngoing(true)
            .build()

    fun buildControlNotification(preset: LocationPreset): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.control_notification_title))
            .setContentText(context.getString(R.string.control_notification_text_template, preset.name))
            .setOngoing(true)
            .build()

    companion object {
        const val CHANNEL_ID = "gps_mock_channel"
        const val NOTIFICATION_ID = 2001
        const val CONTROL_NOTIFICATION_ID = 2002
    }
}
