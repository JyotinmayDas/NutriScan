package com.example.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas as ComposeCanvas
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.model.todayDateKey
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeAccentStrong
import com.example.ui.theme.PastelBlueAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import com.example.ui.theme.ProteinColor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Visual theme options for the generated Social Media Achievement Card.
 */
enum class AchievementCardTheme(
    val label: String,
    val bgStartHex: Int,
    val bgEndHex: Int,
    val cardSurfaceHex: Int,
    val primaryTextHex: Int,
    val secondaryTextHex: Int,
    val accentHex: Int
) {
    EMERALD_DARK(
        label = "Emerald Night",
        bgStartHex = 0xFF0F261B.toInt(),
        bgEndHex = 0xFF1A3D2C.toInt(),
        cardSurfaceHex = 0xFF214B36.toInt(),
        primaryTextHex = 0xFFFFFFFF.toInt(),
        secondaryTextHex = 0xFFB8E2C8.toInt(),
        accentHex = 0xFFA3E635.toInt()
    ),
    SUNLIT_LIME(
        label = "Sunlit Mint",
        bgStartHex = 0xFFD9F99D.toInt(),
        bgEndHex = 0xFFDCFCE7.toInt(),
        cardSurfaceHex = 0xFFFFFFFF.toInt(),
        primaryTextHex = 0xFF14261C.toInt(),
        secondaryTextHex = 0xFF365343.toInt(),
        accentHex = 0xFF16A34A.toInt()
    ),
    ROYAL_CHAMPION(
        label = "Champion Gold",
        bgStartHex = 0xFF1E1B4B.toInt(),
        bgEndHex = 0xFF3B123C.toInt(),
        cardSurfaceHex = 0xFF2E235A.toInt(),
        primaryTextHex = 0xFFFFFFFF.toInt(),
        secondaryTextHex = 0xFFE2D9F3.toInt(),
        accentHex = 0xFFFACC15.toInt()
    )
}

/**
 * Focus mode for the social summary card.
 */
enum class AchievementCardFocus(
    val label: String,
    val badgeTitle: String
) {
    DAILY_SUMMARY("Daily Macros", "DAILY NUTRITION GOAL SUMMARY"),
    LEVEL_AND_XP("Level & XP", "NUTRISCAN LEVEL & XP MILESTONE"),
    STREAK_CHAMPION("Streak Trophy", "CONSISTENCY & STREAK ACHIEVEMENT")
}

/**
 * Aggregated metrics for rendering the Daily Achievement Social Summary Card.
 */
data class DailyAchievementMetrics(
    val userName: String,
    val dateKey: String,
    val consumedCalories: Int,
    val targetCalories: Int,
    val caloriePct: Int,
    val proteinG: Int,
    val targetProteinG: Int,
    val proteinPct: Int,
    val carbsG: Int,
    val targetCarbsG: Int,
    val carbsPct: Int,
    val fatG: Int,
    val targetFatG: Int,
    val fatPct: Int,
    val waterMl: Int,
    val targetWaterMl: Int,
    val waterPct: Int,
    val mealsLoggedCount: Int,
    val streakDays: Int,
    val userLevel: Int,
    val levelTitle: String,
    val levelEmoji: String,
    val totalXp: Int,
    val todayEarnedXp: Int,
    val completedQuestsCount: Int,
    val totalQuestsCount: Int,
    val unlockedBadges: List<String>
)

/**
 * Helper object that computes daily achievement metrics, generates high-resolution
 * 1080x1350 PNG social summary cards, and triggers Android's native Share Sheet
 * for Instagram, WhatsApp, X, Telegram, Facebook, or saving to device.
 */
object SocialAchievementExporter {

    fun buildMetrics(
        context: Context?,
        profile: UserProfile,
        diaryEntries: List<FoodDiaryEntry>,
        waterRecords: List<WaterRecord>,
        weightRecords: List<WeightRecord>,
        selectedDateKey: String = todayDateKey()
    ): DailyAchievementMetrics {
        val levelSummary = UserLevelManager.calculateLevelSummary(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            selectedDateKey = selectedDateKey
        )

        val todayEntries = diaryEntries.filter { it.dateKey == selectedDateKey }
        val consumedCal = todayEntries.sumOf { it.calories }
        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val calPct = ((consumedCal.toFloat() / targetCal.toFloat()) * 100f).roundToInt().coerceIn(0, 150)

        val proG = todayEntries.sumOf { it.protein }.roundToInt()
        val targetProG = profile.effectiveProteinGoalG().coerceAtLeast(25)
        val proPct = ((proG.toFloat() / targetProG.toFloat()) * 100f).roundToInt().coerceIn(0, 150)

        val carbsG = todayEntries.sumOf { it.carbohydrates }.roundToInt()
        val targetCarbsG = profile.effectiveCarbsGoalG().coerceAtLeast(40)
        val carbsPct = ((carbsG.toFloat() / targetCarbsG.toFloat()) * 100f).roundToInt().coerceIn(0, 150)

        val fatG = todayEntries.sumOf { it.fat }.roundToInt()
        val targetFatG = profile.effectiveFatGoalG().coerceAtLeast(15)
        val fatPct = ((fatG.toFloat() / targetFatG.toFloat()) * 100f).roundToInt().coerceIn(0, 150)

        val waterMl = waterRecords.filter { it.dateKey == selectedDateKey }.sumOf { it.quantity }
        val targetWaterMl = profile.dailyWaterGoalMl.coerceAtLeast(1000)
        val waterPct = ((waterMl.toFloat() / targetWaterMl.toFloat()) * 100f).roundToInt().coerceIn(0, 150)

        val badges = mutableListOf<String>()
        if (context != null) {
            val achievementState = AchievementBadgeManager.evaluateAchievements(
                context = context,
                profile = profile,
                diaryEntries = diaryEntries,
                waterRecords = waterRecords,
                selectedDateKey = selectedDateKey
            )
            achievementState.badges.filter { it.isUnlocked }.forEach { b ->
                badges.add("${b.emoji} ${b.title}")
            }
        }
        if (calPct in 85..110 && badges.none { it.contains("Calorie", ignoreCase = true) }) badges.add("🎯 Calorie Target Hit")
        if (proPct >= 90 && badges.none { it.contains("Protein", ignoreCase = true) }) badges.add("💪 Protein Champion")
        if (waterPct >= 100 && badges.none { it.contains("Hydration", ignoreCase = true) }) badges.add("💧 Hydration Hero")
        if (todayEntries.size >= 3 && badges.none { it.contains("Meal", ignoreCase = true) }) badges.add("🍛 3-Meal Combo")
        if (levelSummary.streakDays >= 3 && badges.none { it.contains("Streak", ignoreCase = true) }) badges.add("🔥 ${levelSummary.streakDays}-Day Streak")
        if (badges.isEmpty()) {
            badges.add("🌱 Mindful Tracker")
            badges.add("⚡ +${levelSummary.todayEarnedXp} XP Earned")
        }

        return DailyAchievementMetrics(
            userName = profile.name.ifBlank { "NutriScan Explorer" },
            dateKey = selectedDateKey,
            consumedCalories = consumedCal,
            targetCalories = targetCal,
            caloriePct = calPct,
            proteinG = proG,
            targetProteinG = targetProG,
            proteinPct = proPct,
            carbsG = carbsG,
            targetCarbsG = targetCarbsG,
            carbsPct = carbsPct,
            fatG = fatG,
            targetFatG = targetFatG,
            fatPct = fatPct,
            waterMl = waterMl,
            targetWaterMl = targetWaterMl,
            waterPct = waterPct,
            mealsLoggedCount = todayEntries.size,
            streakDays = levelSummary.streakDays,
            userLevel = levelSummary.currentLevel,
            levelTitle = levelSummary.currentTier.titleEn,
            levelEmoji = levelSummary.currentTier.badgeEmoji,
            totalXp = levelSummary.totalXp,
            todayEarnedXp = levelSummary.todayEarnedXp,
            completedQuestsCount = levelSummary.dailyQuests.count { it.isCompleted },
            totalQuestsCount = levelSummary.dailyQuests.size,
            unlockedBadges = badges
        )
    }

    fun buildDefaultSocialCaption(
        metrics: DailyAchievementMetrics,
        focus: AchievementCardFocus,
        showExactNumbers: Boolean
    ): String {
        val header = when (focus) {
            AchievementCardFocus.DAILY_SUMMARY ->
                "${metrics.levelEmoji} Daily Nutrition Progress on NutriScan AI! (${metrics.dateKey})"
            AchievementCardFocus.LEVEL_AND_XP ->
                "⚡ Level ${metrics.userLevel} ${metrics.levelTitle} (${metrics.totalXp} XP) on NutriScan AI!"
            AchievementCardFocus.STREAK_CHAMPION ->
                "🔥 ${metrics.streakDays}-Day Healthy Nutrition Streak on NutriScan AI!"
        }

        val statsLine = if (showExactNumbers) {
            "🥗 Calories: ${metrics.consumedCalories}/${metrics.targetCalories} kcal (${metrics.caloriePct}%)\n" +
                "💪 Protein: ${metrics.proteinG}/${metrics.targetProteinG}g (${metrics.proteinPct}%) | " +
                "🌾 Carbs: ${metrics.carbsG}g | 🥑 Fat: ${metrics.fatG}g\n" +
                "💧 Hydration: ${metrics.waterMl}/${metrics.targetWaterMl} ml (${metrics.waterPct}%)"
        } else {
            "🎯 Calorie Goal: ${metrics.caloriePct}% | 💪 Protein Goal: ${metrics.proteinPct}%\n" +
                "💧 Hydration Goal: ${metrics.waterPct}% | 🍽️ Meals Logged: ${metrics.mealsLoggedCount}"
        }

        val xpLine = "🏆 Level ${metrics.userLevel} (${metrics.levelTitle}) • +${metrics.todayEarnedXp} XP Today • 🔥 ${metrics.streakDays}-Day Streak"
        val badgesLine = "✨ Unlocked: ${metrics.unlockedBadges.joinToString(" • ")}"
        val hashtags = "#NutriScanAI #HealthyHabits #MacroTracking #FitnessJourney #MindfulEating"

        return "$header\n\n$statsLine\n$xpLine\n$badgesLine\n\n$hashtags"
    }

    /**
     * Renders a crisp 1080x1350 Social Media Summary Card bitmap for sharing.
     */
    fun renderSocialCardBitmap(
        metrics: DailyAchievementMetrics,
        theme: AchievementCardTheme,
        focus: AchievementCardFocus,
        showExactNumbers: Boolean,
        customHeadline: String
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Full-Bleed Gradient Background
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

        // Decorative glowing orbs
        paint.color = theme.accentHex
        paint.alpha = 28
        canvas.drawCircle(width * 0.88f, height * 0.12f, 220f, paint)
        canvas.drawCircle(width * 0.12f, height * 0.85f, 260f, paint)
        paint.alpha = 255

        // 2. Outer Card Frame
        val outerRect = RectF(56f, 56f, width - 56f, height - 56f)
        paint.color = theme.cardSurfaceHex
        paint.alpha = if (theme == AchievementCardTheme.SUNLIT_LIME) 235 else 210
        canvas.drawRoundRect(outerRect, 56f, 56f, paint)
        paint.alpha = 255

        // Border stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = theme.accentHex
            alpha = 110
        }
        canvas.drawRoundRect(outerRect, 56f, 56f, borderPaint)

        // 3. Top Brand & Level Pill Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 40f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 28f
        }
        val accentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 26f
        }

        canvas.drawText("NUTRISCAN AI", 110f, 138f, accentTextPaint)
        canvas.drawText("${metrics.userName} • ${metrics.dateKey}", 110f, 182f, subPaint)

        // Level Pill on Top Right
        val pillRect = RectF(width - 420f, 102f, width - 104f, 178f)
        paint.color = theme.accentHex
        canvas.drawRoundRect(pillRect, 40f, 40f, paint)
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF14261C.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "LV.${metrics.userLevel} • ${metrics.totalXp} XP",
            pillRect.centerX(),
            pillRect.centerY() + 11f,
            pillTextPaint
        )

        // 4. Focus Banner Headline
        val focusBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 25f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(focus.badgeTitle, width / 2f, 255f, focusBannerPaint)

        val headlineText = customHeadline.ifBlank {
            when (focus) {
                AchievementCardFocus.DAILY_SUMMARY -> "Crushing Today's Nutrition Goals!"
                AchievementCardFocus.LEVEL_AND_XP -> "Level ${metrics.userLevel} ${metrics.levelTitle}!"
                AchievementCardFocus.STREAK_CHAMPION -> "${metrics.streakDays}-Day Healthy Tracking Streak!"
            }
        }
        val heroHeadlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 44f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(headlineText.take(36), width / 2f, 315f, heroHeadlinePaint)

        // 5. Center Circular Progress Ring (Calorie Goal / XP / Streak)
        val ringCenterX = width / 2f
        val ringCenterY = 535f
        val ringRadius = 155f
        val ringRect = RectF(
            ringCenterX - ringRadius,
            ringCenterY - ringRadius,
            ringCenterX + ringRadius,
            ringCenterY + ringRadius
        )

        val trackArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 30f
            strokeCap = Paint.Cap.ROUND
            color = theme.primaryTextHex
            alpha = 35
        }
        canvas.drawArc(ringRect, 0f, 360f, false, trackArcPaint)

        val mainProgressFraction = when (focus) {
            AchievementCardFocus.DAILY_SUMMARY -> (metrics.caloriePct / 100f).coerceIn(0.08f, 1f)
            AchievementCardFocus.LEVEL_AND_XP -> ((metrics.totalXp % 500) / 500f).coerceIn(0.15f, 1f)
            AchievementCardFocus.STREAK_CHAMPION -> (metrics.streakDays / 7f).coerceIn(0.15f, 1f)
        }
        val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 30f
            strokeCap = Paint.Cap.ROUND
            color = theme.accentHex
        }
        canvas.drawArc(ringRect, -90f, 360f * mainProgressFraction, false, activeArcPaint)

        // Center text inside ring
        val ringBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        val ringSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }

        val ringPrimaryText = when (focus) {
            AchievementCardFocus.DAILY_SUMMARY ->
                if (showExactNumbers) "${metrics.consumedCalories}" else "${metrics.caloriePct}%"
            AchievementCardFocus.LEVEL_AND_XP -> "LV.${metrics.userLevel}"
            AchievementCardFocus.STREAK_CHAMPION -> "${metrics.streakDays} DAYS"
        }
        val ringSecondaryText = when (focus) {
            AchievementCardFocus.DAILY_SUMMARY ->
                if (showExactNumbers) "of ${metrics.targetCalories} kcal (${metrics.caloriePct}%)" else "Daily Calorie Goal"
            AchievementCardFocus.LEVEL_AND_XP -> "+${metrics.todayEarnedXp} XP Today"
            AchievementCardFocus.STREAK_CHAMPION -> "${metrics.mealsLoggedCount} Meals Logged Today"
        }

        canvas.drawText(ringPrimaryText, ringCenterX, ringCenterY + 12f, ringBigPaint)
        canvas.drawText(ringSecondaryText, ringCenterX, ringCenterY + 58f, ringSubPaint)

        // 6. Four Macro & Hydration Stat Boxes (2x2 Grid)
        val boxTopY = 745f
        val boxWidth = 410f
        val boxHeight = 145f
        val leftColX = 110f
        val rightColX = width - 110f - boxWidth

        val statBoxes = listOf(
            Triple(
                "PROTEIN",
                if (showExactNumbers) "${metrics.proteinG}g / ${metrics.targetProteinG}g" else "${metrics.proteinPct}% Goal",
                metrics.proteinPct
            ),
            Triple(
                "CARBOHYDRATES",
                if (showExactNumbers) "${metrics.carbsG}g / ${metrics.targetCarbsG}g" else "${metrics.carbsPct}% Goal",
                metrics.carbsPct
            ),
            Triple(
                "HEALTHY FATS",
                if (showExactNumbers) "${metrics.fatG}g / ${metrics.targetFatG}g" else "${metrics.fatPct}% Goal",
                metrics.fatPct
            ),
            Triple(
                "HYDRATION",
                if (showExactNumbers) "${metrics.waterMl} / ${metrics.targetWaterMl} ml" else "${metrics.waterPct}% Goal",
                metrics.waterPct
            )
        )

        statBoxes.forEachIndexed { idx, (label, valueStr, pct) ->
            val colX = if (idx % 2 == 0) leftColX else rightColX
            val rowY = boxTopY + (idx / 2) * (boxHeight + 28f)
            val boxRect = RectF(colX, rowY, colX + boxWidth, rowY + boxHeight)

            paint.color = theme.primaryTextHex
            paint.alpha = if (theme == AchievementCardTheme.SUNLIT_LIME) 18 else 24
            canvas.drawRoundRect(boxRect, 28f, 28f, paint)
            paint.alpha = 255

            val boxLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.secondaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f
            }
            val boxValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.primaryTextHex
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 34f
            }

            canvas.drawText(label, colX + 28f, rowY + 46f, boxLabelPaint)
            canvas.drawText(valueStr, colX + 28f, rowY + 92f, boxValuePaint)

            // Progress bar inside box
            val barTrack = RectF(colX + 28f, rowY + 112f, colX + boxWidth - 28f, rowY + 126f)
            paint.color = theme.primaryTextHex
            paint.alpha = 40
            canvas.drawRoundRect(barTrack, 8f, 8f, paint)

            val fillWidth = ((barTrack.width()) * (pct / 100f).coerceIn(0.08f, 1f))
            val barFill = RectF(barTrack.left, barTrack.top, barTrack.left + fillWidth, barTrack.bottom)
            paint.color = theme.accentHex
            paint.alpha = 255
            canvas.drawRoundRect(barFill, 8f, 8f, paint)
        }

        // 7. Unlocked Badges & Streak Strip at Bottom
        val footerStripRect = RectF(110f, 1110f, width - 110f, 1200f)
        paint.color = theme.accentHex
        paint.alpha = 38
        canvas.drawRoundRect(footerStripRect, 26f, 26f, paint)
        paint.alpha = 255

        val badgeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 27f
            textAlign = Paint.Align.CENTER
        }
        val badgeSummaryText = metrics.unlockedBadges.take(3).joinToString("   •   ")
        canvas.drawText(badgeSummaryText, width / 2f, footerStripRect.centerY() + 10f, badgeLinePaint)

        // 8. Bottom Watermark
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryTextHex
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Generated with NutriScan AI • Smart Food & Macro Tracker • 🔥 ${metrics.streakDays}d Streak",
            width / 2f,
            1256f,
            watermarkPaint
        )

        return bitmap
    }

    /**
     * Saves the rendered PNG card to the FileProvider cache directory and launches
     * Android's native Share Sheet with both the image attachment and social caption.
     */
    fun shareAchievementCardToSocialMedia(
        context: Context,
        metrics: DailyAchievementMetrics,
        theme: AchievementCardTheme,
        focus: AchievementCardFocus,
        showExactNumbers: Boolean,
        customHeadline: String,
        captionText: String
    ): Result<File> {
        return runCatching {
            val bitmap = renderSocialCardBitmap(
                metrics = metrics,
                theme = theme,
                focus = focus,
                showExactNumbers = showExactNumbers,
                customHeadline = customHeadline
            )
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val outputFile = File(reportsDir, "nutriscan_achievement_${metrics.dateKey}.png")
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "My Daily Nutrition Progress on NutriScan AI")
                putExtra(Intent.EXTRA_TEXT, captionText)
                clipData = ClipData.newRawUri("NutriScan Achievement Card", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Achievement Card to Social Media").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            outputFile
        }
    }

    /**
     * Shares the text-only summary to social platforms or messaging apps.
     */
    fun shareTextSummaryOnly(context: Context, captionText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My NutriScan AI Daily Achievement")
            putExtra(Intent.EXTRA_TEXT, captionText)
        }
        val chooser = Intent.createChooser(intent, "Share Achievement Summary").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyCaptionToClipboard(context: Context, captionText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("NutriScan Social Caption", captionText))
    }
}

/**
 * Dashboard & Profile Card allowing users to preview their Daily Progress Achievement Summary Card,
 * customize its look, and share it to Instagram, WhatsApp, X, or any social app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareDailyAchievementCard(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    weightRecords: List<WeightRecord>,
    selectedDateKey: String = todayDateKey(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showShareStudioModal by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(AchievementCardTheme.EMERALD_DARK) }
    var selectedFocus by remember { mutableStateOf(AchievementCardFocus.DAILY_SUMMARY) }
    var showExactNumbers by remember { mutableStateOf(true) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    val metrics = remember(profile, diaryEntries, waterRecords, weightRecords, selectedDateKey) {
        SocialAchievementExporter.buildMetrics(
            context = context,
            profile = profile,
            diaryEntries = diaryEntries,
            waterRecords = waterRecords,
            weightRecords = weightRecords,
            selectedDateKey = selectedDateKey
        )
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("share_daily_achievement_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row
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
                        shape = CircleShape,
                        color = PastelGreenCard,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Share Daily Achievement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Generate a social summary card for Instagram, WhatsApp & X",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = PastelGreenCard,
                    modifier = Modifier
                        .clickable { showShareStudioModal = true }
                        .testTag("open_share_studio_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Customize",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                    }
                }
            }

            // 2. Live Interactive Social Summary Card Preview
            SocialAchievementCardLivePreview(
                metrics = metrics,
                theme = selectedTheme,
                focus = selectedFocus,
                showExactNumbers = showExactNumbers,
                customHeadline = "",
                onClick = { showShareStudioModal = true }
            )

            // 3. Quick Theme & Focus Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AchievementCardTheme.entries.forEach { themeOption ->
                        val isSelected = selectedTheme == themeOption
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) DarkCharcoalText else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { selectedTheme = themeOption }
                        ) {
                            Text(
                                text = themeOption.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) LimeAccentStrong else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable { showExactNumbers = !showExactNumbers }
                        .testTag("toggle_achievement_privacy_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (showExactNumbers) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showExactNumbers) "Exact" else "% Only",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (statusFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PastelGreenCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoalText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // 4. Primary Share Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showShareStudioModal = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("customize_achievement_card_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit Card",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        val defaultCaption = SocialAchievementExporter.buildDefaultSocialCaption(
                            metrics = metrics,
                            focus = selectedFocus,
                            showExactNumbers = showExactNumbers
                        )
                        val res = SocialAchievementExporter.shareAchievementCardToSocialMedia(
                            context = context,
                            metrics = metrics,
                            theme = selectedTheme,
                            focus = selectedFocus,
                            showExactNumbers = showExactNumbers,
                            customHeadline = "",
                            captionText = defaultCaption
                        )
                        statusFeedback = if (res.isSuccess) {
                            "✨ Summary Card generated! Choose a social app in the share sheet."
                        } else {
                            "Could not launch image share: ${res.exceptionOrNull()?.localizedMessage}"
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(48.dp)
                        .testTag("share_achievement_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share to Social Media",
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showShareStudioModal) {
        ShareAchievementStudioModalDialog(
            metrics = metrics,
            initialTheme = selectedTheme,
            initialFocus = selectedFocus,
            initialShowExactNumbers = showExactNumbers,
            onDismiss = { showShareStudioModal = false }
        )
    }
}

/**
 * Live Compose preview of the Social Achievement Card matching the 1080x1350 bitmap design.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialAchievementCardLivePreview(
    metrics: DailyAchievementMetrics,
    theme: AchievementCardTheme,
    focus: AchievementCardFocus,
    showExactNumbers: Boolean,
    customHeadline: String,
    onClick: () -> Unit = {}
) {
    val bgBrush = Brush.linearGradient(
        colors = listOf(Color(theme.bgStartHex), Color(theme.bgEndHex))
    )
    val primaryTextColor = Color(theme.primaryTextHex)
    val secondaryTextColor = Color(theme.secondaryTextHex)
    val accentColor = Color(theme.accentHex)

    val headline = customHeadline.ifBlank {
        when (focus) {
            AchievementCardFocus.DAILY_SUMMARY -> "Crushing Today's Nutrition Goals!"
            AchievementCardFocus.LEVEL_AND_XP -> "Level ${metrics.userLevel} ${metrics.levelTitle}!"
            AchievementCardFocus.STREAK_CHAMPION -> "${metrics.streakDays}-Day Healthy Tracking Streak!"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bgBrush)
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("social_achievement_live_preview")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Brand & Level Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NUTRISCAN AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Text(
                        text = "${metrics.userName} • ${metrics.dateKey}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor
                ) {
                    Text(
                        text = "${metrics.levelEmoji} LV.${metrics.userLevel} • ${metrics.totalXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Headline & Center Ring + Macro Summary Row
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = primaryTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left: Circular Progress Ring
                val ringProgress = when (focus) {
                    AchievementCardFocus.DAILY_SUMMARY -> (metrics.caloriePct / 100f).coerceIn(0.08f, 1f)
                    AchievementCardFocus.LEVEL_AND_XP -> ((metrics.totalXp % 500) / 500f).coerceIn(0.15f, 1f)
                    AchievementCardFocus.STREAK_CHAMPION -> (metrics.streakDays / 7f).coerceIn(0.15f, 1f)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(102.dp)
                ) {
                    ComposeCanvas(modifier = Modifier.size(94.dp)) {
                        val strokeW = 9.dp.toPx()
                        drawArc(
                            color = primaryTextColor.copy(alpha = 0.16f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * ringProgress,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (focus) {
                                AchievementCardFocus.DAILY_SUMMARY ->
                                    if (showExactNumbers) "${metrics.consumedCalories}" else "${metrics.caloriePct}%"
                                AchievementCardFocus.LEVEL_AND_XP -> "LV.${metrics.userLevel}"
                                AchievementCardFocus.STREAK_CHAMPION -> "🔥 ${metrics.streakDays}d"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryTextColor
                        )
                        Text(
                            text = when (focus) {
                                AchievementCardFocus.DAILY_SUMMARY ->
                                    if (showExactNumbers) "/ ${metrics.targetCalories} kcal" else "Calorie Goal"
                                AchievementCardFocus.LEVEL_AND_XP -> "+${metrics.todayEarnedXp} XP"
                                AchievementCardFocus.STREAK_CHAMPION -> "${metrics.mealsLoggedCount} meals"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }
                }

                // Right: 4 Macro & Hydration Mini Progress Rows
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PreviewMacroBarRow(
                        label = "Protein",
                        valueText = if (showExactNumbers) "${metrics.proteinG}/${metrics.targetProteinG}g" else "${metrics.proteinPct}%",
                        pct = metrics.proteinPct,
                        primaryTextColor = primaryTextColor,
                        accentColor = accentColor
                    )
                    PreviewMacroBarRow(
                        label = "Carbs",
                        valueText = if (showExactNumbers) "${metrics.carbsG}/${metrics.targetCarbsG}g" else "${metrics.carbsPct}%",
                        pct = metrics.carbsPct,
                        primaryTextColor = primaryTextColor,
                        accentColor = accentColor
                    )
                    PreviewMacroBarRow(
                        label = "Fats",
                        valueText = if (showExactNumbers) "${metrics.fatG}/${metrics.targetFatG}g" else "${metrics.fatPct}%",
                        pct = metrics.fatPct,
                        primaryTextColor = primaryTextColor,
                        accentColor = accentColor
                    )
                    PreviewMacroBarRow(
                        label = "Water",
                        valueText = if (showExactNumbers) "${metrics.waterMl}/${metrics.targetWaterMl}ml" else "${metrics.waterPct}%",
                        pct = metrics.waterPct,
                        primaryTextColor = primaryTextColor,
                        accentColor = accentColor
                    )
                }
            }

            // Unlocked Achievement Badges Strip
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                metrics.unlockedBadges.forEach { badge ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryTextColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMacroBarRow(
    label: String,
    valueText: String,
    pct: Int,
    primaryTextColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor.copy(alpha = 0.85f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = primaryTextColor
            )
        }
        LinearProgressIndicator(
            progress = { (pct / 100f).coerceIn(0.08f, 1f) },
            color = accentColor,
            trackColor = primaryTextColor.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

/**
 * Full Interactive Modal Studio for customizing the Social Achievement Summary Card,
 * editing the social media caption, copying hashtags, and sharing the PNG card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareAchievementStudioModalDialog(
    metrics: DailyAchievementMetrics,
    initialTheme: AchievementCardTheme = AchievementCardTheme.EMERALD_DARK,
    initialFocus: AchievementCardFocus = AchievementCardFocus.DAILY_SUMMARY,
    initialShowExactNumbers: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(initialTheme) }
    var selectedFocus by remember { mutableStateOf(initialFocus) }
    var showExactNumbers by remember { mutableStateOf(initialShowExactNumbers) }
    var customHeadline by remember { mutableStateOf("") }
    var captionInput by remember(selectedFocus, showExactNumbers) {
        mutableStateOf(
            SocialAchievementExporter.buildDefaultSocialCaption(
                metrics = metrics,
                focus = selectedFocus,
                showExactNumbers = showExactNumbers
            )
        )
    }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = FreshGreenPrimary
                )
                Column {
                    Text(
                        text = "Share Daily Achievement Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Customize & post your summary card to social media",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                // Live Preview
                SocialAchievementCardLivePreview(
                    metrics = metrics,
                    theme = selectedTheme,
                    focus = selectedFocus,
                    showExactNumbers = showExactNumbers,
                    customHeadline = customHeadline
                )

                // Card Focus Selector
                Text(
                    text = "1. Achievement Highlight Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AchievementCardFocus.entries.forEach { focusOption ->
                        FilterChip(
                            selected = selectedFocus == focusOption,
                            onClick = { selectedFocus = focusOption },
                            label = { Text(focusOption.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Card Theme Selector
                Text(
                    text = "2. Card Visual Theme",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AchievementCardTheme.entries.forEach { themeOption ->
                        FilterChip(
                            selected = selectedTheme == themeOption,
                            onClick = { selectedTheme = themeOption },
                            label = { Text(themeOption.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkCharcoalText,
                                selectedLabelColor = LimeAccentStrong
                            )
                        )
                    }
                }

                // Custom Headline Input
                OutlinedTextField(
                    value = customHeadline,
                    onValueChange = { customHeadline = it },
                    label = { Text("Custom Card Headline (Optional)") },
                    placeholder = { Text("e.g., Crushing My Protein & Calorie Goals!") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("achievement_custom_headline_input")
                )

                // Privacy Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Exact Calorie & Gram Numbers",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Turn off to share only goal percentages (%) and streaks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showExactNumbers,
                        onCheckedChange = { showExactNumbers = it }
                    )
                }

                // Editable Social Media Caption & Hashtags
                OutlinedTextField(
                    value = captionInput,
                    onValueChange = { captionInput = it },
                    label = { Text("Social Media Post Caption & Hashtags") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("achievement_social_caption_input")
                )

                // Secondary Actions: Copy Caption | Share Text Only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            SocialAchievementExporter.copyCaptionToClipboard(context, captionInput)
                            actionFeedback = "✓ Caption & hashtags copied to clipboard!"
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_social_caption_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Caption", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            SocialAchievementExporter.shareTextSummaryOnly(context, captionInput)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_text_only_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Text", fontWeight = FontWeight.Bold)
                    }
                }

                if (actionFeedback != null) {
                    Text(
                        text = actionFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val res = SocialAchievementExporter.shareAchievementCardToSocialMedia(
                        context = context,
                        metrics = metrics,
                        theme = selectedTheme,
                        focus = selectedFocus,
                        showExactNumbers = showExactNumbers,
                        customHeadline = customHeadline,
                        captionText = captionInput
                    )
                    if (res.isSuccess) {
                        actionFeedback = "✨ Social card ready in Share Sheet!"
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("studio_share_image_card_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post Image Card", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
