package com.example.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WaterRecord
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.WaterBlue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Hydration Tracking Component featuring an animated Circular Progress Indicator,
 * visual glass-by-glass tracker, quick-add container buttons (+150ml, +250ml, +500ml, Custom),
 * daily water goal configuration, and today's intake logs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CircularHydrationTrackerCard(
    todayWaterMl: Int,
    waterGoalMl: Int,
    todayLogs: List<WaterRecord> = emptyList(),
    onAddWater: (Int) -> Unit,
    onDecrementWater: (Int) -> Unit = {},
    onDeleteWater: (String) -> Unit = {},
    onUpdateWaterGoal: (Int) -> Unit = {},
    onOpenFullTracker: (() -> Unit)? = null,
    isCloudSynced: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedStepMl by remember { mutableIntStateOf(250) }

    val safeGoalMl = waterGoalMl.coerceAtLeast(500)
    val rawProgress = (todayWaterMl.toFloat() / safeGoalMl.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(750),
        label = "hydration_circular_progress"
    )
    val pct = ((todayWaterMl.toFloat() / safeGoalMl.toFloat()) * 100).roundToInt().coerceAtLeast(0)
    val remainingMl = (safeGoalMl - todayWaterMl).coerceAtLeast(0)
    val filledGlasses = (todayWaterMl / 250.0).roundToInt()
    val targetGlasses = (safeGoalMl / 250.0).roundToInt().coerceIn(4, 16)

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("circular_hydration_tracker_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row with Title, Goal Badge & Edit Goal Action
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
                        color = PastelBlueCard,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = WaterBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Hydration Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "$filledGlasses of $targetGlasses glasses (250ml) • Goal ${safeGoalMl}ml",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PastelBlueCard,
                        modifier = Modifier
                            .clickable { showGoalDialog = true }
                            .testTag("hydration_set_goal_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Set Hydration Goal",
                                tint = WaterBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Goal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                        }
                    }

                    if (onOpenFullTracker != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { onOpenFullTracker() }
                                .testTag("open_full_water_tracker_button")
                        ) {
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 2. Center Row: Animated Circular Progress Indicator Ring + Hydration Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Progress Indicator Gauge
                HydrationCircularProgressRing(
                    progress = animatedProgress,
                    percentage = pct,
                    todayWaterMl = todayWaterMl,
                    goalMl = safeGoalMl,
                    ringSize = 136.dp
                )

                // Right Column: Intake Status + Quick Container Log Buttons
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (remainingMl == 0) PastelGreenCard else PastelBlueCard.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (remainingMl == 0) Icons.Default.CheckCircle else Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (remainingMl == 0) FreshGreenPrimary else WaterBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (remainingMl == 0) {
                                    "Hydration goal reached!"
                                } else {
                                    "$remainingMl ml left to reach goal"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                        }
                    }

                    Text(
                        text = "Quick Log Water:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Row 1: +150ml Cup & +250ml Glass
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickWaterContainerButton(
                            label = "+150ml",
                            subLabel = "Cup",
                            containerColor = PastelBlueCard,
                            contentColor = DarkCharcoalText,
                            onClick = { onAddWater(150) },
                            testTag = "water_add_150",
                            modifier = Modifier.weight(1f)
                        )
                        QuickWaterContainerButton(
                            label = "+250ml",
                            subLabel = "Glass",
                            containerColor = WaterBlue,
                            contentColor = Color.White,
                            onClick = { onAddWater(250) },
                            testTag = "water_add_250",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: +500ml Bottle & +Custom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickWaterContainerButton(
                            label = "+500ml",
                            subLabel = "Bottle",
                            containerColor = FreshGreenPrimary,
                            contentColor = Color.White,
                            onClick = { onAddWater(500) },
                            testTag = "water_add_500",
                            modifier = Modifier.weight(1f)
                        )
                        QuickWaterContainerButton(
                            label = "+Custom",
                            subLabel = "Any ml",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { showCustomDialog = true },
                            testTag = "water_add_custom",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2B. Simple Daily Water Consumption Tracker with Increment (+) / Decrement (-) Stepper & Firestore Sync
            DailyWaterConsumptionStepperBar(
                todayWaterMl = todayWaterMl,
                waterGoalMl = safeGoalMl,
                selectedStepMl = selectedStepMl,
                onSelectStepMl = { selectedStepMl = it },
                onIncrement = { onAddWater(selectedStepMl) },
                onDecrement = { onDecrementWater(selectedStepMl) },
                isCloudSynced = isCloudSynced
            )

            // 3. Interactive Visual Glass-by-Glass Progress Strip (Tap any cup to log +250ml)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Glasses Progress (Tap a glass to log +250ml)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$filledGlasses / $targetGlasses",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = WaterBlue
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val displayGlassCount = targetGlasses.coerceIn(6, 12)
                    for (glassIdx in 1..displayGlassCount) {
                        val isFilled = glassIdx <= filledGlasses
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFilled) WaterBlue else PastelBlueCard.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onAddWater(250) }
                                .testTag("water_glass_cup_$glassIdx")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = "Glass $glassIdx",
                                    tint = if (isFilled) Color.White else WaterBlue.copy(alpha = 0.65f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Recent Today's Hydration Entries (if any logged today)
            if (todayLogs.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Logged Drinks (${todayLogs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        todayLogs.takeLast(3).forEach { log ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PastelBlueCard.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.quantity}ml",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkCharcoalText
                                    )
                                    if (log.id.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove ${log.quantity}ml",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clickable { onDeleteWater(log.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Water Amount Modal
    if (showCustomDialog) {
        var customMlInput by remember { mutableStateOf("300") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Log Custom Water Intake", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the amount of water, tea, or hydration in milliliters (ml):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customMlInput,
                        onValueChange = { customMlInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (ml)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_water_ml_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(200, 350, 600, 750).forEach { preset ->
                            FilterChip(
                                selected = customMlInput == preset.toString(),
                                onClick = { customMlInput = preset.toString() },
                                label = { Text("${preset}ml") },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ml = (customMlInput.toIntOrNull() ?: 250).coerceIn(10, 5000)
                        onAddWater(ml)
                        showCustomDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = WaterBlue),
                    modifier = Modifier.testTag("confirm_custom_water_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Water", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Daily Water Goal Configuration Modal
    if (showGoalDialog) {
        var goalMlInput by remember(waterGoalMl) { mutableStateOf(safeGoalMl.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Daily Hydration Goal", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recommended daily water intake is 2,000 ml – 3,500 ml depending on activity and climate:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = goalMlInput,
                        onValueChange = { goalMlInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Daily Water Target (ml)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hydration_goal_ml_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2000, 2500, 3000, 3500).forEach { presetGoal ->
                            FilterChip(
                                selected = goalMlInput == presetGoal.toString(),
                                onClick = { goalMlInput = presetGoal.toString() },
                                label = { Text("${presetGoal}ml") },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WaterBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedGoal = (goalMlInput.toIntOrNull() ?: safeGoalMl).coerceIn(500, 10000)
                        onUpdateWaterGoal(updatedGoal)
                        showGoalDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                    modifier = Modifier.testTag("save_hydration_goal_button")
                ) {
                    Text("Save Goal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable Animated Circular Progress Ring Gauge for Hydration Tracking.
 * Uses DrawScope.size.width and DrawScope.size.height per Android Canvas guidelines.
 */
@Composable
fun HydrationCircularProgressRing(
    progress: Float,
    percentage: Int,
    todayWaterMl: Int,
    goalMl: Int,
    ringSize: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ringSize)
            .testTag("hydration_circular_progress_indicator")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 13.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokePx * 1.4f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Background circular track
            drawArc(
                color = WaterBlue.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active hydration progress arc
            val sweep = (360f * progress.coerceIn(0f, 1f)).coerceAtLeast(if (todayWaterMl > 0) 10f else 0f)
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            WaterBlue,
                            FreshGreenPrimary
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Endpoint indicator dot on the circular arc
                val angleRad = Math.toRadians((-90.0 + sweep.toDouble()))
                val radius = diameter / 2f
                val dotCenter = Offset(
                    x = (size.width / 2f) + (radius * cos(angleRad)).toFloat(),
                    y = (size.height / 2f) + (radius * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = Color.White,
                    radius = strokePx * 0.42f,
                    center = dotCenter
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = WaterBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$percentage%",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${todayWaterMl}ml",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = WaterBlue
            )
            Text(
                text = "of ${goalMl}ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickWaterContainerButton(
    label: String,
    subLabel: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Simple Daily Water Consumption Tracker Component with Increment (+) and Decrement (-) buttons
 * that saves and syncs current progress to Cloud Firestore (`users/{userId}/waterHistory`).
 */
@Composable
fun DailyWaterConsumptionStepperBar(
    todayWaterMl: Int,
    waterGoalMl: Int,
    selectedStepMl: Int,
    onSelectStepMl: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isCloudSynced: Boolean = true,
    modifier: Modifier = Modifier
) {
    val safeGoal = waterGoalMl.coerceAtLeast(500)
    val progressFraction = (todayWaterMl.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val glassesCount = (todayWaterMl / 250.0).roundToInt()
    val targetGlasses = (safeGoal / 250.0).roundToInt().coerceAtLeast(4)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PastelBlueCard.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WaterBlue.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .testTag("daily_water_stepper_component")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Step Size Selector + Cloud Firestore Sync Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Step:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoalText.copy(alpha = 0.75f)
                    )
                    listOf(150 to "150ml", 250 to "250ml", 500 to "500ml").forEach { (ml, label) ->
                        val isSelected = selectedStepMl == ml
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) WaterBlue else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clickable { onSelectStepMl(ml) }
                                .testTag("water_step_select_$ml")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else DarkCharcoalText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCloudSynced) PastelGreenCard else Color.White.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (isCloudSynced) FreshGreenPrimary else WaterBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isCloudSynced) "Firestore Sync" else "Local Sync",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                    }
                }
            }

            // Center Row: [-] Decrement Button | Current Intake Display | [+] Increment Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [-] Decrement Button (min 48.dp touch target)
                Surface(
                    shape = CircleShape,
                    color = if (todayWaterMl > 0) DarkCharcoalText else DarkCharcoalText.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(enabled = todayWaterMl > 0) { onDecrement() }
                        .testTag("water_decrement_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease water intake by ${selectedStepMl}ml",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Center Current Progress Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "$todayWaterMl / $safeGoal ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText
                    )
                    Text(
                        text = "$glassesCount of $targetGlasses glasses • ±${selectedStepMl}ml per tap",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkCharcoalText.copy(alpha = 0.72f)
                    )
                }

                // [+] Increment Button (min 48.dp touch target)
                Surface(
                    shape = CircleShape,
                    color = WaterBlue,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onIncrement() }
                        .testTag("water_increment_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase water intake by ${selectedStepMl}ml",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom Linear Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0.04f, 1f) },
                color = if (progressFraction >= 1f) FreshGreenPrimary else WaterBlue,
                trackColor = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

