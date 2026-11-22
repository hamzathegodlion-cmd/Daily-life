package com.example.alarm

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.DailyLifeApplication
import com.example.data.model.ActivityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class AlarmPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var soundPlayer: AlarmSoundPlayer
    private lateinit var voiceManager: VoiceAnnouncementManager
    private var playbackJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        soundPlayer = AlarmSoundPlayer(this)
        voiceManager = VoiceAnnouncementManager(this)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyDailyLife::AlarmWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val activityId = intent?.getStringExtra(EXTRA_ACTIVITY_ID) ?: ""
        val weekdayInt = intent?.getIntExtra(EXTRA_WEEKDAY, LocalDate.now().dayOfWeek.value)
            ?: LocalDate.now().dayOfWeek.value
        val dayOfWeek = DayOfWeek.of(weekdayInt.coerceIn(1, 7))

        when (action) {
            ACTION_DISMISS -> {
                stopPlaybackAndSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                serviceScope.launch {
                    val container = (application as DailyLifeApplication).container
                    val settings = container.settingsDataStore.getSettingsOnce()
                    if (activityId.isNotBlank()) {
                        container.alarmScheduler.scheduleSnooze(activityId, dayOfWeek, settings.snoozeMinutes)
                    }
                    stopPlaybackAndSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_MARK_COMPLETED -> {
                serviceScope.launch {
                    val container = (application as DailyLifeApplication).container
                    if (activityId.isNotBlank()) {
                        container.routineRepository.markActivityStatus(
                            activityId = activityId,
                            dateKey = LocalDate.now().toString(),
                            status = ActivityStatus.COMPLETED
                        )
                    }
                    stopPlaybackAndSelf()
                }
                return START_NOT_STICKY
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Routine Activity"
                val timeText = intent?.getStringExtra(EXTRA_TIME_TEXT) ?: ""
                val announcement = intent?.getStringExtra(EXTRA_ANNOUNCEMENT) ?: title
                val soundKey = intent?.getStringExtra(EXTRA_SOUND_KEY) ?: "sound_gentle_wakeup"

                val notification = AlarmNotificationHelper.buildAlarmNotification(
                    context = this,
                    activityId = activityId,
                    title = title,
                    scheduledTimeText = timeText,
                    announcementText = announcement,
                    weekdayValue = dayOfWeek.value
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        AlarmNotificationHelper.NOTIFICATION_ID_ALARM,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(AlarmNotificationHelper.NOTIFICATION_ID_ALARM, notification)
                }

                try {
                    wakeLock?.acquire(120_000L)
                } catch (e: Exception) {
                    Log.w("AlarmService", "WakeLock acquire failed", e)
                }

                startAlarmSequence(soundKey, announcement)
                return START_NOT_STICKY
            }
        }
    }

    private fun startAlarmSequence(soundKey: String, announcement: String) {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            val container = (application as DailyLifeApplication).container
            val settings = container.settingsDataStore.getSettingsOnce()

            // Step 1: Play selected alarm sound (+ vibration)
            if (settings.soundEnabled) {
                soundPlayer.play(
                    soundKey = soundKey,
                    volume = settings.alarmVolume,
                    looping = true,
                    vibrate = settings.vibrationEnabled,
                    useAlarmStream = true
                )
                val toneDurationMs = settings.soundDurationSeconds.coerceIn(3, 120) * 1000L
                // Play chime up to min(toneDurationMs, 6000ms) before starting TTS so spoken voice is crisp
                delay(minOf(toneDurationMs, 6000L))
                soundPlayer.stop()
            }

            // Step 2 & 3: Speak dynamic announcement and repeat according to settings
            if (settings.ttsEnabled) {
                voiceManager.speakRepeated(
                    text = announcement,
                    speed = settings.ttsSpeed,
                    pitch = settings.ttsPitch,
                    voiceName = settings.ttsVoiceName,
                    repetitions = settings.ttsRepetitions,
                    delaySeconds = settings.ttsDelaySeconds
                )
            }
        }
    }

    private fun stopPlaybackAndSelf() {
        playbackJob?.cancel()
        soundPlayer.stop()
        voiceManager.stop()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        sendBroadcast(Intent(ACTION_ALARM_CLOSED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.stop()
        voiceManager.shutdown()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_ALARM = "com.example.alarm.START"
        const val ACTION_DISMISS = "com.example.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.example.alarm.SNOOZE"
        const val ACTION_MARK_COMPLETED = "com.example.alarm.COMPLETE"
        const val ACTION_ALARM_CLOSED = "com.example.alarm.CLOSED"

        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TIME_TEXT = "extra_time_text"
        const val EXTRA_ANNOUNCEMENT = "extra_announcement"
        const val EXTRA_SOUND_KEY = "extra_sound_key"
        const val EXTRA_WEEKDAY = "extra_weekday"
    }
}
