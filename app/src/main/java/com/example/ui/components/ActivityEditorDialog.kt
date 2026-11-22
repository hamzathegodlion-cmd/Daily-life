package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.data.model.AlarmSoundOption
import com.example.data.model.RoutineActivity
import com.example.data.model.WeekdayMask
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorDialog(
    initialActivity: RoutineActivity?,
    onDismiss: () -> Unit,
    onSave: (RoutineActivity) -> Unit,
    onPreviewSound: (String) -> Unit,
    onPreviewVoice: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialActivity?.title ?: "") }
    var description by remember { mutableStateOf(initialActivity?.description ?: "") }
    var category by remember { mutableStateOf(initialActivity?.category ?: ActivityCategory.CUSTOM) }
    var startHour by remember { mutableIntStateOf(initialActivity?.startHour ?: 8) }
    var startMinute by remember { mutableIntStateOf(initialActivity?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialActivity?.endHour ?: 9) }
    var endMinute by remember { mutableIntStateOf(initialActivity?.endMinute ?: 0) }
    var weekdaysMask by remember { mutableIntStateOf(initialActivity?.weekdaysMask ?: WeekdayMask.ALL_DAYS) }
    var repeatWeekly by remember { mutableStateOf(initialActivity?.repeatWeekly ?: true) }
    var alarmEnabled by remember { mutableStateOf(initialActivity?.alarmEnabled ?: true) }
    var soundKey by remember { mutableStateOf(initialActivity?.soundKey ?: AlarmSoundOption.GENTLE_WAKEUP.key) }
    var announcementText by remember { mutableStateOf(initialActivity?.announcementText ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var soundExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialActivity == null) "Add Routine Activity" else "Edit Routine Activity",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Activity Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ActivityCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time pickers (Start & End)
                TimeStepperRow(
                    label = "Start Time",
                    hour = startHour,
                    minute = startMinute,
                    onChange = { h, m -> startHour = h; startMinute = m }
                )

                TimeStepperRow(
                    label = "End Time",
                    hour = endHour,
                    minute = endMinute,
                    onChange = { h, m -> endHour = h; endMinute = m }
                )

                Text("Weekdays", style = MaterialTheme.typography.labelLarge)
                val orderedDays = listOf(
                    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    orderedDays.forEach { day ->
                        val bit = WeekdayMask.bitFor(day)
                        val selected = (weekdaysMask and bit) != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                weekdaysMask = if (selected) weekdaysMask and bit.inv() else weekdaysMask or bit
                            },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Repeat Weekly", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = repeatWeekly, onCheckedChange = { repeatWeekly = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Alarm & Voice", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = soundExpanded,
                        onExpandedChange = { soundExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = AlarmSoundOption.fromKey(soundKey).displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alarm Sound") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = soundExpanded,
                            onDismissRequest = { soundExpanded = false }
                        ) {
                            AlarmSoundOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text("${opt.displayName} — ${opt.description}") },
                                    onClick = {
                                        soundKey = opt.key
                                        soundExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onPreviewSound(soundKey) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Preview sound")
                    }
                }

                OutlinedTextField(
                    value = announcementText,
                    onValueChange = { announcementText = it.take(600) },
                    label = { Text("Spoken Voice Announcement") },
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(onClick = {
                            onPreviewVoice(announcementText.ifBlank { "It is time for ${title.ifBlank { "your activity" }}." })
                        }) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Preview voice announcement")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saved = RoutineActivity(
                        id = initialActivity?.id ?: "",
                        title = title.trim().ifBlank { "Routine Activity" },
                        description = description.trim(),
                        category = category,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        weekdaysMask = if (weekdaysMask == 0) WeekdayMask.ALL_DAYS else weekdaysMask,
                        repeatWeekly = repeatWeekly,
                        alarmEnabled = alarmEnabled,
                        soundKey = soundKey,
                        announcementText = announcementText.trim().ifBlank {
                            "It is time for ${title.trim().ifBlank { "your activity" }}."
                        },
                        sortOrder = initialActivity?.sortOrder ?: 50,
                        defaultKey = initialActivity?.defaultKey ?: ""
                    )
                    onSave(saved)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("editor_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeStepperRow(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = RoutineActivity.formatTime(hour, minute, use24Hour = false),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onChange((hour + 23) % 24, minute) },
                modifier = Modifier.weight(1f)
            ) { Text("-1h") }
            OutlinedButton(
                onClick = { onChange((hour + 1) % 24, minute) },
                modifier = Modifier.weight(1f)
            ) { Text("+1h") }
            OutlinedButton(
                onClick = { onChange(hour, (minute + 50) % 60) },
                modifier = Modifier.weight(1f)
            ) { Text("-10m") }
            OutlinedButton(
                onClick = { onChange(hour, (minute + 10) % 60) },
                modifier = Modifier.weight(1f)
            ) { Text("+10m") }
        }
    }
}
