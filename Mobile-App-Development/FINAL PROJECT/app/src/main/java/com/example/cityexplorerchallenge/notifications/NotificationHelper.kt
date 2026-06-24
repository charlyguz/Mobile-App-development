package com.example.cityexplorerchallenge.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cityexplorerchallenge.MainActivity
import com.example.cityexplorerchallenge.R
import com.example.cityexplorerchallenge.data.local.ChallengeEntity

class NotificationHelper(private val context: Context) {

    init {
        createNotificationChannel()
    }

    fun showMissionGenerated(challenge: ChallengeEntity) {
        showNotification(
            id = MISSION_GENERATED_ID,
            title = "New exploration mission",
            text = "${challenge.targetPlaceName.ifEmpty { challenge.title }} is waiting nearby."
        )
    }

    fun showMissionCompleted(challenge: ChallengeEntity) {
        showNotification(
            id = MISSION_COMPLETED_ID,
            title = "Mission completed",
            text = "You completed ${challenge.targetPlaceName.ifEmpty { challenge.title }}."
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mission updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for generated and completed exploration missions."
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(id: Int, title: String, text: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private companion object {
        const val CHANNEL_ID = "mission_updates"
        const val MISSION_GENERATED_ID = 1001
        const val MISSION_COMPLETED_ID = 1002
    }
}
