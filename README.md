# My Daily Life Assistant

**My Daily Life Assistant** is a native Android personal routine, smart voice-alarm, weekly planner, workout, study, Quran, and progress-tracking application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **DataStore**, **AlarmManager**, **TextToSpeech**, and **Firebase Firestore + Google Sign-In**.

## Core Features

1. **Exact Daily Routine & 12-Hour AM/PM Display**
   - Preserves your fixed local-timezone timetable while handling midnight-crossing activities (`10:30 PM–12:00 AM`, `4:30–10:30 PM`, etc.):
     - **10:30 PM** — Wake up (prepare for mosque)
     - **10:30 PM–12:00 AM** — Mosque / Fajr Salah (shares 10:30 PM trigger without duplicate alarms)
     - **12:00–12:50 AM** — Quran (Wird: 1 Juz, Muraja'a: 5 Safha)
     - **12:50–1:30 AM** — Sport (automatic Group A / Group B / Sunday Rest)
     - **1:30–11:30 AM** — Learning
     - **11:30 AM–12:00 PM** — Cycling (~22 km round trip; distance/duration logged separately)
     - **12:00–2:30 PM** — Mosque / Kitab
     - **2:30–3:00 PM** — YouTube (edit & upload 2 daily videos)
     - **3:00–4:00 PM** — Studying / Web Development (weekday-aware split)
     - **4:00–4:30 PM** — Daily activity checking & progress review
     - **4:30–10:30 PM** — Sleep (6-hour fixed block with informational teen sleep-health note)
2. **Weekday-Aware Study (4 hrs/wk) & Web Development (3 hrs/wk)**
   - **Sun & Tue**: `3:00–3:30 PM` Studying (30 min) → `3:30–4:00 PM` Web Development (30 min)
   - **Mon, Wed & Thu**: `3:00–4:00 PM` Studying (1 hour), Web Off
   - **Fri & Sat**: `3:00–4:00 PM` Web Development (1 hour), Studying Off
3. **Day-Aware Workout System**
   - **Mon / Wed / Fri (Group A)**: Warm-up, Push-ups (editable goal, default 100), ABS (first 20m) + Biceps & Back (next 20m)
   - **Tue / Thu / Sat (Group B)**: Warm-up, Push-ups, ABS (first 20m) + Triceps & Chest (next 20m)
   - **Sunday**: Dedicated Rest Day & recovery (no forced workout)
4. **Smart Voice & Alarm System**
   - Offline English Android `TextToSpeech` with selectable voices, speed, pitch, repetition count, and repeat delay.
   - 9 bundled `.wav` alarm sounds in `res/raw/`.
   - `AlarmManager` exact scheduling (`setAlarmClock`) with `setAndAllowWhileIdle` fallback, foreground `AlarmPlaybackService`, lock-screen `AlarmActivity`, and `BootReceiver` for reboot, locked-boot, time-change, timezone-change, and app-update restoration.
   - Dismissing an alarm silences audio without falsely marking the activity as completed.
5. **Offline-First + Cross-Device Cloud Sync & Backup**
   - Local **Room** database + **DataStore** preferences work offline for alarms, receivers, and all 10 sections.
   - Real-time **Firebase Firestore** sync gated behind **Google Sign-In** (`CredentialManager` + `GetSignInWithGoogleOption`).
   - Full JSON export/import backup via Android Storage Access Framework (`SAF`).

## Project Structure

- `app/src/main/java/com/example/data/model/` — Domain models & `DefaultTimetableFactory`
- `app/src/main/java/com/example/data/local/` — Room entities/DAOs/DB & `SettingsDataStore`
- `app/src/main/java/com/example/data/remote/` — Firestore models & `FirestoreSyncRepository`
- `app/src/main/java/com/example/data/repository/` — Offline-first `RoutineRepository` & JSON backup
- `app/src/main/java/com/example/alarm/` — `AlarmScheduler`, `AlarmReceiver`, `BootReceiver`, `AlarmPlaybackService`, `AlarmActivity`, `AlarmSoundPlayer`, `VoiceAnnouncementManager`, `AlarmNotificationHelper`
- `app/src/main/java/com/example/ui/` — Auth, ViewModel, Theme, Navigation & all 10 Jetpack Compose screens
- `firestore.rules` & `firebase-blueprint.json` — Zero-trust per-user Firestore security rules

## Device Setup & Permissions

- **Notifications (`POST_NOTIFICATIONS`)**: Requested on Android 13+ so alarm alerts and actions appear cleanly.
- **Exact Alarms (`SCHEDULE_EXACT_ALARM`)**: Pre-granted or promptable in **Settings → Alarm & Battery Reliability** on Android 12+.
- **Battery Optimization**: Use the in-app **Battery Optimization** shortcut in Settings to exempt the app if your OEM aggressively kills background alarms.
- **English TTS Voice**: Uses Android's built-in English TTS engine offline; if voice packs are missing, use the Settings shortcut to install Android TTS voice data.

## Building an APK

### Command Line (Gradle)

Ensure `ANDROID_SDK_ROOT` is configured, then run from the project root:

```bash
# Debug APK
gradle :app:assembleDebug

# Release APK (requires release keystore environment variables)
gradle :app:assembleRelease
```

The debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Tests

```bash
# Run all unit and Robolectric tests
gradle :app:testDebugUnitTest
```

### Android Studio

1. Open the root folder in Android Studio.
2. Wait for Gradle sync to complete (`app/google-services.json` is already configured).
3. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
