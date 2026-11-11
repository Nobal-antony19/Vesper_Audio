package com.vesper.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.playback.SleepTimerState
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    timerState: SleepTimerState,
    onSetTimerMinutes: (Int, Boolean) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onAddMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customMinutes by remember { mutableFloatStateOf(30f) }
    var fadeOut by remember { mutableStateOf(true) }
    val accentColor = com.vesper.audio.ui.theme.LocalVesperAccentColor.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VesperSurfaceElevated,
        contentColor = VesperTextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VesperOutline)
            )
        },
        modifier = modifier.testTag("sleep_timer_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(VesperDeepBurgundy)
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sleep Timer",
                        style = VesperTypography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = VesperTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Countdown Card
            if (timerState.isActive) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VesperDeepBurgundy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, accentColor, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Music stops in",
                            style = VesperTypography.labelMedium.copy(color = VesperTextSecondary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = timerState.formattedRemaining,
                            style = VesperTypography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onAddMinutes(5) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+5 min", color = VesperTextPrimary)
                            }
                            OutlinedButton(
                                onClick = { onAddMinutes(15) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+15 min", color = VesperTextPrimary)
                            }
                            Button(
                                onClick = {
                                    onCancelTimer()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2635)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Presets
            Text(
                text = "Preset Durations",
                style = VesperTypography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    OutlinedButton(
                        onClick = {
                            onSetTimerMinutes(mins, fadeOut)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (timerState.isActive && timerState.initialMinutes == mins) accentColor.copy(alpha = 0.35f) else Color.Transparent
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(if (timerState.isActive && timerState.initialMinutes == mins) accentColor else VesperOutline)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sleep_preset_$mins")
                    ) {
                        Text(
                            text = "${mins}m",
                            style = VesperTypography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = VesperTextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // End of track button
            Surface(
                onClick = {
                    onSetEndOfTrack()
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                color = VesperBackgroundSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VesperOutline, RoundedCornerShape(16.dp))
                    .testTag("sleep_end_of_track_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "At the end of this track",
                            style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = VesperTextPrimary)
                        )
                        Text(
                            text = "Playback will pause smoothly after current song finishes",
                            style = VesperTypography.bodySmall.copy(color = VesperTextMuted)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Duration",
                    style = VesperTypography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextSecondary
                    )
                )
                Text(
                    text = "${customMinutes.toInt()} minutes",
                    style = VesperTypography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }

            Slider(
                value = customMinutes,
                onValueChange = { customMinutes = it },
                valueRange = 5f..120f,
                steps = 22,
                colors = SliderDefaults.colors(
                    thumbColor = VesperTextPrimary,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = VesperOutline
                ),
                modifier = Modifier.testTag("sleep_custom_slider")
            )

            // Fade out toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smooth Fade-Out",
                        style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = VesperTextPrimary)
                    )
                    Text(
                        text = "Gently ramp down volume over last 15 seconds",
                        style = VesperTypography.bodySmall.copy(color = VesperTextMuted)
                    )
                }
                Switch(
                    checked = fadeOut,
                    onCheckedChange = { fadeOut = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VesperTextPrimary,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = VesperTextMuted,
                        uncheckedTrackColor = VesperBackgroundSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onSetTimerMinutes(customMinutes.toInt(), fadeOut)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_custom_sleep_button")
            ) {
                Text(
                    text = "Start Timer (${customMinutes.toInt()}m)",
                    style = VesperTypography.labelLarge.copy(fontWeight = FontWeight.Bold, color = VesperTextPrimary)
                )
            }
        }
    }
}
