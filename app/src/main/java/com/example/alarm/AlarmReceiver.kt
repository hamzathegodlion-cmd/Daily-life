package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.DailyLifeApplication
import com.example.data.model.DefaultTimetableFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(AlarmScheduler.EXTRA_ACTIVITY_ID) ?: return
        val weekdayValue = intent.getIntExtra(
            AlarmScheduler.EXTRA_WEEKDAY,
            LocalDate.now().dayOfWeek.value
        )
        val dayOfWeek = DayOfWeek.of(weekdayValue.coerceIn(1, 7))
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DailyLifeApplication
                val container = app.container
                container.routineRepository.ensureSeededLocally()
                val activity = container.routineRepository.getActivityById(activityId)
                if (activity != null && (activity.alarmEnabled || isSnooze)) {
                    val settings = container.settingsDataStore.getSettingsOnce()
                    val currentDay = LocalDate.now().dayOfWeek
                    val spokenText = DefaultTimetableFactory.buildDynamicAnnouncement(activity, currentDay)
                    val timeText = activity.formatStartTime(settings.use24HourFormat)

                    val serviceIntent = Intent(context, AlarmPlaybackService::class.java).apply {
                        action = AlarmPlaybackService.ACTION_START_ALARM
                        putExtra(AlarmPlaybackService.EXTRA_ACTIVITY_ID, activity.id)
                        putExtra(AlarmPlaybackService.EXTRA_TITLE, activity.title)
                        putExtra(AlarmPlaybackService.EXTRA_TIME_TEXT, timeText)
                        putExtra(AlarmPlaybackService.EXTRA_ANNOUNCEMENT, spokenText)
                        putExtra(AlarmPlaybackService.EXTRA_SOUND_KEY, activity.soundKey)
                        putExtra(AlarmPlaybackService.EXTRA_WEEKDAY, currentDay.value)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    // Schedule next week's occurrence if this was a regular weekly alarm
                    if (!isSnooze && activity.repeatWeekly) {
                        container.alarmScheduler.scheduleNextOccurrence(activity, dayOfWeek)
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
