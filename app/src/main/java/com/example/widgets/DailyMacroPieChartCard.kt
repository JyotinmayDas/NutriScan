package com.example.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.ProteinColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Single macronutrient slice model for the daily summary pie chart.
 */
data class MacroPieSlice(
    val key: String,
    val label: String,
    val grams: Double,
    val kcal: Double,
    val percentage: Int,
    val sweepAngle: Float,
    val targetGrams: Int,
    val color: Color
)

/**
 * Aggregated daily macronutrient breakdown ready for rendering and unit verification.
 */
data class DailyMacroPieBreakdown(
    val slices: List<MacroPieSlice>,
    val totalGrams: Double,
    val totalKcal: Double,
    val hasIntake: Boolean
)

/**
 * Computes percentage breakdown and arc sweep angles for Carbohydrates, Proteins, and Fats.
 * When [byCalories] is true (default), shares are weighted by energy (4 kcal/g carbs & protein, 9 kcal/g fat).
 * When false, shares are weighted by gram mass.
 */
fun computeDailyMacroPieBreakdown(
    carbsG: Double,
    proteinG: Double,
    fatG: Double,
    targetCarbsG: Int = 220,
    targetProteinG: Int = 120,
    targetFatG: Int = 65,
    byCalories: Boolean = true
): DailyMacroPieBreakdown {
    val safeCarbs = carbsG.coerceAtLeast(0.0)
    val safeProtein = proteinG.coerceAtLeast(0.0)
    val safeFat = fatG.coerceAtLeast(0.0)

    val carbsKcal = safeCarbs * 4.0
    val proteinKcal = safeProtein * 4.0
    val fatKcal = safeFat * 9.0

    val totalGrams = safeCarbs + safeProtein + safeFat
    val totalKcal = carbsKcal + proteinKcal + fatKcal

    val weights = if (byCalories) {
        listOf(carbsKcal, proteinKcal, fatKcal)
    } else {
        listOf(safeCarbs, safeProtein, safeFat)
    }
    val totalWeight = weights.sum()
    val hasIntake = totalWeight > 0.001

    val rawPcts = if (hasIntake) {
        weights.map { (it / totalWeight) * 100.0 }
    } else {
        listOf(0.0, 0.0, 0.0)
    }

    // Round percentages cleanly so non-empty breakdowns sum to 100%
    val roundedPcts = rawPcts.map { it.roundToInt() }.toMutableList()
    if (hasIntake) {
        val diff = 100 - roundedPcts.sum()
        if (diff != 0) {
            val maxIdx = weights.indices.maxByOrNull { weights[it] } ?: 0
            roundedPcts[maxIdx] = (roundedPcts[maxIdx] + diff).coerceIn(0, 100)
        }
    }

    val sweeps = if (hasIntake) {
        weights.map { ((it / totalWeight) * 360.0).toFloat() }
    } else {
        listOf(0f, 0f, 0f)
    }

    val slices = listOf(
        MacroPieSlice(
            key = "carbs",
            label = "Carbohydrates",
            grams = safeCarbs,
            kcal = carbsKcal,
            percentage = roundedPcts[0],
            sweepAngle = sweeps[0],
            targetGrams = targetCarbsG.coerceAtLeast(1),
            color = CarbsColor
        ),
        MacroPieSlice(
            key = "protein",
            label = "Protein",
            grams = safeProtein,
            kcal = proteinKcal,
            percentage = roundedPcts[1],
            sweepAngle = sweeps[1],
            targetGrams = targetProteinG.coerceAtLeast(1),
            color = ProteinColor
        ),
        MacroPieSlice(
            key = "fat",
            label = "Fats",
            grams = safeFat,
            kcal = fatKcal,
            percentage = roundedPcts[2],
            sweepAngle = sweeps[2],
            targetGrams = targetFatG.coerceAtLeast(1),
            color = FatColor
        )
    )

    return DailyMacroPieBreakdown(
        slices = slices,
        totalGrams = totalGrams,
        totalKcal = totalKcal,
        hasIntake = hasIntake
    )
}

/**
 * Interactive Pie Chart card for the Daily Nutrition Summary showing the percentage breakdown
 * of Carbohydrates, Proteins, and Fats consumed (by Energy kcal% or Weight g%).
 */
@Composable
fun DailyMacroPieChartCard(
    carbsG: Double,
    proteinG: Double,
    fatG: Double,
    targetCarbsG: Int = 220,
    targetProteinG: Int = 120,
    targetFatG: Int = 65,
    modifier: Modifier = Modifier
) {
    var byCalories by remember { mutableStateOf(true) }
    var selectedSliceIndex by remember { mutableIntStateOf(0) }

    val breakdown = remember(carbsG, proteinG, fatG, targetCarbsG, targetProteinG, targetFatG, byCalories) {
        computeDailyMacroPieBreakdown(
            carbsG = carbsG,
            proteinG = proteinG,
            fatG = fatG,
            targetCarbsG = targetCarbsG,
            targetProteinG = targetProteinG,
            targetFatG = targetFatG,
            byCalories = byCalories
        )
    }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "macro_pie_sweep"
    )
    val textMeasurer = rememberTextMeasurer()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val selectedSlice = breakdown.slices.getOrElse(selectedSliceIndex) { breakdown.slices.first() }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_macro_pie_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row + Energy/Grams Basis Toggle
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
                        color = FreshGreenPrimary.copy(alpha = 0.14f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = "Macronutrient Breakdown Pie Chart",
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Macronutrient Split",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (breakdown.hasIntake) {
                                "${breakdown.totalKcal.roundToInt()} macro kcal • ${breakdown.totalGrams.roundToInt()}g total"
                            } else {
                                "Log meals to see your daily macro ratio"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // % Calories vs % Grams compact segmented pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(true to "kcal %", false to "g %").forEach { (isKcalMode, label) ->
                            val selected = byCalories == isKcalMode
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = if (selected) FreshGreenPrimary else Color.Transparent,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable { byCalories = isKcalMode }
                                    .testTag(if (isKcalMode) "macro_pie_mode_calories" else "macro_pie_mode_grams")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Adaptive Chart + Legend Layout
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 430.dp

                val pieGraphic: @Composable (Modifier) -> Unit = { boxMod ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = boxMod
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(196.dp)
                                .testTag("daily_macro_pie_canvas")
                                .pointerInput(breakdown) {
                                    detectTapGestures { offset ->
                                        if (!breakdown.hasIntake) return@detectTapGestures
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val dx = offset.x - cx
                                        val dy = offset.y - cy
                                        val maxTapRadius = minOf(size.width, size.height) * 0.48f
                                        if (hypot(dx, dy) > maxTapRadius) return@detectTapGestures

                                        // Convert to clockwise degrees starting at -90 deg (12 o'clock)
                                        var angle = (atan2(dy, dx) * 180.0 / PI).toFloat()
                                        angle = (angle + 90f + 360f) % 360f

                                        var accum = 0f
                                        breakdown.slices.forEachIndexed { idx, slice ->
                                            val end = accum + slice.sweepAngle
                                            if (angle in accum..end && slice.sweepAngle > 0f) {
                                                selectedSliceIndex = idx
                                                return@detectTapGestures
                                            }
                                            accum = end
                                        }
                                    }
                                }
                        ) {
                            val piePadding = 12.dp.toPx()
                            val diameter = (size.minDimension - piePadding * 2).coerceAtLeast(40f)
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = diameter / 2f

                            if (!breakdown.hasIntake) {
                                // Empty state pie circle with subtle dashed ring
                                drawCircle(
                                    color = trackColor,
                                    radius = radius,
                                    center = center
                                )
                                drawCircle(
                                    color = onSurfaceVariant.copy(alpha = 0.25f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                val emptyText = textMeasurer.measure(
                                    "0%\nNo Data",
                                    style = TextStyle(
                                        color = onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                drawText(
                                    textLayoutResult = emptyText,
                                    topLeft = Offset(
                                        x = center.x - emptyText.size.width / 2f,
                                        y = center.y - emptyText.size.height / 2f
                                    )
                                )
                            } else {
                                var currentStartAngle = -90f
                                breakdown.slices.forEachIndexed { idx, slice ->
                                    val sweep = slice.sweepAngle * animProgress
                                    if (sweep > 0.5f) {
                                        val isSelected = idx == selectedSliceIndex
                                        val midAngleRad = ((currentStartAngle + sweep / 2f) * PI / 180.0).toFloat()
                                        val popOffset = if (isSelected) 6.dp.toPx() else 0f
                                        val offsetX = cos(midAngleRad) * popOffset
                                        val offsetY = sin(midAngleRad) * popOffset

                                        val sliceTopLeft = Offset(
                                            x = center.x - radius + offsetX,
                                            y = center.y - radius + offsetY
                                        )

                                        drawArc(
                                            color = slice.color,
                                            startAngle = currentStartAngle,
                                            sweepAngle = sweep,
                                            useCenter = true,
                                            topLeft = sliceTopLeft,
                                            size = Size(diameter, diameter)
                                        )

                                        // Clean crisp separator stroke between pie slices
                                        drawArc(
                                            color = surfaceColor,
                                            startAngle = currentStartAngle,
                                            sweepAngle = sweep,
                                            useCenter = true,
                                            topLeft = sliceTopLeft,
                                            size = Size(diameter, diameter),
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )

                                        // Draw percentage label directly on slices >= 10%
                                        if (slice.percentage >= 10 && animProgress > 0.7f) {
                                            val labelRadius = radius * 0.62f
                                            val labelCenterX = center.x + offsetX + cos(midAngleRad) * labelRadius
                                            val labelCenterY = center.y + offsetY + sin(midAngleRad) * labelRadius
                                            val pctText = textMeasurer.measure(
                                                "${slice.percentage}%",
                                                style = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            )
                                            drawText(
                                                textLayoutResult = pctText,
                                                topLeft = Offset(
                                                    x = labelCenterX - pctText.size.width / 2f,
                                                    y = labelCenterY - pctText.size.height / 2f
                                                )
                                            )
                                        }
                                    }
                                    currentStartAngle += sweep
                                }
                            }
                        }
                    }
                }

                val legendColumn: @Composable (Modifier) -> Unit = { colMod ->
                    Column(
                        modifier = colMod,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        breakdown.slices.forEachIndexed { idx, slice ->
                            MacroPieLegendRow(
                                slice = slice,
                                isSelected = breakdown.hasIntake && idx == selectedSliceIndex,
                                byCalories = byCalories,
                                onClick = { selectedSliceIndex = idx },
                                modifier = Modifier.testTag("macro_pie_slice_${slice.key}")
                            )
                        }
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pieGraphic(Modifier.width(200.dp))
                        legendColumn(Modifier.weight(1f))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        pieGraphic(Modifier.fillMaxWidth())
                        legendColumn(Modifier.fillMaxWidth())
                    }
                }
            }

            // Selected Macro Insight Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = selectedSlice.color.copy(alpha = 0.11f),
                border = BorderStroke(1.dp, selectedSlice.color.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(selectedSlice.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (breakdown.hasIntake) {
                                "${selectedSlice.label}: ${selectedSlice.percentage}% (${selectedSlice.grams.roundToInt()}g • ${selectedSlice.kcal.roundToInt()} kcal)"
                            } else {
                                "Recommended balance: ~50% Carbs • 25% Protein • 25% Fats"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (breakdown.hasIntake) {
                        Text(
                            text = "Goal ${selectedSlice.targetGrams}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = selectedSlice.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPieLegendRow(
    slice: MacroPieSlice,
    isSelected: Boolean,
    byCalories: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            slice.color.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.4.dp else 1.dp,
            color = if (isSelected) slice.color.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (byCalories) {
                            "${slice.grams.roundToInt()}g (${slice.kcal.roundToInt()} kcal)"
                        } else {
                            "${slice.grams.roundToInt()}g / ${slice.targetGrams}g"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = slice.color.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${slice.percentage}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = slice.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (slice.percentage / 100f).coerceIn(0f, 1f) },
                color = slice.color,
                trackColor = slice.color.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}
