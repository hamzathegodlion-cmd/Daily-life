package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.DailyLifeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return

        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DailyLifeApplication
                app.container.routineRepository.ensureSeededLocally()
                val activities = app.container.routineRepository.getActivitiesOnce()
                app.container.alarmScheduler.rescheduleAll(activities)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
