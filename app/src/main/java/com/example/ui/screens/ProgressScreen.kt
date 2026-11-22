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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.CompletionRecord
import com.example.ui.components.SectionHeaderCard
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.WeeklySummaryStats
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(
    state: DashboardUiState,
    weeklyStats: WeeklySummaryStats,
    weekOffset: Int,
    onShiftWeek: (Int) -> Unit,
    onSaveDailyCheck: (CompletionRecord, String) -> Unit
) {
    val today = state.now.toLocalDate()
    val todayKey = today.toString()
    val todayStats = state.todayStats
    val dailyCheckRec = state.completionsByKey["${todayKey}__daily_check"]
        ?: CompletionRecord(id = "${todayKey}__daily_check", dateKey = todayKey, activityId = "daily_check")
    val dailyCheckDone = dailyCheckRec.status == ActivityStatus.COMPLETED

    val motivationalMessage = when {
        todayStats.completionPercent >= 90 -> "Outstanding discipline! Almost every scheduled activity is complete."
        todayStats.completionPercent >= 70 -> "Strong momentum! Keep finishing your remaining daily targets."
        todayStats.completionPercent >= 40 -> "Steady progress. Review your incomplete tasks and focus on the next block."
        else -> "Every step counts. Start with one scheduled activity right now."
    }

    val categoryBreakdown = listOf(
        "Quran" to ActivityCategory.QURAN,
        "Workout" to ActivityCategory.SPORT,
        "Studying" to ActivityCategory.STUDY,
        "Web Dev" to ActivityCategory.WEB_DEV,
        "YouTube" to ActivityCategory.YOUTUBE,
        "Cycling" to ActivityCategory.CYCLING,
        "Mosque / Fajr" to ActivityCategory.MOSQUE,
        "Mosque / Kitab" to ActivityCategory.KITAB,
        "Sleep" to ActivityCategory.SLEEP
    ).map { (label, cat) ->
        val acts = state.todayActivities.filter { it.category == cat }
        val done = acts.count { state.completionsByKey["${todayKey}__${it.id}"]?.status == ActivityStatus.COMPLETED }
        Triple(label, done, acts.size)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Daily Activity Checking (4:00 - 4:30 PM Review)
            item {
                SectionHeaderCard(
                    title = "Daily Activity Checking",
                    subtitle = "4:00 PM – 4:30 PM • Review completed, incomplete & missed items",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    accent = SkyAccent
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Daily progress: ${todayStats.completionPercent}%", style = MaterialTheme.typography.headlineMedium, color = SkyAccent)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = EmeraldSuccess.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    "${todayStats.completedCount}/${todayStats.totalScheduled}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { todayStats.completionPercent / 100f },
                            color = EmeraldSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatPill("Completed", todayStats.completedCount.toString(), EmeraldSuccess, Modifier.weight(1f))
                            StatPill("Remaining", todayStats.incompleteCount.toString(), AzurePrimary, Modifier.weight(1f))
                            StatPill("Missed", todayStats.missedCount.toString(), CoralAlert, Modifier.weight(1f))
                        }

                        Text(
                            text = motivationalMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Domain breakdown grid
                        categoryBreakdown.chunked(3).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                rowItems.forEach { (label, done, total) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                            Text(
                                                text = if (total == 0) "Off today" else "$done / $total",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (total > 0 && done == total) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val next = if (dailyCheckDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                onSaveDailyCheck(
                                    dailyCheckRec.copy(
                                        status = next,
                                        completedAtMillis = if (next == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L
                                    ),
                                    if (next == ActivityStatus.COMPLETED) "Daily review completed!" else "Daily review unmarked"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (dailyCheckDone) EmeraldSuccess else AzurePrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("daily_check_complete_button")
                        ) {
                            Text(if (dailyCheckDone) "Daily Review Completed" else "Complete 4:00 PM Daily Review")
                        }
                    }
                }
            }

            // Weekly Statistics Dashboard
            item {
                SectionHeaderCard(
                    title = "Weekly Statistics",
                    subtitle = "Recorded completion data • Browse current & previous weeks",
                    icon = Icons.Default.Insights,
                    accent = VioletAccent
                )
            }

            item {
                val weekEnd = weeklyStats.weekStartDate.plusDays(6)
                val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { onShiftWeek(-1) },
                                modifier = Modifier.testTag("prev_week_button")
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
                            }
                            Text(
                                text = "${weeklyStats.weekStartDate.format(fmt)} – ${weekEnd.format(fmt)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { onShiftWeek(1) },
                                enabled = weekOffset < 0,
                                modifier = Modifier.testTag("next_week_button")
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
                            }
                        }

                        WeeklyBarChart(items = weeklyStats.dailyCompletionPercentages)

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatPill(
                                "Study Hours",
                                String.format(Locale.ENGLISH, "%.1fh / 4h", weeklyStats.totalStudyMinutes / 60.0),
                                AzurePrimary,
                                Modifier.weight(1f)
                            )
                            StatPill(
                                "Web Hours",
                                String.format(Locale.ENGLISH, "%.1fh / 3h", weeklyStats.totalWebMinutes / 60.0),
                                VioletAccent,
                                Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatPill("Workouts", "${weeklyStats.completedWorkouts} / 6", CoralAlert, Modifier.weight(1f))
                            StatPill("Quran Days", "${weeklyStats.quranDaysCompleted} / 7", SkyAccent, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatPill("YouTube Videos", "${weeklyStats.youtubeVideosUploaded}", AmberGold, Modifier.weight(1f))
                            StatPill(
                                "Cycling",
                                String.format(Locale.ENGLISH, "%.1f km", weeklyStats.cyclingDistanceKm),
                                EmeraldSuccess,
                                Modifier.weight(1f)
                            )
                        }
                        StatPill("Daily Reviews Completed", "${weeklyStats.dailyCheckCompletedDays} / 7", SkyAccent, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}
