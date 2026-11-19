package com.example.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.MockupCarbsGreen
import com.example.ui.theme.MockupFatAmber
import com.example.ui.theme.MockupNeonScan
import com.example.ui.theme.MockupSugarCoral
import kotlin.math.roundToInt

/**
 * Data holder for daily macronutrient goals and consumption.
 */
data class DailyMacroSummary(
    val carbsG: Double = 0.0,
    val targetCarbsG: Double = 220.0,
    val fatG: Double = 0.0,
    val targetFatG: Double = 65.0,
    val proteinG: Double = 0.0,
    val targetProteinG: Double = 120.0
)

/**
 * Visual Progress Bar Component displaying the user's daily calorie intake against a set goal,
 * using a clean modern circular progress indicator, percentage badge, and macro breakdowns.
 */
@Composable
fun DailyCalorieCircularProgressCard(
    consumedCalories: Int,
    targetCalories: Int,
    macroSummary: DailyMacroSummary = DailyMacroSummary(),
    onAdjustGoalClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val consumedCarbsG = macroSummary.carbsG
    val targetCarbsG = macroSummary.targetCarbsG
    val consumedFatG = macroSummary.fatG
    val targetFatG = macroSummary.targetFatG
    val consumedProteinG = macroSummary.proteinG
    val targetProteinG = macroSummary.targetProteinG

    val effectiveGoal = targetCalories.coerceAtLeast(800)
    val progressRatio = (consumedCalories.toFloat() / effectiveGoal.toFloat()).coerceAtLeast(0f)
    val remainingCalories = (effectiveGoal - consumedCalories).coerceAtLeast(0)
    val isOverGoal = consumedCalories > effectiveGoal
    val progressPercentage = (progressRatio * 100).roundToInt()

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "calorie_circular_progress"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_calorie_circular_progress")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Icon, Title & Edit Goal Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Daily Calorie Intake",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target: $effectiveGoal kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Adjust Goal Button with 48.dp interactive touch target
                Surface(
                    onClick = onAdjustGoalClick,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("daily_calorie_adjust_goal_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Adjust Calorie Goal",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(196.dp)
                    .testTag("daily_calorie_circular_indicator")
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                val primaryGreen = FreshGreenPrimary
                val neonScan = MockupNeonScan
                val amberWarning = MockupFatAmber
                val coralExceeded = MockupSugarCoral

                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 15.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    // 1. Background Circular Track (360 degrees)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Animated Circular Progress Arc starting from top (-90 degrees)
                    val sweep = 360f * animatedProgress
                    if (sweep > 0f) {
                        val strokeBrush = if (isOverGoal) {
                            Brush.sweepGradient(
                                colors = listOf(primaryGreen, coralExceeded, coralExceeded)
                            )
                        } else {
                            Brush.sweepGradient(
                                colors = listOf(primaryGreen, neonScan, primaryGreen)
                            )
                        }

                        drawArc(
                            brush = strokeBrush,
                            startAngle = -90f,
                            sweepAngle = sweep.coerceAtLeast(4f),
                            useCenter = false,
                            topLeft = topLeftOffset,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center Typography & Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$consumedCalories",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 36.sp,
                        modifier = Modifier.testTag("daily_calorie_intake_text")
                    )

                    Text(
                        text = "kcal of $effectiveGoal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("daily_calorie_goal_text")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Pill Badge (Remaining or Goal Met)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isOverGoal) MockupSugarCoral.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOverGoal) MockupSugarCoral.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            if (isOverGoal) {
                                Text(
                                    text = "+${consumedCalories - effectiveGoal} kcal over",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MockupSugarCoral
                                )
                            } else if (remainingCalories == 0 && consumedCalories > 0) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Goal Reached!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "$remainingCalories kcal left",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Macro Progress Indicators Row (Carbs, Fats, Protein)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Carbs Macro Mini Card
                CalorieMacroMicroCard(
                    title = "Carbs",
                    amountG = consumedCarbsG,
                    targetG = targetCarbsG,
                    icon = Icons.Default.Eco,
                    iconColor = MockupCarbsGreen,
                    bgColor = MockupCarbsGreen.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                )

                // Fats Macro Mini Card
                CalorieMacroMicroCard(
                    title = "Fats",
                    amountG = consumedFatG,
                    targetG = targetFatG,
                    icon = Icons.Default.InvertColors,
                    iconColor = MockupFatAmber,
                    bgColor = MockupFatAmber.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                )

                // Protein Macro Mini Card
                CalorieMacroMicroCard(
                    title = "Protein",
                    amountG = consumedProteinG,
                    targetG = targetProteinG,
                    icon = Icons.Default.ViewInAr,
                    iconColor = MockupSugarCoral,
                    bgColor = MockupSugarCoral.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CalorieMacroMicroCard(
    title: String,
    amountG: Double,
    targetG: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val safeTarget = targetG.coerceAtLeast(1.0)
    val ratio = (amountG / safeTarget).toFloat().coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${amountG.roundToInt()}g",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "of ${targetG.roundToInt()}g",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Micro progress bar
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = iconColor,
                trackColor = bgColor
            )
        }
    }
}
