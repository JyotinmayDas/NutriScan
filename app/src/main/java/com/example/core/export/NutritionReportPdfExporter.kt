package com.example.core.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ReportPeriodFilter(
    val label: String,
    val daysCount: Int?
) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_14_DAYS("Last 14 Days", 14),
    LAST_30_DAYS("Last 30 Days", 30),
    ALL_HISTORY("All History", null)
}

data class NutritionReportExportConfig(
    val period: ReportPeriodFilter = ReportPeriodFilter.LAST_7_DAYS,
    val includeProfileAndBmi: Boolean = true,
    val includeDailySummaryTable: Boolean = true,
    val includeDetailedMealHistory: Boolean = true,
    val includeHydrationAndWeight: Boolean = true,
    val clinicianNotes: String = ""
)

data class DailyNutritionReportRow(
    val dateKey: String,
    val mealsCount: Int,
    val caloriesKcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val waterMl: Int
)

data class NutritionReportSummary(
    val generatedDateText: String,
    val periodLabel: String,
    val dateRangeText: String,
    val activeDaysCount: Int,
    val totalMealsLogged: Int,
    val aiScannedMealsCount: Int,
    val avgCaloriesKcal: Int,
    val avgProteinG: Double,
    val avgCarbsG: Double,
    val avgFatG: Double,
    val avgFiberG: Double,
    val avgWaterMl: Int,
    val calorieAdherencePct: Int,
    val proteinAdherencePct: Int,
    val carbsAdherencePct: Int,
    val fatAdherencePct: Int,
    val waterAdherencePct: Int,
    val bmiValue: Double,
    val bmiCategory: String,
    val dailyRows: List<DailyNutritionReportRow>,
    val filteredEntries: List<FoodDiaryEntry>,
    val filteredWeights: List<WeightRecord>
)

object NutritionReportPdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 38f
    private const val MARGIN_BOTTOM = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)

    fun buildReportSummary(
        profile: UserProfile,
        diaryEntries: List<FoodDiaryEntry>,
        waterRecords: List<WaterRecord>,
        weightRecords: List<WeightRecord>,
        config: NutritionReportExportConfig
    ): NutritionReportSummary {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDisplay = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
        val now = Date()

        val cutoffDateKey: String? = config.period.daysCount?.let { days ->
            val cal = Calendar.getInstance()
            cal.time = now
            cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
            sdfDate.format(cal.time)
        }

        val filteredEntries = diaryEntries
            .filter { entry -> cutoffDateKey == null || entry.dateKey >= cutoffDateKey }
            .sortedWith(compareByDescending<FoodDiaryEntry> { it.dateKey }.thenByDescending { it.calories })

        val filteredWater = waterRecords
            .filter { rec -> cutoffDateKey == null || rec.dateKey >= cutoffDateKey }

        val filteredWeights = weightRecords
            .filter { rec -> cutoffDateKey == null || rec.dateKey >= cutoffDateKey }
            .sortedByDescending { it.dateKey }

        // Group by dateKey across both food diary and water records
        val allDateKeys = (filteredEntries.map { it.dateKey } + filteredWater.map { it.dateKey })
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()

        val dailyRows = allDateKeys.map { dateKey ->
            val dayFood = filteredEntries.filter { it.dateKey == dateKey }
            val dayWater = filteredWater.filter { it.dateKey == dateKey }.sumOf { it.quantity }
            DailyNutritionReportRow(
                dateKey = dateKey,
                mealsCount = dayFood.size,
                caloriesKcal = dayFood.sumOf { it.calories },
                proteinG = dayFood.sumOf { it.protein },
                carbsG = dayFood.sumOf { it.carbohydrates },
                fatG = dayFood.sumOf { it.fat },
                fiberG = dayFood.sumOf { it.fiber },
                waterMl = dayWater
            )
        }

        val foodActiveDays = dailyRows.filter { it.mealsCount > 0 }.size.coerceAtLeast(1)
        val waterActiveDays = dailyRows.filter { it.waterMl > 0 }.size.coerceAtLeast(1)

        val avgCalories = if (dailyRows.any { it.mealsCount > 0 }) {
            (filteredEntries.sumOf { it.calories }.toDouble() / foodActiveDays).roundToInt()
        } else 0
        val avgProtein = if (dailyRows.any { it.mealsCount > 0 }) {
            filteredEntries.sumOf { it.protein } / foodActiveDays
        } else 0.0
        val avgCarbs = if (dailyRows.any { it.mealsCount > 0 }) {
            filteredEntries.sumOf { it.carbohydrates } / foodActiveDays
        } else 0.0
        val avgFat = if (dailyRows.any { it.mealsCount > 0 }) {
            filteredEntries.sumOf { it.fat } / foodActiveDays
        } else 0.0
        val avgFiber = if (dailyRows.any { it.mealsCount > 0 }) {
            filteredEntries.sumOf { it.fiber } / foodActiveDays
        } else 0.0
        val avgWater = if (dailyRows.any { it.waterMl > 0 }) {
            (filteredWater.sumOf { it.quantity }.toDouble() / waterActiveDays).roundToInt()
        } else 0

        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val targetPro = profile.effectiveProteinGoalG().coerceAtLeast(20)
        val targetCarbs = profile.effectiveCarbsGoalG().coerceAtLeast(30)
        val targetFat = profile.effectiveFatGoalG().coerceAtLeast(15)
        val targetWater = profile.dailyWaterGoalMl.coerceAtLeast(500)

        val heightM = (profile.height / 100.0).coerceAtLeast(0.5)
        val bmiVal = ((profile.currentWeight / (heightM * heightM)) * 10.0).roundToInt() / 10.0
        val bmiCat = when {
            bmiVal < 18.5 -> "Underweight"
            bmiVal < 25.0 -> "Normal Weight"
            bmiVal < 30.0 -> "Overweight"
            else -> "Obese"
        }

        val dateRangeStr = if (allDateKeys.isNotEmpty()) {
            "${allDateKeys.last()} to ${allDateKeys.first()}"
        } else {
            val todayStr = sdfDate.format(now)
            "${cutoffDateKey ?: todayStr} to $todayStr"
        }

        val aiCount = filteredEntries.count {
            it.source.equals("ai_scan", ignoreCase = true) ||
                it.source.contains("scan", ignoreCase = true)
        }

        return NutritionReportSummary(
            generatedDateText = sdfDisplay.format(now),
            periodLabel = config.period.label,
            dateRangeText = dateRangeStr,
            activeDaysCount = allDateKeys.size,
            totalMealsLogged = filteredEntries.size,
            aiScannedMealsCount = aiCount,
            avgCaloriesKcal = avgCalories,
            avgProteinG = avgProtein,
            avgCarbsG = avgCarbs,
            avgFatG = avgFat,
            avgFiberG = avgFiber,
            avgWaterMl = avgWater,
            calorieAdherencePct = ((avgCalories.toDouble() / targetCal) * 100).roundToInt().coerceIn(0, 200),
            proteinAdherencePct = ((avgProtein / targetPro) * 100).roundToInt().coerceIn(0, 200),
            carbsAdherencePct = ((avgCarbs / targetCarbs) * 100).roundToInt().coerceIn(0, 200),
            fatAdherencePct = ((avgFat / targetFat) * 100).roundToInt().coerceIn(0, 200),
            waterAdherencePct = ((avgWater.toDouble() / targetWater) * 100).roundToInt().coerceIn(0, 200),
            bmiValue = bmiVal,
            bmiCategory = bmiCat,
            dailyRows = dailyRows,
            filteredEntries = filteredEntries,
            filteredWeights = filteredWeights
        )
    }

    /**
     * Generates a multi-page clinical PDF file in the app's cache/reports directory and returns the File.
     */
    fun generatePdfFile(
        context: Context,
        profile: UserProfile,
        diaryEntries: List<FoodDiaryEntry>,
        waterRecords: List<WaterRecord>,
        weightRecords: List<WeightRecord>,
        config: NutritionReportExportConfig
    ): Result<File> = runCatching {
        val summary = buildReportSummary(profile, diaryEntries, waterRecords, weightRecords, config)
        val pdfDocument = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCFCE7")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F291E")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14532D")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.parseColor("#CBD5E1")
        }

        var currentPageNumber = 1
        var currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
        )
        var canvas = currentPage.canvas
        var y = MARGIN_TOP

        fun drawPageFooter(c: Canvas, pageNum: Int) {
            val footerY = PAGE_HEIGHT - 20f
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
            }
            c.drawLine(MARGIN_H, footerY - 12f, PAGE_WIDTH - MARGIN_H, footerY - 12f, strokePaint)
            c.drawText(
                "NutriScan Clinical & Dietary Summary • For Health Professional Review • Estimates only, not medical diagnosis",
                MARGIN_H,
                footerY,
                footerPaint
            )
            val pageStr = "Page $pageNum"
            val w = footerPaint.measureText(pageStr)
            c.drawText(pageStr, PAGE_WIDTH - MARGIN_H - w, footerY, footerPaint)
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                drawPageFooter(canvas, currentPageNumber)
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                currentPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                )
                canvas = currentPage.canvas
                y = MARGIN_TOP
            }
        }

        // --- 1. TOP CLINICAL REPORT HEADER BANNER ---
        fillPaint.color = Color.parseColor("#15803D") // NutriScan Emerald Green
        val bannerRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 66f)
        canvas.drawRoundRect(bannerRect, 12f, 12f, fillPaint)

        canvas.drawText(
            "NutriScan • Nutritional & Dietary History Report",
            MARGIN_H + 16f,
            y + 26f,
            titlePaint
        )
        canvas.drawText(
            "Report Period: ${summary.periodLabel} (${summary.dateRangeText})  •  Generated: ${summary.generatedDateText}",
            MARGIN_H + 16f,
            y + 44f,
            subtitlePaint
        )
        canvas.drawText(
            "Prepared for Healthcare Professional / Registered Dietitian Consultation",
            MARGIN_H + 16f,
            y + 57f,
            subtitlePaint
        )
        y += 82f

        // --- 2. PATIENT / USER PROFILE & BIOMETRIC OVERVIEW ---
        if (config.includeProfileAndBmi) {
            ensureSpace(88f)
            canvas.drawText("1. Patient / User Profile & Biometrics", MARGIN_H, y, sectionTitlePaint)
            y += 8f

            fillPaint.color = Color.parseColor("#F0FDF4")
            val profileRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 66f)
            canvas.drawRoundRect(profileRect, 10f, 10f, fillPaint)
            canvas.drawRoundRect(profileRect, 10f, 10f, strokePaint)

            val col1X = MARGIN_H + 14f
            val col2X = MARGIN_H + 195f
            val col3X = MARGIN_H + 365f

            canvas.drawText("Name: ${profile.name}", col1X, y + 20f, boldBodyPaint)
            canvas.drawText("Age / Gender: ${profile.age} yrs • ${profile.gender}", col1X, y + 36f, bodyPaint)
            canvas.drawText("Dietary Style: ${profile.dietaryPreference}", col1X, y + 52f, bodyPaint)

            canvas.drawText("Height: ${profile.height.roundToInt()} cm", col2X, y + 20f, boldBodyPaint)
            canvas.drawText("Current Weight: ${profile.currentWeight} kg", col2X, y + 36f, bodyPaint)
            canvas.drawText("Target Weight: ${profile.targetWeight} kg", col2X, y + 52f, bodyPaint)

            canvas.drawText("BMI: ${summary.bmiValue} (${summary.bmiCategory})", col3X, y + 20f, boldBodyPaint)
            canvas.drawText("Goal: ${profile.fitnessGoal}", col3X, y + 36f, bodyPaint)
            canvas.drawText("Activity: ${profile.activityLevel}", col3X, y + 52f, bodyPaint)

            y += 82f
        }

        // --- 3. EXECUTIVE NUTRITIONAL SUMMARY & GOAL ADHERENCE ---
        ensureSpace(155f)
        canvas.drawText(
            "2. Daily Nutritional Targets vs. Average Intake (${summary.activeDaysCount} Active Days, ${summary.totalMealsLogged} Meals)",
            MARGIN_H,
            y,
            sectionTitlePaint
        )
        y += 10f

        // Table Header
        fillPaint.color = Color.parseColor("#DCFCE7")
        val macroHeaderRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 22f)
        canvas.drawRoundRect(macroHeaderRect, 6f, 6f, fillPaint)

        val cNutrient = MARGIN_H + 10f
        val cTarget = MARGIN_H + 160f
        val cAverage = MARGIN_H + 265f
        val cAdherence = MARGIN_H + 365f
        val cBar = MARGIN_H + 435f

        canvas.drawText("Nutrient / Metric", cNutrient, y + 15f, tableHeaderPaint)
        canvas.drawText("Daily Target", cTarget, y + 15f, tableHeaderPaint)
        canvas.drawText("Daily Average", cAverage, y + 15f, tableHeaderPaint)
        canvas.drawText("Adherence", cAdherence, y + 15f, tableHeaderPaint)
        canvas.drawText("Goal Progress", cBar, y + 15f, tableHeaderPaint)
        y += 24f

        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val targetPro = profile.effectiveProteinGoalG()
        val targetCarbs = profile.effectiveCarbsGoalG()
        val targetFat = profile.effectiveFatGoalG()
        val targetFiber = profile.effectiveFiberGoalG()
        val targetWater = profile.dailyWaterGoalMl.coerceAtLeast(500)
        val fiberAdherence = ((summary.avgFiberG / targetFiber.coerceAtLeast(1)) * 100).roundToInt().coerceIn(0, 200)

        data class NutrientRowSpec(
            val name: String,
            val targetStr: String,
            val avgStr: String,
            val adherencePct: Int,
            val barColorHex: String
        )

        val nutrientRows = listOf(
            NutrientRowSpec(
                "Calories (Energy)",
                "$targetCal kcal",
                "${summary.avgCaloriesKcal} kcal",
                summary.calorieAdherencePct,
                "#16A34A"
            ),
            NutrientRowSpec(
                "Protein",
                "$targetPro g",
                String.format(Locale.US, "%.1f g", summary.avgProteinG),
                summary.proteinAdherencePct,
                "#0EA5E9"
            ),
            NutrientRowSpec(
                "Carbohydrates",
                "$targetCarbs g",
                String.format(Locale.US, "%.1f g", summary.avgCarbsG),
                summary.carbsAdherencePct,
                "#D97706"
            ),
            NutrientRowSpec(
                "Total Fat",
                "$targetFat g",
                String.format(Locale.US, "%.1f g", summary.avgFatG),
                summary.fatAdherencePct,
                "#E11D48"
            ),
            NutrientRowSpec(
                "Dietary Fiber",
                "$targetFiber g",
                String.format(Locale.US, "%.1f g", summary.avgFiberG),
                fiberAdherence,
                "#15803D"
            ),
            NutrientRowSpec(
                "Water / Hydration",
                "$targetWater ml",
                "${summary.avgWaterMl} ml",
                summary.waterAdherencePct,
                "#0284C7"
            )
        )

        nutrientRows.forEachIndexed { idx, row ->
            if (idx % 2 == 1) {
                fillPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 20f, fillPaint)
            }
            canvas.drawText(row.name, cNutrient, y + 14f, boldBodyPaint)
            canvas.drawText(row.targetStr, cTarget, y + 14f, bodyPaint)
            canvas.drawText(row.avgStr, cAverage, y + 14f, boldBodyPaint)
            canvas.drawText("${row.adherencePct}%", cAdherence, y + 14f, bodyPaint)

            // Draw visual progress bar
            val trackRect = RectF(cBar, y + 6f, PAGE_WIDTH - MARGIN_H - 10f, y + 14f)
            fillPaint.color = Color.parseColor("#E2E8F0")
            canvas.drawRoundRect(trackRect, 4f, 4f, fillPaint)

            val fillFraction = (row.adherencePct / 100f).coerceIn(0.05f, 1f)
            val progressWidth = trackRect.width() * fillFraction
            val barRect = RectF(cBar, y + 6f, cBar + progressWidth, y + 14f)
            fillPaint.color = Color.parseColor(row.barColorHex)
            canvas.drawRoundRect(barRect, 4f, 4f, fillPaint)

            y += 20f
        }
        y += 14f

        // --- 4. CLINICIAN / PATIENT NOTES (OPTIONAL) ---
        if (config.clinicianNotes.isNotBlank()) {
            ensureSpace(54f)
            canvas.drawText("Clinician / Patient Notes:", MARGIN_H, y, sectionTitlePaint)
            y += 6f
            fillPaint.color = Color.parseColor("#FFFBEB")
            val noteRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 34f)
            canvas.drawRoundRect(noteRect, 8f, 8f, fillPaint)
            canvas.drawText(
                config.clinicianNotes.take(125),
                MARGIN_H + 12f,
                y + 21f,
                bodyPaint
            )
            y += 46f
        }

        // --- 5. DAY-BY-DAY NUTRITIONAL & HYDRATION SUMMARY TABLE ---
        if (config.includeDailySummaryTable) {
            ensureSpace(60f)
            canvas.drawText("3. Day-by-Day Nutritional & Hydration Breakdown", MARGIN_H, y, sectionTitlePaint)
            y += 8f

            fillPaint.color = Color.parseColor("#DCFCE7")
            val dayHeaderRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 20f)
            canvas.drawRoundRect(dayHeaderRect, 6f, 6f, fillPaint)

            val dDate = MARGIN_H + 10f
            val dMeals = MARGIN_H + 95f
            val dCal = MARGIN_H + 150f
            val dPro = MARGIN_H + 225f
            val dCarb = MARGIN_H + 295f
            val dFat = MARGIN_H + 365f
            val dFib = MARGIN_H + 425f
            val dWater = MARGIN_H + 475f

            canvas.drawText("Date", dDate, y + 14f, tableHeaderPaint)
            canvas.drawText("Meals", dMeals, y + 14f, tableHeaderPaint)
            canvas.drawText("Calories", dCal, y + 14f, tableHeaderPaint)
            canvas.drawText("Protein", dPro, y + 14f, tableHeaderPaint)
            canvas.drawText("Carbs", dCarb, y + 14f, tableHeaderPaint)
            canvas.drawText("Fat", dFat, y + 14f, tableHeaderPaint)
            canvas.drawText("Fiber", dFib, y + 14f, tableHeaderPaint)
            canvas.drawText("Water", dWater, y + 14f, tableHeaderPaint)
            y += 22f

            if (summary.dailyRows.isEmpty()) {
                canvas.drawText(
                    "No daily records logged during the selected period.",
                    MARGIN_H + 10f,
                    y + 14f,
                    labelPaint
                )
                y += 24f
            } else {
                summary.dailyRows.take(31).forEachIndexed { idx, day ->
                    ensureSpace(20f)
                    if (idx % 2 == 1) {
                        fillPaint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 18f, fillPaint)
                    }
                    canvas.drawText(day.dateKey, dDate, y + 13f, boldBodyPaint)
                    canvas.drawText("${day.mealsCount}", dMeals, y + 13f, bodyPaint)
                    canvas.drawText("${day.caloriesKcal} kcal", dCal, y + 13f, boldBodyPaint)
                    canvas.drawText("${day.proteinG.roundToInt()}g", dPro, y + 13f, bodyPaint)
                    canvas.drawText("${day.carbsG.roundToInt()}g", dCarb, y + 13f, bodyPaint)
                    canvas.drawText("${day.fatG.roundToInt()}g", dFat, y + 13f, bodyPaint)
                    canvas.drawText("${day.fiberG.roundToInt()}g", dFib, y + 13f, bodyPaint)
                    canvas.drawText("${day.waterMl}ml", dWater, y + 13f, bodyPaint)
                    y += 18f
                }
            }
            y += 14f
        }

        // --- 6. DETAILED MEAL & SCANNED FOOD LOG HISTORY ---
        if (config.includeDetailedMealHistory) {
            ensureSpace(60f)
            canvas.drawText(
                "4. Detailed Meal & Scanned Food History (${summary.filteredEntries.size} Entries)",
                MARGIN_H,
                y,
                sectionTitlePaint
            )
            y += 8f

            fillPaint.color = Color.parseColor("#DCFCE7")
            val logHeaderRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 20f)
            canvas.drawRoundRect(logHeaderRect, 6f, 6f, fillPaint)

            val mDate = MARGIN_H + 8f
            val mSlot = MARGIN_H + 74f
            val mFood = MARGIN_H + 134f
            val mPortion = MARGIN_H + 292f
            val mKcal = MARGIN_H + 342f
            val mMacros = MARGIN_H + 402f

            canvas.drawText("Date", mDate, y + 14f, tableHeaderPaint)
            canvas.drawText("Meal", mSlot, y + 14f, tableHeaderPaint)
            canvas.drawText("Food / Dish Name", mFood, y + 14f, tableHeaderPaint)
            canvas.drawText("Portion", mPortion, y + 14f, tableHeaderPaint)
            canvas.drawText("Calories", mKcal, y + 14f, tableHeaderPaint)
            canvas.drawText("Macros (P / C / F)", mMacros, y + 14f, tableHeaderPaint)
            y += 22f

            if (summary.filteredEntries.isEmpty()) {
                canvas.drawText(
                    "No individual meal entries found for this period.",
                    MARGIN_H + 10f,
                    y + 14f,
                    labelPaint
                )
                y += 22f
            } else {
                summary.filteredEntries.take(80).forEachIndexed { idx, entry ->
                    ensureSpace(20f)
                    if (idx % 2 == 1) {
                        fillPaint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 18f, fillPaint)
                    }
                    val trimmedName = if (entry.foodName.length > 26) {
                        entry.foodName.take(24) + "…"
                    } else {
                        entry.foodName
                    }
                    canvas.drawText(entry.dateKey, mDate, y + 13f, bodyPaint)
                    canvas.drawText(entry.mealType.take(10), mSlot, y + 13f, bodyPaint)
                    canvas.drawText(trimmedName, mFood, y + 13f, boldBodyPaint)
                    canvas.drawText("${entry.quantity.roundToInt()}g", mPortion, y + 13f, bodyPaint)
                    canvas.drawText("${entry.calories} kcal", mKcal, y + 13f, boldBodyPaint)
                    canvas.drawText(
                        "${entry.protein.roundToInt()}g / ${entry.carbohydrates.roundToInt()}g / ${entry.fat.roundToInt()}g",
                        mMacros,
                        y + 13f,
                        bodyPaint
                    )
                    y += 18f
                }
            }
            y += 12f
        }

        // --- 7. WEIGHT LOG HISTORY (IF ENABLED & AVAILABLE) ---
        if (config.includeHydrationAndWeight && summary.filteredWeights.isNotEmpty()) {
            ensureSpace(55f)
            canvas.drawText("5. Logged Body Weight History", MARGIN_H, y, sectionTitlePaint)
            y += 8f
            summary.filteredWeights.take(10).forEach { w ->
                ensureSpace(18f)
                val notePart = if (w.note.isNotBlank()) " • Note: ${w.note}" else ""
                canvas.drawText(
                    "• ${w.dateKey}: ${w.weight} kg$notePart",
                    MARGIN_H + 10f,
                    y + 13f,
                    bodyPaint
                )
                y += 17f
            }
        }

        // Finish last page
        drawPageFooter(canvas, currentPageNumber)
        pdfDocument.finishPage(currentPage)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = profile.name.replace(Regex("[^A-Za-z0-9]"), "_").take(20).ifBlank { "Patient" }
        val fileDate = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val outFile = File(reportsDir, "NutriScan_Report_${safeName}_$fileDate.pdf")

        FileOutputStream(outFile).use { outStream ->
            pdfDocument.writeTo(outStream)
        }
        pdfDocument.close()

        outFile
    }

    /**
     * Writes the generated PDF file to a user-chosen SAF document Uri (CreateDocument("application/pdf")).
     */
    fun writePdfFileToUri(context: Context, sourcePdfFile: File, targetUri: Uri): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            sourcePdfFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: error("Unable to open destination file stream.")
    }

    /**
     * Launches the Android system Share Sheet so the user can share the PDF report with their doctor,
     * nutritionist, or dietitian via Email, WhatsApp, Drive, or PDF viewer apps.
     */
    fun sharePdfReportWithHealthProfessional(
        context: Context,
        pdfFile: File,
        profile: UserProfile,
        summary: NutritionReportSummary
    ): Result<Unit> = runCatching {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)

        val shareSubject = "NutriScan Nutrition & Dietary Report - ${profile.name} (${summary.periodLabel})"
        val shareBody = buildString {
            appendLine("Hello Doctor / Dietitian,")
            appendLine()
            appendLine("Please find attached the NutriScan Nutritional Summary & Meal History PDF report for ${profile.name}.")
            appendLine("• Report Period: ${summary.periodLabel} (${summary.dateRangeText})")
            appendLine("• Daily Average Intake: ${summary.avgCaloriesKcal} kcal (Target: ${profile.dailyCalorieGoal} kcal)")
            appendLine(
                String.format(
                    Locale.US,
                    "• Average Macros: Protein %.1fg | Carbs %.1fg | Fat %.1fg | Fiber %.1fg",
                    summary.avgProteinG,
                    summary.avgCarbsG,
                    summary.avgFatG,
                    summary.avgFiberG
                )
            )
            appendLine("• Average Hydration: ${summary.avgWaterMl} ml / day")
            appendLine("• Total Meals Logged: ${summary.totalMealsLogged} (${summary.aiScannedMealsCount} AI-scanned)")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, shareBody)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri(pdfFile.name, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Share Nutrition Report PDF with Health Professional").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
