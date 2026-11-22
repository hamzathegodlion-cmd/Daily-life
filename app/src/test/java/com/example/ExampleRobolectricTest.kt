package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmNotificationHelper
import com.example.alarm.AlarmPlaybackService
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmScheduler
import com.example.alarm.BootReceiver
import com.example.data.local.RoutineDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.ActivityStatus
import com.example.data.model.AssistantSettings
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.ThemeMode
import com.example.data.model.WorkoutGroupType
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.repository.RoutineRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.DailyLifeTheme
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.RoutineViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var inMemoryDb: RoutineDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, RoutineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun `app name matches My Daily Life Assistant`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("My Daily Life Assistant", appName)
    }

    @Test
    fun `alarm scheduler schedules exact alarms and handles midnight and timezone`() {
        val scheduler = AlarmScheduler(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val wakeUp = DefaultTimetableFactory.defaultActivities().first { it.id == "wake_up" }
        val nyZone = ZoneId.of("America/New_York")
        // Monday at 10:00 PM -> wake_up is 10:30 PM same day
        val refTime = ZonedDateTime.of(2026, 4, 6, 22, 0, 0, 0, nyZone)
        val triggerMillis = scheduler.scheduleNextOccurrence(wakeUp, DayOfWeek.MONDAY, refTime)
        val expectedMillis = ZonedDateTime.of(2026, 4, 6, 22, 30, 0, 0, nyZone).toInstant().toEpochMilli()
        assertEquals(expectedMillis, triggerMillis)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        // After 10:30 PM on Monday (11:00 PM), next Monday wake_up rolls +7 days cleanly
        val afterTime = ZonedDateTime.of(2026, 4, 6, 23, 0, 0, 0, nyZone)
        val nextWeek = AlarmScheduler.computeNextTriggerTime(22, 30, DayOfWeek.MONDAY, afterTime)
        assertEquals(ZonedDateTime.of(2026, 4, 13, 22, 30, 0, 0, nyZone), nextWeek)
    }

    @Test
    fun `alarm scheduler reschedules all enabled alarms and skips mosque_fajr duplicate`() {
        val scheduler = AlarmScheduler(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val defaults = DefaultTimetableFactory.defaultActivities()
        scheduler.rescheduleAll(defaults)

        val scheduledCount = shadowAlarmManager.scheduledAlarms.size
        // Check exact count:
        // Daily (7 alarms): wake_up, quran, sport, learning, cycling, mosque_kitab, youtube, daily_check, sleep = 9 * 7 = 63
        // Study Sun/Tue = 2, Study Mon/Wed/Thu = 3, Web Sun/Tue = 2, Web Fri/Sat = 2 => +9 = 72
        assertEquals(72, scheduledCount)
    }

    @Test
    fun `boot receiver restores alarms on reboot and timezone change`() {
        val receiver = BootReceiver()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        // Allow background coroutine in BootReceiver to finish seeding & rescheduling
        Thread.sleep(500)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        shadowAlarmManager.scheduledAlarms.clear()
        receiver.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        Thread.sleep(500)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())
    }

    @Test
    fun `alarm receiver starts foreground AlarmPlaybackService`() {
        val receiver = AlarmReceiver()
        val app = ApplicationProvider.getApplicationContext<DailyLifeApplication>()
        val shadowApp = shadowOf(app)

        val intent = Intent(AlarmScheduler.ACTION_TRIGGER_ALARM).apply {
            putExtra(AlarmScheduler.EXTRA_ACTIVITY_ID, "quran")
            putExtra(AlarmScheduler.EXTRA_WEEKDAY, DayOfWeek.MONDAY.value)
            putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
        }
        receiver.onReceive(app, intent)
        Thread.sleep(500)

        val startedService = shadowApp.nextStartedService
        assertNotNull(startedService)
        assertEquals(AlarmPlaybackService::class.java.name, startedService.component?.className)
        assertEquals(AlarmPlaybackService.ACTION_START_ALARM, startedService.action)
        assertEquals("quran", startedService.getStringExtra(AlarmPlaybackService.EXTRA_ACTIVITY_ID))
    }

    @Test
    fun `alarm notification builds full screen intent and dismiss snooze complete actions`() {
        val notification = AlarmNotificationHelper.buildAlarmNotification(
            context = context,
            activityId = "sport",
            title = "Sport",
            scheduledTimeText = "12:50 AM",
            announcementText = "Sport time! Today is Monday! Your workout group is Group A.",
            weekdayValue = DayOfWeek.MONDAY.value
        )

        assertNotNull(notification.fullScreenIntent)
        assertNotNull(notification.contentIntent)
        assertEquals(3, notification.actions.size)
        val titles = notification.actions.map { it.title.toString() }
        assertTrue(titles.contains(context.getString(R.string.action_complete)))
        assertTrue(titles.contains(context.getString(R.string.action_snooze)))
        assertTrue(titles.contains(context.getString(R.string.action_dismiss)))
    }

    @Test
    fun `room database and repository persist offline edits completions and json backup`() = runBlocking {
        val dataStore = SettingsDataStore(context)
        val scheduler = AlarmScheduler(context)
        val firestore = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        val auth = FirebaseAuth.getInstance()
        val syncRepo = FirestoreSyncRepository(firestore, auth)
        val repo = RoutineRepository(inMemoryDb, dataStore, syncRepo, scheduler, auth)

        repo.ensureSeededLocally()
        val seededActs = repo.getActivitiesOnce()
        assertEquals(14, seededActs.size)

        // Edit Cycling activity & save offline
        val cycling = seededActs.first { it.id == "cycling" }
        repo.upsertActivity(cycling.copy(title = "Morning Mosque Cycling", description = "24 km round trip"))
        assertEquals("Morning Mosque Cycling", repo.getActivityById("cycling")?.title)

        // Mark completed
        val dateKey = LocalDate.of(2026, 4, 6).toString()
        repo.markActivityStatus("quran", dateKey, ActivityStatus.COMPLETED, "Finished 1 Juz")
        val rec = inMemoryDb.completionDao().getRecordById("${dateKey}__quran")
        assertEquals(ActivityStatus.COMPLETED.name, rec?.status)

        // Dismiss action check: incomplete activities are NOT auto-completed unless markActivityStatus(COMPLETED) is called
        assertNull(inMemoryDb.completionDao().getRecordById("${dateKey}__wake_up"))

        // Export JSON and restore original timetable
        val backupJson = repo.exportBackupJson()
        assertTrue(backupJson.contains("Morning Mosque Cycling"))
        assertTrue(backupJson.contains("Finished 1 Juz"))

        repo.restoreOriginalTimetable()
        assertEquals("Cycling", repo.getActivityById("cycling")?.title)

        // Import backup restores edited activity & completion
        val importRes = repo.importBackupJson(backupJson)
        assertTrue(importRes.isSuccess)
        assertEquals("Morning Mosque Cycling", repo.getActivityById("cycling")?.title)
        assertEquals(ActivityStatus.COMPLETED.name, inMemoryDb.completionDao().getRecordById("${dateKey}__quran")?.status)
    }

    @Test
    fun `settings datastore persists tts and personal goal preferences`() = runBlocking {
        val dataStore = SettingsDataStore(context)
        val updated = AssistantSettings(
            displayName = "Abdullah",
            quranWirdJuz = 2,
            quranMurajaahPages = 10,
            cyclingTargetKm = 25.5,
            youtubeDailyVideos = 3,
            pushupGoal = 120,
            ttsEnabled = true,
            ttsSpeed = 1.25f,
            ttsPitch = 0.95f,
            ttsRepetitions = 3,
            ttsDelaySeconds = 4,
            alarmVolume = 0.8f,
            vibrationEnabled = false,
            soundEnabled = true,
            soundDurationSeconds = 30,
            snoozeMinutes = 10,
            themeMode = ThemeMode.LIGHT,
            use24HourFormat = true
        )
        dataStore.updateSettings(updated)
        val loaded = dataStore.getSettingsOnce()
        assertEquals(updated, loaded)
    }

    @Test
    fun `home screen renders digital clock workout card and progress card`() {
        val now = ZonedDateTime.of(2026, 4, 6, 15, 10, 0, 0, ZoneId.systemDefault())
        val defaults = DefaultTimetableFactory.defaultActivities()
        val (nextAct, countdown) = RoutineViewModel.findNextActivityAndCountdown(defaults, now)
        val uiState = DashboardUiState(
            now = now,
            activities = defaults,
            todayActivities = defaults,
            currentActivity = defaults.first { it.id == "study_mon_wed_thu" },
            nextActivity = nextAct,
            countdownToNext = countdown,
            todayWorkoutGroup = WorkoutGroupType.GROUP_A,
            workoutPlans = DefaultTimetableFactory.defaultWorkoutPlans(),
            completionsByKey = emptyMap(),
            todayStats = RoutineViewModel.computeDailyStats(now.toLocalDate(), defaults, emptyMap(), now.toLocalTime()),
            settings = AssistantSettings()
        )

        composeTestRule.setContent {
            DailyLifeTheme {
                HomeScreen(
                    state = uiState,
                    onNavigate = {},
                    onToggleComplete = {},
                    onEditActivity = {},
                    onPreviewVoice = {},
                    onOpenAllSections = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_clock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_countdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("TODAY IS MONDAY").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_workout_card"))
        composeTestRule.onNodeWithTag("home_workout_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_progress_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Group A").assertIsDisplayed()
    }
}
