package com.example.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.core.export.VisualReportPeriod
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccentStrong
import com.example.ui.theme.ProteinColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Aggregated nutrition data point for a single day in the 7-day weekly trend chart.
 */
data class DailyNutritionTrendPoint(
    val dateKey: String,
    val dayShortLabel: String,
    val fullDateLabel: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val mealCount: Int,
    val isToday: Boolean
)

/**
 * Weekly adherence and goal-comparison summary derived from 7 daily trend points.
 */
data class WeeklyGoalComparisonSummary(
    val daysLogged: Int,
    val daysOnTarget: Int,
    val daysOverGoal: Int,
    val avgCalories: Int,
    val avgProteinG: Int,
    val avgCarbsG: Int,
    val avgFatG: Int,
    val totalWeeklyCalories: Int,
    val weeklyGoalCalories: Int,
    val deltaFromDailyGoal: Int,
    val weeklyBudgetPercent: Int
)

fun computeWeeklyGoalComparisonSummary(
    weeklyPoints: List<DailyNutritionTrendPoint>,
    dailyCalorieGoal: Int
): WeeklyGoalComparisonSummary {
    val safeGoal = dailyCalorieGoal.coerceAtLeast(800)
    val activeDays = weeklyPoints.filter { it.calories > 0 }
    val divisor = activeDays.size.coerceAtLeast(1)
    val avgCalories = (activeDays.sumOf { it.calories }.toDouble() / divisor).roundToInt()
    val avgProteinG = (activeDays.sumOf { it.proteinG } / divisor).roundToInt()
    val avgCarbsG = (activeDays.sumOf { it.carbsG } / divisor).roundToInt()
    val avgFatG = (activeDays.sumOf { it.fatG } / divisor).roundToInt()

    val lowerBound = (safeGoal * 0.80).roundToInt()
    val upperBound = (safeGoal * 1.10).roundToInt()
    val daysOnTarget = activeDays.count { it.calories in lowerBound..upperBound }
    val daysOverGoal = activeDays.count { it.calories > upperBound }

    val totalWeeklyCalories = weeklyPoints.sumOf { it.calories }
    val weeklyGoalCalories = safeGoal * weeklyPoints.size.coerceAtLeast(7)
    val weeklyBudgetPercent = if (weeklyGoalCalories > 0) {
        ((totalWeeklyCalories.toDouble() / weeklyGoalCalories.toDouble()) * 100.0).roundToInt()
    } else 0

    return WeeklyGoalComparisonSummary(
        daysLogged = activeDays.size,
        daysOnTarget = daysOnTarget,
        daysOverGoal = daysOverGoal,
        avgCalories = avgCalories,
        avgProteinG = avgProteinG,
        avgCarbsG = avgCarbsG,
        avgFatG = avgFatG,
        totalWeeklyCalories = totalWeeklyCalories,
        weeklyGoalCalories = weeklyGoalCalories,
        deltaFromDailyGoal = if (activeDays.isEmpty()) 0 else avgCalories - safeGoal,
        weeklyBudgetPercent = weeklyBudgetPercent
    )
}

/**
 * Computes the last 7 chronological days (ending Today) of calorie and macronutrient intake
 * from the user's FoodDiaryEntry list.
 */
fun buildWeeklyNutritionTrendPoints(
    diaryEntries: List<FoodDiaryEntry>
): List<DailyNutritionTrendPoint> {
    val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFormatter = SimpleDateFormat("EEE", Locale.US)
    val fullFormatter = SimpleDateFormat("EEE, MMM d", Locale.US)
    val entriesByDate = diaryEntries.groupBy { it.dateKey }

    return (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val dateKey = keyFormatter.format(cal.time)
        val dayEntries = entriesByDate[dateKey].orEmpty()

        DailyNutritionTrendPoint(
            dateKey = dateKey,
            dayShortLabel = dayFormatter.format(cal.time),
            fullDateLabel = when (daysAgo) {
                0 -> "Today (${fullFormatter.format(cal.time)})"
                1 -> "Yesterday (${fullFormatter.format(cal.time)})"
                else -> fullFormatter.format(cal.time)
            },
            calories = dayEntries.sumOf { it.calories },
            proteinG = dayEntries.sumOf { it.protein },
            carbsG = dayEntries.sumOf { it.carbohydrates },
            fatG = dayEntries.sumOf { it.fat },
            fiberG = dayEntries.sumOf { it.fiber },
            mealCount = dayEntries.size,
            isToday = daysAgo == 0
        )
    }
}

private fun formatAxisTick(value: Float): String {
    val intVal = value.roundToInt()
    return when {
        intVal >= 1000 -> {
            val k = intVal / 1000f
            if ((intVal % 1000) == 0) "${intVal / 1000}k"
            else String.format(Locale.US, "%.1fk", k)
        }
        else -> "$intVal"
    }
}

/**
 * Interactive Weekly Calorie & Macronutrient Intake vs Recommended Goal Visualization Card.
 * Built natively with Jetpack Compose Canvas & TextMeasurer:
 *  0 = Grouped Bar Chart (Daily Caloric Intake vs Recommended Goal) + Goal Reference Line + Trend Curve
 *  1 = Multi-Line Macronutrient Spline Area Chart (Protein, Carbs, Fat in grams)
 *  2 = Stacked Macronutrient Energy Bar Chart (P/C/F kcal contribution vs Goal)
 */
@Composable
fun WeeklyCalorieAndMacroTrendsChartCard(
    diaryEntries: List<FoodDiaryEntry>,
    profile: UserProfile,
    waterRecords: List<WaterRecord> = emptyList(),
    weightRecords: List<WeightRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    val weeklyPoints = remember(diaryEntries) {
        buildWeeklyNutritionTrendPoints(diaryEntries)
    }

    var selectedChartMode by remember { mutableIntStateOf(0) } // 0 = Intake vs Goal, 1 = Macros (g), 2 = Macro Split
    var selectedDayIndex by remember { mutableIntStateOf(weeklyPoints.lastIndex.coerceAtLeast(0)) }
    var showExportWeeklyPdfModal by remember { mutableStateOf(false) }
    var showExportWeeklyVisualModal by remember { mutableStateOf(false) }

    val targetCalories = profile.dailyCalorieGoal.coerceAtLeast(800)
    val targetProteinG = profile.effectiveProteinGoalG()
    val targetCarbsG = profile.effectiveCarbsGoalG()
    val targetFatG = profile.effectiveFatGoalG()

    val summary = remember(weeklyPoints, targetCalories) {
        computeWeeklyGoalComparisonSummary(weeklyPoints, targetCalories)
    }

    val selectedPoint = weeklyPoints.getOrElse(selectedDayIndex) { weeklyPoints.last() }
    val selectedDelta = selectedPoint.calories - targetCalories
    val overGoalColor = Color(0xFFF97316)
    val goalBarColor = Color(0xFF94A3B8)

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(700),
        label = "weekly_chart_anim"
    )
    val textMeasurer = rememberTextMeasurer()

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val selectedSurfaceColor = MaterialTheme.colorScheme.primaryContainer
    val axisLabelStyle = TextStyle(
        color = onSurfaceVariantColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_calorie_intake_trend_chart")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row + Quick Goal Adherence Pill
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
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Weekly Intake vs. Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "7-Day Caloric Trend • Tap any bar to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showExportWeeklyPdfModal = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("weekly_chart_export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export Weekly PDF Summary Report",
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PDF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary
                    )
                }
            }

            // 1B. Weekly KPI Summary Strip (Avg vs Goal, On-Target Days, Over/Under Delta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyKpiMiniCard(
                    title = "Daily Avg",
                    value = "${summary.avgCalories} kcal",
                    subtitle = "Goal $targetCalories",
                    accentColor = FreshGreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                WeeklyKpiMiniCard(
                    title = "On Target",
                    value = "${summary.daysOnTarget}/${weeklyPoints.size}",
                    subtitle = "${summary.daysLogged} active days",
                    accentColor = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f)
                )
                val deltaSign = if (summary.deltaFromDailyGoal > 0) "+" else ""
                val deltaAccent = if (summary.deltaFromDailyGoal > (targetCalories * 0.1).roundToInt()) {
                    overGoalColor
                } else {
                    FreshGreenPrimary
                }
                WeeklyKpiMiniCard(
                    title = "Avg Delta",
                    value = if (summary.daysLogged == 0) "—" else "$deltaSign${summary.deltaFromDailyGoal}",
                    subtitle = if (summary.daysLogged == 0) "No logs yet" else "${summary.weeklyBudgetPercent}% wk budget",
                    accentColor = deltaAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Chart Mode Switcher Pills
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        0 to "Intake vs Goal",
                        1 to "Macros (g)",
                        2 to "Macro Split"
                    ).forEach { (modeIdx, label) ->
                        val isSelected = selectedChartMode == modeIdx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) FreshGreenPrimary else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedChartMode = modeIdx }
                                .testTag("weekly_chart_mode_$modeIdx")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedChartMode == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChartLegendDot(label = "On/Under Goal", color = FreshGreenPrimary)
                        ChartLegendDot(label = "Over Goal", color = overGoalColor)
                        ChartLegendDot(label = "Rec. Goal", color = goalBarColor)
                        ChartLegendDot(label = "Trend", color = onSurfaceColor)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChartLegendDot(label = "Protein (${targetProteinG}g)", color = ProteinColor)
                        ChartLegendDot(label = "Carbs (${targetCarbsG}g)", color = CarbsColor)
                        ChartLegendDot(label = "Fat (${targetFatG}g)", color = FatColor)
                    }
                }
            }

            // 4. Interactive Y-Axis + Bar/Trend Canvas Chart Area + Day Labels
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    val leftAxisWidthDp = 38.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .testTag("weekly_trends_canvas")
                            .pointerInput(weeklyPoints) {
                                val axisPx = leftAxisWidthDp.toPx()
                                detectTapGestures { tapOffset ->
                                    val plotWidth = (size.width - axisPx).coerceAtLeast(1f)
                                    val xInPlot = (tapOffset.x - axisPx).coerceIn(0f, plotWidth - 1f)
                                    val slotWidth = plotWidth / weeklyPoints.size.coerceAtLeast(1).toFloat()
                                    val tappedIdx = (xInPlot / slotWidth).toInt()
                                        .coerceIn(0, weeklyPoints.lastIndex)
                                    selectedDayIndex = tappedIdx
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val axisWidthPx = leftAxisWidthDp.toPx()
                            val plotLeft = axisWidthPx
                            val plotWidth = (size.width - plotLeft).coerceAtLeast(40f)
                            val topPadding = 16.dp.toPx()
                            val chartBottom = size.height - 8.dp.toPx()
                            val usableHeight = (chartBottom - topPadding).coerceAtLeast(20f)
                            val count = weeklyPoints.size.coerceAtLeast(1)
                            val slotWidth = plotWidth / count.toFloat()

                            // Helper to draw horizontal gridlines + Y-axis labels
                            fun drawYAxisGrid(maxVal: Float, unitSuffix: String = "") {
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                listOf(0f, 0.33f, 0.66f, 1.0f).forEach { frac ->
                                    val y = chartBottom - (frac * usableHeight)
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(plotLeft, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = if (frac == 0f) null else dashEffect
                                    )
                                    val tickVal = maxVal * frac
                                    val labelText = formatAxisTick(tickVal) + unitSuffix
                                    val measured = textMeasurer.measure(labelText, style = axisLabelStyle)
                                    drawText(
                                        textLayoutResult = measured,
                                        topLeft = Offset(
                                            x = (axisWidthPx - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                                            y = (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height)
                                        )
                                    )
                                }
                            }

                            when (selectedChartMode) {
                                0 -> {
                                    // MODE 0: Grouped Bar Chart (Daily Intake vs Recommended Goal) + Goal Reference Line + Trend Spline
                                    val maxDataCal = weeklyPoints.maxOfOrNull { it.calories } ?: 0
                                    val rawMax = maxOf(targetCalories * 1.25f, maxDataCal * 1.15f, 1200f)
                                    // Round max up to nearest 500 for clean Y-axis ticks
                                    val maxCal = (((rawMax + 499f) / 500f).toInt() * 500f).coerceAtLeast(1500f)

                                    drawYAxisGrid(maxCal)

                                    // Dashed Recommended Goal Reference Line across the plot
                                    val goalY = chartBottom - ((targetCalories / maxCal).coerceIn(0f, 1f) * usableHeight)
                                    drawLine(
                                        color = overGoalColor,
                                        start = Offset(plotLeft, goalY),
                                        end = Offset(size.width, goalY),
                                        strokeWidth = 1.8.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                                    )

                                    val intakeBarWidth = (slotWidth * 0.34f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                                    val goalBarWidth = (slotWidth * 0.22f).coerceIn(6.dp.toPx(), 12.dp.toPx())
                                    val pairGap = 3.dp.toPx()
                                    val trendOffsets = mutableListOf<Offset>()

                                    weeklyPoints.forEachIndexed { idx, pt ->
                                        val slotStart = plotLeft + idx * slotWidth
                                        val slotCenterX = slotStart + slotWidth * 0.5f

                                        // Selected day highlight column
                                        if (idx == selectedDayIndex) {
                                            drawRoundRect(
                                                color = FreshGreenPrimary.copy(alpha = 0.11f),
                                                topLeft = Offset(slotStart + 2.dp.toPx(), 4.dp.toPx()),
                                                size = Size(slotWidth - 4.dp.toPx(), size.height - 6.dp.toPx()),
                                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                            )
                                        }

                                        // Right bar of group: Recommended Daily Goal reference bar
                                        val goalBarHeight = ((targetCalories / maxCal) * animProgress).coerceIn(0.05f, 1f) * usableHeight
                                        val goalBarLeft = slotCenterX + pairGap / 2f
                                        drawRoundRect(
                                            color = goalBarColor.copy(alpha = if (idx == selectedDayIndex) 0.42f else 0.26f),
                                            topLeft = Offset(goalBarLeft, chartBottom - goalBarHeight),
                                            size = Size(goalBarWidth, goalBarHeight),
                                            cornerRadius = CornerRadius(goalBarWidth / 2f, goalBarWidth / 2f)
                                        )

                                        // Left bar of group: Actual Caloric Intake bar
                                        val intakeRatio = if (pt.calories > 0) {
                                            ((pt.calories.toFloat() / maxCal) * animProgress).coerceIn(0.04f, 1f)
                                        } else {
                                            0.025f
                                        }
                                        val intakeBarHeight = intakeRatio * usableHeight
                                        val intakeTopY = chartBottom - intakeBarHeight
                                        val intakeBarLeft = slotCenterX - pairGap / 2f - intakeBarWidth

                                        trendOffsets.add(Offset(intakeBarLeft + intakeBarWidth / 2f, intakeTopY))

                                        val isOverGoal = pt.calories > (targetCalories * 1.10)
                                        val intakeBrush = when {
                                            pt.calories == 0 -> Brush.verticalGradient(
                                                colors = listOf(gridLineColor, gridLineColor),
                                                startY = intakeTopY,
                                                endY = chartBottom
                                            )
                                            isOverGoal -> Brush.verticalGradient(
                                                colors = listOf(Color(0xFFF59E0B), overGoalColor),
                                                startY = intakeTopY,
                                                endY = chartBottom
                                            )
                                            idx == selectedDayIndex -> Brush.verticalGradient(
                                                colors = listOf(LimeAccentStrong, FreshGreenPrimary),
                                                startY = intakeTopY,
                                                endY = chartBottom
                                            )
                                            else -> Brush.verticalGradient(
                                                colors = listOf(FreshGreenPrimary, FreshGreenPrimary.copy(alpha = 0.78f)),
                                                startY = intakeTopY,
                                                endY = chartBottom
                                            )
                                        }

                                        drawRoundRect(
                                            brush = intakeBrush,
                                            topLeft = Offset(intakeBarLeft, intakeTopY),
                                            size = Size(intakeBarWidth, intakeBarHeight),
                                            cornerRadius = CornerRadius(intakeBarWidth / 2f, intakeBarWidth / 2f)
                                        )
                                    }

                                    // Smooth Cubic Trend Curve connecting daily intake tops
                                    if (trendOffsets.size >= 2) {
                                        val curvePath = buildSmoothCurvePath(trendOffsets)
                                        drawPath(
                                            path = curvePath,
                                            color = onSurfaceColor.copy(alpha = 0.75f),
                                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        trendOffsets.forEachIndexed { idx, ptOffset ->
                                            val isSelected = idx == selectedDayIndex
                                            val ptOverGoal = weeklyPoints[idx].calories > (targetCalories * 1.10)
                                            drawCircle(
                                                color = Color.White,
                                                radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                                center = ptOffset
                                            )
                                            drawCircle(
                                                color = when {
                                                    ptOverGoal -> overGoalColor
                                                    isSelected -> FreshGreenPrimary
                                                    else -> onSurfaceColor
                                                },
                                                radius = if (isSelected) 3.8.dp.toPx() else 2.2.dp.toPx(),
                                                center = ptOffset
                                            )
                                        }
                                    }

                                    // Floating "Goal XXXX" tag on the Goal Reference Line
                                    val goalTagText = "Goal $targetCalories"
                                    val goalTagMeasured = textMeasurer.measure(
                                        goalTagText,
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    val padH = 5.dp.toPx()
                                    val padV = 2.dp.toPx()
                                    val pillW = goalTagMeasured.size.width + padH * 2
                                    val pillH = goalTagMeasured.size.height + padV * 2
                                    val pillX = (size.width - pillW - 4.dp.toPx()).coerceAtLeast(plotLeft)
                                    val pillY = (goalY - pillH - 3.dp.toPx()).coerceIn(2.dp.toPx(), chartBottom - pillH)
                                    drawRoundRect(
                                        color = overGoalColor,
                                        topLeft = Offset(pillX, pillY),
                                        size = Size(pillW, pillH),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                    drawText(
                                        textLayoutResult = goalTagMeasured,
                                        topLeft = Offset(pillX + padH, pillY + padV)
                                    )
                                }

                                1 -> {
                                    // MODE 1: Multi-Line Macronutrient Spline Chart (Protein, Carbs, Fat in grams)
                                    val rawMaxGram = maxOf(
                                        targetCarbsG * 1.2f,
                                        targetProteinG * 1.2f,
                                        weeklyPoints.maxOfOrNull { maxOf(it.carbsG, it.proteinG, it.fatG) }?.toFloat() ?: 250f,
                                        150f
                                    )
                                    val maxGram = (((rawMaxGram + 49f) / 50f).toInt() * 50f).coerceAtLeast(150f)

                                    drawYAxisGrid(maxGram, "g")

                                    val proteinPoints = mutableListOf<Offset>()
                                    val carbsPoints = mutableListOf<Offset>()
                                    val fatPoints = mutableListOf<Offset>()

                                    weeklyPoints.forEachIndexed { idx, pt ->
                                        val slotStart = plotLeft + idx * slotWidth
                                        val centerX = slotStart + slotWidth * 0.5f
                                        if (idx == selectedDayIndex) {
                                            drawRoundRect(
                                                color = FreshGreenPrimary.copy(alpha = 0.11f),
                                                topLeft = Offset(slotStart + 2.dp.toPx(), 4.dp.toPx()),
                                                size = Size(slotWidth - 4.dp.toPx(), size.height - 6.dp.toPx()),
                                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                            )
                                        }
                                        val pY = chartBottom - (((pt.proteinG.toFloat() / maxGram) * animProgress).coerceIn(0.03f, 1f) * usableHeight)
                                        val cY = chartBottom - (((pt.carbsG.toFloat() / maxGram) * animProgress).coerceIn(0.03f, 1f) * usableHeight)
                                        val fY = chartBottom - (((pt.fatG.toFloat() / maxGram) * animProgress).coerceIn(0.03f, 1f) * usableHeight)

                                        proteinPoints.add(Offset(centerX, pY))
                                        carbsPoints.add(Offset(centerX, cY))
                                        fatPoints.add(Offset(centerX, fY))
                                    }

                                    listOf(
                                        carbsPoints to CarbsColor,
                                        proteinPoints to ProteinColor,
                                        fatPoints to FatColor
                                    ).forEach { (pts, lineColor) ->
                                        if (pts.size >= 2) {
                                            val linePath = buildSmoothCurvePath(pts)
                                            val fillPath = Path().apply {
                                                addPath(linePath)
                                                lineTo(pts.last().x, chartBottom)
                                                lineTo(pts.first().x, chartBottom)
                                                close()
                                            }
                                            drawPath(
                                                path = fillPath,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(lineColor.copy(alpha = 0.20f), Color.Transparent),
                                                    startY = topPadding,
                                                    endY = chartBottom
                                                )
                                            )
                                            drawPath(
                                                path = linePath,
                                                color = lineColor,
                                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            pts.forEachIndexed { idx, offset ->
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = if (idx == selectedDayIndex) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                                    center = offset
                                                )
                                                drawCircle(
                                                    color = lineColor,
                                                    radius = if (idx == selectedDayIndex) 3.8.dp.toPx() else 2.2.dp.toPx(),
                                                    center = offset
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    // MODE 2: Stacked Macronutrient Energy Bar Chart vs Goal Line
                                    val rawMaxEnergy = maxOf(
                                        targetCalories * 1.25f,
                                        weeklyPoints.maxOfOrNull {
                                            ((it.proteinG * 4) + (it.carbsG * 4) + (it.fatG * 9)).toFloat()
                                        } ?: targetCalories.toFloat(),
                                        1200f
                                    )
                                    val maxEnergy = (((rawMaxEnergy + 499f) / 500f).toInt() * 500f).coerceAtLeast(1500f)

                                    drawYAxisGrid(maxEnergy)

                                    val goalY = chartBottom - ((targetCalories / maxEnergy).coerceIn(0f, 1f) * usableHeight)
                                    drawLine(
                                        color = overGoalColor,
                                        start = Offset(plotLeft, goalY),
                                        end = Offset(size.width, goalY),
                                        strokeWidth = 1.6.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                                    )

                                    val barWidth = (slotWidth * 0.44f).coerceIn(10.dp.toPx(), 26.dp.toPx())

                                    weeklyPoints.forEachIndexed { idx, pt ->
                                        val slotStart = plotLeft + idx * slotWidth
                                        val centerX = slotStart + slotWidth * 0.5f
                                        if (idx == selectedDayIndex) {
                                            drawRoundRect(
                                                color = FreshGreenPrimary.copy(alpha = 0.11f),
                                                topLeft = Offset(slotStart + 2.dp.toPx(), 4.dp.toPx()),
                                                size = Size(slotWidth - 4.dp.toPx(), size.height - 6.dp.toPx()),
                                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                            )
                                        }

                                        val pKcal = (pt.proteinG * 4.0).toFloat()
                                        val cKcal = (pt.carbsG * 4.0).toFloat()
                                        val fKcal = (pt.fatG * 9.0).toFloat()
                                        val totalMacroKcal = (pKcal + cKcal + fKcal).coerceAtLeast(1f)

                                        val totalBarHeight = if (pt.calories > 0) {
                                            ((totalMacroKcal / maxEnergy) * animProgress).coerceIn(0.06f, 1f) * usableHeight
                                        } else {
                                            0.03f * usableHeight
                                        }

                                        val pHeight = totalBarHeight * (pKcal / totalMacroKcal)
                                        val cHeight = totalBarHeight * (cKcal / totalMacroKcal)
                                        val fHeight = totalBarHeight * (fKcal / totalMacroKcal)

                                        var currentBottom = chartBottom

                                        drawRect(
                                            color = ProteinColor,
                                            topLeft = Offset(centerX - barWidth / 2f, currentBottom - pHeight),
                                            size = Size(barWidth, pHeight.coerceAtLeast(2f))
                                        )
                                        currentBottom -= pHeight

                                        drawRect(
                                            color = CarbsColor,
                                            topLeft = Offset(centerX - barWidth / 2f, currentBottom - cHeight),
                                            size = Size(barWidth, cHeight.coerceAtLeast(2f))
                                        )
                                        currentBottom -= cHeight

                                        drawRoundRect(
                                            color = FatColor,
                                            topLeft = Offset(centerX - barWidth / 2f, currentBottom - fHeight),
                                            size = Size(barWidth, fHeight.coerceAtLeast(2f)),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 7-Day X-Axis Interactive Day Selector Labels (aligned under plot area)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = leftAxisWidthDp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weeklyPoints.forEachIndexed { idx, pt ->
                            val isSelected = idx == selectedDayIndex
                            val isOver = pt.calories > (targetCalories * 1.10)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) selectedSurfaceColor else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, FreshGreenPrimary.copy(alpha = 0.45f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedDayIndex = idx }
                                    .testTag("weekly_chart_day_${pt.dayShortLabel.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        text = pt.dayShortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected || pt.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = "${pt.calories}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            pt.calories == 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            isOver -> overGoalColor
                                            else -> FreshGreenPrimary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Selected Day Interactive Tooltip Breakdown Card (Dark-Mode Safe)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, FreshGreenPrimary.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_chart_selected_day_tooltip")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPoint.fullDateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Variance badge vs Recommended Goal
                        val isOver = selectedDelta > 0
                        val varianceLabel = when {
                            selectedPoint.calories == 0 -> "0 / $targetCalories kcal"
                            isOver -> "+$selectedDelta vs goal"
                            selectedDelta < 0 -> "-${abs(selectedDelta)} under goal"
                            else -> "Exact goal"
                        }
                        val badgeColor = when {
                            selectedPoint.calories == 0 -> MaterialTheme.colorScheme.surfaceVariant
                            selectedDelta > (targetCalories * 0.1) -> overGoalColor.copy(alpha = 0.16f)
                            else -> FreshGreenPrimary.copy(alpha = 0.15f)
                        }
                        val badgeTextColor = when {
                            selectedPoint.calories == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            selectedDelta > (targetCalories * 0.1) -> overGoalColor
                            else -> FreshGreenPrimary
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = badgeColor
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (selectedPoint.calories > 0) {
                                    Icon(
                                        imageVector = if (isOver) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = badgeTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = varianceLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeTextColor
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TooltipMacroStat(
                            label = "Intake",
                            valueText = "${selectedPoint.calories} kcal",
                            subText = "Goal $targetCalories",
                            color = FreshGreenPrimary
                        )
                        TooltipMacroStat(
                            label = "Protein",
                            valueText = "${selectedPoint.proteinG.roundToInt()}g / ${targetProteinG}g",
                            subText = "${((selectedPoint.proteinG / targetProteinG.coerceAtLeast(1)) * 100).roundToInt()}%",
                            color = ProteinColor
                        )
                        TooltipMacroStat(
                            label = "Carbs",
                            valueText = "${selectedPoint.carbsG.roundToInt()}g / ${targetCarbsG}g",
                            subText = "${((selectedPoint.carbsG / targetCarbsG.coerceAtLeast(1)) * 100).roundToInt()}%",
                            color = CarbsColor
                        )
                        TooltipMacroStat(
                            label = "Fat",
                            valueText = "${selectedPoint.fatG.roundToInt()}g / ${targetFatG}g",
                            subText = "${((selectedPoint.fatG / targetFatG.coerceAtLeast(1)) * 100).roundToInt()}%",
                            color = FatColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            // 6. 7-Day Weekly Average Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "7-Day Daily Avg:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${summary.avgCalories} kcal • P:${summary.avgProteinG}g • C:${summary.avgCarbsG}g • F:${summary.avgFatG}g",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = FreshGreenPrimary
                )
            }

            // 7. Direct Export Weekly Nutrition Visual Summary & PDF Report Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showExportWeeklyVisualModal = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_weekly_visual_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Visual Summary",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { showExportWeeklyPdfModal = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_weekly_nutrition_pdf_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF Report",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FreshGreenPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showExportWeeklyVisualModal) {
        NutritionVisualSummaryExportDialog(
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            initialPeriod = VisualReportPeriod.WEEKLY,
            onDismiss = { showExportWeeklyVisualModal = false }
        )
    }

    if (showExportWeeklyPdfModal) {
        NutritionPdfReportExportDialog(
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            onDismiss = { showExportWeeklyPdfModal = false }
        )
    }
}

private fun buildSmoothCurvePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val current = points[i]
        val next = points[i + 1]
        val controlX1 = (current.x + next.x) / 2f
        val controlY1 = current.y
        val controlX2 = (current.x + next.x) / 2f
        val controlY2 = next.y
        path.cubicTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y)
    }
    return path
}

@Composable
private fun WeeklyKpiMiniCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartLegendDot(
    label: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TooltipMacroStat(
    label: String,
    valueText: String,
    subText: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
