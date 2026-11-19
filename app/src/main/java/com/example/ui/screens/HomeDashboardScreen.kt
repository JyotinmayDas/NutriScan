package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FoodDiaryEntry
import com.example.model.SavedRecipe
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.QuickThemeToggleButton
import com.example.ui.theme.WaterBlue
import com.example.widgets.DailyCalorieCircularProgressCard
import com.example.widgets.DailyMacroSummary
import com.example.widgets.MyRecipesHomeQuickAccessCard
import com.example.widgets.NativeAdvancedAdCard
import com.example.widgets.SetDailyNutritionGoalsDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    selectedDateKey: String,
    diaryEntries: List<FoodDiaryEntry>,
    waterRecords: List<WaterRecord>,
    savedRecipes: List<SavedRecipe> = emptyList(),
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    onNavigateToScan: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToMyRecipes: () -> Unit = {},
    onQuickAddWater: (Int) -> Unit,
    onDecrementWater: (Int) -> Unit = {},
    onSaveProfile: (UserProfile) -> Unit = {},
    onQuickLogSavedRecipe: (SavedRecipe) -> Unit = {}
) {
    var showSetGoalsModal by remember { mutableStateOf(false) }

    val todayEntries = remember(diaryEntries, selectedDateKey) {
        diaryEntries.filter { it.dateKey == selectedDateKey }
    }
    val consumedCalories = todayEntries.sumOf { it.calories }
    val targetCalories = profile.dailyCalorieGoal.coerceAtLeast(800)

    val totalProtein = todayEntries.sumOf { it.protein }
    val totalCarbs = todayEntries.sumOf { it.carbohydrates }
    val totalFat = todayEntries.sumOf { it.fat }

    val targetProteinG = profile.effectiveProteinGoalG()
    val targetCarbsG = profile.effectiveCarbsGoalG()
    val targetFatG = profile.effectiveFatGoalG()

    val todayWaterMl = remember(waterRecords, selectedDateKey) {
        waterRecords.filter { it.dateKey == selectedDateKey }.sumOf { it.quantity }
    }
    val waterGoalMl = profile.dailyWaterGoalMl.coerceAtLeast(1000)

    val displayName = remember(profile.name) {
        profile.name.trim().substringBefore(" ").ifBlank { "there" }
    }
    val todayLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Minimal Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Hi, $displayName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                QuickThemeToggleButton(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }

        // 2. Daily Calorie Ring & Macro Progress Card
        item {
            DailyCalorieCircularProgressCard(
                consumedCalories = consumedCalories,
                targetCalories = targetCalories,
                macroSummary = DailyMacroSummary(
                    carbsG = totalCarbs,
                    targetCarbsG = targetCarbsG.toDouble(),
                    fatG = totalFat,
                    targetFatG = targetFatG.toDouble(),
                    proteinG = totalProtein,
                    targetProteinG = targetProteinG.toDouble()
                ),
                onAdjustGoalClick = { showSetGoalsModal = true }
            )
        }

        // 3. Clean Two-Action Row: Scan Food & Add Meal
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToScan,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(54.dp)
                        .testTag("dashboard_quick_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Food",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = onNavigateToDiary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("dashboard_quick_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Meal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Compact Hydration Card
        item {
            SimpleWaterCard(
                consumedMl = todayWaterMl,
                goalMl = waterGoalMl,
                onAdd = { onQuickAddWater(250) },
                onSubtract = { onDecrementWater(250) }
            )
        }

        // 5. Today's Meals Summary
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Meals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onNavigateToDiary)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Open Diary",
                                style = MaterialTheme.typography.labelMedium,
                                color = FreshGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val mealSlots = listOf(
                        Triple("Breakfast", Icons.Rounded.FreeBreakfast, Color(0xFFF59E0B)),
                        Triple("Lunch", Icons.Rounded.LunchDining, FreshGreenPrimary),
                        Triple("Dinner", Icons.Rounded.DinnerDining, Color(0xFF6366F1)),
                        Triple("Snacks", Icons.Rounded.Icecream, Color(0xFFEC4899))
                    )

                    mealSlots.forEachIndexed { idx, (slot, icon, accent) ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                        val slotEntries = todayEntries.filter { it.mealType == slot }
                        val slotKcal = slotEntries.sumOf { it.calories }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onNavigateToDiary)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = accent.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = slot,
                                            tint = accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = slot,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (slotEntries.isEmpty()) "Not logged" else "${slotEntries.size} item(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "$slotKcal kcal",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (slotKcal > 0) FreshGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 6. Native Advanced Ad Card (hides automatically when unloaded or Pro)
        item {
            NativeAdvancedAdCard(isPremium = profile.isPremium)
        }

        // 7. Quick-Access Saved Recipes (only shown when user has saved recipes)
        if (savedRecipes.isNotEmpty()) {
            item {
                MyRecipesHomeQuickAccessCard(
                    savedRecipes = savedRecipes,
                    onQuickLogRecipe = onQuickLogSavedRecipe,
                    onOpenMyRecipes = onNavigateToMyRecipes
                )
            }
        }
    }

    if (showSetGoalsModal) {
        SetDailyNutritionGoalsDialog(
            profile = profile,
            onDismiss = { showSetGoalsModal = false },
            onSaveGoals = { updated ->
                onSaveProfile(updated)
                showSetGoalsModal = false
            }
        )
    }
}

@Composable
private fun SimpleWaterCard(
    consumedMl: Int,
    goalMl: Int,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    val progress = (consumedMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_water_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = WaterBlue.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = "Water Intake",
                                tint = WaterBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Water",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$consumedMl / $goalMl ml",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onSubtract,
                        enabled = consumedMl > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("water_minus_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Remove 250 ml water",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("water_plus_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = WaterBlue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add 250 ml water",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = WaterBlue,
                trackColor = WaterBlue.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}
