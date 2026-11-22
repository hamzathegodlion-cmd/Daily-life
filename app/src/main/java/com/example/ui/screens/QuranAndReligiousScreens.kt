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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.AssistantSettings
import com.example.data.model.CompletionRecord
import com.example.ui.components.SectionHeaderCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.WeeklySummaryStats

@Composable
fun QuranScreen(
    state: DashboardUiState,
    weeklyStats: WeeklySummaryStats,
    onSaveRecord: (CompletionRecord, String) -> Unit,
    onUpdateSettings: (AssistantSettings) -> Unit,
    onPreviewVoice: (String) -> Unit
) {
    val todayKey = state.now.toLocalDate().toString()
    val settings = state.settings
    val quranRecord = state.completionsByKey["${todayKey}__quran"]
        ?: CompletionRecord(id = "${todayKey}__quran", dateKey = todayKey, activityId = "quran")

    val fajrRecord = state.completionsByKey["${todayKey}__mosque_fajr"]
        ?: CompletionRecord(id = "${todayKey}__mosque_fajr", dateKey = todayKey, activityId = "mosque_fajr")
    val kitabRecord = state.completionsByKey["${todayKey}__mosque_kitab"]
        ?: CompletionRecord(id = "${todayKey}__mosque_kitab", dateKey = todayKey, activityId = "mosque_kitab")

    var quranNotes by remember(quranRecord.notes) { mutableStateOf(quranRecord.notes) }
    var religiousNotes by remember(kitabRecord.notes) { mutableStateOf(kitabRecord.notes) }

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
                    title = "Quran Management",
                    subtitle = "12:00 AM – 12:50 AM • Daily Wird & Muraja'a",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = SkyAccent,
                    trailing = {
                        IconButton(
                            onClick = {
                                onPreviewVoice("It is Quran time. Begin your daily Wird and Muraja'a.")
                            },
                            modifier = Modifier.testTag("quran_voice_preview")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Announce Quran time", tint = SkyAccent)
                        }
                    }
                )
            }

            // Wird & Muraja'a Progress + Editable Targets
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Today's Reading Progress", style = MaterialTheme.typography.titleLarge)

                        // Wird Progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Wird (Juz)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Goal: ${settings.quranWirdJuz} Juz • Completed: ${quranRecord.quranWirdDone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SkyAccent
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val next = (quranRecord.quranWirdDone - 1).coerceAtLeast(0)
                                    onSaveRecord(quranRecord.copy(quranWirdDone = next), "Updated Wird progress")
                                }) { Icon(Icons.Default.Remove, contentDescription = "Decrease Wird") }
                                Text("${quranRecord.quranWirdDone}", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    val next = (quranRecord.quranWirdDone + 1).coerceAtMost(30)
                                    onSaveRecord(quranRecord.copy(quranWirdDone = next), "Updated Wird progress")
                                }) { Icon(Icons.Default.Add, contentDescription = "Increase Wird") }
                            }
                        }

                        // Muraja'a Progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Muraja'a (Safha / Pages)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Goal: ${settings.quranMurajaahPages} Safha • Completed: ${quranRecord.quranMurajaahDone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldSuccess
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val next = (quranRecord.quranMurajaahDone - 1).coerceAtLeast(0)
                                    onSaveRecord(quranRecord.copy(quranMurajaahDone = next), "Updated Muraja'a progress")
                                }) { Icon(Icons.Default.Remove, contentDescription = "Decrease Muraja'a") }
                                Text("${quranRecord.quranMurajaahDone}", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    val next = (quranRecord.quranMurajaahDone + 1).coerceAtMost(604)
                                    onSaveRecord(quranRecord.copy(quranMurajaahDone = next), "Updated Muraja'a progress")
                                }) { Icon(Icons.Default.Add, contentDescription = "Increase Muraja'a") }
                            }
                        }

                        // Editable Targets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateSettings(settings.copy(quranWirdJuz = (settings.quranWirdJuz % 10) + 1))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Target Wird: ${settings.quranWirdJuz} Juz")
                            }
                            OutlinedButton(
                                onClick = {
                                    val nextPages = if (settings.quranMurajaahPages >= 20) 5 else settings.quranMurajaahPages + 1
                                    onUpdateSettings(settings.copy(quranMurajaahPages = nextPages))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Target Safha: ${settings.quranMurajaahPages}")
                            }
                        }

                        OutlinedTextField(
                            value = quranNotes,
                            onValueChange = { quranNotes = it.take(1000) },
                            label = { Text("Personal Quran Notes / Reflection") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val isDone = quranRecord.status == ActivityStatus.COMPLETED
                        Button(
                            onClick = {
                                val nextStatus = if (isDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                onSaveRecord(
                                    quranRecord.copy(
                                        status = nextStatus,
                                        notes = quranNotes,
                                        completedAtMillis = if (nextStatus == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L
                                    ),
                                    if (nextStatus == ActivityStatus.COMPLETED) "Quran reading marked completed" else "Quran marked incomplete"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDone) EmeraldSuccess else AzurePrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("quran_complete_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isDone) "Completed Today" else "Mark Quran Completed")
                        }
                    }
                }
            }

            // Weekly Quran Consistency
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
                        Text("Weekly Quran Consistency", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${weeklyStats.quranDaysCompleted} / 7 days completed this week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyAccent
                        )
                        LinearProgressIndicator(
                            progress = { (weeklyStats.quranDaysCompleted / 7f).coerceIn(0f, 1f) },
                            color = SkyAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }

            // Mosque & Kitab Religious Activities Section
            item {
                SectionHeaderCard(
                    title = "Mosque & Kitab Activities",
                    subtitle = "10:30 PM – 12:00 AM Fajr Salah • 12:00 PM – 2:30 PM Kitab",
                    icon = Icons.Default.Mosque,
                    accent = EmeraldSuccess
                )
            }

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
                        val fajrDone = fajrRecord.status == ActivityStatus.COMPLETED
                        val kitabDone = kitabRecord.status == ActivityStatus.COMPLETED

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("10:30 PM – 12:00 AM • Mosque / Fajr Salah", style = MaterialTheme.typography.titleMedium)
                                Text("Go to the mosque and pray Fajr Salah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    val next = if (fajrDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                    onSaveRecord(
                                        fajrRecord.copy(
                                            status = next,
                                            completedAtMillis = if (next == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L
                                        ),
                                        "Updated Fajr Salah status"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (fajrDone) EmeraldSuccess else AzurePrimary)
                            ) {
                                Text(if (fajrDone) "Done" else "Complete")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("12:00 PM – 2:30 PM • Mosque / Kitab", style = MaterialTheme.typography.titleMedium)
                                Text("Mosque activities and reading Kitab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    val next = if (kitabDone) ActivityStatus.INCOMPLETE else ActivityStatus.COMPLETED
                                    onSaveRecord(
                                        kitabRecord.copy(
                                            status = next,
                                            notes = religiousNotes,
                                            completedAtMillis = if (next == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L
                                        ),
                                        "Updated Mosque / Kitab status"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (kitabDone) EmeraldSuccess else AzurePrimary)
                            ) {
                                Text(if (kitabDone) "Done" else "Complete")
                            }
                        }

                        OutlinedTextField(
                            value = religiousNotes,
                            onValueChange = { religiousNotes = it.take(1000) },
                            label = { Text("Kitab & Mosque Notes") },
                            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = AmberGold) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                onSaveRecord(kitabRecord.copy(notes = religiousNotes), "Saved religious notes")
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Notes")
                        }
                    }
                }
            }
        }
    }
}
