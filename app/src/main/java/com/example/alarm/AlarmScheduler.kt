package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.RoutineActivity
import com.example.data.model.WeekdayMask
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun rescheduleAll(activities: List<RoutineActivity>) {
        // Cancel existing slots for every activity across all 7 weekdays before scheduling fresh ones
        activities.forEach { cancelActivityAlarms(it.id) }
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        activities.filter { it.alarmEnabled }.forEach { activity ->
            DayOfWeek.entries.forEach { day ->
                if (WeekdayMask.includes(activity.weekdaysMask, day)) {
                    scheduleNextOccurrence(activity, day, now)
                }
            }
        }
    }

    fun scheduleNextOccurrence(
        activity: RoutineActivity,
        dayOfWeek: DayOfWeek,
        referenceTime: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ): Long {
        val triggerTime = computeNextTriggerTime(
            startHour = activity.startHour,
            startMinute = activity.startMinute,
            dayOfWeek = dayOfWeek,
            now = referenceTime
        )
        val triggerMillis = triggerTime.toInstant().toEpochMilli()
        val requestCode = requestCodeFor(activity.id, dayOfWeek)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ACTIVITY_ID, activity.id)
            putExtra(EXTRA_WEEKDAY, dayOfWeek.value)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(triggerMillis, pendingIntent)
        return triggerMillis
    }

    fun scheduleSnooze(activityId: String, dayOfWeek: DayOfWeek, snoozeMinutes: Int): Long {
        val triggerMillis = System.currentTimeMillis() + snoozeMinutes.coerceIn(1, 60) * 60_000L
        val requestCode = snoozeRequestCodeFor(activityId)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ACTIVITY_ID, activityId)
            putExtra(EXTRA_WEEKDAY, dayOfWeek.value)
            putExtra(EXTRA_IS_SNOOZE, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrFallback(triggerMillis, pendingIntent)
        return triggerMillis
    }

    fun cancelActivityAlarms(activityId: String) {
        DayOfWeek.entries.forEach { day ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_ALARM
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(activityId, day),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            snoozeRequestCodeFor(activityId),
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePi != null) {
            alarmManager.cancel(snoozePi)
            snoozePi.cancel()
        }
    }

    private fun scheduleExactOrFallback(triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (canScheduleExactAlarms()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    999001,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            Log.w("AlarmScheduler", "Exact alarm permission denied, using inexact allowWhileIdle", se)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.ACTION_TRIGGER_ROUTINE_ALARM"
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_WEEKDAY = "extra_weekday"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        fun requestCodeFor(activityId: String, dayOfWeek: DayOfWeek): Int =
            ((activityId.hashCode() and 0x0FFFFFFF) * 10) + dayOfWeek.value

        fun snoozeRequestCodeFor(activityId: String): Int =
            ((activityId.hashCode() and 0x0FFFFFFF) * 10) + 9

        fun computeNextTriggerTime(
            startHour: Int,
            startMinute: Int,
            dayOfWeek: DayOfWeek,
            now: ZonedDateTime
        ): ZonedDateTime {
            var candidate = now
                .with(TemporalAdjusters.nextOrSame(dayOfWeek))
                .withHour(startHour.coerceIn(0, 23))
                .withMinute(startMinute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusWeeks(1)
            }
            return candidate
        }
    }
}
