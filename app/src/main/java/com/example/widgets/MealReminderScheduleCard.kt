package com.example.widgets

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.notifications.MealReminderNotificationManager
import com.example.core.notifications.MealReminderSchedule
import com.example.core.notifications.MealSlotSchedule
import com.example.core.notifications.MealSlotType
import com.example.model.FoodDiaryEntry
import com.example.model.todayDateKey
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import java.util.Locale

/**
 * Interactive Push Notification & Daily Meal Reminder Schedule Card.
 * Allows users to configure typical Breakfast, Lunch, Snack, and Dinner reminder times,
 * automatically syncs daily background alarms via AlarmManager, requests POST_NOTIFICATIONS
 * permission on Android 13+, and lets users send an instant test notification.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealReminderScheduleCard(
    todayEntries: List<FoodDiaryEntry> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var schedule by remember {
        mutableStateOf(MealReminderNotificationManager.loadSchedule(context))
    }
    var hasPermission by remember {
        mutableStateOf(MealReminderNotificationManager.hasNotificationPermission(context))
    }
    var statusBannerText by remember { mutableStateOf<String?>(null) }
    var editingSlot by remember { mutableStateOf<MealSlotSchedule?>(null) }
    var pendingTestSlotAfterPerm by remember { mutableStateOf<MealSlotType?>(null) }

    // Sync today's logged meals to SharedPreferences so BroadcastReceiver knows which meals are logged
    LaunchedEffect(todayEntries) {
        val todayKey = todayDateKey()
        todayEntries.forEach { entry ->
            MealReminderNotificationManager.markMealLoggedForDate(context, todayKey, entry.mealType)
        }
        MealReminderNotificationManager.syncAllAlarms(context, schedule)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            MealReminderNotificationManager.syncAllAlarms(context, schedule)
            val testSlot = pendingTestSlotAfterPerm
            if (testSlot != null) {
                MealReminderNotificationManager.showMealReminderNotification(
                    context = context,
                    slot = testSlot,
                    isTestNotification = true
                )
                statusBannerText = "Push notification sent for ${testSlot.displayName}! Check your status bar."
                pendingTestSlotAfterPerm = null
            } else {
                statusBannerText = "Notification permission granted! Daily meal reminders are active."
            }
        } else {
            statusBannerText = "Notification permission was denied. Enable notifications in Android Settings to receive alerts."
        }
    }

    fun updateSchedule(newSchedule: MealReminderSchedule, feedback: String? = null) {
        schedule = newSchedule
        MealReminderNotificationManager.saveAndSyncSchedule(context, newSchedule)
        if (feedback != null) {
            statusBannerText = feedback
        }
    }

    fun triggerTestNotification(slot: MealSlotType) {
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingTestSlotAfterPerm = slot
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val sent = MealReminderNotificationManager.showMealReminderNotification(
            context = context,
            slot = slot,
            isTestNotification = true
        )
        statusBannerText = if (sent) {
            "Sent ${slot.emoji} ${slot.displayName} push notification! Swipe down your status bar to view it."
        } else {
            "Please grant notification permission to receive meal reminders."
        }
    }

    val upcomingInfo = remember(schedule) { schedule.nextUpcomingSlot() }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("meal_reminder_schedule_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header Row with Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (schedule.masterEnabled) PastelGreenCard else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (schedule.masterEnabled) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (schedule.masterEnabled) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Meal Logging Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Scheduled push alerts for Breakfast, Lunch & Dinner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = schedule.masterEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        updateSchedule(
                            schedule.copy(masterEnabled = enabled),
                            feedback = if (enabled) "Daily meal reminders enabled." else "All meal reminders paused."
                        )
                    },
                    modifier = Modifier.testTag("meal_reminders_master_switch")
                )
            }

            // Next Upcoming Reminder Pill Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (schedule.masterEnabled && upcomingInfo != null) PastelGreenCard else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (schedule.masterEnabled) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (schedule.masterEnabled && upcomingInfo != null) {
                                "Next Reminder: ${upcomingInfo.first.slot.emoji} ${upcomingInfo.first.slot.displayName} • ${upcomingInfo.second}"
                            } else {
                                "Meal reminders are currently paused"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                    }
                }
            }

            // Android 13+ Permission Request Banner if not yet granted
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelPeachCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Push Notifications",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                            Text(
                                text = "Grant notification permission so NutriScan can alert you at Breakfast, Lunch & Dinner.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkCharcoalText.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCharcoalText,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("grant_notification_permission_button")
                        ) {
                            Text("Allow", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Feedback Banner when user tests or updates a schedule
            statusBannerText?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PastelBlueCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Individual Meal Schedule Rows (Breakfast, Lunch, Evening Snack, Dinner)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                schedule.allSlots().forEach { slotSchedule ->
                    val slot = slotSchedule.slot
                    val isLoggedToday = todayEntries.any {
                        it.mealType.equals(slot.id, ignoreCase = true)
                    }

                    MealReminderSlotRow(
                        slotSchedule = slotSchedule,
                        masterEnabled = schedule.masterEnabled,
                        isLoggedToday = isLoggedToday,
                        onClickEditTime = { editingSlot = slotSchedule },
                        onToggleEnabled = { enabled ->
                            val updated = schedule.updateSlot(slot = slot, enabled = enabled)
                            updateSchedule(
                                updated,
                                feedback = if (enabled) {
                                    "${slot.displayName} reminder scheduled for ${slotSchedule.formatted12Hour()}."
                                } else {
                                    "${slot.displayName} reminder turned off."
                                }
                            )
                        },
                        onSendTestNotification = { triggerTestNotification(slot) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            // Smart Skip Toggle + Quick Test All Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Skip When Already Logged",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Skip sending the reminder if you have already logged that meal today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = schedule.smartSkipLoggedMeals,
                    onCheckedChange = { skip ->
                        updateSchedule(
                            schedule.copy(smartSkipLoggedMeals = skip),
                            feedback = if (skip) {
                                "Smart Skip enabled: logged meals won't trigger duplicate alerts."
                            } else {
                                "Reminders will ring even if a meal is already logged."
                            }
                        )
                    },
                    modifier = Modifier.testTag("meal_reminder_smart_skip_switch")
                )
            }
        }
    }

    // Time Picker & Preset Editor Dialog for Selected Meal Slot
    editingSlot?.let { currentSlot ->
        MealSlotTimePickerDialog(
            slotSchedule = currentSlot,
            onDismiss = { editingSlot = null },
            onSaveTime = { newHour, newMinute ->
                val updated = schedule.updateSlot(
                    slot = currentSlot.slot,
                    enabled = true,
                    hourOfDay = newHour,
                    minute = newMinute
                )
                val updatedSlot = updated.allSlots().first { it.slot == currentSlot.slot }
                updateSchedule(
                    updated,
                    feedback = "Saved ${currentSlot.slot.displayName} reminder for ${updatedSlot.formatted12Hour()}."
                )
                editingSlot = null
            }
        )
    }
}

@Composable
private fun MealReminderSlotRow(
    slotSchedule: MealSlotSchedule,
    masterEnabled: Boolean,
    isLoggedToday: Boolean,
    onClickEditTime: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit
) {
    val slot = slotSchedule.slot
    val effectiveActive = masterEnabled && slotSchedule.enabled

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (effectiveActive) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (effectiveActive) FreshGreenPrimary.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("meal_reminder_row_${slot.id.lowercase(Locale.US)}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (effectiveActive) PastelGreenCard else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = slot.emoji,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = slot.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isLoggedToday) PastelGreenCard else PastelPeachCard
                            ) {
                                Text(
                                    text = if (isLoggedToday) "✓ Logged" else "Not logged",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLoggedToday) FreshGreenPrimary else DarkCharcoalText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${slot.bengaliName} • Daily push alert",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = slotSchedule.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("meal_reminder_switch_${slot.id.lowercase(Locale.US)}")
                )
            }

            // Bottom Row: Time Chip (Tap to change time) + "Send Test Notification" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClickEditTime() }
                        .border(
                            1.dp,
                            FreshGreenPrimary.copy(alpha = 0.45f),
                            RoundedCornerShape(50)
                        )
                        .testTag("meal_reminder_time_button_${slot.id.lowercase(Locale.US)}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Edit Reminder Time",
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = slotSchedule.formatted12Hour(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshGreenPrimary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSendTestNotification,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("meal_reminder_test_${slot.id.lowercase(Locale.US)}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Test Alert",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FreshGreenPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealSlotTimePickerDialog(
    slotSchedule: MealSlotSchedule,
    onDismiss: () -> Unit,
    onSaveTime: (hourOfDay: Int, minute: Int) -> Unit
) {
    val slot = slotSchedule.slot
    var hourText by remember(slotSchedule) { mutableStateOf(slotSchedule.hourOfDay.toString()) }
    var minuteText by remember(slotSchedule) {
        mutableStateOf(String.format(Locale.US, "%02d", slotSchedule.minute))
    }

    val presets: List<Pair<Int, Int>> = remember(slot) {
        when (slot) {
            MealSlotType.BREAKFAST -> listOf(7 to 30, 8 to 0, 8 to 30, 9 to 0, 9 to 30, 10 to 0)
            MealSlotType.LUNCH -> listOf(12 to 30, 13 to 0, 13 to 15, 13 to 30, 14 to 0, 14 to 30)
            MealSlotType.SNACK -> listOf(16 to 0, 16 to 30, 17 to 0, 17 to 30, 18 to 0)
            MealSlotType.DINNER -> listOf(19 to 30, 20 to 0, 20 to 30, 21 to 0, 21 to 30, 22 to 0)
        }
    }

    val parsedHour = (hourText.toIntOrNull() ?: slotSchedule.hourOfDay).coerceIn(0, 23)
    val parsedMinute = (minuteText.toIntOrNull() ?: slotSchedule.minute).coerceIn(0, 59)
    val previewSlot = MealSlotSchedule(slot, true, parsedHour, parsedMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${slot.emoji} Set ${slot.displayName} Reminder Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose a typical time for ${slot.displayName} or enter a custom 24-hour time below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Quick Schedule Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (h, m) ->
                        val chipSchedule = MealSlotSchedule(slot, true, h, m)
                        val isSelected = parsedHour == h && parsedMinute == m
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                hourText = h.toString()
                                minuteText = String.format(Locale.US, "%02d", m)
                            },
                            label = {
                                Text(
                                    text = chipSchedule.formatted12Hour(),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Hour (0-23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("meal_time_hour_input")
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Minute (0-59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("meal_time_minute_input")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PastelGreenCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Scheduled Daily Alert: ${previewSlot.formatted12Hour()} (${previewSlot.formatted24Hour()})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTime(parsedHour, parsedMinute) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = DarkCharcoalText
                ),
                modifier = Modifier.testTag("save_meal_reminder_time_button")
            ) {
                Text("Save Schedule", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Modal Dialog wrapper so users can open the Meal Reminder Schedule directly from the
 * Notification Bell icon on the Home Dashboard.
 */
@Composable
fun MealReminderScheduleDialog(
    todayEntries: List<FoodDiaryEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Meal Reminder Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                MealReminderScheduleCard(todayEntries = todayEntries)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCharcoalText,
                    contentColor = Color.White
                )
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
