package com.example

import com.example.core.export.NutritionVisualReportExporter
import com.example.core.export.VisualReportPeriod
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionVisualReportTest {

    private val testProfile = UserProfile(
        name = "Aarav Sharma",
        dailyCalorieGoal = 2000,
        dailyProteinGoalG = 120,
        dailyCarbsGoalG = 240,
        dailyFatGoalG = 60,
        dailyFiberGoalG = 30,
        useCustomNutritionGoals = true,
        dailyWaterGoalMl = 2500
    )

    @Test
    fun buildVisualReportData_dailyPeriod_aggregatesMealsMacrosAndWater() {
        val entries = listOf(
            FoodDiaryEntry(
                foodName = "Masala Dosa",
                calories = 420,
                protein = 11.0,
                carbohydrates = 62.0,
                fat = 14.0,
                fiber = 5.0,
                mealType = "Breakfast",
                dateKey = "2026-11-14"
            ),
            FoodDiaryEntry(
                foodName = "Dal Bhat",
                calories = 680,
                protein = 26.0,
                carbohydrates = 104.0,
                fat = 15.0,
                fiber = 11.0,
                mealType = "Lunch",
                dateKey = "2026-11-14"
            ),
            FoodDiaryEntry(
                foodName = "Previous Day Meal",
                calories = 999,
                protein = 50.0,
                carbohydrates = 90.0,
                fat = 30.0,
                fiber = 8.0,
                mealType = "Dinner",
                dateKey = "2026-11-13"
            )
        )
        val water = listOf(
            WaterRecord(quantity = 500, dateKey = "2026-11-14"),
            WaterRecord(quantity = 750, dateKey = "2026-11-14"),
            WaterRecord(quantity = 1000, dateKey = "2026-11-13")
        )

        val report = NutritionVisualReportExporter.buildVisualReportData(
            profile = testProfile,
            diaryEntries = entries,
            waterRecords = water,
            period = VisualReportPeriod.DAILY,
            anchorDateKey = "2026-11-14"
        )

        assertEquals(VisualReportPeriod.DAILY, report.period)
        assertEquals("Aarav Sharma", report.userName)
        assertEquals(1100, report.caloriesDisplay)
        assertEquals(2000, report.calorieGoal)
        assertEquals(55, report.calorieProgressPct)
        assertEquals(37, report.proteinG)
        assertEquals(166, report.carbsG)
        assertEquals(29, report.fatG)
        assertEquals(16, report.fiberG)
        assertEquals(1250, report.waterMl)
        assertEquals(2, report.totalMealsLogged)
        assertEquals(1, report.activeDaysCount)
        assertTrue(report.weeklyBars.isEmpty())

        val breakfast = report.mealBreakdown.first { it.mealType == "Breakfast" }
        val lunch = report.mealBreakdown.first { it.mealType == "Lunch" }
        val dinner = report.mealBreakdown.first { it.mealType == "Dinner" }
        assertEquals(420, breakfast.calories)
        assertEquals(680, lunch.calories)
        assertEquals(0, dinner.calories)
    }

    @Test
    fun buildVisualReportData_weeklyPeriod_buildsSevenChronologicalDaysAndAveragesActiveDays() {
        val entries = listOf(
            FoodDiaryEntry(
                foodName = "Paneer Tikka Bowl",
                calories = 1800,
                protein = 100.0,
                carbohydrates = 180.0,
                fat = 50.0,
                fiber = 25.0,
                mealType = "Lunch",
                dateKey = "2026-11-14"
            ),
            FoodDiaryEntry(
                foodName = "Idli Sambar & Dinner",
                calories = 1600,
                protein = 80.0,
                carbohydrates = 200.0,
                fat = 40.0,
                fiber = 20.0,
                mealType = "Breakfast",
                dateKey = "2026-11-12"
            ),
            // Outside 7-day window (2026-11-08 to 2026-11-14): should be excluded
            FoodDiaryEntry(
                foodName = "Old Meal",
                calories = 3000,
                protein = 90.0,
                carbohydrates = 300.0,
                fat = 90.0,
                fiber = 10.0,
                mealType = "Dinner",
                dateKey = "2026-11-01"
            )
        )
        val water = listOf(
            WaterRecord(quantity = 2000, dateKey = "2026-11-14"),
            WaterRecord(quantity = 1500, dateKey = "2026-11-10")
        )

        val report = NutritionVisualReportExporter.buildVisualReportData(
            profile = testProfile,
            diaryEntries = entries,
            waterRecords = water,
            period = VisualReportPeriod.WEEKLY,
            anchorDateKey = "2026-11-14"
        )

        assertEquals(VisualReportPeriod.WEEKLY, report.period)
        assertEquals(7, report.weeklyBars.size)
        assertEquals("2026-11-08", report.weeklyBars.first().dateKey)
        assertEquals("2026-11-14", report.weeklyBars.last().dateKey)
        assertEquals(2, report.activeDaysCount)
        assertEquals(2, report.totalMealsLogged)
        assertEquals(3400, report.weeklyTotalCalories)
        assertEquals(1700, report.caloriesDisplay) // (1800 + 1600) / 2 active days
        assertEquals(90, report.proteinG) // (100 + 80) / 2
        assertEquals(1750, report.waterMl) // (2000 + 1500) / 2 hydrated days
        assertTrue(report.weeklyBars.last().metGoal)
        assertFalse(report.weeklyBars.first().metGoal)
    }

    @Test
    fun buildVisualReportData_emptyHistory_returnsZeroesGracefully() {
        val daily = NutritionVisualReportExporter.buildVisualReportData(
            profile = testProfile,
            diaryEntries = emptyList(),
            waterRecords = emptyList(),
            period = VisualReportPeriod.DAILY,
            anchorDateKey = "2026-11-14"
        )
        val weekly = NutritionVisualReportExporter.buildVisualReportData(
            profile = testProfile,
            diaryEntries = emptyList(),
            waterRecords = emptyList(),
            period = VisualReportPeriod.WEEKLY,
            anchorDateKey = "2026-11-14"
        )

        assertEquals(0, daily.caloriesDisplay)
        assertEquals(0, daily.activeDaysCount)
        assertEquals(0, weekly.caloriesDisplay)
        assertEquals(0, weekly.activeDaysCount)
        assertEquals(7, weekly.weeklyBars.size)
        assertTrue(daily.insightText.contains("No meals"))
        assertTrue(weekly.insightText.contains("No meals"))
    }

    @Test
    fun computeWeeklyGoalComparisonSummary_calculatesOnTargetOverGoalAndDeltas() {
        val points = listOf(
            com.example.widgets.DailyNutritionTrendPoint("2026-11-08", "Sun", "Sun", 1950, 110.0, 220.0, 55.0, 25.0, 3, false),
            com.example.widgets.DailyNutritionTrendPoint("2026-11-09", "Mon", "Mon", 2050, 120.0, 230.0, 60.0, 28.0, 3, false),
            com.example.widgets.DailyNutritionTrendPoint("2026-11-10", "Tue", "Tue", 2500, 130.0, 300.0, 80.0, 22.0, 4, false),
            com.example.widgets.DailyNutritionTrendPoint("2026-11-11", "Wed", "Wed", 0, 0.0, 0.0, 0.0, 0.0, 0, false),
            com.example.widgets.DailyNutritionTrendPoint("2026-11-12", "Thu", "Thu", 1800, 100.0, 200.0, 50.0, 24.0, 3, false),
            com.example.widgets.DailyNutritionTrendPoint("2026-11-13", "Fri", "Fri", 2100, 115.0, 240.0, 62.0, 26.0, 3, false),
            com.example.widgets.DailyNutritionTrendPoint("2026-11-14", "Sat", "Sat", 1900, 110.0, 210.0, 58.0, 25.0, 3, true)
        )

        val summary = com.example.widgets.computeWeeklyGoalComparisonSummary(points, dailyCalorieGoal = 2000)

        assertEquals(6, summary.daysLogged)
        assertEquals(5, summary.daysOnTarget) // 1950, 2050, 1800, 2100, 1900 within [1600..2200]
        assertEquals(1, summary.daysOverGoal) // 2500 > 2200
        assertEquals(2050, summary.avgCalories) // (1950+2050+2500+1800+2100+1900)/6 = 12300/6 = 2050
        assertEquals(50, summary.deltaFromDailyGoal)
        assertEquals(12300, summary.totalWeeklyCalories)
        assertEquals(14000, summary.weeklyGoalCalories)
        assertEquals(88, summary.weeklyBudgetPercent)
    }

    @Test
    fun computeDailyMacroPieBreakdown_calculatesEnergyAndGramPercentagesCorrectly() {
        // 100g Carbs (400 kcal = 50%), 50g Protein (200 kcal = 25%), ~22.22g Fat (200 kcal = 25%)
        val byCalories = com.example.widgets.computeDailyMacroPieBreakdown(
            carbsG = 100.0,
            proteinG = 50.0,
            fatG = 22.2222,
            byCalories = true
        )
        assertTrue(byCalories.hasIntake)
        assertEquals(100, byCalories.slices.sumOf { it.percentage })
        assertEquals(50, byCalories.slices[0].percentage) // Carbs
        assertEquals(25, byCalories.slices[1].percentage) // Protein
        assertEquals(25, byCalories.slices[2].percentage) // Fat

        // By grams: 100g / 200g = 50%, 60g / 200g = 30%, 40g / 200g = 20%
        val byGrams = com.example.widgets.computeDailyMacroPieBreakdown(
            carbsG = 100.0,
            proteinG = 60.0,
            fatG = 40.0,
            byCalories = false
        )
        assertTrue(byGrams.hasIntake)
        assertEquals(100, byGrams.slices.sumOf { it.percentage })
        assertEquals(50, byGrams.slices[0].percentage)
        assertEquals(30, byGrams.slices[1].percentage)
        assertEquals(20, byGrams.slices[2].percentage)

        // Empty intake state
        val empty = com.example.widgets.computeDailyMacroPieBreakdown(
            carbsG = 0.0,
            proteinG = 0.0,
            fatG = 0.0
        )
        assertFalse(empty.hasIntake)
        assertEquals(0, empty.slices.sumOf { it.percentage })
    }
}
