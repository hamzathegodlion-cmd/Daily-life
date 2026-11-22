package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityStatus
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.RoutineActivity
import com.example.data.model.WeekdayMask
import com.example.ui.components.ActivityTimelineCard
import com.example.ui.components.SectionHeaderCard
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DashboardUiState
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyRoutineScreen(
    state: DashboardUiState,
    onAddActivity: () -> Unit,
    onEditActivity: (RoutineActivity) -> Unit,
    onDeleteActivity: (RoutineActivity) -> Unit,
    onToggleComplete: (RoutineActivity) -> Unit,
    onPreviewVoice: (RoutineActivity) -> Unit,
    onRestoreOriginal: () -> Unit
) {
    val todayKey = state.now.toLocalDate().toString()
    val use24 = state.settings.use24HourFormat
    var filterMode by remember { mutableStateOf("TODAY") } // TODAY, ALL, INCOMPLETE
    var pendingDelete by remember { mutableStateOf<RoutineActivity?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val displayed = when (filterMode) {
        "ALL" -> state.activities
        "INCOMPLETE" -> state.todayActivities.filter {
            state.completionsByKey["${todayKey}__${it.id}"]?.status != ActivityStatus.COMPLETED
        }
        else -> state.todayActivities
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeaderCard(
                    title = "Daily Routine",
                    subtitle = "10:30 PM Wake-up to 4:30 PM Sleep • Local Timezone",
                    icon = Icons.Default.Schedule,
                    accent = SkyAccent,
                    trailing = {
                        OutlinedButton(
                            onClick = { showRestoreConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("restore_timetable_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore Original Timetable")
                        }
                    }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterMode == "TODAY",
                        onClick = { filterMode = "TODAY" },
                        label = { Text("Today (${state.todayActivities.size})") }
                    )
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("All (${state.activities.size})") }
                    )
                    FilterChip(
                        selected = filterMode == "INCOMPLETE",
                        onClick = { filterMode = "INCOMPLETE" },
                        label = { Text("Remaining") }
                    )
                }
            }

            items(displayed, key = { it.id }) { activity ->
                val status = state.completionsByKey["${todayKey}__${activity.id}"]?.status ?: ActivityStatus.INCOMPLETE
                ActivityTimelineCard(
                    activity = activity,
                    status = status,
                    use24Hour = use24,
                    isCurrent = state.currentActivity?.id == activity.id,
                    onToggleComplete = { onToggleComplete(activity) },
                    onEdit = { onEditActivity(activity) },
                    onPreviewVoice = { onPreviewVoice(activity) },
                    onDelete = { pendingDelete = activity }
                )
            }
        }

        FloatingActionButton(
            onClick = onAddActivity,
            containerColor = AzurePrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_activity_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add new activity")
        }
    }

    pendingDelete?.let { act ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Activity?") },
            text = { Text("Are you sure you want to delete \"${act.title}\"? Its scheduled alarms will also be cancelled.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteActivity(act)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_activity")
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Original Timetable?") },
            text = { Text("This restores your exact default daily schedule, study/web weekday slots, and Group A/Group B/Sunday workout plans.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreOriginal()
                        showRestoreConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_restore_timetable")
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun WeeklyCalendarScreen(
    state: DashboardUiState,
    selectedDay: DayOfWeek,
    onSelectDay: (DayOfWeek) -> Unit,
    onEditActivity: (RoutineActivity) -> Unit,
    onPreviewVoice: (RoutineActivity) -> Unit,
    onToggleComplete: (RoutineActivity) -> Unit
) {
    val todayDay = state.now.dayOfWeek
    val todayKey = state.now.toLocalDate().toString()
    val use24 = state.settings.use24HourFormat
    val dayActivities = state.activities
        .filter { WeekdayMask.includes(it.weekdaysMask, selectedDay) }
        .sortedBy { it.sortOrder }

    val workoutGroup = DefaultTimetableFactory.workoutGroupForDay(selectedDay)
    val studyMins = DefaultTimetableFactory.studyMinutesForDay(selectedDay)
    val webMins = DefaultTimetableFactory.webMinutesForDay(selectedDay)

    val orderedWeekdays = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeaderCard(
                    title = "7-Day Weekly Calendar",
                    subtitle = "Tap any day from Sunday to Saturday to inspect & edit its schedule",
                    icon = Icons.Default.CalendarMonth,
                    accent = AzurePrimary
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orderedWeekdays) { day ->
                        val isToday = day == todayDay
                        val isSelected = day == selectedDay
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDay(day) },
                            label = {
                                val short = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                                Text(if (isToday) "$short • Today" else short)
                            },
                            modifier = Modifier.testTag("calendar_day_${day.name}")
                        )
                    }
                }
            }

            item {
                val dayFull = selectedDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dayFull, style = MaterialTheme.typography.titleLarge)
                            if (selectedDay == todayDay) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = EmeraldSuccess.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        "CURRENT DAY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Workout: ${workoutGroup.title} (${workoutGroup.focus})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (workoutGroup.title == "Rest Day") EmeraldSuccess else CoralAlert
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Study: ${if (studyMins > 0) "$studyMins min" else "Off"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyAccent
                            )
                            Text(
                                text = "Web Dev: ${if (webMins > 0) "$webMins min" else "Off"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            items(dayActivities, key = { "cal_${selectedDay.name}_${it.id}" }) { activity ->
                val status = if (selectedDay == todayDay) {
                    state.completionsByKey["${todayKey}__${activity.id}"]?.status ?: ActivityStatus.INCOMPLETE
                } else {
                    ActivityStatus.INCOMPLETE
                }
                ActivityTimelineCard(
                    activity = activity,
                    status = status,
                    use24Hour = use24,
                    isCurrent = selectedDay == todayDay && state.currentActivity?.id == activity.id,
                    onToggleComplete = { if (selectedDay == todayDay) onToggleComplete(activity) },
                    onEdit = { onEditActivity(activity) },
                    onPreviewVoice = { onPreviewVoice(activity) }
                )
            }
        }
    }
}
