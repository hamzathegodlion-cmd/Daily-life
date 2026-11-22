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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityStatus
import com.example.data.model.CompletionRecord
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutGroupType
import com.example.data.model.WorkoutPlan
import com.example.ui.components.SectionHeaderCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.WeeklySummaryStats
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WorkoutScreen(
    state: DashboardUiState,
    weeklyStats: WeeklySummaryStats,
    onUpdateExercise: (WorkoutPlan, WorkoutExercise) -> Unit,
    onSaveWorkoutProgress: (CompletionRecord, String) -> Unit,
    onPreviewWorkoutVoice: (String) -> Unit
) {
    val today = state.now.toLocalDate()
    val todayKey = today.toString()
    val dayOfWeek = today.dayOfWeek
    val dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val groupType = DefaultTimetableFactory.workoutGroupForDay(dayOfWeek)

    val activePlan = when (groupType) {
        WorkoutGroupType.GROUP_A -> state.workoutPlans.firstOrNull { it.id == "group_a" }
        WorkoutGroupType.GROUP_B -> state.workoutPlans.firstOrNull { it.id == "group_b" }
        WorkoutGroupType.REST_DAY -> state.workoutPlans.firstOrNull { it.id == "sunday_rest" }
    } ?: DefaultTimetableFactory.defaultWorkoutPlans(state.settings.pushupGoal).first()

    val sportActivity = state.activities.firstOrNull { it.id == "sport" }
    val existingRecord = state.completionsByKey["${todayKey}__sport"]
    val isWorkoutDone = existingRecord?.status == ActivityStatus.COMPLETED

    val checkedMap = remember(todayKey) { mutableStateMapOf<String, Boolean>() }

    // Exercise & Rest Countdown Timer
    var timerSeconds by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerMode by remember { mutableStateOf("Exercise") }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
            if (timerSeconds == 0) {
                isTimerRunning = false
            }
        }
    }

    val dynamicAnnouncement = if (sportActivity != null) {
        DefaultTimetableFactory.buildDynamicAnnouncement(sportActivity, dayOfWeek)
    } else {
        "Today is $dayName. ${groupType.title}: ${groupType.focus}."
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
                    title = "$dayName • ${groupType.title}",
                    subtitle = "${groupType.focus} • 12:50 AM – 1:30 AM",
                    icon = if (groupType == WorkoutGroupType.REST_DAY) Icons.Default.SelfImprovement else Icons.Default.FitnessCenter,
                    accent = if (groupType == WorkoutGroupType.REST_DAY) EmeraldSuccess else CoralAlert,
                    trailing = {
                        IconButton(
                            onClick = { onPreviewWorkoutVoice(dynamicAnnouncement) },
                            modifier = Modifier.testTag("workout_voice_preview")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Announce today's workout", tint = SkyAccent)
                        }
                    }
                )
            }

            // Safety & Age-Appropriate Modification Guidance
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmberGold.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AmberGold)
                        Text(
                            text = "Personal Goal & Safety: Repetitions (including the 100 push-up notebook target) are adjustable personal goals. Take rest breaks, modify exercises for safe form, and never exercise through pain, exhaustion, or injury.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Exercise & Rest Timer Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$timerMode Timer", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = String.format(Locale.ENGLISH, "%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = MonoFontFamily),
                                color = SkyAccent
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    isTimerRunning = false
                                    timerMode = "Exercise"
                                    timerSeconds = 60
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("60s Work") }
                            OutlinedButton(
                                onClick = {
                                    isTimerRunning = false
                                    timerMode = "Rest"
                                    timerSeconds = 45
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("45s Rest") }
                            Button(
                                onClick = { isTimerRunning = !isTimerRunning },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isTimerRunning) "Pause timer" else "Start timer"
                                )
                            }
                            IconButton(onClick = { isTimerRunning = false; timerSeconds = 60 }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                            }
                        }
                    }
                }
            }

            if (activePlan.restDay) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.14f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Sunday Recovery Day", style = MaterialTheme.typography.titleLarge, color = EmeraldSuccess)
                            Text(
                                text = "Today is Sunday. It is your rest day. Allow your body to recover, hydrate, stretch lightly, and prepare for the new week.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                val first20 = activePlan.exercises.filter { it.phase == "FIRST_20" }
                val next20 = activePlan.exercises.filter { it.phase == "NEXT_20" }

                item {
                    Text("First 20 Minutes • Core & Conditioning", style = MaterialTheme.typography.titleMedium, color = SkyAccent)
                }
                items(first20, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        checked = checkedMap[exercise.id] ?: false,
                        onCheckedChange = { checkedMap[exercise.id] = it },
                        onUpdateExercise = { onUpdateExercise(activePlan, it) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Next 20 Minutes • ${activePlan.focusArea}", style = MaterialTheme.typography.titleMedium, color = CoralAlert)
                }
                items(next20, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        checked = checkedMap[exercise.id] ?: false,
                        onCheckedChange = { checkedMap[exercise.id] = it },
                        onUpdateExercise = { onUpdateExercise(activePlan, it) }
                    )
                }
            }

            // Complete Workout Button + Weekly Progress
            item {
                val completedExercises = checkedMap.values.count { it }
                Button(
                    onClick = {
                        val nextStatus = if (isWorkoutDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                        val rec = (existingRecord ?: CompletionRecord(
                            id = "${todayKey}__sport",
                            dateKey = todayKey,
                            activityId = "sport"
                        )).copy(
                            status = nextStatus,
                            completedAtMillis = if (nextStatus == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L,
                            workoutCompletedCount = completedExercises
                        )
                        onSaveWorkoutProgress(rec, if (nextStatus == ActivityStatus.COMPLETED) "Workout marked completed!" else "Workout marked incomplete")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWorkoutDone) EmeraldSuccess else AzurePrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("workout_complete_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isWorkoutDone) "Today's Workout Completed" else "Complete Today's Session")
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Weekly Workout Schedule & Progress", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Completed this week: ${weeklyStats.completedWorkouts} / 6 training days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldSuccess
                        )
                        LinearProgressIndicator(
                            progress = { (weeklyStats.completedWorkouts / 6f).coerceIn(0f, 1f) },
                            color = EmeraldSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Text(
                            text = "Mon/Wed/Fri: Group A (Biceps & Back) • Tue/Thu/Sat: Group B (Triceps & Chest) • Sun: Rest Day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onUpdateExercise: (WorkoutExercise) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(48.dp)
                    )
                    Column {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${exercise.targetSets} sets × ${exercise.targetReps} reps • ~${exercise.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyAccent
                        )
                    }
                }
            }
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reps:", style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = {
                        onUpdateExercise(exercise.copy(targetReps = (exercise.targetReps - 5).coerceAtLeast(1)))
                    }
                ) { Icon(Icons.Default.Remove, contentDescription = "Decrease reps") }
                Text("${exercise.targetReps}", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = {
                        onUpdateExercise(exercise.copy(targetReps = (exercise.targetReps + 5).coerceAtMost(500)))
                    }
                ) { Icon(Icons.Default.Add, contentDescription = "Increase reps") }

                Spacer(modifier = Modifier.width(8.dp))
                Text("Sets:", style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = {
                        onUpdateExercise(exercise.copy(targetSets = (exercise.targetSets - 1).coerceAtLeast(1)))
                    }
                ) { Icon(Icons.Default.Remove, contentDescription = "Decrease sets") }
                Text("${exercise.targetSets}", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = {
                        onUpdateExercise(exercise.copy(targetSets = (exercise.targetSets + 1).coerceAtMost(12)))
                    }
                ) { Icon(Icons.Default.Add, contentDescription = "Increase sets") }
            }
        }
    }
}
