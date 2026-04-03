package com.example.gpstick.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

    fun buildActiveNotification(preset: LocationPreset, simulationState: SimulationControlState): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(
                context.getString(
                    R.string.notification_text_template,
                    preset.name,
                    formatFeatureSummary(simulationState.activeSettings),
                ),
            )
            .setOngoing(true)
            .build()

    fun buildControlNotification(preset: LocationPreset, simulationState: SimulationControlState): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.control_notification_title))
            .setContentText(
                context.getString(
                    R.string.control_notification_text_template,
                    preset.name,
                    formatFeatureSummary(simulationState.activeSettings),
                ),
            )
            .setOngoing(true)
            .addAction(
                0,
                context.getString(R.string.control_notification_stop_action),
                PendingIntent.getService(
                    context,
                    1002,
                    ServiceCommandFactory.stopSimulation(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    private fun formatFeatureSummary(settings: SimulationFeatureSettings): String = buildList {
        if (settings.featuresEnabled && settings.isGpsMockEnabled) add("GPS")
        if (settings.featuresEnabled && settings.isWifiMockEnabled) add("Wi-Fi")
        if (settings.featuresEnabled && settings.isCellMockEnabled) add("Cell")
        if (settings.featuresEnabled && settings.isMovementSimulationEnabled) add("Move")
    }.ifEmpty { listOf("None") }.joinToString(separator = ", ")

    companion object {
        const val CHANNEL_ID = "gps_mock_channel"
        const val NOTIFICATION_ID = 2001
        const val CONTROL_NOTIFICATION_ID = 2002
    }
}
