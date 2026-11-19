package com.example

import com.example.model.FoodDiaryEntry
import com.example.widgets.AchievementBadgeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementBadgeManagerTest {

    @Test
    fun calculateConsecutiveFoodStreak_sevenConsecutiveDays_achievesSevenDayStreak() {
        val entries = listOf(
            FoodDiaryEntry(foodName = "Oatmeal", calories = 250, dateKey = "2026-11-01"),
            FoodDiaryEntry(foodName = "Rice & Fish", calories = 450, dateKey = "2026-11-02"),
            FoodDiaryEntry(foodName = "Eggs & Toast", calories = 320, dateKey = "2026-11-03"),
            FoodDiaryEntry(foodName = "Chicken Salad", calories = 400, dateKey = "2026-11-04"),
            FoodDiaryEntry(foodName = "Dal & Roti", calories = 380, dateKey = "2026-11-05"),
            FoodDiaryEntry(foodName = "Greek Yogurt", calories = 200, dateKey = "2026-11-06"),
            FoodDiaryEntry(foodName = "Fruit Bowl", calories = 180, dateKey = "2026-11-07")
        )

        val streakInfo = AchievementBadgeManager.calculateConsecutiveFoodStreak(
            context = null,
            diaryEntries = entries,
            todayKey = "2026-11-07"
        )

        assertEquals(7, streakInfo.currentStreakDays)
        assertEquals(7, streakInfo.longestStreakDays)
        assertTrue(streakInfo.isSevenDayStreakMet)
        assertTrue(streakInfo.hasLoggedToday)
    }

    @Test
    fun calculateConsecutiveFoodStreak_brokenStreak_calculatesCorrectCurrentStreak() {
        // Logged on Nov 1, 2, 3, skipped Nov 4, logged Nov 5, 6
        val entries = listOf(
            FoodDiaryEntry(foodName = "Meal 1", calories = 300, dateKey = "2026-11-01"),
            FoodDiaryEntry(foodName = "Meal 2", calories = 300, dateKey = "2026-11-02"),
            FoodDiaryEntry(foodName = "Meal 3", calories = 300, dateKey = "2026-11-03"),
            FoodDiaryEntry(foodName = "Meal 5", calories = 300, dateKey = "2026-11-05"),
            FoodDiaryEntry(foodName = "Meal 6", calories = 300, dateKey = "2026-11-06")
        )

        val streakInfo = AchievementBadgeManager.calculateConsecutiveFoodStreak(
            context = null,
            diaryEntries = entries,
            todayKey = "2026-11-06"
        )

        assertEquals(2, streakInfo.currentStreakDays)
        assertEquals(3, streakInfo.longestStreakDays)
        assertFalse(streakInfo.isSevenDayStreakMet)
    }

    @Test
    fun calculateConsecutiveFoodStreak_emptyEntries_returnsZeroStreak() {
        val streakInfo = AchievementBadgeManager.calculateConsecutiveFoodStreak(
            context = null,
            diaryEntries = emptyList(),
            todayKey = "2026-11-07"
        )

        assertEquals(0, streakInfo.currentStreakDays)
        assertEquals(0, streakInfo.longestStreakDays)
        assertFalse(streakInfo.isSevenDayStreakMet)
        assertFalse(streakInfo.hasLoggedToday)
    }
}
