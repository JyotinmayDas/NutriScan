package com.example.core.export

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.todayDateKey
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class VisualReportPeriod(
    val label: String,
    val badgeTitle: String
) {
    DAILY("Daily", "DAILY NUTRITION SUMMARY"),
    WEEKLY("Weekly (7 Days)", "WEEKLY NUTRITION SUMMARY")
}

enum class VisualReportTheme(
    val label: String,
    val bgStartHex: Int,
    val bgEndHex: Int,
    val cardHex: Int,
    val subCardHex: Int,
    val primaryTextHex: Int,
    val secondaryTextHex: Int,
    val accentHex: Int,
    val accentSoftHex: Int,
    val borderHex: Int
) {
    FRESH_LIGHT(
        label = "Fresh White",
        bgStartHex = 0xFFF4FAF6.toInt(),
        bgEndHex = 0xFFE6F5EB.toInt(),
        cardHex = 0xFFFFFFFF.toInt(),
        subCardHex = 0xFFF1F5F9.toInt(),
        primaryTextHex = 0xFF0F172A.toInt(),
        secondaryTextHex = 0xFF475569.toInt(),
        accentHex = 0xFF129A1B.toInt(),
        accentSoftHex = 0xFFDCFCE7.toInt(),
        borderHex = 0xFFBBF7D0.toInt()
    ),
    SOFT_MINT(
        label = "Soft Mint",
        bgStartHex = 0xFFDCFAE0.toInt(),
        bgEndHex = 0xFFE9FCEB.toInt(),
        cardHex = 0xFFFFFFFF.toInt(),
        subCardHex = 0xFFF0FDF4.toInt(),
        primaryTextHex = 0xFF0A2E12.toInt(),
        secondaryTextHex = 0xFF335C3B.toInt(),
        accentHex = 0xFF129A1B.toInt(),
        accentSoftHex = 0xFFBBF7D0.toInt(),
        borderHex = 0xFF86EFAC.toInt()
    ),
    EMERALD_DARK(
        label = "Emerald Night",
        bgStartHex = 0xFF0A1D12.toInt(),
        bgEndHex = 0xFF123320.toInt(),
        cardHex = 0xFF163D27.toInt(),
        subCardHex = 0xFF1F4F34.toInt(),
        primaryTextHex = 0xFFF8FAFC.toInt(),
        secondaryTextHex = 0xFFB7DFC6.toInt(),
        accentHex = 0xFF4ED122.toInt(),
        accentSoftHex = 0xFF215E3B.toInt(),
        borderHex = 0xFF2E6B47.toInt()
    )
}

data class MealSlotSummary(
    val mealType: String,
    val calories: Int,
    val itemCount: Int
)

data class WeeklyDayBarSummary(
    val dateKey: String,
    val dayLabel: String,
    val calories: Int,
    val mealCount: Int,
    val metGoal: Boolean
)

data class NutritionVisualReportData(
    val period: VisualReportPeriod,
    val userName: String,
    val anchorDateKey: String,
    val dateRangeLabel: String,
    val calorieGoal: Int,
    val caloriesDisplay: Int,
    val weeklyTotalCalories: Int,
    val calorieProgressPct: Int,
    val proteinG: Int,
    val proteinGoalG: Int,
    val proteinPct: Int,
    val carbsG: Int,
    val carbsGoalG: Int,
    val carbsPct: Int,
    val fatG: Int,
    val fatGoalG: Int,
    val fatPct: Int,
    val fiberG: Int,
    val fiberGoalG: Int,
    val fiberPct: Int,
    val waterMl: Int,
    val waterGoalMl: Int,
    val totalMealsLogged: Int,
    val activeDaysCount: Int,
    val mealBreakdown: List<MealSlotSummary>,
    val weeklyBars: List<WeeklyDayBarSummary>,
    val insightText: String
)

object NutritionVisualReportExporter {

    private val STANDARD_MEAL_SLOTS = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    fun buildVisualReportData(
        profile: UserProfile,
        diaryEntries: List<FoodDiaryEntry>,
        waterRecords: List<WaterRecord> = emptyList(),
        period: VisualReportPeriod,
        anchorDateKey: String = todayDateKey()
    ): NutritionVisualReportData {
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val dayShortFormat = SimpleDateFormat("EEE", Locale.US)
        val dailyDisplayFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
        val shortRangeFormat = SimpleDateFormat("MMM d", Locale.US)
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)

        val anchorDate = runCatching { keyFormat.parse(anchorDateKey) }.getOrNull() ?: Date()
        val resolvedAnchorKey = keyFormat.format(anchorDate)

        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val targetPro = profile.effectiveProteinGoalG().coerceAtLeast(20)
        val targetCarbs = profile.effectiveCarbsGoalG().coerceAtLeast(30)
        val targetFat = profile.effectiveFatGoalG().coerceAtLeast(15)
        val targetFiber = profile.effectiveFiberGoalG().coerceAtLeast(10)
        val targetWater = profile.dailyWaterGoalMl.coerceAtLeast(500)

        val userName = profile.name.ifBlank { "NutriScan Member" }

        return if (period == VisualReportPeriod.DAILY) {
            val dayEntries = diaryEntries.filter { it.dateKey == resolvedAnchorKey }
            val dayWaterMl = waterRecords.filter { it.dateKey == resolvedAnchorKey }.sumOf { it.quantity }

            val totalCal = dayEntries.sumOf { it.calories }
            val proG = dayEntries.sumOf { it.protein }.roundToInt()
            val carbsG = dayEntries.sumOf { it.carbohydrates }.roundToInt()
            val fatG = dayEntries.sumOf { it.fat }.roundToInt()
            val fiberG = dayEntries.sumOf { it.fiber }.roundToInt()

            val calPct = ((totalCal.toDouble() / targetCal) * 100.0).roundToInt().coerceIn(0, 200)
            val proPct = ((proG.toDouble() / targetPro) * 100.0).roundToInt().coerceIn(0, 200)
            val carbsPct = ((carbsG.toDouble() / targetCarbs) * 100.0).roundToInt().coerceIn(0, 200)
            val fatPct = ((fatG.toDouble() / targetFat) * 100.0).roundToInt().coerceIn(0, 200)
            val fiberPct = ((fiberG.toDouble() / targetFiber) * 100.0).roundToInt().coerceIn(0, 200)

            val meals = buildMealBreakdown(dayEntries)

            val insight = when {
                dayEntries.isEmpty() -> "No meals logged on this day yet."
                totalCal < targetCal -> "${targetCal - totalCal} kcal remaining • Protein at $proPct% of daily goal"
                totalCal <= (targetCal * 1.1).roundToInt() -> "Right on calorie target! ($calPct% of $targetCal kcal)"
                else -> "${totalCal - targetCal} kcal above daily target • ${dayEntries.size} meals logged"
            }

            NutritionVisualReportData(
                period = period,
                userName = userName,
                anchorDateKey = resolvedAnchorKey,
                dateRangeLabel = dailyDisplayFormat.format(anchorDate),
                calorieGoal = targetCal,
                caloriesDisplay = totalCal,
                weeklyTotalCalories = totalCal,
                calorieProgressPct = calPct,
                proteinG = proG,
                proteinGoalG = targetPro,
                proteinPct = proPct,
                carbsG = carbsG,
                carbsGoalG = targetCarbs,
                carbsPct = carbsPct,
                fatG = fatG,
                fatGoalG = targetFat,
                fatPct = fatPct,
                fiberG = fiberG,
                fiberGoalG = targetFiber,
                fiberPct = fiberPct,
                waterMl = dayWaterMl,
                waterGoalMl = targetWater,
                totalMealsLogged = dayEntries.size,
                activeDaysCount = if (dayEntries.isNotEmpty()) 1 else 0,
                mealBreakdown = meals,
                weeklyBars = emptyList(),
                insightText = insight
            )
        } else {
            val entriesByDate = diaryEntries.groupBy { it.dateKey }
            val waterByDate = waterRecords.groupBy { it.dateKey }

            val days = (6 downTo 0).map { offset ->
                val cal = Calendar.getInstance()
                cal.time = anchorDate
                cal.add(Calendar.DAY_OF_YEAR, -offset)
                val key = keyFormat.format(cal.time)
                val label = dayShortFormat.format(cal.time)
                val dayEntries = entriesByDate[key].orEmpty()
                val dayCals = dayEntries.sumOf { it.calories }
                val minGoal = (targetCal * 0.75).roundToInt()
                val maxGoal = (targetCal * 1.15).roundToInt()
                Triple(
                    cal.time,
                    dayEntries,
                    WeeklyDayBarSummary(
                        dateKey = key,
                        dayLabel = label,
                        calories = dayCals,
                        mealCount = dayEntries.size,
                        metGoal = dayCals in minGoal..maxGoal
                    )
                )
            }

            val weekEntries = days.flatMap { it.second }
            val bars = days.map { it.third }
            val activeDays = bars.count { it.mealCount > 0 }
            val divisor = activeDays.coerceAtLeast(1)

            val totalWeekCal = weekEntries.sumOf { it.calories }
            val avgCal = if (activeDays > 0) (totalWeekCal.toDouble() / divisor).roundToInt() else 0
            val avgPro = if (activeDays > 0) (weekEntries.sumOf { it.protein } / divisor).roundToInt() else 0
            val avgCarbs = if (activeDays > 0) (weekEntries.sumOf { it.carbohydrates } / divisor).roundToInt() else 0
            val avgFat = if (activeDays > 0) (weekEntries.sumOf { it.fat } / divisor).roundToInt() else 0
            val avgFiber = if (activeDays > 0) (weekEntries.sumOf { it.fiber } / divisor).roundToInt() else 0

            val waterDays = bars.map { bar ->
                waterByDate[bar.dateKey].orEmpty().sumOf { it.quantity }
            }.filter { it > 0 }
            val avgWater = if (waterDays.isNotEmpty()) {
                (waterDays.sum().toDouble() / waterDays.size).roundToInt()
            } else 0

            val calPct = ((avgCal.toDouble() / targetCal) * 100.0).roundToInt().coerceIn(0, 200)
            val proPct = ((avgPro.toDouble() / targetPro) * 100.0).roundToInt().coerceIn(0, 200)
            val carbsPct = ((avgCarbs.toDouble() / targetCarbs) * 100.0).roundToInt().coerceIn(0, 200)
            val fatPct = ((avgFat.toDouble() / targetFat) * 100.0).roundToInt().coerceIn(0, 200)
            val fiberPct = ((avgFiber.toDouble() / targetFiber) * 100.0).roundToInt().coerceIn(0, 200)

            val startDate = days.first().first
            val endDate = days.last().first
            val rangeStr = "${shortRangeFormat.format(startDate)} – ${shortRangeFormat.format(endDate)}, ${yearFormat.format(endDate)}"

            val meals = buildMealBreakdown(weekEntries)
            val onTargetDays = bars.count { it.metGoal }

            val insight = when {
                weekEntries.isEmpty() -> "No meals logged during this 7-day window yet."
                onTargetDays > 0 -> "$activeDays/7 days logged • $onTargetDays day(s) within calorie goal range"
                else -> "$activeDays/7 days logged • Averaging $avgCal kcal/day across ${weekEntries.size} meals"
            }

            NutritionVisualReportData(
                period = period,
                userName = userName,
                anchorDateKey = resolvedAnchorKey,
                dateRangeLabel = rangeStr,
                calorieGoal = targetCal,
                caloriesDisplay = avgCal,
                weeklyTotalCalories = totalWeekCal,
                calorieProgressPct = calPct,
                proteinG = avgPro,
                proteinGoalG = targetPro,
                proteinPct = proPct,
                carbsG = avgCarbs,
                carbsGoalG = targetCarbs,
                carbsPct = carbsPct,
                fatG = avgFat,
                fatGoalG = targetFat,
                fatPct = fatPct,
                fiberG = avgFiber,
                fiberGoalG = targetFiber,
                fiberPct = fiberPct,
                waterMl = avgWater,
                waterGoalMl = targetWater,
                totalMealsLogged = weekEntries.size,
                activeDaysCount = activeDays,
                mealBreakdown = meals,
                weeklyBars = bars,
                insightText = insight
            )
        }
    }

    private fun buildMealBreakdown(entries: List<FoodDiaryEntry>): List<MealSlotSummary> {
        return STANDARD_MEAL_SLOTS.map { slot ->
            val matching = entries.filter { it.mealType.equals(slot, ignoreCase = true) }
            MealSlotSummary(
                mealType = slot,
                calories = matching.sumOf { it.calories },
                itemCount = matching.size
            )
        }
    }

    /**
     * Renders a crisp 1080x1440 visual nutrition summary card bitmap.
     */
    fun renderVisualReportBitmap(
        data: NutritionVisualReportData,
        theme: VisualReportTheme
    ): Bitmap {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Soft gradient canvas background
        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            theme.bgStartHex,
            theme.bgEndHex,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Decorative subtle glow circles
        paint.color = theme.accentHex
        paint.alpha = 20
        canvas.drawCircle(width * 0.9f, height * 0.08f, 200f, paint)
        canvas.drawCircle(width * 0.08f, height * 0.92f, 220f, paint)
        paint.alpha = 255

        // 2. Main outer summary sheet
        val sheetMargin = 48f
        val sheetRect = RectF(sheetMargin, sheetMargin, width - sheetMargin, height - sheetMargin)
        paint.color = theme.cardHex
        canvas.drawRoundRect(sheetRect, 48f, 48f, paint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = theme.borderHex
        }
        canvas.drawRoundRect(sheetRect, 48f, 48f, borderPaint)

        val contentLeft = sheetMargin + 44f
        val contentRight = width - sheetMargin - 44f
        val contentWidth = contentRight - contentLeft

        // 3. Header Band
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 44f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 26f
        }

        // Pill badge for Period
        val pillRect = RectF(contentLeft, 96f, contentLeft + 410f, 142f)
        paint.color = theme.accentSoftHex
        canvas.drawRoundRect(pillRect, 24f, 24f, paint)
        canvas.drawText(data.period.badgeTitle, contentLeft + 22f, 128f, badgePaint)

        // Brand tag on top right
        val brandPill = RectF(contentRight - 210f, 96f, contentRight, 142f)
        paint.color = theme.accentHex
        canvas.drawRoundRect(brandPill, 24f, 24f, paint)
        val brandTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("NutriScan", brandPill.centerX(), 128f, brandTextPaint)

        canvas.drawText(data.userName.take(26), contentLeft, 202f, titlePaint)
        canvas.drawText(data.dateRangeLabel, contentLeft, 242f, subPaint)

        // Divider line
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.borderHex
            strokeWidth = 2f
        }
        canvas.drawLine(contentLeft, 272f, contentRight, 272f, dividerPaint)

        // 4. Hero Section (Daily Ring vs Weekly 7-Day Bar Chart)
        val heroTop = 298f
        val heroBottom = 640f
        val heroRect = RectF(contentLeft, heroTop, contentRight, heroBottom)
        paint.color = theme.subCardHex
        canvas.drawRoundRect(heroRect, 32f, 32f, paint)

        if (data.period == VisualReportPeriod.DAILY) {
            // Left: Circular Calorie Progress Ring
            val ringCenterX = contentLeft + 195f
            val ringCenterY = (heroTop + heroBottom) / 2f
            val ringRadius = 115f
            val ringRect = RectF(
                ringCenterX - ringRadius,
                ringCenterY - ringRadius,
                ringCenterX + ringRadius,
                ringCenterY + ringRadius
            )
            val trackArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 26f
                strokeCap = Paint.Cap.ROUND
                color = theme.borderHex
            }
            canvas.drawArc(ringRect, 0f, 360f, false, trackArc)

            val sweep = 360f * (data.calorieProgressPct / 100f).coerceIn(0.03f, 1f)
            val activeArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 26f
                strokeCap = Paint.Cap.ROUND
                color = theme.accentHex
            }
            canvas.drawArc(ringRect, -90f, sweep, false, activeArc)

            val ringNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.primaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 52f
                textAlign = Paint.Align.CENTER
            }
            val ringLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${data.caloriesDisplay}", ringCenterX, ringCenterY + 8f, ringNumPaint)
            canvas.drawText("of ${data.calorieGoal} kcal", ringCenterX, ringCenterY + 42f, ringLblPaint)

            // Right: 3 Clean Daily Metric Blocks
            val rightStartX = contentLeft + 380f
            val rightBoxW = contentRight - rightStartX - 28f
            val stats = listOf(
                Triple("GOAL ADHERENCE", "${data.calorieProgressPct}% of target", theme.accentHex),
                Triple("MEALS LOGGED", "${data.totalMealsLogged} entries today", 0xFFFF6200.toInt()),
                Triple("HYDRATION", "${data.waterMl} / ${data.waterGoalMl} ml", 0xFF0284C7.toInt())
            )
            stats.forEachIndexed { idx, (lbl, valStr, dotColor) ->
                val bTop = heroTop + 26f + idx * 98f
                val bRect = RectF(rightStartX, bTop, rightStartX + rightBoxW, bTop + 82f)
                paint.color = theme.cardHex
                canvas.drawRoundRect(bRect, 20f, 20f, paint)

                paint.color = dotColor
                canvas.drawCircle(rightStartX + 24f, bTop + 41f, 8f, paint)

                val sLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = theme.secondaryTextHex
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 18f
                }
                val sVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = theme.primaryTextHex
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 28f
                }
                canvas.drawText(lbl, rightStartX + 46f, bTop + 32f, sLbl)
                canvas.drawText(valStr, rightStartX + 46f, bTop + 65f, sVal)
            }
        } else {
            // WEEKLY HERO: Avg Daily Calories + 7-Day Bar Chart
            val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.primaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 30f
            }
            val subHdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f
            }
            canvas.drawText(
                "Avg ${data.caloriesDisplay} kcal/day",
                contentLeft + 28f,
                heroTop + 44f,
                hdrPaint
            )
            val rightMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 23f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                "Goal: ${data.calorieGoal} kcal • ${data.activeDaysCount}/7 days • ${data.totalMealsLogged} meals",
                contentRight - 28f,
                heroTop + 44f,
                rightMetaPaint
            )

            // 7-Day Bar Chart inside Hero Box
            val chartLeft = contentLeft + 32f
            val chartRight = contentRight - 32f
            val chartTop = heroTop + 80f
            val chartBottom = heroBottom - 56f
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(10f)

            val maxVal = maxOf(
                data.calorieGoal * 1.25f,
                (data.weeklyBars.maxOfOrNull { it.calories } ?: data.calorieGoal).toFloat(),
                1000f
            )

            // Goal dashed line
            val goalY = chartBottom - ((data.calorieGoal / maxVal).coerceIn(0f, 1f) * chartHeight)
            val goalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFF6200.toInt()
                strokeWidth = 3f
                pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }
            canvas.drawLine(chartLeft, goalY, chartRight, goalY, goalLinePaint)

            val bars = data.weeklyBars
            val slotW = (chartRight - chartLeft) / bars.size.coerceAtLeast(1)
            val barW = (slotW * 0.48f).coerceAtMost(56f)

            val dayLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            val barValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.primaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 19f
                textAlign = Paint.Align.CENTER
            }

            bars.forEachIndexed { idx, bar ->
                val cx = chartLeft + (idx + 0.5f) * slotW
                // Track
                val trackRect = RectF(cx - barW / 2f, chartTop, cx + barW / 2f, chartBottom)
                paint.color = theme.borderHex
                paint.alpha = 90
                canvas.drawRoundRect(trackRect, 14f, 14f, paint)
                paint.alpha = 255

                val barH = if (bar.calories > 0) {
                    ((bar.calories / maxVal).coerceIn(0.05f, 1f) * chartHeight)
                } else {
                    10f
                }
                val barRect = RectF(cx - barW / 2f, chartBottom - barH, cx + barW / 2f, chartBottom)
                paint.color = when {
                    bar.calories == 0 -> theme.borderHex
                    bar.metGoal -> theme.accentHex
                    else -> 0xFF28B822.toInt()
                }
                canvas.drawRoundRect(barRect, 14f, 14f, paint)

                if (bar.calories > 0) {
                    canvas.drawText("${bar.calories}", cx, chartBottom - barH - 10f, barValPaint)
                }
                canvas.drawText(bar.dayLabel, cx, heroBottom - 20f, dayLblPaint)
            }
        }

        // 5. Macronutrient Breakdown Card
        val macroTitleY = 690f
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
        }
        val macroSubtitle = if (data.period == VisualReportPeriod.DAILY) {
            "Macronutrient Intake vs Daily Goal"
        } else {
            "Average Daily Macronutrients vs Goal"
        }
        canvas.drawText(macroSubtitle, contentLeft, macroTitleY, sectionTitlePaint)

        val macroCardTop = 710f
        val macroCardBottom = 1020f
        val macroCardRect = RectF(contentLeft, macroCardTop, contentRight, macroCardBottom)
        paint.color = theme.subCardHex
        canvas.drawRoundRect(macroCardRect, 30f, 30f, paint)

        data class MacroRow(val name: String, val actual: Int, val goal: Int, val pct: Int, val colorHex: Int)
        val macroRows = listOf(
            MacroRow("Protein", data.proteinG, data.proteinGoalG, data.proteinPct, 0xFF129A1B.toInt()),
            MacroRow("Carbohydrates", data.carbsG, data.carbsGoalG, data.carbsPct, 0xFFFF6200.toInt()),
            MacroRow("Total Fat", data.fatG, data.fatGoalG, data.fatPct, 0xFF0284C7.toInt()),
            MacroRow("Dietary Fiber", data.fiberG, data.fiberGoalG, data.fiberPct, 0xFF4ED122.toInt())
        )

        val rowLeft = contentLeft + 28f
        val rowRight = contentRight - 28f
        val rowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
        }
        val rowValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 23f
            textAlign = Paint.Align.RIGHT
        }

        macroRows.forEachIndexed { idx, row ->
            val yBase = macroCardTop + 44f + idx * 72f
            canvas.drawText(row.name, rowLeft, yBase, rowLabelPaint)
            canvas.drawText(
                "${row.actual}g / ${row.goal}g (${row.pct}%)",
                rowRight,
                yBase,
                rowValPaint
            )

            val trackRect = RectF(rowLeft, yBase + 12f, rowRight, yBase + 28f)
            paint.color = theme.borderHex
            canvas.drawRoundRect(trackRect, 8f, 8f, paint)

            val frac = (row.pct / 100f).coerceIn(0.03f, 1f)
            val fillRect = RectF(rowLeft, yBase + 12f, rowLeft + trackRect.width() * frac, yBase + 28f)
            paint.color = row.colorHex
            canvas.drawRoundRect(fillRect, 8f, 8f, paint)
        }

        // 6. Meal Breakdown & Hydration Row
        val mealTitleY = 1072f
        val mealSectionLabel = if (data.period == VisualReportPeriod.DAILY) {
            "Meal Distribution"
        } else {
            "Weekly Calories by Meal Slot"
        }
        canvas.drawText(mealSectionLabel, contentLeft, mealTitleY, sectionTitlePaint)

        val boxGap = 14f
        val mealBoxW = (contentWidth - boxGap * 3) / 4f
        val mealBoxTop = 1092f
        val mealBoxBottom = 1210f

        val mTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val mCalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        val mCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }

        data.mealBreakdown.forEachIndexed { idx, slot ->
            val left = contentLeft + idx * (mealBoxW + boxGap)
            val rect = RectF(left, mealBoxTop, left + mealBoxW, mealBoxBottom)
            paint.color = theme.subCardHex
            canvas.drawRoundRect(rect, 22f, 22f, paint)

            val cx = rect.centerX()
            canvas.drawText(slot.mealType, cx, mealBoxTop + 34f, mTitlePaint)
            canvas.drawText("${slot.calories} kcal", cx, mealBoxTop + 72f, mCalPaint)
            canvas.drawText("${slot.itemCount} items", cx, mealBoxTop + 100f, mCountPaint)
        }

        // 7. Insight Banner & Footer
        val insightRect = RectF(contentLeft, 1234f, contentRight, 1308f)
        paint.color = theme.accentSoftHex
        canvas.drawRoundRect(insightRect, 22f, 22f, paint)

        val insightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 23f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.insightText.take(64), insightRect.centerX(), insightRect.centerY() + 8f, insightPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "NutriScan Summary Report • Estimates for personal nutrition tracking only",
            width / 2f,
            1356f,
            footerPaint
        )

        return bitmap
    }

    fun generateVisualReportPngFile(
        context: Context,
        data: NutritionVisualReportData,
        theme: VisualReportTheme
    ): Result<File> = runCatching {
        val bitmap = renderVisualReportBitmap(data, theme)
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val periodSlug = if (data.period == VisualReportPeriod.DAILY) "Daily" else "Weekly"
        val fileName = "NutriScan_${periodSlug}_Summary_${data.anchorDateKey}.png"
        val outFile = File(reportsDir, fileName)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }
        outFile
    }

    /**
     * Shares the generated visual PNG summary report via Android's native Share Sheet
     * using FileProvider (zero storage permissions required).
     */
    fun shareVisualReport(
        context: Context,
        data: NutritionVisualReportData,
        theme: VisualReportTheme
    ): Result<File> = runCatching {
        val pngFile = generateVisualReportPngFile(context, data, theme).getOrThrow()
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, pngFile)

        val periodWord = if (data.period == VisualReportPeriod.DAILY) "Daily" else "Weekly"
        val calorieLabel = if (data.period == VisualReportPeriod.DAILY) "Calories" else "Daily Avg Calories"
        val shareText = buildString {
            appendLine("NutriScan $periodWord Nutrition Summary (${data.dateRangeLabel})")
            appendLine("• $calorieLabel: ${data.caloriesDisplay} / ${data.calorieGoal} kcal (${data.calorieProgressPct}%)")
            appendLine("• Macros: P ${data.proteinG}g | C ${data.carbsG}g | F ${data.fatG}g | Fiber ${data.fiberG}g")
            appendLine("• Hydration: ${data.waterMl} ml | Meals Logged: ${data.totalMealsLogged}")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_SUBJECT, "NutriScan $periodWord Nutrition Summary (${data.dateRangeLabel})")
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri(pngFile.name, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share $periodWord Nutrition Visual Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        pngFile
    }

    /**
     * Saves the generated PNG visual report to Pictures/NutriScan using scoped MediaStore on Android 10+
     * (zero storage permissions needed), or app-scoped external Pictures directory on older Android versions.
     */
    fun saveVisualReportToGallery(
        context: Context,
        data: NutritionVisualReportData,
        theme: VisualReportTheme
    ): Result<String> = runCatching {
        val bitmap = renderVisualReportBitmap(data, theme)
        val periodSlug = if (data.period == VisualReportPeriod.DAILY) "Daily" else "Weekly"
        val stamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
        val displayName = "NutriScan_${periodSlug}_${data.anchorDateKey}_$stamp.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NutriScan")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create image entry in Gallery.")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        "Failed to encode PNG report image."
                    }
                    out.flush()
                } ?: error("Unable to write to Gallery image stream.")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
            "Pictures/NutriScan/$displayName"
        } else {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val targetDir = File(baseDir, "NutriScan").apply { mkdirs() }
            val outFile = File(targetDir, displayName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }
            outFile.absolutePath
        }
    }
}
