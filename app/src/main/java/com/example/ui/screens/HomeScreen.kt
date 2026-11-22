package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ActivityStatus
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.RoutineActivity
import com.example.ui.components.ActivityTimelineCard
import com.example.ui.components.AppSection
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DashboardUiState
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    state: DashboardUiState,
    onNavigate: (AppSection) -> Unit,
    onToggleComplete: (RoutineActivity) -> Unit,
    onEditActivity: (RoutineActivity) -> Unit,
    onPreviewVoice: (RoutineActivity) -> Unit,
    onOpenAllSections: () -> Unit
) {
    val now = state.now
    val use24 = state.settings.use24HourFormat
    val timeFormatter = DateTimeFormatter.ofPattern(if (use24) "HH:mm:ss" else "h:mm:ss a", Locale.ENGLISH)
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    val weekdayName = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
    val todayKey = now.toLocalDate().toString()

    val incompleteToday = state.todayActivities.filter {
        state.completionsByKey["${todayKey}__${it.id}"]?.status != ActivityStatus.COMPLETED
    }
    val completedToday = state.todayActivities.filter {
        state.completionsByKey["${todayKey}__${it.id}"]?.status == ActivityStatus.COMPLETED
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("home_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Digital Clock + Current Weekday + Sound Wave Card
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_routine_banner),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(NavyVoid.copy(alpha = 0.55f), NavyDeep.copy(alpha = 0.88f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = AzurePrimary.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "TODAY IS $weekdayName",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SkyAccent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                                OutlinedButton(
                                    onClick = onOpenAllSections,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("open_all_sections_button")
                                ) {
                                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Menu", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Text(
                                text = now.format(timeFormatter),
                                style = MaterialTheme.typography.displayLarge.copy(fontFamily = MonoFontFamily),
                                color = Color.White,
                                modifier = Modifier.testTag("dashboard_clock")
                            )

                            Text(
                                text = now.format(dateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )

                            SoundWaveVisualizer(
                                modifier = Modifier.fillMaxWidth(),
                                barColor = SkyAccent,
                                isActive = true
                            )
                        }
                    }
                }
            }

            // Current / Next Activity & Countdown Card
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CURRENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                Text(
                                    text = state.currentActivity?.title ?: "Transition / Free Window",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.currentActivity?.description ?: "Stay prepared for your next scheduled activity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.currentActivity != null) {
                                Button(
                                    onClick = { onPreviewVoice(state.currentActivity) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("home_speak_current")
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Announce current activity")
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = SkyAccent)
                                    Column {
                                        Text(
                                            text = "Next: ${state.nextActivity?.title ?: "None"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Next alarm: ${state.nextActivity?.formatStartTime(use24) ?: "--"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = formatCountdown(state.countdownToNext),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AmberGold,
                                    modifier = Modifier.testTag("home_countdown")
                                )
                            }
                        }
                    }
                }
            }

            // Workout Group + Daily Progress Row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(AppSection.WORKOUT) }
                            .testTag("home_workout_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = SkyAccent)
                            Text("WORKOUT", style = MaterialTheme.typography.labelSmall, color = SkyAccent)
                            Text(
                                text = state.todayWorkoutGroup.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.todayWorkoutGroup.focus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(AppSection.PROGRESS) }
                            .testTag("home_progress_card")
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
                                Text("PROGRESS", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${state.todayStats.completionPercent}%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { state.todayStats.completionPercent / 100f },
                                color = EmeraldSuccess,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Text(
                                text = "${state.todayStats.completedCount} of ${state.todayStats.totalScheduled} done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Today's Study / Web Quick Summary + Quick Access Pills
            item {
                val studyMin = DefaultTimetableFactory.studyMinutesForDay(now.dayOfWeek)
                val webMin = DefaultTimetableFactory.webMinutesForDay(now.dayOfWeek)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickAccessChip(
                            icon = Icons.Default.CalendarMonth,
                            label = "Weekly Calendar",
                            onClick = { onNavigate(AppSection.WEEKLY_CALENDAR) }
                        )
                    }
                    item {
                        QuickAccessChip(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = "Quran (${state.settings.quranWirdJuz} Juz)",
                            onClick = { onNavigate(AppSection.QURAN) }
                        )
                    }
                    item {
                        QuickAccessChip(
                            icon = Icons.Default.School,
                            label = "Study: ${studyMin}m today",
                            onClick = { onNavigate(AppSection.STUDYING) }
                        )
                    }
                    item {
                        QuickAccessChip(
                            icon = Icons.Default.Schedule,
                            label = "Web: ${webMin}m today",
                            onClick = { onNavigate(AppSection.WEB_DEV) }
                        )
                    }
                    item {
                        QuickAccessChip(
                            icon = Icons.Default.PlayCircle,
                            label = "YouTube (${state.settings.youtubeDailyVideos})",
                            onClick = { onNavigate(AppSection.YOUTUBE) }
                        )
                    }
                }
            }

            // Incomplete Today Section
            item {
                Text(
                    text = "Today's Remaining Activities (${incompleteToday.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (incompleteToday.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.14f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "All scheduled activities for today are completed. Great consistency!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(incompleteToday, key = { "inc_${it.id}" }) { activity ->
                    val status = state.completionsByKey["${todayKey}__${activity.id}"]?.status ?: ActivityStatus.INCOMPLETE
                    ActivityTimelineCard(
                        activity = activity,
                        status = status,
                        use24Hour = use24,
                        isCurrent = state.currentActivity?.id == activity.id,
                        onToggleComplete = { onToggleComplete(activity) },
                        onEdit = { onEditActivity(activity) },
                        onPreviewVoice = { onPreviewVoice(activity) }
                    )
                }
            }

            // Completed Today Section
            if (completedToday.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Today (${completedToday.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldSuccess
                    )
                }
                items(completedToday, key = { "done_${it.id}" }) { activity ->
                    ActivityTimelineCard(
                        activity = activity,
                        status = ActivityStatus.COMPLETED,
                        use24Hour = use24,
                        isCurrent = false,
                        onToggleComplete = { onToggleComplete(activity) },
                        onEdit = { onEditActivity(activity) },
                        onPreviewVoice = { onPreviewVoice(activity) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = SkyAccent, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun formatCountdown(duration: Duration): String {
    val secs = duration.seconds.coerceAtLeast(0)
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s)
}
