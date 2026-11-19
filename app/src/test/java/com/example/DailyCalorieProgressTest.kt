package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class DailyCalorieProgressTest {

    @Test
    fun calculateCalorieProgress_normalIntake_computesCorrectPercentageAndRemaining() {
        val consumed = 1450
        val target = 2000

        val ratio = (consumed.toFloat() / target.toFloat()).coerceAtLeast(0f)
        val remaining = (target - consumed).coerceAtLeast(0)
        val percentage = (ratio * 100).roundToInt()
        val isOverGoal = consumed > target

        assertEquals(0.725f, ratio, 0.001f)
        assertEquals(550, remaining)
        assertEquals(73, percentage)
        assertFalse(isOverGoal)
    }

    @Test
    fun calculateCalorieProgress_exceededGoal_identifiesOverBudgetCorrectly() {
        val consumed = 2350
        val target = 2000

        val ratio = (consumed.toFloat() / target.toFloat()).coerceAtLeast(0f)
        val remaining = (target - consumed).coerceAtLeast(0)
        val excess = consumed - target
        val isOverGoal = consumed > target

        assertEquals(0, remaining)
        assertEquals(350, excess)
        assertTrue(isOverGoal)
    }

    @Test
    fun calculateCalorieProgress_zeroIntake_handlesGracefully() {
        val consumed = 0
        val target = 1800

        val ratio = (consumed.toFloat() / target.toFloat()).coerceAtLeast(0f)
        val remaining = (target - consumed).coerceAtLeast(0)
        val percentage = (ratio * 100).roundToInt()

        assertEquals(0f, ratio, 0.001f)
        assertEquals(1800, remaining)
        assertEquals(0, percentage)
    }
}
