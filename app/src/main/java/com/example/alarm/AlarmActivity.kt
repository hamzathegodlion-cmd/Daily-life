package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.ui.theme.AzurePrimary
import com.example.ui.theme.DailyLifeTheme
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.SkyAccent
import java.time.LocalDate

class AlarmActivity : ComponentActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter(AlarmPlaybackService.ACTION_ALARM_CLOSED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val activityId = intent.getStringExtra(AlarmPlaybackService.EXTRA_ACTIVITY_ID) ?: ""
        val title = intent.getStringExtra(AlarmPlaybackService.EXTRA_TITLE) ?: "Routine Alarm"
        val timeText = intent.getStringExtra(AlarmPlaybackService.EXTRA_TIME_TEXT) ?: ""
        val announcement = intent.getStringExtra(AlarmPlaybackService.EXTRA_ANNOUNCEMENT) ?: ""
        val weekday = intent.getIntExtra(
            AlarmPlaybackService.EXTRA_WEEKDAY,
            LocalDate.now().dayOfWeek.value
        )

        setContent {
            DailyLifeTheme {
                AlarmLockScreenContent(
                    title = title,
                    timeText = timeText,
                    announcement = announcement,
                    onMarkCompleted = {
                        sendServiceAction(AlarmPlaybackService.ACTION_MARK_COMPLETED, activityId, weekday)
                        finish()
                    },
                    onSnooze = {
                        sendServiceAction(AlarmPlaybackService.ACTION_SNOOZE, activityId, weekday)
                        finish()
                    },
                    onDismiss = {
                        sendServiceAction(AlarmPlaybackService.ACTION_DISMISS, activityId, weekday)
                        finish()
                    },
                    onOpenActivity = {
                        sendServiceAction(AlarmPlaybackService.ACTION_DISMISS, activityId, weekday)
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(AlarmNotificationHelper.EXTRA_OPEN_ACTIVITY_ID, activityId)
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }

    private fun sendServiceAction(action: String, activityId: String, weekday: Int) {
        val intent = Intent(this, AlarmPlaybackService::class.java).apply {
            this.action = action
            putExtra(AlarmPlaybackService.EXTRA_ACTIVITY_ID, activityId)
            putExtra(AlarmPlaybackService.EXTRA_WEEKDAY, weekday)
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeReceiver)
        } catch (_: Exception) {
        }
    }
}

@Composable
private fun AlarmLockScreenContent(
    title: String,
    timeText: String,
    announcement: String,
    onMarkCompleted: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onOpenActivity: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyVoid, NavyDeep, NavySurface)))
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MY DAILY LIFE ASSISTANT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyAccent
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = timeText.ifBlank { "Routine Alert" },
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SkyAccent.copy(alpha = 0.16f * pulse),
                        radius = size.minDimension * 0.48f
                    )
                    drawCircle(
                        color = AzurePrimary.copy(alpha = 0.45f * pulse),
                        radius = size.minDimension * 0.37f,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Active alarm",
                    tint = SkyAccent,
                    modifier = Modifier.size(64.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = NavySurface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = announcement,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onMarkCompleted,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("alarm_mark_completed")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Mark Completed", style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSnooze,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("alarm_snooze")
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Snooze")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("alarm_dismiss")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Dismiss")
                    }
                }

                OutlinedButton(
                    onClick = onOpenActivity,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("alarm_open_activity")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Open Activity in App")
                }
            }
        }
    }
}
