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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.CompletionRecord
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.RoutineActivity
import com.example.ui.components.SectionHeaderCard
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.WeeklySummaryStats
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StudyingScreen(
    state: DashboardUiState,
    weeklyStats: WeeklySummaryStats,
    onEditActivity: (RoutineActivity) -> Unit,
    onSaveRecord: (CompletionRecord, String) -> Unit
) {
    val today = state.now.toLocalDate()
    val dayOfWeek = today.dayOfWeek
    val todayMinutes = DefaultTimetableFactory.studyMinutesForDay(dayOfWeek)
    val todayAct = state.todayActivities.firstOrNull { it.category == ActivityCategory.STUDY }

    ScheduleFocusScreen(
        title = "Weekly Study Schedule",
        subtitle = "Total: 4 hours/week • Sun & Tue 30m • Mon/Wed/Thu 1h • Fri/Sat Off",
        icon = Icons.Default.School,
        accent = AzurePrimary,
        todayMinutesScheduled = todayMinutes,
        todayActivity = todayAct,
        todayKey = today.toString(),
        state = state,
        weeklyLoggedMinutes = weeklyStats.totalStudyMinutes,
        weeklyTargetMinutes = 240,
        weeklyRows = listOf(
            "Sunday" to "3:00 – 3:30 PM (30 min)",
            "Monday" to "3:00 – 4:00 PM (1 hour)",
            "Tuesday" to "3:00 – 3:30 PM (30 min)",
            "Wednesday" to "3:00 – 4:00 PM (1 hour)",
            "Thursday" to "3:00 – 4:00 PM (1 hour)",
            "Friday" to "No scheduled study",
            "Saturday" to "No scheduled study"
        ),
        isStudyMode = true,
        onEditActivity = onEditActivity,
        onSaveRecord = onSaveRecord
    )
}

@Composable
fun WebDevelopmentScreen(
    state: DashboardUiState,
    weeklyStats: WeeklySummaryStats,
    onEditActivity: (RoutineActivity) -> Unit,
    onSaveRecord: (CompletionRecord, String) -> Unit
) {
    val today = state.now.toLocalDate()
    val dayOfWeek = today.dayOfWeek
    val todayMinutes = DefaultTimetableFactory.webMinutesForDay(dayOfWeek)
    val todayAct = state.todayActivities.firstOrNull { it.category == ActivityCategory.WEB_DEV }

    ScheduleFocusScreen(
        title = "Weekly Web Development",
        subtitle = "Total: 3 hours/week • Sun & Tue 3:30–4 PM (30m) • Fri & Sat 3–4 PM (1h)",
        icon = Icons.Default.Code,
        accent = VioletAccent,
        todayMinutesScheduled = todayMinutes,
        todayActivity = todayAct,
        todayKey = today.toString(),
        state = state,
        weeklyLoggedMinutes = weeklyStats.totalWebMinutes,
        weeklyTargetMinutes = 180,
        weeklyRows = listOf(
            "Sunday" to "3:30 – 4:00 PM (30 min)",
            "Monday" to "No scheduled Web learning",
            "Tuesday" to "3:30 – 4:00 PM (30 min)",
            "Wednesday" to "No scheduled Web learning",
            "Thursday" to "No scheduled Web learning",
            "Friday" to "3:00 – 4:00 PM (1 hour)",
            "Saturday" to "3:00 – 4:00 PM (1 hour)"
        ),
        isStudyMode = false,
        onEditActivity = onEditActivity,
        onSaveRecord = onSaveRecord
    )
}

@Composable
private fun ScheduleFocusScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    todayMinutesScheduled: Int,
    todayActivity: RoutineActivity?,
    todayKey: String,
    state: DashboardUiState,
    weeklyLoggedMinutes: Int,
    weeklyTargetMinutes: Int,
    weeklyRows: List<Pair<String, String>>,
    isStudyMode: Boolean,
    onEditActivity: (RoutineActivity) -> Unit,
    onSaveRecord: (CompletionRecord, String) -> Unit
) {
    val dayName = state.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val activityId = todayActivity?.id ?: if (isStudyMode) "study_manual" else "web_manual"
    val existing = state.completionsByKey["${todayKey}__${activityId}"]
    val isDone = existing?.status == ActivityStatus.COMPLETED

    var remainingSeconds by remember(todayMinutesScheduled) {
        mutableIntStateOf((todayMinutesScheduled.coerceAtLeast(15)) * 60)
    }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0) running = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeaderCard(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    accent = accent,
                    trailing = if (todayActivity != null) {
                        {
                            IconButton(onClick = { onEditActivity(todayActivity) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit duration or alarm", tint = accent)
                            }
                        }
                    } else null
                )
            }

            // Today's Status + Timer
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (todayMinutesScheduled > 0) {
                                "Today ($dayName): $todayMinutesScheduled minutes scheduled"
                            } else {
                                "Today ($dayName): No session scheduled"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (todayMinutesScheduled > 0) SkyAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (todayMinutesScheduled > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = String.format(Locale.ENGLISH, "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = MonoFontFamily),
                                    color = accent
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { running = !running }) {
                                        Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Toggle session timer")
                                    }
                                    OutlinedButton(onClick = {
                                        running = false
                                        remainingSeconds = todayMinutesScheduled * 60
                                    }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset session timer")
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val nextStatus = if (isDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                    val rec = (existing ?: CompletionRecord(
                                        id = "${todayKey}__${activityId}",
                                        dateKey = todayKey,
                                        activityId = activityId
                                    )).copy(
                                        status = nextStatus,
                                        completedAtMillis = if (nextStatus == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L,
                                        studyMinutes = if (isStudyMode && nextStatus == ActivityStatus.COMPLETED) todayMinutesScheduled else (existing?.studyMinutes ?: 0),
                                        webMinutes = if (!isStudyMode && nextStatus == ActivityStatus.COMPLETED) todayMinutesScheduled else (existing?.webMinutes ?: 0)
                                    )
                                    onSaveRecord(rec, if (nextStatus == ActivityStatus.COMPLETED) "Session logged!" else "Session unmarked")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDone) EmeraldSuccess else accent),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag(if (isStudyMode) "study_complete_button" else "web_complete_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isDone) "Completed Today" else "Mark Session Completed")
                            }
                        }
                    }
                }
            }

            // Weekly Progress
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val loggedHrs = String.format(Locale.ENGLISH, "%.1f", weeklyLoggedMinutes / 60.0)
                        val targetHrs = String.format(Locale.ENGLISH, "%.1f", weeklyTargetMinutes / 60.0)
                        Text("Weekly Total Logged", style = MaterialTheme.typography.titleMedium)
                        Text("$loggedHrs / $targetHrs hours completed this week", style = MaterialTheme.typography.bodyMedium, color = EmeraldSuccess)
                        LinearProgressIndicator(
                            progress = { (weeklyLoggedMinutes.toFloat() / weeklyTargetMinutes.toFloat()).coerceIn(0f, 1f) },
                            color = EmeraldSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }

            // 7-Day Table
            items(weeklyRows) { (dayLabel, slot) ->
                val isTodayRow = dayLabel.equals(dayName, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isTodayRow) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = if (isTodayRow) "$dayLabel (Today)" else dayLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isTodayRow) accent else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = slot,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
