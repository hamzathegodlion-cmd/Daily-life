package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object AlarmNotificationHelper {

    const val CHANNEL_ALARM = "routine_alarm_channel_v1"
    const val CHANNEL_REMINDER = "routine_reminder_channel_v1"
    const val NOTIFICATION_ID_ALARM = 4201

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Routine Alarms & Voice Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled routine alarms with spoken voice announcements"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null) // Audio handled by AlarmPlaybackService + TTS
            }
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Daily Check & Routine Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General routine reminders and sync updates"
            }
            nm.createNotificationChannel(alarmChannel)
            nm.createNotificationChannel(reminderChannel)
        }
    }

    fun buildAlarmNotification(
        context: Context,
        activityId: String,
        title: String,
        scheduledTimeText: String,
        announcementText: String,
        weekdayValue: Int
    ): Notification {
        ensureChannels(context)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmPlaybackService.EXTRA_ACTIVITY_ID, activityId)
            putExtra(AlarmPlaybackService.EXTRA_TITLE, title)
            putExtra(AlarmPlaybackService.EXTRA_TIME_TEXT, scheduledTimeText)
            putExtra(AlarmPlaybackService.EXTRA_ANNOUNCEMENT, announcementText)
            putExtra(AlarmPlaybackService.EXTRA_WEEKDAY, weekdayValue)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ACTIVITY_ID, activityId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1002,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPending = serviceActionPendingIntent(
            context,
            AlarmPlaybackService.ACTION_DISMISS,
            activityId,
            weekdayValue,
            1003
        )
        val snoozePending = serviceActionPendingIntent(
            context,
            AlarmPlaybackService.ACTION_SNOOZE,
            activityId,
            weekdayValue,
            1004
        )
        val completePending = serviceActionPendingIntent(
            context,
            AlarmPlaybackService.ACTION_MARK_COMPLETED,
            activityId,
            weekdayValue,
            1005
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$title • $scheduledTimeText")
            .setContentText(announcementText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(announcementText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, context.getString(R.string.action_complete), completePending)
            .addAction(0, context.getString(R.string.action_snooze), snoozePending)
            .addAction(0, context.getString(R.string.action_dismiss), dismissPending)
            .build()
    }

    private fun serviceActionPendingIntent(
        context: Context,
        action: String,
        activityId: String,
        weekdayValue: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmPlaybackService::class.java).apply {
            this.action = action
            putExtra(AlarmPlaybackService.EXTRA_ACTIVITY_ID, activityId)
            putExtra(AlarmPlaybackService.EXTRA_WEEKDAY, weekdayValue)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    const val EXTRA_OPEN_ACTIVITY_ID = "extra_open_activity_id"
}
