package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import com.example.data.model.AlarmSoundOption

class AlarmSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun resolveRawResId(soundKey: String): Int = when (AlarmSoundOption.fromKey(soundKey)) {
        AlarmSoundOption.GENTLE_WAKEUP -> R.raw.sound_gentle_wakeup
        AlarmSoundOption.DIGITAL_ALARM -> R.raw.sound_digital_alarm
        AlarmSoundOption.SOFT_BELL -> R.raw.sound_soft_bell
        AlarmSoundOption.CLASSIC_ALARM -> R.raw.sound_classic_alarm
        AlarmSoundOption.MOTIVATIONAL -> R.raw.sound_motivational
        AlarmSoundOption.QURAN_TONE -> R.raw.sound_quran_tone
        AlarmSoundOption.SPORT_TONE -> R.raw.sound_sport_tone
        AlarmSoundOption.LEARNING_TONE -> R.raw.sound_learning_tone
        AlarmSoundOption.SLEEP_TONE -> R.raw.sound_sleep_tone
    }

    fun play(
        soundKey: String,
        volume: Float = 0.9f,
        looping: Boolean = true,
        vibrate: Boolean = false,
        useAlarmStream: Boolean = true
    ) {
        stop()
        try {
            val resId = resolveRawResId(soundKey)
            val afd = context.resources.openRawResourceFd(resId) ?: return
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(if (useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = looping
                val clampedVol = volume.coerceIn(0.05f, 1.0f)
                setVolume(clampedVol, clampedVol)
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Failed to play sound $soundKey", e)
        }

        if (vibrate) {
            startVibration()
        }
    }

    private fun startVibration() {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = vib
            val pattern = longArrayOf(0, 450, 350, 450, 650)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundPlayer", "Vibration unavailable", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundPlayer", "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        } finally {
            vibrator = null
        }
    }
}
