package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.AppStrings
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeAccentStrong
import com.example.ui.theme.PastelBlueAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenAccent
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachAccent
import com.example.ui.theme.PastelPeachCard
import com.example.ui.theme.ProteinColor
import com.example.ui.theme.StreakBannerDark
import com.example.ui.theme.WaterBlue
import com.example.widgets.MacroBadgeChip
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun HomeDashboardScreen(
    strings: AppStrings,
    profile: UserProfile,
    selectedDateKey: String,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    weightRecords: List<WeightRecord>,
    remainingScansToday: Int,
    onNavigateToScan: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onQuickAddWater: (Int) -> Unit,
    onOpenOnboardingEdit: () -> Unit
) {
    var selectedViewMode by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Weekly Stats

    val todayEntries = diaryEntries.filter { it.dateKey == selectedDateKey }
    val consumedCalories = todayEntries.sumOf { it.calories }
    val targetCalories = profile.dailyCalorieGoal.coerceAtLeast(1000)
    val remainingCalories = (targetCalories - consumedCalories).coerceAtLeast(0)

    val totalProtein = todayEntries.sumOf { it.protein }
    val totalCarbs = todayEntries.sumOf { it.carbohydrates }
    val totalFat = todayEntries.sumOf { it.fat }

    val targetProteinG = ((targetCalories * 0.25) / 4.0).roundToInt().coerceAtLeast(50)
    val targetCarbsG = ((targetCalories * 0.50) / 4.0).roundToInt().coerceAtLeast(120)
    val targetFatG = ((targetCalories * 0.25) / 9.0).roundToInt().coerceAtLeast(40)

    val todayWaterMl = waterRecords.filter { it.dateKey == selectedDateKey }.sumOf { it.quantity }
    val waterGoalMl = profile.dailyWaterGoalMl.coerceAtLeast(1000)
    val remainingWaterMl = (waterGoalMl - todayWaterMl).coerceAtLeast(0)
    val glassesCount = (todayWaterMl / 250.0).roundToInt()

    val weekDays = remember {
        val cal = Calendar.getInstance()
        val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        val currentDowIndex = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
        dayLabels.mapIndexed { idx, dLabel ->
            val num = (todayDayOfMonth + (idx - currentDowIndex)).coerceIn(1, 31)
            Triple(dLabel, num.toString().padStart(2, '0'), idx == currentDowIndex)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top User Greeting & Notification/Goal Pill (Matching Uploaded Images 1 & 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // User Avatar Circle with Initial
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, FreshGreenPrimary.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = profile.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshGreenPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Today • $selectedDateKey",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Good Morning, ${profile.name.substringBefore(" ")}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Goal & Scan Quota Button + Notification Bell
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onOpenOnboardingEdit() }
                            .testTag("edit_goals_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$remainingScansToday scans",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onOpenOnboardingEdit() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Edit Goals",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Today / Weekly Segmented Pill Toggle (Matching Uploaded Image 1 Left)
        item {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    listOf(0 to "Today", 1 to "Weekly").forEach { (idx, title) ->
                        val isSelected = selectedViewMode == idx
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) DarkCharcoalText else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedViewMode = idx }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedViewMode == 0) {
            // 3. Soft Lime-Green Hero Card with 7-Day Strip & Semi-Circular Calorie Arc Gauge (Image 1 & 8)
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFD9F99D),
                                    Color(0xFFDCFCE7)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 7-Day Horizontal Calendar Strip (S M T [W 12] T F S)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            weekDays.forEach { (dayLetter, dayNum, isToday) ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isToday) DarkCharcoalText else Color.Transparent,
                                    modifier = Modifier.width(42.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = dayLetter,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isToday) Color.White.copy(alpha = 0.75f) else DarkCharcoalText.copy(alpha = 0.65f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = dayNum,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isToday) Color.White else DarkCharcoalText,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Semi-Circle Calorie Gauge with Eaten (left) and Target/Burned (right)
                        val progress by animateFloatAsState(
                            targetValue = (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f),
                            animationSpec = tween(750),
                            label = "hero_calorie_arc"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Eaten
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$consumedCalories",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkCharcoalText
                                )
                                Text(
                                    text = "Eaten",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkCharcoalText.copy(alpha = 0.7f)
                                )
                            }

                            // Center: Semi-Circular Arc Gauge (Matching Image 1 & 9)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(165.dp)
                                    .height(115.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(150.dp)
                                        .padding(top = 12.dp)
                                ) {
                                    val strokeWidth = 14.dp.toPx()
                                    drawArc(
                                        color = DarkCharcoalText.copy(alpha = 0.12f),
                                        startAngle = 180f,
                                        sweepAngle = 180f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = DarkCharcoalText,
                                        startAngle = 180f,
                                        sweepAngle = 180f * progress.coerceAtLeast(0.08f),
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 24.dp)
                                ) {
                                    Text(
                                        text = "$remainingCalories",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkCharcoalText
                                    )
                                    Text(
                                        text = strings.caloriesRemaining,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkCharcoalText.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Right: Daily Goal
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$targetCalories",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkCharcoalText
                                )
                                Text(
                                    text = "Goal kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkCharcoalText.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Three Pastel Macro Cards Side-by-Side (Carbs, Proteins, Fats - Matching Image 1 & 9)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PastelMacroCard(
                        title = strings.carbs,
                        currentG = totalCarbs.roundToInt(),
                        targetG = targetCarbsG,
                        cardBg = PastelBlueCard,
                        accentColor = PastelBlueAccent,
                        modifier = Modifier.weight(1f)
                    )
                    PastelMacroCard(
                        title = strings.protein,
                        currentG = totalProtein.roundToInt(),
                        targetG = targetProteinG,
                        cardBg = PastelGreenCard,
                        accentColor = PastelGreenAccent,
                        modifier = Modifier.weight(1f)
                    )
                    PastelMacroCard(
                        title = strings.fat,
                        currentG = totalFat.roundToInt(),
                        targetG = targetFatG,
                        cardBg = PastelPeachCard,
                        accentColor = PastelPeachAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. "✨ Generate My Day" Full-Width Lime Pill Button + Quick Scan / Quick Add Row (Image 1)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateToCoach,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = DarkCharcoalText
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("dashboard_ai_coach_card")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate My Day • Smart Meal Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToScan,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCharcoalText,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.15f)
                                .height(52.dp)
                                .testTag("dashboard_quick_scan_button")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.quickScanMeal, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onNavigateToDiary,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("dashboard_quick_add_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.quickAddFood, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 6. Log Water Intake Card with - 03 + Stepper (Matching Uploaded Image 2 Center)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Log Water Intake",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = PastelBlueCard,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = WaterBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${todayWaterMl}/${waterGoalMl}ml",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (remainingWaterMl > 0) "You still need $remainingWaterMl ml today"
                                        else "Hydration goal reached today!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // -  03  + Stepper + Quick Glass Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { onQuickAddWater(150) }
                                        .testTag("water_add_150")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Add small cup",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = glassesCount.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = FreshGreenPrimary,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { onQuickAddWater(250) }
                                        .testTag("water_add_250")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add 250ml Glass",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Planned Meals 2x2 Grid (Breakfast, Lunch, Dinner, Snacks - Matching Uploaded Image 2 Center)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Planned Meals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToDiary() }
                        ) {
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.labelLarge,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val mealPairs = listOf(
                        listOf("Breakfast" to "350 - 450", "Lunch" to "550 - 700"),
                        listOf("Dinner" to "450 - 600", "Snacks" to "150 - 250")
                    )

                    mealPairs.forEach { rowMeals ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMeals.forEach { (mealTitle, recRange) ->
                                val loggedKcal = todayEntries.filter { it.mealType == mealTitle }.sumOf { it.calories }
                                PlannedMealGridCard(
                                    mealTitle = mealTitle,
                                    recommendedKcalRange = recRange,
                                    loggedKcal = loggedKcal,
                                    onAddClick = onNavigateToDiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 8. Today's Activity / Logged Dishes Cards (Matching Uploaded Images 3 & 9)
            if (todayEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "Today's Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                todayEntries.take(5).forEach { entry ->
                    item {
                        LoggedMealActivityCard(
                            entry = entry,
                            onClick = onNavigateToDiary
                        )
                    }
                }
            }
        } else {
            // WEEKLY PROGRESS & STATISTICS VIEW (Matching Uploaded Image 1 Right & Image 2 Right)
            item {
                WeeklyProgressAndStatsSection(
                    diaryEntries = diaryEntries,
                    profile = profile,
                    weightRecords = weightRecords,
                    consumedCalories = consumedCalories,
                    targetCalories = targetCalories
                )
            }
        }

        item {
            Text(
                text = strings.medicalDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PastelMacroCard(
    title: String,
    currentG: Int,
    targetG: Int,
    cardBg: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val pct = ((currentG.toFloat() / targetG.coerceAtLeast(1).toFloat()) * 100).roundToInt().coerceIn(0, 100)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = DarkCharcoalText.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${currentG}/${targetG}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DarkCharcoalText
            )
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0.08f, 1f) },
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlannedMealGridCard(
    mealTitle: String,
    recommendedKcalRange: String,
    loggedKcal: Int,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onAddClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = mealTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = FreshGreenPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (loggedKcal > 0) "Logged $loggedKcal kcal" else "Rec. $recommendedKcalRange kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overlapping circular dish thumbnails
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Surface(
                        shape = CircleShape,
                        color = PastelGreenCard,
                        modifier = Modifier
                            .size(34.dp)
                            .border(2.dp, Color.White, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = PastelGreenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Soft Mint Circular + Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to $mealTitle",
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoggedMealActivityCard(
    entry: FoodDiaryEntry,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                contentDescription = entry.foodName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${entry.calories} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary
                    )
                }
                Text(
                    text = "${entry.mealType} • ${entry.quantity.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MacroBadgeChip(label = "P", grams = entry.protein, color = ProteinColor)
                    MacroBadgeChip(label = "C", grams = entry.carbohydrates, color = CarbsColor)
                    MacroBadgeChip(label = "F", grams = entry.fat, color = FatColor)
                }
            }
        }
    }
}

/**
 * Weekly Progress & Statistics Section matching Uploaded Image 1 (Right) & Image 2 (Right).
 */
@Composable
private fun WeeklyProgressAndStatsSection(
    diaryEntries: List<FoodDiaryEntry>,
    profile: UserProfile,
    weightRecords: List<WeightRecord>,
    consumedCalories: Int,
    targetCalories: Int
) {
    val progressPct = ((consumedCalories.toFloat() / targetCalories.coerceAtLeast(1).toFloat()) * 100)
        .roundToInt()
        .coerceIn(15, 100)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Lime Green "Total progress" Card with Floating Dark Tooltip (Image 1 Right)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LimeAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCharcoalText
                    ) {
                        Text(
                            text = "$progressPct%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressPct / 100f },
                    color = DarkCharcoalText,
                    trackColor = DarkCharcoalText.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Balanced meals fuel energy, boost focus, and drive consistent progress toward ${profile.targetWeight} kg.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkCharcoalText.copy(alpha = 0.8f)
                )
            }
        }

        // 2. Two Pastel Cards Side-by-Side: Nutrition (Peach) & Fitness Target (Blue) (Image 1 Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PastelPeachCard),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔥 Nutrition", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = DarkCharcoalText)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = DarkCharcoalText, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "$consumedCalories kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PastelBlueCard),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⚡ Target", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = DarkCharcoalText)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = DarkCharcoalText, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "$targetCalories kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkCharcoalText
                    )
                }
            }
        }

        // 3. Dark "DAY STREAK KEEP GOING!" Banner Card with Food Bowl Photo (Image 1 Right)
        val streakDays = diaryEntries.map { it.dateKey }.distinct().size.coerceAtLeast(4)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StreakBannerDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$streakDays",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DAY STREAK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "KEEP GOING!",
                            style = MaterialTheme.typography.labelLarge,
                            color = LimeAccentStrong,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.img_hero_nutrition_bowl_1793693566593),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(2.dp, LimeAccentStrong, CircleShape)
                )
            }
        }

        // 4. Weekly Stats Vertical Bar Chart (Sat Sun Mon Tue Wed Thu Fri - Image 2 Right)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly Stats",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$consumedCalories kcal",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$targetCalories kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FreshGreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val days = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
                val ratios = listOf(0.58f, 0.85f, 0.48f, 0.62f, 0.72f, 0.54f, (consumedCalories.toFloat() / targetCalories).coerceIn(0.35f, 0.95f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEachIndexed { i, dayLabel ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .width(14.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFDCFCE7))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(ratios[i])
                                        .clip(RoundedCornerShape(50))
                                        .background(FreshGreenPrimary)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Current Weight: ${profile.currentWeight} kg • Goal: ${profile.targetWeight} kg (${weightRecords.size} logs)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
