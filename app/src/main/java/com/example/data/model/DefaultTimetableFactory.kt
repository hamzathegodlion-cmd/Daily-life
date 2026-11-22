package com.example.data.model

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

object DefaultTimetableFactory {

    fun defaultActivities(): List<RoutineActivity> = listOf(
        RoutineActivity(
            id = "wake_up",
            title = "Wake up",
            description = "Wake up and prepare for mosque",
            category = ActivityCategory.WAKE_UP,
            startHour = 22,
            startMinute = 30,
            endHour = 22,
            endMinute = 30,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.GENTLE_WAKEUP.key,
            announcementText = "Wake up! Wake up! Wake up! It is time to wake up. Prepare yourself and go to the mosque for Fajr Salah.",
            sortOrder = 1,
            defaultKey = "wake_up"
        ),
        RoutineActivity(
            id = "mosque_fajr",
            title = "Mosque / Fajr Salah",
            description = "Go to the mosque and pray Fajr Salah",
            category = ActivityCategory.MOSQUE,
            startHour = 22,
            startMinute = 30,
            endHour = 0,
            endMinute = 0,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = false, // Shares 10:30 PM wake-up trigger to avoid duplicate alarms
            soundKey = AlarmSoundOption.SOFT_BELL.key,
            announcementText = "It is time to go to the mosque and pray Fajr Salah.",
            sortOrder = 2,
            defaultKey = "mosque_fajr"
        ),
        RoutineActivity(
            id = "quran",
            title = "Quran",
            description = "Quran reading (Wird: 1 Juz, Muraja'a: 5 Safha)",
            category = ActivityCategory.QURAN,
            startHour = 0,
            startMinute = 0,
            endHour = 0,
            endMinute = 50,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.QURAN_TONE.key,
            announcementText = "Quran! Quran! Quran! It is time for Quran. Begin your daily Wird and Muraja'a.",
            sortOrder = 3,
            defaultKey = "quran"
        ),
        RoutineActivity(
            id = "sport",
            title = "Sport",
            description = "Follow the correct daily workout (Group A / Group B / Sunday Rest)",
            category = ActivityCategory.SPORT,
            startHour = 0,
            startMinute = 50,
            endHour = 1,
            endMinute = 30,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.SPORT_TONE.key,
            announcementText = "Sport time! Sport time! Prepare for today's workout.",
            sortOrder = 4,
            defaultKey = "sport"
        ),
        RoutineActivity(
            id = "learning",
            title = "Learning",
            description = "Dedicated learning period",
            category = ActivityCategory.LEARNING,
            startHour = 1,
            startMinute = 30,
            endHour = 11,
            endMinute = 30,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.LEARNING_TONE.key,
            announcementText = "Learning time! Focus on your learning. Use your time wisely.",
            sortOrder = 5,
            defaultKey = "learning"
        ),
        RoutineActivity(
            id = "cycling",
            title = "Cycling",
            description = "Cycling journey, approximately 22 km round trip to the mosque and back",
            category = ActivityCategory.CYCLING,
            startHour = 11,
            startMinute = 30,
            endHour = 12,
            endMinute = 0,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.MOTIVATIONAL.key,
            announcementText = "Cycling time! Prepare for your cycling journey.",
            sortOrder = 6,
            defaultKey = "cycling"
        ),
        RoutineActivity(
            id = "mosque_kitab",
            title = "Mosque / Kitab",
            description = "Mosque activities and reading Kitab",
            category = ActivityCategory.KITAB,
            startHour = 12,
            startMinute = 0,
            endHour = 14,
            endMinute = 30,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.SOFT_BELL.key,
            announcementText = "Kitab time! It is time to go to the mosque and read Kitab.",
            sortOrder = 7,
            defaultKey = "mosque_kitab"
        ),
        RoutineActivity(
            id = "youtube",
            title = "YouTube",
            description = "Edit and upload two videos to YouTube",
            category = ActivityCategory.YOUTUBE,
            startHour = 14,
            startMinute = 30,
            endHour = 15,
            endMinute = 0,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.DIGITAL_ALARM.key,
            announcementText = "YouTube time! Start editing and uploading your two videos.",
            sortOrder = 8,
            defaultKey = "youtube"
        ),
        // Study on Sunday & Tuesday: 3:00 - 3:30 PM (30 min)
        RoutineActivity(
            id = "study_sun_tue",
            title = "Studying (30 min)",
            description = "Scheduled 30-minute study session before Web development",
            category = ActivityCategory.STUDY,
            startHour = 15,
            startMinute = 0,
            endHour = 15,
            endMinute = 30,
            weekdaysMask = WeekdayMask.STUDY_SUN_TUE,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.LEARNING_TONE.key,
            announcementText = "Study time! Today is your 30-minute study session from 3:00 to 3:30 PM.",
            sortOrder = 9,
            defaultKey = "study_sun_tue"
        ),
        // Study on Monday, Wednesday, Thursday: 3:00 - 4:00 PM (1 hour)
        RoutineActivity(
            id = "study_mon_wed_thu",
            title = "Studying (1 hour)",
            description = "Full 1-hour dedicated study session",
            category = ActivityCategory.STUDY,
            startHour = 15,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            weekdaysMask = WeekdayMask.STUDY_MON_WED_THU,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.LEARNING_TONE.key,
            announcementText = "Study time! Focus for your full 1-hour study session from 3:00 to 4:00 PM.",
            sortOrder = 10,
            defaultKey = "study_mon_wed_thu"
        ),
        // Web Development on Sunday & Tuesday: 3:30 - 4:00 PM (30 min)
        RoutineActivity(
            id = "web_sun_tue",
            title = "Web Development (30 min)",
            description = "Scheduled 30-minute Web development session",
            category = ActivityCategory.WEB_DEV,
            startHour = 15,
            startMinute = 30,
            endHour = 16,
            endMinute = 0,
            weekdaysMask = WeekdayMask.STUDY_SUN_TUE,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.MOTIVATIONAL.key,
            announcementText = "Web development time! Switch to your 30-minute Web learning session.",
            sortOrder = 11,
            defaultKey = "web_sun_tue"
        ),
        // Web Development on Friday & Saturday: 3:00 - 4:00 PM (1 hour)
        RoutineActivity(
            id = "web_fri_sat",
            title = "Web Development (1 hour)",
            description = "Full 1-hour continuous Web development session",
            category = ActivityCategory.WEB_DEV,
            startHour = 15,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            weekdaysMask = WeekdayMask.WEB_FRI_SAT,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.MOTIVATIONAL.key,
            announcementText = "Web development time! Begin your 1-hour Web learning session from 3:00 to 4:00 PM.",
            sortOrder = 12,
            defaultKey = "web_fri_sat"
        ),
        RoutineActivity(
            id = "daily_check",
            title = "Daily activity checking",
            description = "Review and check completed daily activities and progress",
            category = ActivityCategory.DAILY_CHECK,
            startHour = 16,
            startMinute = 0,
            endHour = 16,
            endMinute = 30,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.CLASSIC_ALARM.key,
            announcementText = "Daily checking time. Review your activities and check your progress.",
            sortOrder = 13,
            defaultKey = "daily_check"
        ),
        RoutineActivity(
            id = "sleep",
            title = "Sleep",
            description = "Fixed sleeping period (6 hours: 4:30 PM – 10:30 PM)",
            category = ActivityCategory.SLEEP,
            startHour = 16,
            startMinute = 30,
            endHour = 22,
            endMinute = 30,
            weekdaysMask = WeekdayMask.ALL_DAYS,
            repeatWeekly = true,
            alarmEnabled = true,
            soundKey = AlarmSoundOption.SLEEP_TONE.key,
            announcementText = "It is time to sleep. Rest well and prepare for your next day.",
            sortOrder = 14,
            defaultKey = "sleep"
        )
    )

    fun defaultWorkoutPlans(pushupGoal: Int = 100): List<WorkoutPlan> = listOf(
        WorkoutPlan(
            id = "group_a",
            groupName = "Group A",
            focusArea = "Biceps and Back",
            weekdaysMask = WeekdayMask.GROUP_A_DAYS,
            restDay = false,
            exercises = listOf(
                WorkoutExercise(
                    id = "a_warmup",
                    name = "Warm-up",
                    phase = "FIRST_20",
                    targetReps = 1,
                    targetSets = 1,
                    durationMinutes = 5,
                    description = "Dynamic joint mobility, arm circles, and light cardio warm-up."
                ),
                WorkoutExercise(
                    id = "a_pushups",
                    name = "Push-ups",
                    phase = "FIRST_20",
                    targetReps = pushupGoal,
                    targetSets = 4,
                    durationMinutes = 8,
                    description = "Editable personal goal ($pushupGoal total reps across comfortable sets). Modify or pause if fatigued."
                ),
                WorkoutExercise(
                    id = "a_abs",
                    name = "ABS",
                    phase = "FIRST_20",
                    targetReps = 20,
                    targetSets = 3,
                    durationMinutes = 7,
                    description = "Core crunches, leg raises, and planks with controlled breathing."
                ),
                WorkoutExercise(
                    id = "a_biceps",
                    name = "Biceps",
                    phase = "NEXT_20",
                    targetReps = 12,
                    targetSets = 3,
                    durationMinutes = 10,
                    description = "Bicep curls or resistance band curls with proper posture."
                ),
                WorkoutExercise(
                    id = "a_back",
                    name = "Back",
                    phase = "NEXT_20",
                    targetReps = 12,
                    targetSets = 3,
                    durationMinutes = 10,
                    description = "Rows, pull-ups, or Superman back extensions for upper-back strength."
                )
            )
        ),
        WorkoutPlan(
            id = "group_b",
            groupName = "Group B",
            focusArea = "Triceps and Chest",
            weekdaysMask = WeekdayMask.GROUP_B_DAYS,
            restDay = false,
            exercises = listOf(
                WorkoutExercise(
                    id = "b_warmup",
                    name = "Warm-up",
                    phase = "FIRST_20",
                    targetReps = 1,
                    targetSets = 1,
                    durationMinutes = 5,
                    description = "Shoulder rotations, chest openers, and gentle warm-up."
                ),
                WorkoutExercise(
                    id = "b_pushups",
                    name = "Push-ups",
                    phase = "FIRST_20",
                    targetReps = pushupGoal,
                    targetSets = 4,
                    durationMinutes = 8,
                    description = "Editable personal goal ($pushupGoal total reps across comfortable sets). Modify or rest as needed."
                ),
                WorkoutExercise(
                    id = "b_abs",
                    name = "ABS",
                    phase = "FIRST_20",
                    targetReps = 20,
                    targetSets = 3,
                    durationMinutes = 7,
                    description = "Core stabilization, planks, and bicycle crunches."
                ),
                WorkoutExercise(
                    id = "b_triceps",
                    name = "Triceps",
                    phase = "NEXT_20",
                    targetReps = 12,
                    targetSets = 3,
                    durationMinutes = 10,
                    description = "Tricep dips or overhead extensions with controlled form."
                ),
                WorkoutExercise(
                    id = "b_chest",
                    name = "Chest",
                    phase = "NEXT_20",
                    targetReps = 12,
                    targetSets = 3,
                    durationMinutes = 10,
                    description = "Incline/decline presses or wide push variations without straining."
                )
            )
        ),
        WorkoutPlan(
            id = "sunday_rest",
            groupName = "Rest Day",
            focusArea = "Recovery & Weekly Preparation",
            weekdaysMask = WeekdayMask.SUNDAY,
            restDay = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "sun_rest",
                    name = "Active Rest & Hydration",
                    phase = "FIRST_20",
                    targetReps = 1,
                    targetSets = 1,
                    durationMinutes = 15,
                    description = "Light stretching, hydration, and full muscular recovery for the new week."
                )
            )
        )
    )

    fun workoutGroupForDay(dayOfWeek: DayOfWeek): WorkoutGroupType = when (dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> WorkoutGroupType.GROUP_A
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY -> WorkoutGroupType.GROUP_B
        DayOfWeek.SUNDAY -> WorkoutGroupType.REST_DAY
    }

    fun studyMinutesForDay(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.SUNDAY, DayOfWeek.TUESDAY -> 30
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> 60
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> 0
    }

    fun webMinutesForDay(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.SUNDAY, DayOfWeek.TUESDAY -> 30
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> 60
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> 0
    }

    fun buildDynamicAnnouncement(activity: RoutineActivity, dayOfWeek: DayOfWeek): String {
        val dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return when (activity.category) {
            ActivityCategory.SPORT -> {
                when (workoutGroupForDay(dayOfWeek)) {
                    WorkoutGroupType.GROUP_A ->
                        "Sport time! Today is $dayName! Your workout group is Group A. Today's workout focuses on biceps and back."
                    WorkoutGroupType.GROUP_B ->
                        "Sport time! Today is $dayName! Your workout group is Group B. Today's workout focuses on triceps and chest."
                    WorkoutGroupType.REST_DAY ->
                        "Today is Sunday. It is your rest day. Allow your body to recover and prepare for the new week."
                }
            }
            ActivityCategory.STUDY -> {
                val mins = studyMinutesForDay(dayOfWeek)
                if (mins > 0) {
                    "Today is $dayName. It is study time for $mins minutes. ${activity.announcementText}"
                } else {
                    activity.announcementText
                }
            }
            ActivityCategory.WEB_DEV -> {
                val mins = webMinutesForDay(dayOfWeek)
                if (mins > 0) {
                    "Today is $dayName. It is Web development time for $mins minutes. ${activity.announcementText}"
                } else {
                    activity.announcementText
                }
            }
            else -> activity.announcementText.ifBlank {
                "It is time for ${activity.title}. ${activity.description}"
            }
        }
    }
}
