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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityStatus
import com.example.data.model.CompletionRecord
import com.example.ui.components.SectionHeaderCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.WeeklySummaryStats
import java.util.Locale

@Composable
fun YouTubeScreen(
    state: DashboardUiState,
    weeklyStats: WeeklySummaryStats,
    onSaveRecord: (CompletionRecord, String) -> Unit,
    onPreviewVoice: (String) -> Unit
) {
    val todayKey = state.now.toLocalDate().toString()
    val ytRec = state.completionsByKey["${todayKey}__youtube"]
        ?: CompletionRecord(id = "${todayKey}__youtube", dateKey = todayKey, activityId = "youtube")
    val cyclingRec = state.completionsByKey["${todayKey}__cycling"]
        ?: CompletionRecord(
            id = "${todayKey}__cycling",
            dateKey = todayKey,
            activityId = "cycling",
            cyclingDistanceKm = state.settings.cyclingTargetKm
        )
    val sleepRec = state.completionsByKey["${todayKey}__sleep"]
        ?: CompletionRecord(
            id = "${todayKey}__sleep",
            dateKey = todayKey,
            activityId = "sleep",
            sleepDurationMin = 360
        )

    var ytNotes by remember(ytRec.notes) { mutableStateOf(ytRec.notes) }
    var cyclingKmText by remember(cyclingRec.cyclingDistanceKm) {
        mutableStateOf(
            if (cyclingRec.cyclingDistanceKm > 0.0) cyclingRec.cyclingDistanceKm.toString()
            else state.settings.cyclingTargetKm.toString()
        )
    }
    var cyclingDurationText by remember(cyclingRec.cyclingDurationMin) {
        mutableStateOf(if (cyclingRec.cyclingDurationMin > 0) cyclingRec.cyclingDurationMin.toString() else "")
    }
    var sleepHoursText by remember(sleepRec.sleepDurationMin) {
        mutableStateOf(String.format(Locale.ENGLISH, "%.1f", (if (sleepRec.sleepDurationMin > 0) sleepRec.sleepDurationMin else 360) / 60.0))
    }

    val ytChecklistCount = listOf(ytRec.v1Edited, ytRec.v1Uploaded, ytRec.v2Edited, ytRec.v2Uploaded).count { it }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. YouTube Productivity Section
            item {
                SectionHeaderCard(
                    title = "YouTube Productivity",
                    subtitle = "2:30 PM – 3:00 PM • Edit & Upload ${state.settings.youtubeDailyVideos} Videos Daily",
                    icon = Icons.Default.PlayCircle,
                    accent = CoralAlert,
                    trailing = {
                        IconButton(onClick = {
                            onPreviewVoice("YouTube time! Start editing and uploading your two videos.")
                        }) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Announce YouTube time", tint = SkyAccent)
                        }
                    }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Daily Progress: $ytChecklistCount / 4 steps", style = MaterialTheme.typography.titleMedium, color = SkyAccent)
                        LinearProgressIndicator(
                            progress = { ytChecklistCount / 4f },
                            color = CoralAlert,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        ChecklistRow("Video 1 — Editing completed", ytRec.v1Edited) { checked ->
                            onSaveRecord(ytRec.copy(v1Edited = checked), "Updated Video 1 edit")
                        }
                        ChecklistRow("Video 1 — Uploaded to YouTube", ytRec.v1Uploaded) { checked ->
                            onSaveRecord(ytRec.copy(v1Uploaded = checked), "Updated Video 1 upload")
                        }
                        ChecklistRow("Video 2 — Editing completed", ytRec.v2Edited) { checked ->
                            onSaveRecord(ytRec.copy(v2Edited = checked), "Updated Video 2 edit")
                        }
                        ChecklistRow("Video 2 — Uploaded to YouTube", ytRec.v2Uploaded) { checked ->
                            onSaveRecord(ytRec.copy(v2Uploaded = checked), "Updated Video 2 upload")
                        }

                        OutlinedTextField(
                            value = ytNotes,
                            onValueChange = { ytNotes = it.take(1000) },
                            label = { Text("Video Topics & Notes") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val ytDone = ytRec.status == ActivityStatus.COMPLETED
                        Button(
                            onClick = {
                                val next = if (ytDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                onSaveRecord(
                                    ytRec.copy(
                                        status = next,
                                        notes = ytNotes,
                                        completedAtMillis = if (next == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L
                                    ),
                                    if (next == ActivityStatus.COMPLETED) "YouTube task completed!" else "Marked incomplete"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (ytDone) EmeraldSuccess else CoralAlert),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("youtube_complete_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (ytDone) "Daily YouTube Goal Completed" else "Mark YouTube Completed")
                        }

                        Text(
                            text = "Weekly uploaded videos: ${weeklyStats.youtubeVideosUploaded}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    }
                }
            }

            // 2. Cycling Tracker Section (11:30 AM, ~22 km round trip)
            item {
                SectionHeaderCard(
                    title = "Cycling Journey",
                    subtitle = "Starts 11:30 AM • ~${state.settings.cyclingTargetKm} km round trip (schedule & distance editable separately)",
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    accent = EmeraldSuccess
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = cyclingKmText,
                                onValueChange = { cyclingKmText = it },
                                label = { Text("Distance (km)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cyclingDurationText,
                                onValueChange = { cyclingDurationText = it },
                                label = { Text("Duration (min, optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val cycDone = cyclingRec.status == ActivityStatus.COMPLETED
                        Button(
                            onClick = {
                                val dist = cyclingKmText.toDoubleOrNull()?.coerceIn(0.0, 500.0) ?: state.settings.cyclingTargetKm
                                val dur = cyclingDurationText.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                                val next = if (cycDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                onSaveRecord(
                                    cyclingRec.copy(
                                        status = next,
                                        cyclingDistanceKm = dist,
                                        cyclingDurationMin = dur,
                                        completedAtMillis = if (next == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L
                                    ),
                                    "Cycling journey saved (${dist} km)"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (cycDone) EmeraldSuccess else AzurePrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(if (cycDone) "Cycling Logged (${cyclingRec.cyclingDistanceKm} km)" else "Log & Complete Cycling")
                        }
                        Text(
                            text = "Weekly recorded distance: ${String.format(Locale.ENGLISH, "%.1f", weeklyStats.cyclingDistanceKm)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyAccent
                        )
                    }
                }
            }

            // 3. Sleep Management Section (4:30 PM - 10:30 PM, 6 hours fixed)
            item {
                SectionHeaderCard(
                    title = "Sleep Schedule & Tracker",
                    subtitle = "4:30 PM – 10:30 PM • Fixed 6-hour rest period",
                    icon = Icons.Default.Bedtime,
                    accent = VioletAccent
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberGold.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AmberGold)
                                Text(
                                    text = "Health Note: 6 hours is below the general recommended sleep duration (8–10 hours) for teenagers. Your fixed schedule remains unchanged.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = sleepHoursText,
                                onValueChange = { sleepHoursText = it },
                                label = { Text("Actual Sleep (hours)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    val mins = ((sleepHoursText.toDoubleOrNull() ?: 6.0) * 60.0).toInt().coerceIn(0, 1440)
                                    onSaveRecord(
                                        sleepRec.copy(
                                            status = ActivityStatus.COMPLETED,
                                            sleepDurationMin = mins,
                                            completedAtMillis = System.currentTimeMillis()
                                        ),
                                        "Sleep duration logged"
                                    )
                                },
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Log Sleep")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(48.dp)
        )
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
