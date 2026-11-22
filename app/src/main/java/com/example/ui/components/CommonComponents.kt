package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.AlarmSoundOption
import com.example.data.model.RoutineActivity
import com.example.data.model.WeekdayMask
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.VioletAccent
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

enum class AppSection(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Home),
    DAILY_ROUTINE("daily_routine", "Daily Routine", Icons.Default.Schedule),
    WEEKLY_CALENDAR("weekly_calendar", "Weekly Calendar", Icons.Default.CalendarMonth),
    WORKOUT("workout", "Workout", Icons.Default.FitnessCenter),
    QURAN("quran", "Quran", Icons.AutoMirrored.Filled.MenuBook),
    STUDYING("studying", "Studying", Icons.Default.School),
    WEB_DEV("web_dev", "Web Dev", Icons.Default.Code),
    YOUTUBE("youtube", "YouTube", Icons.Default.PlayCircle),
    PROGRESS("progress", "Progress", Icons.Default.Insights),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

fun categoryIcon(category: ActivityCategory): ImageVector = when (category) {
    ActivityCategory.WAKE_UP -> Icons.Default.WbSunny
    ActivityCategory.MOSQUE -> Icons.Default.Mosque
    ActivityCategory.QURAN -> Icons.AutoMirrored.Filled.MenuBook
    ActivityCategory.SPORT -> Icons.Default.FitnessCenter
    ActivityCategory.LEARNING -> Icons.Default.School
    ActivityCategory.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    ActivityCategory.KITAB -> Icons.Default.Book
    ActivityCategory.YOUTUBE -> Icons.Default.PlayCircle
    ActivityCategory.STUDY -> Icons.Default.School
    ActivityCategory.WEB_DEV -> Icons.Default.Code
    ActivityCategory.DAILY_CHECK -> Icons.AutoMirrored.Filled.FactCheck
    ActivityCategory.SLEEP -> Icons.Default.Bedtime
    ActivityCategory.CUSTOM -> Icons.Default.Schedule
}

fun categoryAccent(category: ActivityCategory): Color = when (category) {
    ActivityCategory.WAKE_UP -> AmberGold
    ActivityCategory.MOSQUE, ActivityCategory.KITAB -> EmeraldSuccess
    ActivityCategory.QURAN -> SkyAccent
    ActivityCategory.SPORT -> CoralAlert
    ActivityCategory.LEARNING, ActivityCategory.STUDY -> AzurePrimary
    ActivityCategory.CYCLING -> EmeraldSuccess
    ActivityCategory.YOUTUBE -> CoralAlert
    ActivityCategory.WEB_DEV -> VioletAccent
    ActivityCategory.DAILY_CHECK -> SkyAccent
    ActivityCategory.SLEEP -> VioletAccent
    ActivityCategory.CUSTOM -> AzurePrimary
}

@Composable
fun SoundWaveVisualizer(
    modifier: Modifier = Modifier,
    barColor: Color = SkyAccent,
    isActive: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "sound_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier.height(28.dp)) {
        val barCount = 18
        val spacing = size.width / (barCount * 1.8f)
        val barWidth = spacing * 0.9f
        for (i in 0 until barCount) {
            val factor = if (isActive) {
                (0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(phase + i * 0.48f))).coerceIn(0.2f, 1f)
            } else {
                0.22f
            }
            val barH = size.height * factor
            val x = i * (barWidth + spacing)
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = barColor.copy(alpha = 0.55f + 0.45f * factor),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun ActivityTimelineCard(
    activity: RoutineActivity,
    status: ActivityStatus,
    use24Hour: Boolean,
    isCurrent: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onPreviewVoice: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = categoryAccent(activity.category)
    val isDone = status == ActivityStatus.COMPLETED
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("activity_card_${activity.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.16f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIcon(activity.category),
                                contentDescription = activity.category.label,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = activity.formatTimeRange(use24Hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyAccent
                            )
                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = EmeraldSuccess.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ACTIVE NOW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_complete_${activity.id}")
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isDone) "Completed" else "Mark completed",
                        tint = if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (activity.alarmEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                                contentDescription = null,
                                tint = if (activity.alarmEnabled) AzurePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activity.alarmEnabled) AlarmSoundOption.fromKey(activity.soundKey).displayName else "Muted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = WeekdayMask.formatDays(activity.weekdaysMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviewVoice,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Preview spoken announcement",
                            tint = SkyAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_activity_${activity.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit activity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_activity_${activity.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete activity",
                                tint = CoralAlert,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    items: List<Pair<DayOfWeek, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = AzurePrimary
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { (day, pct) ->
                val clamped = pct.coerceIn(0, 100)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$clamped%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (clamped >= 75) EmeraldSuccess else textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(92.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(trackColor),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((92f * (clamped / 100f)).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (clamped >= 80) EmeraldSuccess else barColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeaderCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color = SkyAccent,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}
