package com.example.widgets

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.model.todayDateKey
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeAccentStrong
import com.example.ui.theme.PastelBlueAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Data structures for the NutriScan User Level & Experience Points (XP) System.
 */
data class LevelTierInfo(
    val level: Int,
    val titleEn: String,
    val titleBn: String,
    val badgeEmoji: String,
    val minXp: Int,
    val maxXp: Int,
    val perkDescription: String
)

data class DailyNutritionQuest(
    val id: String,
    val title: String,
    val subtitle: String,
    val xpReward: Int,
    val isCompleted: Boolean,
    val progressFraction: Float,
    val progressLabel: String,
    val category: String // "MEAL", "GOAL", "HYDRATION"
)

data class XpRewardEvent(
    val title: String,
    val subtitle: String,
    val xpAmount: Int,
    val category: String
)

data class UserLevelSummary(
    val currentLevel: Int,
    val currentTier: LevelTierInfo,
    val nextTier: LevelTierInfo?,
    val totalXp: Int,
    val xpIntoCurrentLevel: Int,
    val xpNeededForNextLevel: Int,
    val levelProgressFraction: Float,
    val todayEarnedXp: Int,
    val mealLoggingXp: Int,
    val nutritionGoalsXp: Int,
    val hydrationAndStreakXp: Int,
    val bonusClaimedXp: Int,
    val streakDays: Int,
    val completedMealsCount: Int,
    val goalsMetDaysCount: Int,
    val dailyQuests: List<DailyNutritionQuest>,
    val recentXpEvents: List<XpRewardEvent>,
    val isDailyMilestoneClaimedToday: Boolean,
    val canClaimDailyMilestoneToday: Boolean
)

/**
 * Central engine that calculates Experience Points (XP), User Level tiers,
 * Daily Nutrition Quests, and persists bonus milestone claims.
 */
object UserLevelManager {
    private const val PREFS_NAME = "nutriscan_xp_level_prefs"
    private const val KEY_BONUS_XP = "claimed_bonus_xp"
    private const val KEY_LAST_MILESTONE_DATE = "last_milestone_claimed_date"

    // XP Award Constants
    const val XP_PER_MEAL_LOGGED = 25
    const val XP_BONUS_AI_OR_BARCODE_SCAN = 15
    const val XP_FULL_DAY_MEALS_COMBO = 40 // Breakfast + Lunch + Dinner logged on same day
    const val XP_CALORIE_GOAL_MET = 60     // Within 85% - 110% of daily calorie target
    const val XP_PROTEIN_GOAL_MET = 50     // >= 90% of daily protein target
    const val XP_FIBER_GOAL_MET = 35       // >= 80% of daily fiber target
    const val XP_BALANCED_MACROS_MET = 40  // Both Carbs & Fat within healthy target zone
    const val XP_HYDRATION_GOAL_MET = 45   // >= 100% of daily water goal
    const val XP_PER_WATER_LOG = 10
    const val XP_PER_WEIGHT_LOG = 20
    const val XP_PER_STREAK_DAY = 30
    const val XP_DAILY_MILESTONE_CHEST = 100

    private val _bonusXpFlow = MutableStateFlow(0)
    val bonusXpFlow: StateFlow<Int> = _bonusXpFlow.asStateFlow()

    val levelTiers: List<LevelTierInfo> = listOf(
        LevelTierInfo(
            level = 1,
            titleEn = "Mindful Starter",
            titleBn = "সচেতন শুরু",
            badgeEmoji = "🌱",
            minXp = 0,
            maxXp = 200,
            perkDescription = "Begin your smart food & macro tracking journey"
        ),
        LevelTierInfo(
            level = 2,
            titleEn = "Calorie Explorer",
            titleBn = "ক্যালোরি এক্সপ্লোরার",
            badgeEmoji = "🥗",
            minXp = 200,
            maxXp = 450,
            perkDescription = "Consistent meal logging & portion awareness"
        ),
        LevelTierInfo(
            level = 3,
            titleEn = "Macro Balancer",
            titleBn = "ম্যাক্রো ব্যালেন্সার",
            badgeEmoji = "⚖️",
            minXp = 450,
            maxXp = 800,
            perkDescription = "Balancing Protein, Carbs & Fats with precision"
        ),
        LevelTierInfo(
            level = 4,
            titleEn = "Protein Champion",
            titleBn = "প্রোটিন চ্যাম্পিয়ন",
            badgeEmoji = "💪",
            minXp = 800,
            maxXp = 1250,
            perkDescription = "Hitting daily protein & muscle recovery targets"
        ),
        LevelTierInfo(
            level = 5,
            titleEn = "Hydration & Habit Hero",
            titleBn = "হাইড্রেশন হিরো",
            badgeEmoji = "💧",
            minXp = 1250,
            maxXp = 1800,
            perkDescription = "Mastering daily hydration and 3-meal consistency"
        ),
        LevelTierInfo(
            level = 6,
            titleEn = "Smart Scanner Pro",
            titleBn = "স্মার্ট স্ক্যানার প্রো",
            badgeEmoji = "📸",
            minXp = 1800,
            maxXp = 2500,
            perkDescription = "Expert AI meal & packaged food barcode tracker"
        ),
        LevelTierInfo(
            level = 7,
            titleEn = "Thali Nutritionist",
            titleBn = "থালি নিউট্রিশনিস্ট",
            badgeEmoji = "🍛",
            minXp = 2500,
            maxXp = 3350,
            perkDescription = "Master of balanced Indian & Bengali meal planning"
        ),
        LevelTierInfo(
            level = 8,
            titleEn = "Metabolic Master",
            titleBn = "মেটাবলিক মাস্টার",
            badgeEmoji = "🔥",
            minXp = 3350,
            maxXp = 4400,
            perkDescription = "Sustained calorie & macronutrient goal excellence"
        ),
        LevelTierInfo(
            level = 9,
            titleEn = "Vitality Architect",
            titleBn = "ভাইটালিটি আর্কিটেক্ট",
            badgeEmoji = "🏆",
            minXp = 4400,
            maxXp = 5700,
            perkDescription = "Elite daily nutrition discipline & streak mastery"
        ),
        LevelTierInfo(
            level = 10,
            titleEn = "NutriScan Grandmaster",
            titleBn = "নিউট্রিস্ক্যান গ্র্যান্ডমাস্টার",
            badgeEmoji = "👑",
            minXp = 5700,
            maxXp = 10000,
            perkDescription = "Top-tier nutrition, macro & wellness legend"
        )
    )

    fun loadBonusXp(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bonus = prefs.getInt(KEY_BONUS_XP, 0)
        _bonusXpFlow.value = bonus
        return bonus
    }

    fun isMilestoneClaimedForDate(context: Context, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_MILESTONE_DATE, "") == dateKey
    }

    fun claimDailyMilestoneChest(context: Context, dateKey: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_MILESTONE_DATE, "") == dateKey) {
            return prefs.getInt(KEY_BONUS_XP, 0)
        }
        val updated = prefs.getInt(KEY_BONUS_XP, 0) + XP_DAILY_MILESTONE_CHEST
        prefs.edit()
            .putInt(KEY_BONUS_XP, updated)
            .putString(KEY_LAST_MILESTONE_DATE, dateKey)
            .apply()
        _bonusXpFlow.value = updated
        return updated
    }

    fun addBonusXp(context: Context, amount: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_BONUS_XP, 0)
        val updated = current + amount
        prefs.edit().putInt(KEY_BONUS_XP, updated).apply()
        _bonusXpFlow.value = updated
        return updated
    }

    /**
     * Calculates the complete UserLevelSummary from the user's profile, logged meals,
     * hydration records, weight records, and selected date.
     */
    fun calculateLevelSummary(
        context: Context?,
        profile: UserProfile,
        diaryEntries: List<FoodDiaryEntry>,
        waterRecords: List<WaterRecord>,
        weightRecords: List<WeightRecord>,
        selectedDateKey: String = todayDateKey()
    ): UserLevelSummary {
        val bonusXp = if (context != null) loadBonusXp(context) else _bonusXpFlow.value
        val milestoneClaimedToday = if (context != null) {
            isMilestoneClaimedForDate(context, selectedDateKey)
        } else {
            false
        }

        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val targetProtein = profile.effectiveProteinGoalG().coerceAtLeast(25)
        val targetCarbs = profile.effectiveCarbsGoalG().coerceAtLeast(40)
        val targetFat = profile.effectiveFatGoalG().coerceAtLeast(15)
        val targetFiber = profile.effectiveFiberGoalG().coerceAtLeast(15)
        val targetWater = profile.dailyWaterGoalMl.coerceAtLeast(1000)

        // 1. Meal Logging XP across all logged entries
        var mealLoggingXp = 0
        val recentEvents = mutableListOf<XpRewardEvent>()

        diaryEntries.forEach { entry ->
            val isScan = entry.source.contains("Scan", ignoreCase = true) ||
                entry.notes.contains("Barcode", ignoreCase = true)
            val entryXp = XP_PER_MEAL_LOGGED + if (isScan) XP_BONUS_AI_OR_BARCODE_SCAN else 0
            mealLoggingXp += entryXp
        }

        // Group entries by dateKey to evaluate daily combos & daily nutrition goal hits
        val entriesByDate = diaryEntries.groupBy { it.dateKey }
        val waterByDate = waterRecords.groupBy { it.dateKey }

        var nutritionGoalsXp = 0
        var goalsMetDaysCount = 0

        entriesByDate.forEach { (dateKey, dayEntries) ->
            val mealTypes = dayEntries.map { it.mealType }.toSet()
            val hasThreeMainMeals = mealTypes.containsAll(listOf("Breakfast", "Lunch", "Dinner"))
            if (hasThreeMainMeals) {
                mealLoggingXp += XP_FULL_DAY_MEALS_COMBO
            }

            val dayCal = dayEntries.sumOf { it.calories }
            val dayPro = dayEntries.sumOf { it.protein }
            val dayCarbs = dayEntries.sumOf { it.carbohydrates }
            val dayFat = dayEntries.sumOf { it.fat }
            val dayFiber = dayEntries.sumOf { it.fiber }

            var dayGoalsHit = 0

            // Calorie target met: 85% to 110% of dailyCalorieGoal
            val calRatio = dayCal.toFloat() / targetCal.toFloat()
            if (calRatio in 0.85f..1.10f) {
                nutritionGoalsXp += XP_CALORIE_GOAL_MET
                dayGoalsHit++
            }

            // Protein target met: >= 90% of effectiveProteinGoalG
            if (dayPro >= targetProtein * 0.90) {
                nutritionGoalsXp += XP_PROTEIN_GOAL_MET
                dayGoalsHit++
            }

            // Fiber target met: >= 80% of effectiveFiberGoalG
            if (dayFiber >= targetFiber * 0.80) {
                nutritionGoalsXp += XP_FIBER_GOAL_MET
                dayGoalsHit++
            }

            // Balanced Carbs & Fat: within 75%..115%
            val carbsRatio = dayCarbs / targetCarbs.toDouble()
            val fatRatio = dayFat / targetFat.toDouble()
            if (carbsRatio in 0.75..1.15 && fatRatio in 0.70..1.15) {
                nutritionGoalsXp += XP_BALANCED_MACROS_MET
                dayGoalsHit++
            }

            if (dayGoalsHit > 0) {
                goalsMetDaysCount++
            }
        }

        // 2. Hydration & Streak XP
        var hydrationAndStreakXp = waterRecords.size * XP_PER_WATER_LOG +
            weightRecords.size * XP_PER_WEIGHT_LOG

        waterByDate.forEach { (_, dayWaterLogs) ->
            val totalMl = dayWaterLogs.sumOf { it.quantity }
            if (totalMl >= targetWater) {
                hydrationAndStreakXp += XP_HYDRATION_GOAL_MET
            }
        }

        val distinctActiveDays = (entriesByDate.keys + waterByDate.keys).distinct().size.coerceAtLeast(1)
        hydrationAndStreakXp += distinctActiveDays * XP_PER_STREAK_DAY

        // Base starter XP so a new user starts with motivating momentum (120 XP)
        val starterOnboardingXp = 120
        val totalXp = starterOnboardingXp + mealLoggingXp + nutritionGoalsXp + hydrationAndStreakXp + bonusXp

        // 3. Today's Breakdown & Daily Quests for `selectedDateKey`
        val todayEntries = entriesByDate[selectedDateKey].orEmpty()
        val todayWaterLogs = waterByDate[selectedDateKey].orEmpty()
        val todayCalories = todayEntries.sumOf { it.calories }
        val todayProtein = todayEntries.sumOf { it.protein }
        val todayCarbs = todayEntries.sumOf { it.carbohydrates }
        val todayFat = todayEntries.sumOf { it.fat }
        val todayFiber = todayEntries.sumOf { it.fiber }
        val todayWaterMl = todayWaterLogs.sumOf { it.quantity }

        val todayMealTypes = todayEntries.map { it.mealType }.toSet()
        val mainMealsLoggedToday = listOf("Breakfast", "Lunch", "Dinner").count { it in todayMealTypes }
        val hasAllThreeMainMealsToday = mainMealsLoggedToday == 3

        val isCalorieGoalMetToday = todayCalories.toFloat() / targetCal.toFloat() in 0.85f..1.10f
        val isProteinGoalMetToday = todayProtein >= targetProtein * 0.90
        val isFiberGoalMetToday = todayFiber >= targetFiber * 0.80
        val isHydrationGoalMetToday = todayWaterMl >= targetWater

        var todayEarnedXp = todayEntries.sumOf { entry ->
            val isScan = entry.source.contains("Scan", ignoreCase = true) ||
                entry.notes.contains("Barcode", ignoreCase = true)
            XP_PER_MEAL_LOGGED + if (isScan) XP_BONUS_AI_OR_BARCODE_SCAN else 0
        } + (todayWaterLogs.size * XP_PER_WATER_LOG)

        if (hasAllThreeMainMealsToday) todayEarnedXp += XP_FULL_DAY_MEALS_COMBO
        if (isCalorieGoalMetToday) todayEarnedXp += XP_CALORIE_GOAL_MET
        if (isProteinGoalMetToday) todayEarnedXp += XP_PROTEIN_GOAL_MET
        if (isFiberGoalMetToday) todayEarnedXp += XP_FIBER_GOAL_MET
        if (isHydrationGoalMetToday) todayEarnedXp += XP_HYDRATION_GOAL_MET
        if (milestoneClaimedToday) todayEarnedXp += XP_DAILY_MILESTONE_CHEST

        // Build Daily Quests list
        val dailyQuests = listOf(
            DailyNutritionQuest(
                id = "quest_first_meal",
                title = "Log a Meal Today",
                subtitle = "Scan a dish, scan a barcode, or add from database (+25 XP per meal, +15 XP scan bonus)",
                xpReward = XP_PER_MEAL_LOGGED,
                isCompleted = todayEntries.isNotEmpty(),
                progressFraction = if (todayEntries.isNotEmpty()) 1f else 0f,
                progressLabel = "${todayEntries.size} meal(s) logged",
                category = "MEAL"
            ),
            DailyNutritionQuest(
                id = "quest_three_meals",
                title = "Full-Day Meal Combo (Breakfast, Lunch & Dinner)",
                subtitle = "Log all 3 main meals today for consistent energy",
                xpReward = XP_FULL_DAY_MEALS_COMBO,
                isCompleted = hasAllThreeMainMealsToday,
                progressFraction = (mainMealsLoggedToday / 3f).coerceIn(0f, 1f),
                progressLabel = "$mainMealsLoggedToday / 3 main meals",
                category = "MEAL"
            ),
            DailyNutritionQuest(
                id = "quest_calorie_goal",
                title = "Hit Daily Calorie Target Zone",
                subtitle = "Reach 85%–110% of your ${targetCal} kcal goal",
                xpReward = XP_CALORIE_GOAL_MET,
                isCompleted = isCalorieGoalMetToday,
                progressFraction = (todayCalories.toFloat() / targetCal.toFloat()).coerceIn(0f, 1f),
                progressLabel = "$todayCalories / $targetCal kcal",
                category = "GOAL"
            ),
            DailyNutritionQuest(
                id = "quest_protein_goal",
                title = "Reach Daily Protein Target (90%+)",
                subtitle = "Fuel muscle recovery with ${targetProtein}g protein",
                xpReward = XP_PROTEIN_GOAL_MET,
                isCompleted = isProteinGoalMetToday,
                progressFraction = (todayProtein.toFloat() / targetProtein.toFloat()).coerceIn(0f, 1f),
                progressLabel = "${todayProtein.roundToInt()}g / ${targetProtein}g",
                category = "GOAL"
            ),
            DailyNutritionQuest(
                id = "quest_hydration_goal",
                title = "Complete Daily Hydration Target",
                subtitle = "Drink ${targetWater}ml water today (+10 XP per glass)",
                xpReward = XP_HYDRATION_GOAL_MET,
                isCompleted = isHydrationGoalMetToday,
                progressFraction = (todayWaterMl.toFloat() / targetWater.toFloat()).coerceIn(0f, 1f),
                progressLabel = "$todayWaterMl / ${targetWater}ml",
                category = "HYDRATION"
            )
        )

        // Populate recent XP reward feed
        todayEntries.take(4).forEach { entry ->
            val isScan = entry.source.contains("Scan", ignoreCase = true) ||
                entry.notes.contains("Barcode", ignoreCase = true)
            val xp = XP_PER_MEAL_LOGGED + if (isScan) XP_BONUS_AI_OR_BARCODE_SCAN else 0
            recentEvents.add(
                XpRewardEvent(
                    title = "Logged ${entry.foodName}",
                    subtitle = "${entry.mealType} • ${entry.calories} kcal (${entry.source})",
                    xpAmount = xp,
                    category = "MEAL"
                )
            )
        }
        if (isProteinGoalMetToday) {
            recentEvents.add(
                XpRewardEvent(
                    title = "Daily Protein Goal Reached!",
                    subtitle = "${todayProtein.roundToInt()}g / ${targetProtein}g target achieved",
                    xpAmount = XP_PROTEIN_GOAL_MET,
                    category = "GOAL"
                )
            )
        }
        if (isCalorieGoalMetToday) {
            recentEvents.add(
                XpRewardEvent(
                    title = "Daily Calorie Goal Hit!",
                    subtitle = "$todayCalories kcal within target zone",
                    xpAmount = XP_CALORIE_GOAL_MET,
                    category = "GOAL"
                )
            )
        }
        if (isHydrationGoalMetToday) {
            recentEvents.add(
                XpRewardEvent(
                    title = "Hydration Target Completed!",
                    subtitle = "${todayWaterMl}ml water logged today",
                    xpAmount = XP_HYDRATION_GOAL_MET,
                    category = "HYDRATION"
                )
            )
        }

        // Determine current Level Tier & Next Tier
        val currentTier = levelTiers.lastOrNull { totalXp >= it.minXp } ?: levelTiers.first()
        val nextTier = levelTiers.firstOrNull { it.level == currentTier.level + 1 }

        val tierSpan = (currentTier.maxXp - currentTier.minXp).coerceAtLeast(100)
        val xpIntoLevel = (totalXp - currentTier.minXp).coerceAtLeast(0)
        val xpNeeded = if (nextTier != null) {
            (nextTier.minXp - totalXp).coerceAtLeast(0)
        } else {
            0
        }
        val progressFraction = if (nextTier != null) {
            (xpIntoLevel.toFloat() / tierSpan.toFloat()).coerceIn(0.04f, 1f)
        } else {
            1f
        }

        val completedQuestsCount = dailyQuests.count { it.isCompleted }
        val canClaimMilestone = completedQuestsCount >= 2 && !milestoneClaimedToday

        return UserLevelSummary(
            currentLevel = currentTier.level,
            currentTier = currentTier,
            nextTier = nextTier,
            totalXp = totalXp,
            xpIntoCurrentLevel = xpIntoLevel,
            xpNeededForNextLevel = xpNeeded,
            levelProgressFraction = progressFraction,
            todayEarnedXp = todayEarnedXp,
            mealLoggingXp = mealLoggingXp,
            nutritionGoalsXp = nutritionGoalsXp,
            hydrationAndStreakXp = hydrationAndStreakXp,
            bonusClaimedXp = bonusXp,
            streakDays = distinctActiveDays,
            completedMealsCount = diaryEntries.size,
            goalsMetDaysCount = goalsMetDaysCount,
            dailyQuests = dailyQuests,
            recentXpEvents = recentEvents,
            isDailyMilestoneClaimedToday = milestoneClaimedToday,
            canClaimDailyMilestoneToday = canClaimMilestone
        )
    }
}

/**
 * Compact Header Pill showing User Level & XP for the Home Dashboard top bar.
 */
@Composable
fun UserLevelHeaderBadge(
    summary: UserLevelSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = DarkCharcoalText,
        modifier = modifier
            .clickable { onClick() }
            .testTag("header_user_level_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = summary.currentTier.badgeEmoji,
                fontSize = 13.sp
            )
            Text(
                text = "Lv.${summary.currentLevel}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LimeAccentStrong
            )
            Text(
                text = "• ${summary.totalXp} XP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Primary Interactive User Level & Experience Points (XP) Motivation Card.
 * Displays:
 * - Level Shield Ring + Current Level Title + Total XP + Progress bar to next level
 * - Today's XP Earned badge & Category Breakdown chips (Meal Logs, Goal Hits, Hydration/Streaks)
 * - Expandable Daily Nutrition Quests checklist with live progress bars
 * - Daily Milestone Bonus Chest (+100 XP) claim button & Full Level Roadmap Modal Dialog
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserLevelAndXpTrackerCard(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    weightRecords: List<WeightRecord>,
    selectedDateKey: String = todayDateKey(),
    onNavigateToLogMeal: () -> Unit = {},
    onNavigateToScanMeal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bonusRefreshTrigger by remember { mutableIntStateOf(0) }
    var isQuestsExpanded by remember { mutableStateOf(true) }
    var showRoadmapDialog by remember { mutableStateOf(false) }
    var justClaimedBanner by remember { mutableStateOf<String?>(null) }

    val summary = remember(profile, diaryEntries, waterRecords, weightRecords, selectedDateKey, bonusRefreshTrigger) {
        UserLevelManager.calculateLevelSummary(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            selectedDateKey = selectedDateKey
        )
    }

    val animatedProgress by animateFloatAsState(
        targetValue = summary.levelProgressFraction,
        animationSpec = tween(durationMillis = 800),
        label = "xp_level_progress"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_level_xp_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Banner: Level Ring Badge, Title, Today's XP Pill, and Roadmap Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF14261C),
                                Color(0xFF1E3A2B)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Circular XP Ring with Level Number
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Canvas(modifier = Modifier.size(60.dp)) {
                                    val strokeW = 6.dp.toPx()
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.16f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = LimeAccentStrong,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = summary.currentTier.badgeEmoji,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "LV.${summary.currentLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LimeAccentStrong
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = LimeAccentStrong
                                    ) {
                                        Text(
                                            text = "LEVEL ${summary.currentLevel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DarkCharcoalText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${summary.totalXp} XP",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = summary.currentTier.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${summary.currentTier.titleBn} • ${summary.currentTier.perkDescription}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Today's Earned XP Badge
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable { showRoadmapDialog = true }
                                .testTag("open_level_roadmap_button")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = LimeAccentStrong,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "+${summary.todayEarnedXp} XP",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LimeAccentStrong
                                    )
                                }
                                Text(
                                    text = "Today • Tiers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // XP Progress Bar to Next Level
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (summary.nextTier != null) {
                                    "Next: Lv.${summary.nextTier.level} ${summary.nextTier.titleEn} ${summary.nextTier.badgeEmoji}"
                                } else {
                                    "Max Tier Reached: NutriScan Grandmaster 👑"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = if (summary.nextTier != null) {
                                    "${summary.xpNeededForNextLevel} XP to Level ${summary.nextTier.level}"
                                } else {
                                    "${summary.totalXp} Total XP"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LimeAccentStrong
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.16f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                FreshGreenPrimary,
                                                LimeAccentStrong
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // 2. Three XP Source Pillar Badges (Meals Logged, Nutrition Goals Hit, Streak & Hydration)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XpSourceMiniTile(
                    title = "Meal Logs",
                    xpText = "${summary.mealLoggingXp} XP",
                    subtitle = "${summary.completedMealsCount} dishes",
                    bgColor = PastelGreenCard,
                    accentColor = FreshGreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                XpSourceMiniTile(
                    title = "Goals Met",
                    xpText = "${summary.nutritionGoalsXp} XP",
                    subtitle = "${summary.goalsMetDaysCount} goal days",
                    bgColor = PastelPeachCard,
                    accentColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                )
                XpSourceMiniTile(
                    title = "Hydration",
                    xpText = "${summary.hydrationAndStreakXp} XP",
                    subtitle = "${summary.streakDays}d streak",
                    bgColor = PastelBlueCard,
                    accentColor = PastelBlueAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Daily Milestone Bonus Chest (Unlocks when 2+ Daily Quests are completed)
            val completedQuestsCount = summary.dailyQuests.count { it.isCompleted }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (summary.isDailyMilestoneClaimedToday) {
                    PastelGreenCard.copy(alpha = 0.65f)
                } else if (summary.canClaimDailyMilestoneToday) {
                    LimeAccent.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (summary.canClaimDailyMilestoneToday) FreshGreenPrimary else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (summary.isDailyMilestoneClaimedToday || summary.canClaimDailyMilestoneToday) {
                                FreshGreenPrimary
                            } else {
                                DarkCharcoalText.copy(alpha = 0.12f)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (summary.isDailyMilestoneClaimedToday || summary.canClaimDailyMilestoneToday) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (summary.isDailyMilestoneClaimedToday) {
                                    "Daily Goal Bonus Claimed! (+100 XP)"
                                } else {
                                    "Daily Nutrition Milestone Chest (+100 XP)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (summary.isDailyMilestoneClaimedToday) {
                                    "Awesome work! Keep logging meals to level up faster."
                                } else {
                                    "Complete 2+ daily quests ($completedQuestsCount/5 done) to unlock +100 Bonus XP"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (summary.isDailyMilestoneClaimedToday) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = FreshGreenPrimary
                        ) {
                            Text(
                                text = "CLAIMED ✓",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (summary.canClaimDailyMilestoneToday) {
                                    UserLevelManager.claimDailyMilestoneChest(context, selectedDateKey)
                                    bonusRefreshTrigger++
                                    justClaimedBanner = "🎉 +100 Bonus XP Claimed! You're now at ${summary.totalXp + 100} XP!"
                                } else {
                                    onNavigateToLogMeal()
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (summary.canClaimDailyMilestoneToday) FreshGreenPrimary else DarkCharcoalText,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("claim_daily_xp_chest_button")
                        ) {
                            Text(
                                text = if (summary.canClaimDailyMilestoneToday) "Claim +100 XP" else "Earn XP",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (justClaimedBanner != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PastelGreenCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = justClaimedBanner.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // 4. Expandable Daily Quests & XP Missions Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isQuestsExpanded = !isQuestsExpanded }
                    .padding(vertical = 2.dp)
                    .testTag("toggle_daily_quests_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Today's XP Quests ($completedQuestsCount/${summary.dailyQuests.size} Completed)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isQuestsExpanded) "Hide" else "Show",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FreshGreenPrimary
                    )
                    Icon(
                        imageVector = if (isQuestsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isQuestsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.dailyQuests.forEach { quest ->
                        DailyQuestItemRow(
                            quest = quest,
                            onActionClick = {
                                if (quest.category == "MEAL") onNavigateToLogMeal()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action Buttons Row: Scan Meal for +40 XP | View Full Level Roadmap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRoadmapDialog = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_xp_roadmap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Level Roadmap",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onNavigateToScanMeal,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCharcoalText,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("scan_meal_for_xp_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = LimeAccentStrong,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan Meal (+40 XP)",
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRoadmapDialog) {
        UserLevelRoadmapModalDialog(
            summary = summary,
            onDismiss = { showRoadmapDialog = false }
        )
    }
}

@Composable
private fun XpSourceMiniTile(
    title: String,
    xpText: String,
    subtitle: String,
    bgColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DarkCharcoalText.copy(alpha = 0.7f),
                maxLines = 1
            )
            Text(
                text = xpText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DarkCharcoalText.copy(alpha = 0.75f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DailyQuestItemRow(
    quest: DailyNutritionQuest,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (quest.isCompleted) {
            PastelGreenCard.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!quest.isCompleted) onActionClick() }
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
                    Icon(
                        imageVector = when {
                            quest.isCompleted -> Icons.Default.CheckCircle
                            quest.category == "MEAL" -> Icons.Default.Restaurant
                            quest.category == "HYDRATION" -> Icons.Default.WaterDrop
                            else -> Icons.Default.LocalFireDepartment
                        },
                        contentDescription = null,
                        tint = if (quest.isCompleted) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = quest.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (quest.isCompleted) DarkCharcoalText else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = quest.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (quest.isCompleted) {
                                DarkCharcoalText.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (quest.isCompleted) FreshGreenPrimary else DarkCharcoalText
                ) {
                    Text(
                        text = if (quest.isCompleted) "+${quest.xpReward} XP ✓" else "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (quest.isCompleted) Color.White else LimeAccentStrong,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { quest.progressFraction.coerceIn(0.06f, 1f) },
                    color = if (quest.isCompleted) FreshGreenPrimary else PastelBlueAccent,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Text(
                    text = quest.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (quest.isCompleted) DarkCharcoalText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Modal Dialog displaying the full Level 1–10 Progression Roadmap,
 * how XP is earned, and today's XP activity log.
 */
@Composable
fun UserLevelRoadmapModalDialog(
    summary: UserLevelSummary,
    profile: UserProfile? = null,
    diaryEntries: List<FoodDiaryEntry> = emptyList(),
    waterRecords: List<WaterRecord> = emptyList(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = summary.currentTier.badgeEmoji, fontSize = 24.sp)
                Column {
                    Text(
                        text = "Level ${summary.currentLevel} • ${summary.currentTier.titleEn}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${summary.totalXp} Total XP • ${summary.xpNeededForNextLevel} XP to Next Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = FreshGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Virtual Badges & Milestones Section
                if (profile != null) {
                    AchievementBadgesRoadmapSection(
                        profile = profile,
                        diaryEntries = diaryEntries,
                        waterRecords = waterRecords
                    )
                    HorizontalDivider(color = LightOutline.copy(alpha = 0.5f))
                }

                // How to Earn XP Guide
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelGreenCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⚡ How You Earn XP Every Day",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                        Text(
                            text = "• Log any meal or food item: +25 XP\n" +
                                "• AI Camera or Barcode food scan bonus: +15 XP\n" +
                                "• Log Breakfast, Lunch & Dinner in one day: +40 XP\n" +
                                "• Hit Daily Calorie Goal (85%–110%): +60 XP\n" +
                                "• Hit Daily Protein Goal (90%+): +50 XP\n" +
                                "• Balanced Carbs & Fat Macros: +40 XP\n" +
                                "• Reach Daily Hydration Goal: +45 XP (+10 XP/glass)\n" +
                                "• Daily Milestone Chest (2+ Quests): +100 XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkCharcoalText.copy(alpha = 0.85f)
                        )
                    }
                }

                // Recent XP Activity
                if (summary.recentXpEvents.isNotEmpty()) {
                    Text(
                        text = "Today's XP Activity (+${summary.todayEarnedXp} XP)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    summary.recentXpEvents.forEach { ev ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ev.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "+${ev.xpAmount} XP",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshGreenPrimary
                            )
                        }
                    }
                    HorizontalDivider()
                }

                // All 10 Level Tiers
                Text(
                    text = "NutriScan Level Tiers (Lv.1 – Lv.10)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                UserLevelManager.levelTiers.forEach { tier ->
                    val isUnlocked = summary.totalXp >= tier.minXp
                    val isCurrent = summary.currentLevel == tier.level
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isCurrent -> LimeAccent.copy(alpha = 0.45f)
                            isUnlocked -> PastelGreenCard.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrent) 1.5.dp else 0.dp,
                                color = if (isCurrent) FreshGreenPrimary else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = tier.badgeEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Lv.${tier.level} • ${tier.titleEn}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${tier.titleBn} • ${tier.perkDescription}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isUnlocked) FreshGreenPrimary else DarkCharcoalText.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isCurrent) "CURRENT" else if (isUnlocked) "UNLOCKED" else "${tier.minXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.ExtraBold, color = FreshGreenPrimary)
            }
        }
    )
}
