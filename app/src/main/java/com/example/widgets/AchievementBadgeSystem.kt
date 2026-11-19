package com.example.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.todayDateKey
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.FreshGreenPrimaryContainer
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeAccentStrong
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.PastelBlueAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachAccent
import com.example.ui.theme.PastelPeachCard
import com.example.ui.theme.WarmAmberTertiary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Categories for NutriScan Virtual Achievement Badges.
 */
enum class BadgeCategory(val displayName: String, val emoji: String) {
    ALL("All Badges", "🏆"),
    STREAK("Streaks", "🔥"),
    NUTRITION("Nutritional Targets", "🎯"),
    HABIT("Daily Habits", "💧"),
    DISCOVERY("AI & Discovery", "✨")
}

/**
 * Represents a single virtual achievement badge with its milestone criteria,
 * unlock status, progress fraction (0f..1f), and XP reward.
 */
data class VirtualAchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: BadgeCategory,
    val xpReward: Int,
    val isUnlocked: Boolean,
    val progress: Float, // 0f to 1f
    val progressLabel: String,
    val criteriaDetail: String,
    val unlockedDate: String? = null
)

/**
 * Consecutive food logging streak telemetry.
 */
data class ConsecutiveStreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val hasLoggedToday: Boolean,
    val hasLoggedYesterday: Boolean,
    val totalFoodLoggedDays: Int,
    val isSevenDayStreakMet: Boolean,
    val recent7DaysStatus: List<DayStreakStatus>
)

data class DayStreakStatus(
    val dayLabel: String,
    val dateKey: String,
    val isLogged: Boolean,
    val isToday: Boolean
)

/**
 * Combined state of the user's achievements and virtual badges.
 */
data class AchievementSystemState(
    val badges: List<VirtualAchievementBadge>,
    val streakInfo: ConsecutiveStreakInfo,
    val unlockedCount: Int,
    val totalCount: Int,
    val totalBadgesXpEarned: Int,
    val isSevenDayStreakUnlocked: Boolean,
    val newlyUnlockedBadge: VirtualAchievementBadge? = null
)

/**
 * Central engine to evaluate nutritional milestones, calculate consecutive food logging streaks,
 * and persist unlocked virtual badges and bonus XP awards.
 */
object AchievementBadgeManager {
    private const val PREFS_NAME = "nutriscan_achievement_badges_prefs"
    private const val KEY_UNLOCKED_BADGES = "unlocked_badge_ids"
    private const val KEY_CLAIMED_XP_BADGES = "claimed_xp_badge_ids"
    private const val KEY_BADGE_DATE_PREFIX = "unlocked_date_"
    private const val KEY_DEMO_7_DAY_STREAK = "demo_7_day_streak_active"

    private val _newlyUnlockedEvent = MutableStateFlow<VirtualAchievementBadge?>(null)
    val newlyUnlockedEvent: StateFlow<VirtualAchievementBadge?> = _newlyUnlockedEvent.asStateFlow()

    fun isDemoStreakActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEMO_7_DAY_STREAK, false)
    }

    fun setDemoStreakActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEMO_7_DAY_STREAK, active).apply()
    }

    /**
     * Calculates consecutive days where the user logged at least 1 food entry.
     */
    fun calculateConsecutiveFoodStreak(
        context: Context?,
        diaryEntries: List<FoodDiaryEntry>,
        todayKey: String = todayDateKey()
    ): ConsecutiveStreakInfo {
        val isDemo = if (context != null) isDemoStreakActive(context) else false

        val loggedDateKeys = diaryEntries
            .map { it.dateKey.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayDate = try { sdf.parse(todayKey) } catch (e: Exception) { null } ?: Date()

        // Helper to get normalized epoch day at 12:00 UTC
        fun toEpochDay(d: Date): Long {
            val cal = Calendar.getInstance(Locale.US)
            cal.time = d
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis / (24L * 60 * 60 * 1000)
        }

        val todayEpoch = toEpochDay(todayDate)

        val dayEpochs = loggedDateKeys.mapNotNull { key ->
            try {
                sdf.parse(key)?.let { toEpochDay(it) }
            } catch (e: Exception) {
                null
            }
        }.distinct().sorted()

        val hasLoggedToday = dayEpochs.contains(todayEpoch)
        val hasLoggedYesterday = dayEpochs.contains(todayEpoch - 1)

        // Calculate all-time longest streak
        var longestStreak = if (dayEpochs.isNotEmpty()) 1 else 0
        var runningStreak = 1
        for (i in 1 until dayEpochs.size) {
            if (dayEpochs[i] == dayEpochs[i - 1] + 1) {
                runningStreak++
                if (runningStreak > longestStreak) {
                    longestStreak = runningStreak
                }
            } else if (dayEpochs[i] > dayEpochs[i - 1] + 1) {
                runningStreak = 1
            }
        }

        // Calculate current ongoing streak
        var currentStreak = 0
        if (hasLoggedToday || hasLoggedYesterday) {
            var anchor = if (hasLoggedToday) todayEpoch else todayEpoch - 1
            while (dayEpochs.contains(anchor)) {
                currentStreak++
                anchor--
            }
        }

        // Apply Demo Mode if enabled by tester/user
        val finalCurrentStreak = if (isDemo) maxOf(currentStreak, 7) else currentStreak
        val finalLongestStreak = if (isDemo) maxOf(longestStreak, 7) else maxOf(longestStreak, currentStreak)
        val finalLoggedToday = if (isDemo) true else hasLoggedToday

        // Build recent 7 days status trail for visual rendering
        val recent7 = mutableListOf<DayStreakStatus>()
        val cal = Calendar.getInstance(Locale.US)
        cal.time = todayDate
        cal.add(Calendar.DAY_OF_YEAR, -6)

        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        for (offset in 0..6) {
            val d = cal.time
            val dKey = sdf.format(d)
            val dLabel = dayFormat.format(d).take(2)
            val dEpoch = toEpochDay(d)
            val isLogged = if (isDemo) true else dayEpochs.contains(dEpoch)
            val isToday = (offset == 6)
            recent7.add(
                DayStreakStatus(
                    dayLabel = dLabel,
                    dateKey = dKey,
                    isLogged = isLogged,
                    isToday = isToday
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return ConsecutiveStreakInfo(
            currentStreakDays = finalCurrentStreak,
            longestStreakDays = finalLongestStreak,
            hasLoggedToday = finalLoggedToday,
            hasLoggedYesterday = hasLoggedYesterday,
            totalFoodLoggedDays = if (isDemo) maxOf(dayEpochs.size, 7) else dayEpochs.size,
            isSevenDayStreakMet = finalLongestStreak >= 7,
            recent7DaysStatus = recent7
        )
    }

    /**
     * Evaluates all nutritional milestones, streak milestones, and unlocks virtual badges.
     */
    fun evaluateAchievements(
        context: Context,
        profile: UserProfile,
        diaryEntries: List<FoodDiaryEntry>,
        waterRecords: List<WaterRecord>,
        selectedDateKey: String = todayDateKey()
    ): AchievementSystemState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUnlockedIds = prefs.getStringSet(KEY_UNLOCKED_BADGES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val savedClaimedXpIds = prefs.getStringSet(KEY_CLAIMED_XP_BADGES, emptySet())?.toMutableSet() ?: mutableSetOf()

        val streakInfo = calculateConsecutiveFoodStreak(context, diaryEntries, selectedDateKey)

        // Today's consumption and targets
        val todayEntries = diaryEntries.filter { it.dateKey == selectedDateKey }
        val todayCal = todayEntries.sumOf { it.calories }
        val todayPro = todayEntries.sumOf { it.protein }.roundToInt()
        val todayCarbs = todayEntries.sumOf { it.carbohydrates }.roundToInt()
        val todayFat = todayEntries.sumOf { it.fat }.roundToInt()
        val todayFiber = todayEntries.sumOf { it.fiber }.roundToInt()

        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val targetPro = profile.effectiveProteinGoalG().coerceAtLeast(25)
        val targetCarbs = profile.effectiveCarbsGoalG().coerceAtLeast(40)
        val targetFat = profile.effectiveFatGoalG().coerceAtLeast(15)
        val targetFiber = profile.effectiveFiberGoalG().coerceAtLeast(15)
        val targetWater = profile.dailyWaterGoalMl.coerceAtLeast(1000)

        val todayWaterMl = waterRecords.filter { it.dateKey == selectedDateKey }.sumOf { it.quantity }

        // Historical evaluation across all logged dates
        val entriesByDate = diaryEntries.groupBy { it.dateKey }
        val waterByDate = waterRecords.groupBy { it.dateKey }

        // Nutritional conditions
        // 1. Calorie target hit (85% to 110%) on any logged day or today
        val anyDayCalorieTargetHit = entriesByDate.any { (_, dayEntries) ->
            val dayCals = dayEntries.sumOf { it.calories }
            dayCals.toFloat() / targetCal.toFloat() in 0.85f..1.10f
        } || (todayCal.toFloat() / targetCal.toFloat() in 0.85f..1.10f)

        // 2. Protein goal hit (>= 90% or 100%) on any day
        val anyDayProteinHit = entriesByDate.any { (_, dayEntries) ->
            dayEntries.sumOf { it.protein } >= targetPro * 0.90
        } || (todayPro >= targetPro * 0.90)

        // 3. Hydration goal hit (>= 100%) on any day
        val anyDayWaterHit = waterByDate.any { (_, dayLogs) ->
            dayLogs.sumOf { it.quantity } >= targetWater
        } || (todayWaterMl >= targetWater)

        // 4. Macro harmony: Balanced carbs, protein & fat on a single day
        val anyDayMacroHarmony = entriesByDate.any { (_, dayEntries) ->
            val p = dayEntries.sumOf { it.protein }
            val c = dayEntries.sumOf { it.carbohydrates }
            val f = dayEntries.sumOf { it.fat }
            p >= targetPro * 0.85 && c >= targetCarbs * 0.70 && f >= targetFat * 0.70
        } || (todayPro >= targetPro * 0.85 && todayCarbs >= targetCarbs * 0.70 && todayFat >= targetFat * 0.70)

        // 5. Fiber goal hit (>= 80%) on any day
        val anyDayFiberHit = entriesByDate.any { (_, dayEntries) ->
            dayEntries.sumOf { it.fiber } >= targetFiber * 0.80
        } || (todayFiber >= targetFiber * 0.80)

        // 6. Three main meals logged on same day
        val anyDayThreeMealsCombo = entriesByDate.any { (_, dayEntries) ->
            val types = dayEntries.map { it.mealType }.toSet()
            types.containsAll(listOf("Breakfast", "Lunch", "Dinner"))
        }

        // 7. Scanned food with AI lens or barcode
        val hasScannedFood = diaryEntries.any {
            it.source.contains("Scan", ignoreCase = true) || it.notes.contains("Barcode", ignoreCase = true)
        }

        // 8. 7-Day Streak of logging food
        val is7DayStreakAchieved = streakInfo.isSevenDayStreakMet

        // 9. First meal logged
        val hasLoggedFirstMeal = diaryEntries.isNotEmpty()

        // 10. Century club (100 meals logged)
        val hasCenturyClub = diaryEntries.size >= 100

        // Helper to check and record unlocked status
        fun checkUnlocked(badgeId: String, conditionMet: Boolean): Boolean {
            val alreadyUnlocked = savedUnlockedIds.contains(badgeId)
            if (conditionMet && !alreadyUnlocked) {
                savedUnlockedIds.add(badgeId)
                val todayStr = todayDateKey()
                prefs.edit()
                    .putStringSet(KEY_UNLOCKED_BADGES, savedUnlockedIds)
                    .putString(KEY_BADGE_DATE_PREFIX + badgeId, todayStr)
                    .apply()
            }
            return conditionMet || alreadyUnlocked
        }

        // Define Badges List
        val b7StreakUnlocked = checkUnlocked("badge_7_day_streak", is7DayStreakAchieved)
        val bCalUnlocked = checkUnlocked("badge_calorie_target", anyDayCalorieTargetHit)
        val bProUnlocked = checkUnlocked("badge_protein_goal", anyDayProteinHit)
        val bWaterUnlocked = checkUnlocked("badge_hydration_hero", anyDayWaterHit)
        val bMacroUnlocked = checkUnlocked("badge_macro_harmony", anyDayMacroHarmony)
        val bFiberUnlocked = checkUnlocked("badge_fiber_champion", anyDayFiberHit)
        val bThreeMealsUnlocked = checkUnlocked("badge_3_meals_combo", anyDayThreeMealsCombo)
        val bScanUnlocked = checkUnlocked("badge_ai_scanner", hasScannedFood)
        val bFirstMealUnlocked = checkUnlocked("badge_first_meal", hasLoggedFirstMeal)
        val bCenturyUnlocked = checkUnlocked("badge_100_meals", hasCenturyClub)

        fun getUnlockedDate(id: String): String? {
            return prefs.getString(KEY_BADGE_DATE_PREFIX + id, null)
        }

        val badges = listOf(
            VirtualAchievementBadge(
                id = "badge_7_day_streak",
                title = "7-Day Streak Master",
                description = "Maintain a 7-day streak of logging food consecutive days",
                emoji = "🔥",
                category = BadgeCategory.STREAK,
                xpReward = 500,
                isUnlocked = b7StreakUnlocked,
                progress = (streakInfo.currentStreakDays / 7f).coerceIn(0f, 1f),
                progressLabel = "${streakInfo.currentStreakDays} / 7 days logged",
                criteriaDetail = "Log at least one food or meal entry every day for 7 consecutive days to build healthy habits.",
                unlockedDate = getUnlockedDate("badge_7_day_streak")
            ),
            VirtualAchievementBadge(
                id = "badge_calorie_target",
                title = "Calorie Target Master",
                description = "Hit your daily calorie target within healthy range (85%–110%)",
                emoji = "🎯",
                category = BadgeCategory.NUTRITION,
                xpReward = 150,
                isUnlocked = bCalUnlocked,
                progress = if (bCalUnlocked) 1f else (todayCal.toFloat() / targetCal.toFloat()).coerceIn(0f, 1f),
                progressLabel = "$todayCal / $targetCal kcal",
                criteriaDetail = "Stay within 85% to 110% of your daily calorie target to maintain metabolic balance.",
                unlockedDate = getUnlockedDate("badge_calorie_target")
            ),
            VirtualAchievementBadge(
                id = "badge_protein_goal",
                title = "Protein Powerhouse",
                description = "Reach 100% of your daily protein target for muscle & satiety",
                emoji = "💪",
                category = BadgeCategory.NUTRITION,
                xpReward = 150,
                isUnlocked = bProUnlocked,
                progress = if (bProUnlocked) 1f else (todayPro.toFloat() / targetPro.toFloat()).coerceIn(0f, 1f),
                progressLabel = "${todayPro}g / ${targetPro}g protein",
                criteriaDetail = "Fuel your body with essential amino acids by reaching 100% of your daily protein goal.",
                unlockedDate = getUnlockedDate("badge_protein_goal")
            ),
            VirtualAchievementBadge(
                id = "badge_hydration_hero",
                title = "Hydration Hero",
                description = "Drink and log 100% of your daily water intake goal",
                emoji = "💧",
                category = BadgeCategory.HABIT,
                xpReward = 100,
                isUnlocked = bWaterUnlocked,
                progress = if (bWaterUnlocked) 1f else (todayWaterMl.toFloat() / targetWater.toFloat()).coerceIn(0f, 1f),
                progressLabel = "$todayWaterMl / $targetWater ml",
                criteriaDetail = "Keep your metabolism and hydration optimal by meeting your full daily water target.",
                unlockedDate = getUnlockedDate("badge_hydration_hero")
            ),
            VirtualAchievementBadge(
                id = "badge_macro_harmony",
                title = "Macro Harmony",
                description = "Hit balanced Carbs, Protein, and Fat proportions in one day",
                emoji = "⚖️",
                category = BadgeCategory.NUTRITION,
                xpReward = 200,
                isUnlocked = bMacroUnlocked,
                progress = if (bMacroUnlocked) 1f else 0.65f,
                progressLabel = if (bMacroUnlocked) "Balanced!" else "In Progress",
                criteriaDetail = "Reach healthy intake zones for Carbs, Protein, and Fats simultaneously on the same day.",
                unlockedDate = getUnlockedDate("badge_macro_harmony")
            ),
            VirtualAchievementBadge(
                id = "badge_fiber_champion",
                title = "Fiber Champion",
                description = "Hit at least 80% of your daily gut-healthy fiber goal",
                emoji = "🌾",
                category = BadgeCategory.NUTRITION,
                xpReward = 120,
                isUnlocked = bFiberUnlocked,
                progress = if (bFiberUnlocked) 1f else (todayFiber.toFloat() / targetFiber.toFloat()).coerceIn(0f, 1f),
                progressLabel = "${todayFiber}g / ${targetFiber}g fiber",
                criteriaDetail = "Promote gut health and steady digestion by reaching at least 80% of your daily dietary fiber goal.",
                unlockedDate = getUnlockedDate("badge_fiber_champion")
            ),
            VirtualAchievementBadge(
                id = "badge_3_meals_combo",
                title = "Full Day Nutritionist",
                description = "Log Breakfast, Lunch, and Dinner all in a single day",
                emoji = "🍛",
                category = BadgeCategory.HABIT,
                xpReward = 150,
                isUnlocked = bThreeMealsUnlocked,
                progress = if (bThreeMealsUnlocked) 1f else 0.5f,
                progressLabel = if (bThreeMealsUnlocked) "3/3 Meals" else "Breakfast, Lunch, Dinner",
                criteriaDetail = "Log your 3 primary daily meals to ensure steady energy and consistent macro tracking.",
                unlockedDate = getUnlockedDate("badge_3_meals_combo")
            ),
            VirtualAchievementBadge(
                id = "badge_ai_scanner",
                title = "AI Lens Pioneer",
                description = "Scan food or barcodes with AI smart camera scanner",
                emoji = "📸",
                category = BadgeCategory.DISCOVERY,
                xpReward = 100,
                isUnlocked = bScanUnlocked,
                progress = if (bScanUnlocked) 1f else 0f,
                progressLabel = if (bScanUnlocked) "Scanned!" else "Scan 1 meal",
                criteriaDetail = "Use the NutriScan AI camera food lens or barcode scanner to log a dish automatically.",
                unlockedDate = getUnlockedDate("badge_ai_scanner")
            ),
            VirtualAchievementBadge(
                id = "badge_first_meal",
                title = "First Step Forward",
                description = "Log your very first meal or snack in NutriScan",
                emoji = "🌱",
                category = BadgeCategory.DISCOVERY,
                xpReward = 50,
                isUnlocked = bFirstMealUnlocked,
                progress = if (bFirstMealUnlocked) 1f else 0f,
                progressLabel = if (bFirstMealUnlocked) "1st meal logged" else "Log 1 meal",
                criteriaDetail = "Kickstart your nutrition transformation journey by recording your first meal entry.",
                unlockedDate = getUnlockedDate("badge_first_meal")
            ),
            VirtualAchievementBadge(
                id = "badge_100_meals",
                title = "Century Club",
                description = "Log 100 total meals on your lifelong wellness journey",
                emoji = "👑",
                category = BadgeCategory.STREAK,
                xpReward = 500,
                isUnlocked = bCenturyUnlocked,
                progress = (diaryEntries.size / 100f).coerceIn(0f, 1f),
                progressLabel = "${diaryEntries.size} / 100 meals",
                criteriaDetail = "Demonstrate elite dedication to your nutrition by logging 100 total meals in NutriScan.",
                unlockedDate = getUnlockedDate("badge_100_meals")
            )
        )

        // Award bonus XP for newly unlocked badges that haven't credited XP yet
        var totalBadgesXpEarned = 0
        var newlyUnlockedCandidate: VirtualAchievementBadge? = null

        badges.filter { it.isUnlocked }.forEach { badge ->
            totalBadgesXpEarned += badge.xpReward
            if (!savedClaimedXpIds.contains(badge.id)) {
                savedClaimedXpIds.add(badge.id)
                UserLevelManager.addBonusXp(context, badge.xpReward)
                newlyUnlockedCandidate = badge
            }
        }

        if (savedClaimedXpIds.isNotEmpty()) {
            prefs.edit().putStringSet(KEY_CLAIMED_XP_BADGES, savedClaimedXpIds).apply()
        }

        val unlockedCount = badges.count { it.isUnlocked }

        return AchievementSystemState(
            badges = badges,
            streakInfo = streakInfo,
            unlockedCount = unlockedCount,
            totalCount = badges.size,
            totalBadgesXpEarned = totalBadgesXpEarned,
            isSevenDayStreakUnlocked = b7StreakUnlocked,
            newlyUnlockedBadge = newlyUnlockedCandidate
        )
    }

    /**
     * Shares a celebratory badge message via Android native share intent.
     */
    fun shareBadge(context: Context, badge: VirtualAchievementBadge, streakDays: Int) {
        val shareText = buildString {
            append("🏆 I unlocked the '${badge.title}' badge on NutriScan AI! ${badge.emoji}\n\n")
            append("✨ ${badge.description}\n")
            if (badge.id == "badge_7_day_streak") {
                append("🔥 Maintained a $streakDays-day consecutive food logging streak!\n")
            }
            append("⚡ Earned +${badge.xpReward} XP for my nutrition & wellness journey.\n\n")
            append("#NutriScanAI #HealthGoals #NutritionMilestone #FoodTracker")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Achievement Badge")
        context.startActivity(shareIntent)
    }
}

/**
 * Main Achievement Badges Card displayed on the Home Dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementBadgesCard(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    selectedDateKey: String = todayDateKey(),
    onNavigateToLogMeal: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDemoStreakActive by remember {
        mutableStateOf(AchievementBadgeManager.isDemoStreakActive(context))
    }

    val achievementState = remember(profile, diaryEntries, waterRecords, selectedDateKey, isDemoStreakActive) {
        AchievementBadgeManager.evaluateAchievements(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            selectedDateKey = selectedDateKey
        )
    }

    var selectedCategory by remember { mutableStateOf(BadgeCategory.ALL) }
    var selectedBadgeForDetail by remember { mutableStateOf<VirtualAchievementBadge?>(null) }
    var showOnlyUnlocked by remember { mutableStateOf(false) }

    val filteredBadges = remember(achievementState.badges, selectedCategory, showOnlyUnlocked) {
        achievementState.badges.filter { badge ->
            val matchesCategory = (selectedCategory == BadgeCategory.ALL || badge.category == selectedCategory)
            val matchesUnlocked = !showOnlyUnlocked || badge.isUnlocked
            matchesCategory && matchesUnlocked
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_badges_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with Title & Unlocked Badges Counter Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PastelGreenCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏆", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "Milestones & Badges",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                        Text(
                            text = "Unlock virtual trophies as you eat & log",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlateText
                        )
                    }
                }

                // Unlock Counter Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FreshGreenPrimaryContainer,
                    modifier = Modifier.testTag("badge_count_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Badges Unlocked",
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${achievementState.unlockedCount}/${achievementState.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshGreenPrimary
                        )
                    }
                }
            }

            // 2. FEATURED HERO BANNER: 7-Day Food Logging Streak Milestone
            SevenDayStreakHighlightBanner(
                streakInfo = achievementState.streakInfo,
                isUnlocked = achievementState.isSevenDayStreakUnlocked,
                isDemoActive = isDemoStreakActive,
                onToggleDemo = {
                    val newActive = !isDemoStreakActive
                    AchievementBadgeManager.setDemoStreakActive(context, newActive)
                    isDemoStreakActive = newActive
                },
                onBadgeClick = {
                    val b7 = achievementState.badges.firstOrNull { it.id == "badge_7_day_streak" }
                    if (b7 != null) selectedBadgeForDetail = b7
                }
            )

            // 3. Filter Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == BadgeCategory.ALL && !showOnlyUnlocked,
                        onClick = {
                            selectedCategory = BadgeCategory.ALL
                            showOnlyUnlocked = false
                        },
                        label = { Text("All (${achievementState.totalCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FreshGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_all_badges")
                    )
                }
                item {
                    FilterChip(
                        selected = showOnlyUnlocked,
                        onClick = { showOnlyUnlocked = !showOnlyUnlocked },
                        label = { Text("Unlocked (${achievementState.unlockedCount})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FreshGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_unlocked_badges")
                    )
                }
                BadgeCategory.entries.filter { it != BadgeCategory.ALL }.forEach { cat ->
                    item {
                        FilterChip(
                            selected = selectedCategory == cat && !showOnlyUnlocked,
                            onClick = {
                                selectedCategory = cat
                                showOnlyUnlocked = false
                            },
                            label = { Text("${cat.emoji} ${cat.displayName}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_category_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            // 4. Badges Carousel / Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                filteredBadges.forEach { badge ->
                    BadgeGridItem(
                        badge = badge,
                        onClick = { selectedBadgeForDetail = badge },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("badge_item_${badge.id}")
                    )
                }
            }

            // 5. Bottom XP Reward Callout
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PastelGreenCard.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Text(
                            text = "Total Badges XP: +${achievementState.totalBadgesXpEarned} XP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                    }
                    Text(
                        text = "Boosts Your Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = FreshGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Badge Details & Share Modal Dialog
    if (selectedBadgeForDetail != null) {
        BadgeDetailDialog(
            badge = selectedBadgeForDetail!!,
            streakDays = achievementState.streakInfo.currentStreakDays,
            onDismiss = { selectedBadgeForDetail = null },
            onShare = {
                AchievementBadgeManager.shareBadge(
                    context,
                    selectedBadgeForDetail!!,
                    achievementState.streakInfo.currentStreakDays
                )
            }
        )
    }
}

/**
 * Featured Banner showcasing the 7-Day Food Logging Streak milestone.
 */
@Composable
fun SevenDayStreakHighlightBanner(
    streakInfo: ConsecutiveStreakInfo,
    isUnlocked: Boolean,
    isDemoActive: Boolean,
    onToggleDemo: () -> Unit,
    onBadgeClick: () -> Unit
) {
    val progressFraction = (streakInfo.currentStreakDays / 7f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "7DayStreakProgress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onBadgeClick() }
            .testTag("streak_highlight_banner"),
        shape = RoundedCornerShape(18.dp),
        color = if (isUnlocked) PastelGreenCard else PastelPeachCard.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isUnlocked) 1.5.dp else 1.dp,
            color = if (isUnlocked) FreshGreenPrimary else WarmAmberTertiary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Emoji, Title, and Streak Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) Brush.linearGradient(
                                    listOf(FreshGreenPrimary, LimeAccent)
                                ) else Brush.linearGradient(
                                    listOf(WarmAmberTertiary.copy(alpha = 0.8f), PastelPeachAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔥", fontSize = 22.sp)
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "7-Day Food Streak Master",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                            if (isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unlocked",
                                    tint = FreshGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isUnlocked) {
                                "🏆 Unlocked! Maintained 7-day logging streak (+500 XP)"
                            } else {
                                "${streakInfo.currentStreakDays} of 7 days logged (${7 - streakInfo.currentStreakDays} days to go!)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUnlocked) FreshGreenPrimary else MutedSlateText,
                            fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Streak flame count badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUnlocked) FreshGreenPrimary else WarmAmberTertiary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = "🔥", fontSize = 12.sp)
                        Text(
                            text = "${streakInfo.currentStreakDays}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 7-Day Visual Trail (7 Dots / Day Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                streakInfo.recent7DaysStatus.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isLogged -> FreshGreenPrimary
                                        day.isToday -> LimeAccent.copy(alpha = 0.5f)
                                        else -> Color.LightGray.copy(alpha = 0.35f)
                                    }
                                )
                                .border(
                                    width = if (day.isToday) 2.dp else 0.dp,
                                    color = if (day.isToday) FreshGreenPrimary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.isLogged) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Day Logged",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) DarkCharcoalText else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (day.isToday) "Today" else day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (day.isToday) FreshGreenPrimary else MutedSlateText,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isUnlocked) FreshGreenPrimary else WarmAmberTertiary,
                trackColor = Color.White.copy(alpha = 0.7f),
                strokeCap = StrokeCap.Round
            )

            // Test / Demo Streak Toggle Row (Allows testers to test 7-day streak easily)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isUnlocked) "Tap badge to inspect & share" else "Log food daily to preserve streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlateText
                )

                // Quick test toggle button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDemoActive) FreshGreenPrimaryContainer else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isDemoActive) FreshGreenPrimary else Color.LightGray
                    ),
                    modifier = Modifier
                        .clickable { onToggleDemo() }
                        .testTag("toggle_demo_streak_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test 7-Day Streak",
                            modifier = Modifier.size(12.dp),
                            tint = if (isDemoActive) FreshGreenPrimary else Color.Gray
                        )
                        Text(
                            text = if (isDemoActive) "Demo Streak: 7 Days (ON)" else "Test 7-Day Streak",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemoActive) FreshGreenPrimary else DarkCharcoalText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual badge tile in the grid.
 */
@Composable
fun BadgeGridItem(
    badge: VirtualAchievementBadge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (badge.isUnlocked) {
        PastelGreenCard.copy(alpha = 0.35f)
    } else {
        Color(0xFFF9FBF9)
    }

    val borderColor = if (badge.isUnlocked) {
        FreshGreenPrimary.copy(alpha = 0.45f)
    } else {
        Color.LightGray.copy(alpha = 0.3f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Row: Avatar & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (badge.isUnlocked) {
                                Brush.linearGradient(listOf(PastelGreenCard, LimeAccent.copy(alpha = 0.5f)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFEAEAEA), Color(0xFFE0E0E0)))
                            }
                        )
                        .border(
                            width = if (badge.isUnlocked) 1.5.dp else 0.5.dp,
                            color = if (badge.isUnlocked) FreshGreenPrimary else Color.Gray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.emoji,
                        fontSize = 18.sp
                    )
                }

                if (badge.isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FreshGreenPrimaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Unlocked",
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "UNLOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshGreenPrimary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.Gray,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "LOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Title
            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (badge.isUnlocked) DarkCharcoalText else Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Short Description
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MutedSlateText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            // Progress bar and XP reward
            LinearProgressIndicator(
                progress = { badge.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (badge.isUnlocked) FreshGreenPrimary else LimeAccentStrong,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badge.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (badge.isUnlocked) FreshGreenPrimary else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "+${badge.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = WarmAmberTertiary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Rich Modal Dialog for inspecting badge criteria, progress, and sharing virtual badge.
 */
@Composable
fun BadgeDetailDialog(
    badge: VirtualAchievementBadge,
    streakDays: Int,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (badge.isUnlocked) {
                                Brush.linearGradient(listOf(FreshGreenPrimary, LimeAccent))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFEAEAEA), Color(0xFFC0C0C0)))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge.emoji, fontSize = 26.sp)
                }
                Column {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (badge.isUnlocked) FreshGreenPrimaryContainer else Color.LightGray.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = if (badge.isUnlocked) "🎉 UNLOCKED" else "🔒 IN PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) FreshGreenPrimary else Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "• +${badge.xpReward} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmAmberTertiary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkCharcoalText
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PastelGreenCard.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🎯 Milestone Criteria",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                        Text(
                            text = badge.criteriaDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlateText
                        )
                    }
                }

                // Current Progress Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Status",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                        Text(
                            text = badge.progressLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (badge.isUnlocked) FreshGreenPrimary else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { badge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (badge.isUnlocked) FreshGreenPrimary else LimeAccentStrong,
                        trackColor = Color.LightGray.copy(alpha = 0.35f),
                        strokeCap = StrokeCap.Round
                    )
                }

                if (badge.isUnlocked && !badge.unlockedDate.isNullOrBlank()) {
                    Text(
                        text = "📅 Unlocked on: ${badge.unlockedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlateText
                    )
                }
            }
        },
        confirmButton = {
            if (badge.isUnlocked) {
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("share_badge_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Badge")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Keep Going!")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}

/**
 * Section to embed inside UserLevelRoadmapModalDialog so user can inspect badges
 * from the top header Heart icon as well.
 */
@Composable
fun AchievementBadgesRoadmapSection(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    selectedDateKey: String = todayDateKey()
) {
    val context = LocalContext.current
    val achievementState = remember(profile, diaryEntries, waterRecords, selectedDateKey) {
        AchievementBadgeManager.evaluateAchievements(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            selectedDateKey = selectedDateKey
        )
    }

    var selectedBadge by remember { mutableStateOf<VirtualAchievementBadge?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏆 Virtual Badges (${achievementState.unlockedCount}/${achievementState.totalCount} Unlocked)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkCharcoalText
            )
            Text(
                text = "+${achievementState.totalBadgesXpEarned} XP",
                style = MaterialTheme.typography.labelSmall,
                color = WarmAmberTertiary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(achievementState.badges) { badge ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (badge.isUnlocked) PastelGreenCard else Color(0xFFF5F5F5),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (badge.isUnlocked) FreshGreenPrimary.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .clickable { selectedBadge = badge }
                        .testTag("modal_badge_${badge.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = badge.emoji, fontSize = 20.sp)
                        Text(
                            text = badge.title.take(12),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badge.isUnlocked) DarkCharcoalText else Color.Gray
                        )
                        if (badge.isUnlocked) {
                            Text(
                                text = "Unlocked",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(
                                text = "${(badge.progress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedBadge != null) {
        BadgeDetailDialog(
            badge = selectedBadge!!,
            streakDays = achievementState.streakInfo.currentStreakDays,
            onDismiss = { selectedBadge = null },
            onShare = {
                AchievementBadgeManager.shareBadge(
                    context,
                    selectedBadge!!,
                    achievementState.streakInfo.currentStreakDays
                )
            }
        )
    }
}
