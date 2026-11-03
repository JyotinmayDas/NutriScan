package com.example.features.water_tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.ui.theme.WaterBlue
import kotlin.math.roundToInt

@Composable
fun WaterTrackerScreen(
    profile: UserProfile,
    selectedDateKey: String,
    waterRecords: List<WaterRecord>,
    onAddWater: (Int) -> Unit,
    onDeleteWater: (String) -> Unit,
    onUpdateWaterGoal: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showCustomAmountDialog by remember { mutableStateOf(false) }
    var showGoalConfigDialog by remember { mutableStateOf(false) }

    val todayLogs = waterRecords.filter { it.dateKey == selectedDateKey }
    val todayTotalMl = todayLogs.sumOf { it.quantity }
    val goalMl = profile.dailyWaterGoalMl.coerceAtLeast(500)
    val progress = (todayTotalMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("water_tracker_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Water Intake Tracker",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                OutlinedButton(onClick = { showGoalConfigDialog = true }) {
                    Text("Goal: ${goalMl}ml")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = WaterBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$todayTotalMl / $goalMl ml (${(progress * 100).roundToInt()}%)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = WaterBlue,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(150, 250, 500).forEach { ml ->
                            Button(
                                onClick = { onAddWater(ml) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+${ml}ml")
                            }
                        }
                        FilledTonalButton(
                            onClick = { showCustomAmountDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Text("Custom")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Today's Hydration Logs ($selectedDateKey)",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (todayLogs.isEmpty()) {
            item {
                Text(
                    text = "No water logged yet for $selectedDateKey. Tap a button above to log your hydration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(todayLogs, key = { it.id.ifBlank { it.quantity.toString() } }) { record ->
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${record.quantity} ml",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (record.id.isNotBlank()) {
                            IconButton(onClick = { onDeleteWater(record.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete water entry",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomAmountDialog) {
        var customMl by remember { mutableStateOf("300") }
        AlertDialog(
            onDismissRequest = { showCustomAmountDialog = false },
            title = { Text("Add Custom Water Amount") },
            text = {
                OutlinedTextField(
                    value = customMl,
                    onValueChange = { customMl = it },
                    label = { Text("Water Quantity (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ml = (customMl.toIntOrNull() ?: 250).coerceIn(10, 5000)
                        onAddWater(ml)
                        showCustomAmountDialog = false
                    }
                ) {
                    Text("Add Water")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAmountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGoalConfigDialog) {
        var newGoalStr by remember(profile) { mutableStateOf(profile.dailyWaterGoalMl.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalConfigDialog = false },
            title = { Text("Configure Daily Hydration Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newGoalStr,
                        onValueChange = { newGoalStr = it },
                        label = { Text("Daily Water Goal (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Hydration needs vary by climate, body weight, and activity level.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = (newGoalStr.toIntOrNull() ?: 2500).coerceIn(500, 10000)
                        onUpdateWaterGoal(g)
                        showGoalConfigDialog = false
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
