package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AssistantSettings
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val QURAN_WIRD_JUZ = intPreferencesKey("quran_wird_juz")
        val QURAN_MURAJAAH_PAGES = intPreferencesKey("quran_murajaah_pages")
        val CYCLING_TARGET_KM = doublePreferencesKey("cycling_target_km")
        val YOUTUBE_DAILY_VIDEOS = intPreferencesKey("youtube_daily_videos")
        val PUSHUP_GOAL = intPreferencesKey("pushup_goal")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val TTS_VOICE_NAME = stringPreferencesKey("tts_voice_name")
        val TTS_REPETITIONS = intPreferencesKey("tts_repetitions")
        val TTS_DELAY_SECONDS = intPreferencesKey("tts_delay_seconds")
        val ALARM_VOLUME = floatPreferencesKey("alarm_volume")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_DURATION_SECONDS = intPreferencesKey("sound_duration_seconds")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
    }

    val settingsFlow: Flow<AssistantSettings> = context.settingsDataStore.data.map { prefs ->
        AssistantSettings(
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            quranWirdJuz = (prefs[Keys.QURAN_WIRD_JUZ] ?: 1).coerceIn(1, 30),
            quranMurajaahPages = (prefs[Keys.QURAN_MURAJAAH_PAGES] ?: 5).coerceIn(1, 604),
            cyclingTargetKm = (prefs[Keys.CYCLING_TARGET_KM] ?: 22.0).coerceIn(0.5, 300.0),
            youtubeDailyVideos = (prefs[Keys.YOUTUBE_DAILY_VIDEOS] ?: 2).coerceIn(1, 20),
            pushupGoal = (prefs[Keys.PUSHUP_GOAL] ?: 100).coerceIn(1, 500),
            ttsEnabled = prefs[Keys.TTS_ENABLED] ?: true,
            ttsSpeed = (prefs[Keys.TTS_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
            ttsPitch = (prefs[Keys.TTS_PITCH] ?: 1.0f).coerceIn(0.5f, 2.0f),
            ttsVoiceName = prefs[Keys.TTS_VOICE_NAME] ?: "",
            ttsRepetitions = (prefs[Keys.TTS_REPETITIONS] ?: 2).coerceIn(1, 10),
            ttsDelaySeconds = (prefs[Keys.TTS_DELAY_SECONDS] ?: 3).coerceIn(1, 30),
            alarmVolume = (prefs[Keys.ALARM_VOLUME] ?: 0.9f).coerceIn(0.0f, 1.0f),
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            soundDurationSeconds = (prefs[Keys.SOUND_DURATION_SECONDS] ?: 20).coerceIn(3, 120),
            snoozeMinutes = (prefs[Keys.SNOOZE_MINUTES] ?: 5).coerceIn(1, 60),
            themeMode = ThemeMode.safeFrom(prefs[Keys.THEME_MODE]),
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: false
        )
    }

    suspend fun getSettingsOnce(): AssistantSettings = settingsFlow.first()

    suspend fun updateSettings(settings: AssistantSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = settings.displayName.take(100)
            prefs[Keys.QURAN_WIRD_JUZ] = settings.quranWirdJuz.coerceIn(1, 30)
            prefs[Keys.QURAN_MURAJAAH_PAGES] = settings.quranMurajaahPages.coerceIn(1, 604)
            prefs[Keys.CYCLING_TARGET_KM] = settings.cyclingTargetKm.coerceIn(0.5, 300.0)
            prefs[Keys.YOUTUBE_DAILY_VIDEOS] = settings.youtubeDailyVideos.coerceIn(1, 20)
            prefs[Keys.PUSHUP_GOAL] = settings.pushupGoal.coerceIn(1, 500)
            prefs[Keys.TTS_ENABLED] = settings.ttsEnabled
            prefs[Keys.TTS_SPEED] = settings.ttsSpeed.coerceIn(0.5f, 2.0f)
            prefs[Keys.TTS_PITCH] = settings.ttsPitch.coerceIn(0.5f, 2.0f)
            prefs[Keys.TTS_VOICE_NAME] = settings.ttsVoiceName.take(120)
            prefs[Keys.TTS_REPETITIONS] = settings.ttsRepetitions.coerceIn(1, 10)
            prefs[Keys.TTS_DELAY_SECONDS] = settings.ttsDelaySeconds.coerceIn(1, 30)
            prefs[Keys.ALARM_VOLUME] = settings.alarmVolume.coerceIn(0.0f, 1.0f)
            prefs[Keys.VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[Keys.SOUND_ENABLED] = settings.soundEnabled
            prefs[Keys.SOUND_DURATION_SECONDS] = settings.soundDurationSeconds.coerceIn(3, 120)
            prefs[Keys.SNOOZE_MINUTES] = settings.snoozeMinutes.coerceIn(1, 60)
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.USE_24_HOUR] = settings.use24HourFormat
        }
    }
}
