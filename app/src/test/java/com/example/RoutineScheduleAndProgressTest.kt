package com.example

import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.CompletionRecord
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.RoutineActivity
import com.example.data.model.WeekdayMask
import com.example.data.model.WorkoutGroupType
import com.example.ui.viewmodel.RoutineViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class RoutineScheduleAndProgressTest {

    private val defaults = DefaultTimetableFactory.defaultActivities()

    @Test
    fun `default timetable preserves all exact required times and 12h AM PM formatting`() {
        assertEquals(14, defaults.size)

        val byId = defaults.associateBy { it.id }

        assertEquals("10:30 PM", byId["wake_up"]!!.formatStartTime(false))
        assertEquals("10:30 PM – 12:00 AM", byId["mosque_fajr"]!!.formatTimeRange(false))
        assertEquals("12:00 AM – 12:50 AM", byId["quran"]!!.formatTimeRange(false))
        assertEquals("12:50 AM – 1:30 AM", byId["sport"]!!.formatTimeRange(false))
        assertEquals("1:30 AM – 11:30 AM", byId["learning"]!!.formatTimeRange(false))
        assertEquals("11:30 AM – 12:00 PM", byId["cycling"]!!.formatTimeRange(false))
        assertEquals("12:00 PM – 2:30 PM", byId["mosque_kitab"]!!.formatTimeRange(false))
        assertEquals("2:30 PM – 3:00 PM", byId["youtube"]!!.formatTimeRange(false))
        assertEquals("3:00 PM – 3:30 PM", byId["study_sun_tue"]!!.formatTimeRange(false))
        assertEquals("3:00 PM – 4:00 PM", byId["study_mon_wed_thu"]!!.formatTimeRange(false))
        assertEquals("3:30 PM – 4:00 PM", byId["web_sun_tue"]!!.formatTimeRange(false))
        assertEquals("3:00 PM – 4:00 PM", byId["web_fri_sat"]!!.formatTimeRange(false))
        assertEquals("4:00 PM – 4:30 PM", byId["daily_check"]!!.formatTimeRange(false))
        assertEquals("4:30 PM – 10:30 PM", byId["sleep"]!!.formatTimeRange(false))
    }

    @Test
    fun `midnight crossing detection and active time window work accurately`() {
        val mosqueFajr = defaults.first { it.id == "mosque_fajr" }
        assertTrue(mosqueFajr.crossesMidnight)
        assertEquals(90, mosqueFajr.durationMinutes())
        assertTrue(mosqueFajr.isActiveAt(LocalTime.of(22, 45)))
        assertTrue(mosqueFajr.isActiveAt(LocalTime.of(23, 59)))
        assertFalse(mosqueFajr.isActiveAt(LocalTime.of(0, 0)))

        val sleep = defaults.first { it.id == "sleep" }
        assertFalse(sleep.crossesMidnight)
        assertEquals(360, sleep.durationMinutes())
        assertTrue(sleep.isActiveAt(LocalTime.of(16, 30)))
        assertTrue(sleep.isActiveAt(LocalTime.of(22, 29)))
        assertFalse(sleep.isActiveAt(LocalTime.of(22, 30)))

        // Custom midnight crossing e.g. 11:00 PM to 1:00 AM
        val customNight = RoutineActivity(
            id = "custom_night",
            title = "Night",
            description = "",
            category = ActivityCategory.CUSTOM,
            startHour = 23,
            startMinute = 0,
            endHour = 1,
            endMinute = 0,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            announcementText = ""
        )
        assertTrue(customNight.crossesMidnight)
        assertEquals(120, customNight.durationMinutes())
        assertTrue(customNight.isActiveAt(LocalTime.of(23, 30)))
        assertTrue(customNight.isActiveAt(LocalTime.of(0, 45)))
        assertFalse(customNight.isActiveAt(LocalTime.of(1, 0)))
    }

    @Test
    fun `weekly study and web dev schedules total 4 hours study and 3 hours web with exact transitions`() {
        val totalStudy = DayOfWeek.entries.sumOf { DefaultTimetableFactory.studyMinutesForDay(it) }
        val totalWeb = DayOfWeek.entries.sumOf { DefaultTimetableFactory.webMinutesForDay(it) }
        assertEquals(240, totalStudy) // 4 hours/week
        assertEquals(180, totalWeb)   // 3 hours/week

        // Sunday & Tuesday: 3:00-3:30 Study, 3:30-4:00 Web
        listOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY).forEach { day ->
            assertEquals(30, DefaultTimetableFactory.studyMinutesForDay(day))
            assertEquals(30, DefaultTimetableFactory.webMinutesForDay(day))
            val activeForDay = defaults.filter { WeekdayMask.includes(it.weekdaysMask, day) }
            val afternoon = activeForDay.filter { it.category == ActivityCategory.STUDY || it.category == ActivityCategory.WEB_DEV }
            assertEquals(2, afternoon.size)
            assertEquals("study_sun_tue", afternoon[0].id)
            assertEquals("web_sun_tue", afternoon[1].id)
        }

        // Mon, Wed, Thu: 1 hour Study, 0 Web
        listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY).forEach { day ->
            assertEquals(60, DefaultTimetableFactory.studyMinutesForDay(day))
            assertEquals(0, DefaultTimetableFactory.webMinutesForDay(day))
            val activeForDay = defaults.filter { WeekdayMask.includes(it.weekdaysMask, day) }
            val afternoon = activeForDay.filter { it.category == ActivityCategory.STUDY || it.category == ActivityCategory.WEB_DEV }
            assertEquals(1, afternoon.size)
            assertEquals("study_mon_wed_thu", afternoon[0].id)
        }

        // Fri & Sat: 0 Study, 1 hour Web
        listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach { day ->
            assertEquals(0, DefaultTimetableFactory.studyMinutesForDay(day))
            assertEquals(60, DefaultTimetableFactory.webMinutesForDay(day))
            val activeForDay = defaults.filter { WeekdayMask.includes(it.weekdaysMask, day) }
            val afternoon = activeForDay.filter { it.category == ActivityCategory.STUDY || it.category == ActivityCategory.WEB_DEV }
            assertEquals(1, afternoon.size)
            assertEquals("web_fri_sat", afternoon[0].id)
        }
    }

    @Test
    fun `no overlapping enabled alarms exist on any weekday`() {
        DayOfWeek.entries.forEach { day ->
            val enabledTimes = defaults
                .filter { it.alarmEnabled && WeekdayMask.includes(it.weekdaysMask, day) }
                .map { it.startHour to it.startMinute }
            assertEquals(
                "Duplicate alarm time found on $day: $enabledTimes",
                enabledTimes.distinct().size,
                enabledTimes.size
            )
        }
    }

    @Test
    fun `workout groups match Mon-Wed-Fri Group A, Tue-Thu-Sat Group B, and Sunday Rest`() {
        assertEquals(WorkoutGroupType.GROUP_A, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.MONDAY))
        assertEquals(WorkoutGroupType.GROUP_A, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.WEDNESDAY))
        assertEquals(WorkoutGroupType.GROUP_A, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.FRIDAY))

        assertEquals(WorkoutGroupType.GROUP_B, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.TUESDAY))
        assertEquals(WorkoutGroupType.GROUP_B, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.THURSDAY))
        assertEquals(WorkoutGroupType.GROUP_B, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.SATURDAY))

        assertEquals(WorkoutGroupType.REST_DAY, DefaultTimetableFactory.workoutGroupForDay(DayOfWeek.SUNDAY))

        val plans = DefaultTimetableFactory.defaultWorkoutPlans(pushupGoal = 100)
        assertEquals(3, plans.size)
        val groupA = plans.first { it.id == "group_a" }
        val groupB = plans.first { it.id == "group_b" }
        val sunRest = plans.first { it.id == "sunday_rest" }

        assertFalse(groupA.restDay)
        assertTrue(groupA.exercises.any { it.name == "Biceps" } && groupA.exercises.any { it.name == "Back" })
        assertTrue(groupA.exercises.first { it.name == "Push-ups" }.targetReps == 100)

        assertFalse(groupB.restDay)
        assertTrue(groupB.exercises.any { it.name == "Triceps" } && groupB.exercises.any { it.name == "Chest" })

        assertTrue(sunRest.restDay)
    }

    @Test
    fun `dynamic voice announcement changes by day of week for Sport, Study, and Web`() {
        val sport = defaults.first { it.id == "sport" }
        val monSpeech = DefaultTimetableFactory.buildDynamicAnnouncement(sport, DayOfWeek.MONDAY)
        val tueSpeech = DefaultTimetableFactory.buildDynamicAnnouncement(sport, DayOfWeek.TUESDAY)
        val sunSpeech = DefaultTimetableFactory.buildDynamicAnnouncement(sport, DayOfWeek.SUNDAY)

        assertTrue(monSpeech.contains("Monday") && monSpeech.contains("Group A") && monSpeech.contains("biceps and back"))
        assertTrue(tueSpeech.contains("Tuesday") && tueSpeech.contains("Group B") && tueSpeech.contains("triceps and chest"))
        assertTrue(sunSpeech.contains("Sunday") && sunSpeech.contains("rest day"))

        val studyTue = defaults.first { it.id == "study_sun_tue" }
        assertTrue(DefaultTimetableFactory.buildDynamicAnnouncement(studyTue, DayOfWeek.TUESDAY).contains("30 minutes"))

        val webSat = defaults.first { it.id == "web_fri_sat" }
        assertTrue(DefaultTimetableFactory.buildDynamicAnnouncement(webSat, DayOfWeek.SATURDAY).contains("60 minutes"))
    }

    @Test
    fun `daily and weekly progress calculations aggregate completed activities accurately`() {
        val today = LocalDate.now()
        val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val monday = sunday.plusDays(1)
        val monActs = defaults.filter { WeekdayMask.includes(it.weekdaysMask, DayOfWeek.MONDAY) }

        val completions = listOf(
            CompletionRecord(
                id = "${monday}__wake_up",
                dateKey = monday.toString(),
                activityId = "wake_up",
                status = ActivityStatus.COMPLETED
            ),
            CompletionRecord(
                id = "${monday}__quran",
                dateKey = monday.toString(),
                activityId = "quran",
                status = ActivityStatus.COMPLETED,
                quranWirdDone = 1,
                quranMurajaahDone = 5
            ),
            CompletionRecord(
                id = "${monday}__sport",
                dateKey = monday.toString(),
                activityId = "sport",
                status = ActivityStatus.COMPLETED,
                workoutCompletedCount = 5
            ),
            CompletionRecord(
                id = "${monday}__cycling",
                dateKey = monday.toString(),
                activityId = "cycling",
                status = ActivityStatus.COMPLETED,
                cyclingDistanceKm = 22.0
            ),
            CompletionRecord(
                id = "${monday}__youtube",
                dateKey = monday.toString(),
                activityId = "youtube",
                status = ActivityStatus.COMPLETED,
                v1Edited = true,
                v1Uploaded = true,
                v2Edited = true,
                v2Uploaded = true
            ),
            CompletionRecord(
                id = "${monday}__study_mon_wed_thu",
                dateKey = monday.toString(),
                activityId = "study_mon_wed_thu",
                status = ActivityStatus.COMPLETED,
                studyMinutes = 60
            )
        )

        val compMap = completions.associateBy { it.id }
        val dailyStats = RoutineViewModel.computeDailyStats(monday, monActs, compMap, LocalTime.of(10, 0))
        assertEquals(monActs.size, dailyStats.totalScheduled)
        assertEquals(6, dailyStats.completedCount)
        assertEquals((6 * 100.0 / monActs.size).toInt(), dailyStats.completionPercent)

        val weeklyStats = RoutineViewModel.computeWeeklyStats(0, defaults, completions)
        assertEquals(60, weeklyStats.totalStudyMinutes)
        assertEquals(1, weeklyStats.completedWorkouts)
        assertEquals(1, weeklyStats.quranDaysCompleted)
        assertEquals(2, weeklyStats.youtubeVideosUploaded)
        assertEquals(22.0, weeklyStats.cyclingDistanceKm, 0.01)
    }

    @Test
    fun `next activity countdown picks upcoming Sunday 3_30 PM Web transition from 3_15 PM`() {
        val sundayAfternoon = ZonedDateTime.of(2026, 4, 5, 15, 15, 0, 0, ZoneId.of("UTC"))
        val (nextAct, countdown) = RoutineViewModel.findNextActivityAndCountdown(defaults, sundayAfternoon)
        assertEquals("web_sun_tue", nextAct?.id)
        assertEquals(15L, countdown.toMinutes())
    }
}
