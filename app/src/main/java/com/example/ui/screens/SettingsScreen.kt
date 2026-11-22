package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.alarm.EnglishVoiceInfo
import com.example.data.model.AlarmSoundOption
import com.example.data.model.AssistantSettings
import com.example.data.model.ThemeMode
import com.example.ui.components.SectionHeaderCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyAccent
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AssistantSettings,
    availableVoices: List<EnglishVoiceInfo>,
    canScheduleExactAlarms: Boolean,
    currentUser: FirebaseUser?,
    syncError: String?,
    onUpdateSettings: (AssistantSettings) -> Unit,
    onPreviewVoice: (String, AssistantSettings) -> Unit,
    onPreviewSound: (String, Float) -> Unit,
    onRestoreDefaults: () -> Unit,
    onClearHistory: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPreviewSound by remember { mutableStateOf(AlarmSoundOption.GENTLE_WAKEUP.key) }
    var soundMenuExpanded by remember { mutableStateOf(false) }
    var voiceMenuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }

    val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = onExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                backupMessage = "Backup exported successfully"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportJson(text)
                    backupMessage = "Backup imported"
                }
            }
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
                    title = "Settings & Assistant Controls",
                    subtitle = "My Daily Life Assistant • Language: English (Default)",
                    icon = Icons.Default.Settings,
                    accent = SkyAccent
                )
            }

            // 1. Voice Announcement Settings
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Voice Announcement System", style = MaterialTheme.typography.titleLarge)

                        SettingSwitch("Enable Spoken Announcements", settings.ttsEnabled) {
                            onUpdateSettings(settings.copy(ttsEnabled = it))
                        }

                        // Voice Selection
                        ExposedDropdownMenuBox(
                            expanded = voiceMenuExpanded,
                            onExpandedChange = { voiceMenuExpanded = it }
                        ) {
                            val currentLabel = availableVoices.firstOrNull { it.name == settings.ttsVoiceName }?.label
                                ?: "Default English System Voice"
                            OutlinedTextField(
                                value = currentLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("English Voice Selection") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceMenuExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = voiceMenuExpanded,
                                onDismissRequest = { voiceMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default English System Voice") },
                                    onClick = {
                                        onUpdateSettings(settings.copy(ttsVoiceName = ""))
                                        voiceMenuExpanded = false
                                    }
                                )
                                availableVoices.take(15).forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice.label) },
                                        onClick = {
                                            onUpdateSettings(settings.copy(ttsVoiceName = voice.name))
                                            voiceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Speech Speed: ${String.format(Locale.ENGLISH, "%.2fx", settings.ttsSpeed)}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.ttsSpeed,
                            onValueChange = { onUpdateSettings(settings.copy(ttsSpeed = it)) },
                            valueRange = 0.5f..2.0f
                        )

                        Text("Speech Pitch: ${String.format(Locale.ENGLISH, "%.2fx", settings.ttsPitch)}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.ttsPitch,
                            onValueChange = { onUpdateSettings(settings.copy(ttsPitch = it)) },
                            valueRange = 0.5f..2.0f
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val nextRep = if (settings.ttsRepetitions >= 5) 1 else settings.ttsRepetitions + 1
                                    onUpdateSettings(settings.copy(ttsRepetitions = nextRep))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Repetitions: ${settings.ttsRepetitions}x") }

                            OutlinedButton(
                                onClick = {
                                    val nextDelay = if (settings.ttsDelaySeconds >= 10) 2 else settings.ttsDelaySeconds + 2
                                    onUpdateSettings(settings.copy(ttsDelaySeconds = nextDelay))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Delay: ${settings.ttsDelaySeconds}s") }
                        }

                        Button(
                            onClick = {
                                onPreviewVoice(
                                    "Wake up! Wake up! Wake up! This is My Daily Life Assistant speaking clearly in English.",
                                    settings
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("settings_preview_voice")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preview Voice Announcement")
                        }
                    }
                }
            }

            // 2. Alarm & Sound Management
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Alarm Sound & Behavior", style = MaterialTheme.typography.titleLarge)

                        SettingSwitch("Enable Alarm Sound", settings.soundEnabled) {
                            onUpdateSettings(settings.copy(soundEnabled = it))
                        }
                        SettingSwitch("Enable Vibration", settings.vibrationEnabled) {
                            onUpdateSettings(settings.copy(vibrationEnabled = it))
                        }

                        Text("Alarm Volume: ${(settings.alarmVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.alarmVolume,
                            onValueChange = { onUpdateSettings(settings.copy(alarmVolume = it)) },
                            valueRange = 0.1f..1.0f
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val nextSnooze = listOf(5, 10, 15, 20).let { opts ->
                                        opts[(opts.indexOf(settings.snoozeMinutes) + 1) % opts.size]
                                    }
                                    onUpdateSettings(settings.copy(snoozeMinutes = nextSnooze))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Snooze: ${settings.snoozeMinutes}m") }

                            OutlinedButton(
                                onClick = {
                                    val nextDur = listOf(10, 20, 30, 45).let { opts ->
                                        opts[(opts.indexOf(settings.soundDurationSeconds) + 1) % opts.size]
                                    }
                                    onUpdateSettings(settings.copy(soundDurationSeconds = nextDur))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Duration: ${settings.soundDurationSeconds}s") }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = soundMenuExpanded,
                                onExpandedChange = { soundMenuExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = AlarmSoundOption.fromKey(selectedPreviewSound).displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sound Library (9 Built-in Tones)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundMenuExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = soundMenuExpanded,
                                    onDismissRequest = { soundMenuExpanded = false }
                                ) {
                                    AlarmSoundOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text("${opt.displayName} — ${opt.description}") },
                                            onClick = {
                                                selectedPreviewSound = opt.key
                                                soundMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { onPreviewSound(selectedPreviewSound, settings.alarmVolume) },
                                modifier = Modifier.testTag("settings_preview_sound")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Preview sound")
                            }
                        }
                    }
                }
            }

            // 3. Appearance & Time Format
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Display & Theme", style = MaterialTheme.typography.titleLarge)

                        ExposedDropdownMenuBox(
                            expanded = themeMenuExpanded,
                            onExpandedChange = { themeMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = settings.themeMode.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Theme Selection") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeMenuExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = themeMenuExpanded,
                                onDismissRequest = { themeMenuExpanded = false }
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        onClick = {
                                            onUpdateSettings(settings.copy(themeMode = mode))
                                            themeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        SettingSwitch("Use 24-Hour Format (Default: 12-Hour AM/PM)", settings.use24HourFormat) {
                            onUpdateSettings(settings.copy(use24HourFormat = it))
                        }
                    }
                }
            }

            // 4. Android Permissions & Battery Guidance
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Permissions & Reliable Alarm Delivery", style = MaterialTheme.typography.titleLarge)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notifications", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (notificationGranted) "Granted" else "Permission needed for lock-screen alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (notificationGranted) EmeraldSuccess else CoralAlert
                                )
                            }
                            if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                OutlinedButton(onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                    Text("Allow")
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    })
                                }) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Settings")
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Exact Alarm Permission", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (canScheduleExactAlarms) "Enabled (Exact timing)" else "Restricted — tap to grant Alarms & Reminders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (canScheduleExactAlarms) EmeraldSuccess else AmberGold
                                )
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                OutlinedButton(onClick = {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    })
                                }) { Text("Manage") }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = AmberGold)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Battery Optimization Guidance", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Some device manufacturers restrict background alarms under strict battery saver. For best reliability, set battery usage to Unrestricted in Android App Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open Android App Battery Settings") }
                    }
                }
            }

            // 5. Real-Time Cloud Sync, Backup & Restore, Reset
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Cloud Sync, Backup & Data", style = MaterialTheme.typography.titleLarge)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = if (syncError == null) EmeraldSuccess else AmberGold)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Signed in: ${currentUser?.email ?: currentUser?.uid ?: "Offline"}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = syncError ?: "Real-time Firestore sync active across all your devices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (syncError == null) EmeraldSuccess else AmberGold
                                )
                            }
                        }

                        if (backupMessage != null) {
                            Text(backupMessage!!, style = MaterialTheme.typography.bodySmall, color = SkyAccent)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("daily_life_assistant_backup.json") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export")
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import")
                            }
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("settings_restore_defaults")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Default Schedule")
                        }

                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Completion History")
                        }

                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(containerColor = AzurePrimary),
                            modifier = Modifier.fillMaxWidth().testTag("settings_sign_out")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out")
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "About: My Daily Life Assistant v1.0 • Offline-first Room + DataStore + Exact AlarmManager + English TTS + Real-Time Firestore Sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Default Schedule?") },
            text = { Text("This restores the original fixed daily timetable and weekly Group A/B/Sunday workout plans.") },
            confirmButton = {
                Button(onClick = { onRestoreDefaults(); showRestoreDialog = false }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Completion History?") },
            text = { Text("This permanently deletes all recorded daily completions and weekly statistics across synced devices.") },
            confirmButton = {
                Button(
                    onClick = { onClearHistory(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert)
                ) { Text("Clear History") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
