package com.example.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Interactive Daily Calorie & Macronutrient Goal Tracker card displaying animated intake
 * progress bars for Calories, Protein, Carbohydrates, Fat, and Dietary Fiber, with a
 * built-in "Set Goals" modal to customize daily targets.
 */
@Composable
fun DailyCalorieAndMacroGoalTrackerCard(
    profile: UserProfile,
    consumedCalories: Int,
    consumedProteinG: Double,
    consumedCarbsG: Double,
    consumedFatG: Double,
    consumedFiberG: Double = 0.0,
    onSaveUpdatedProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSetGoalsModal by remember { mutableStateOf(false) }

    val targetCalories = profile.dailyCalorieGoal.coerceAtLeast(800)
    val targetProteinG = profile.effectiveProteinGoalG()
    val targetCarbsG = profile.effectiveCarbsGoalG()
    val targetFatG = profile.effectiveFatGoalG()
    val targetFiberG = profile.effectiveFiberGoalG()

    val calProgress by animateFloatAsState(
        targetValue = (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(650),
        label = "cal_goal_bar"
    )
    val calPct = ((consumedCalories.toFloat() / targetCalories.toFloat()) * 100).roundToInt().coerceAtLeast(0)
    val remainingCal = targetCalories - consumedCalories

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_calorie_macro_goal_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header + "Set Goals" Action Pill
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Calorie & Macro Goals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (profile.useCustomNutritionGoals) {
                                "Custom Targets • ${targetCalories} kcal"
                            } else {
                                "Recommended Targets • ${targetCalories} kcal"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = FreshGreenPrimary,
                    modifier = Modifier
                        .clickable { showSetGoalsModal = true }
                        .testTag("open_set_macro_goals_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Set Daily Targets",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Set Goals",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // 1. Primary Daily Calorie Intake Progress Bar
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PastelGreenCard.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Calories Intake",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkCharcoalText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (remainingCal >= 0) FreshGreenPrimary.copy(alpha = 0.15f) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (remainingCal >= 0) "$remainingCal kcal left" else "${-remainingCal} kcal over",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingCal >= 0) FreshGreenPrimary else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "$consumedCalories / $targetCalories kcal ($calPct%)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                    }

                    LinearProgressIndicator(
                        progress = { calProgress.coerceAtLeast(0.03f) },
                        color = FreshGreenPrimary,
                        trackColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag("calorie_goal_progress_bar")
                    )
                }
            }

            // 2. Macronutrient Progress Bars (Protein, Carbohydrates, Fat, Fiber)
            MacroIntakeProgressBarRow(
                label = "Protein",
                consumedG = consumedProteinG.roundToInt(),
                targetG = targetProteinG,
                kcalPerGram = 4,
                barColor = ProteinColor,
                testTag = "protein_goal_progress_bar"
            )

            MacroIntakeProgressBarRow(
                label = "Carbohydrates",
                consumedG = consumedCarbsG.roundToInt(),
                targetG = targetCarbsG,
                kcalPerGram = 4,
                barColor = CarbsColor,
                testTag = "carbs_goal_progress_bar"
            )

            MacroIntakeProgressBarRow(
                label = "Healthy Fats",
                consumedG = consumedFatG.roundToInt(),
                targetG = targetFatG,
                kcalPerGram = 9,
                barColor = FatColor,
                testTag = "fat_goal_progress_bar"
            )

            MacroIntakeProgressBarRow(
                label = "Dietary Fiber",
                consumedG = consumedFiberG.roundToInt(),
                targetG = targetFiberG,
                kcalPerGram = 0,
                barColor = FreshGreenPrimary,
                testTag = "fiber_goal_progress_bar"
            )
        }
    }

    if (showSetGoalsModal) {
        SetDailyNutritionGoalsDialog(
            profile = profile,
            onDismiss = { showSetGoalsModal = false },
            onSaveGoals = { updatedProfile ->
                onSaveUpdatedProfile(updatedProfile)
                showSetGoalsModal = false
            }
        )
    }
}

@Composable
private fun MacroIntakeProgressBarRow(
    label: String,
    consumedG: Int,
    targetG: Int,
    kcalPerGram: Int,
    barColor: Color,
    testTag: String
) {
    val safeTarget = targetG.coerceAtLeast(1)
    val rawRatio = consumedG.toFloat() / safeTarget.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = testTag
    )
    val pct = (rawRatio * 100).roundToInt().coerceAtLeast(0)
    val remainingG = (safeTarget - consumedG).coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (kcalPerGram > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${consumedG * kcalPerGram} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${consumedG}g / ${safeTarget}g",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "($pct% • ${remainingG}g left)",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress.coerceAtLeast(0.03f) },
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp))
                .testTag(testTag)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetDailyNutritionGoalsDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSaveGoals: (UserProfile) -> Unit
) {
    var caloriesInput by remember(profile) {
        mutableStateOf(profile.dailyCalorieGoal.coerceIn(800, 6000).toString())
    }
    var proteinInput by remember(profile) {
        mutableStateOf(profile.effectiveProteinGoalG().toString())
    }
    var carbsInput by remember(profile) {
        mutableStateOf(profile.effectiveCarbsGoalG().toString())
    }
    var fatInput by remember(profile) {
        mutableStateOf(profile.effectiveFatGoalG().toString())
    }
    var fiberInput by remember(profile) {
        mutableStateOf(profile.effectiveFiberGoalG().toString())
    }
    var selectedPreset by remember { mutableStateOf("Custom") }

    val parsedCalories = (caloriesInput.toIntOrNull() ?: profile.dailyCalorieGoal).coerceIn(800, 6000)
    val parsedProtein = (proteinInput.toIntOrNull() ?: profile.effectiveProteinGoalG()).coerceIn(20, 400)
    val parsedCarbs = (carbsInput.toIntOrNull() ?: profile.effectiveCarbsGoalG()).coerceIn(30, 700)
    val parsedFat = (fatInput.toIntOrNull() ?: profile.effectiveFatGoalG()).coerceIn(15, 250)
    val parsedFiber = (fiberInput.toIntOrNull() ?: profile.effectiveFiberGoalG()).coerceIn(10, 100)

    val macroCalculatedCalories = (parsedProtein * 4) + (parsedCarbs * 4) + (parsedFat * 9)

    fun applyPresetSplit(name: String, pPct: Int, cPct: Int, fPct: Int) {
        selectedPreset = name
        val (pG, cG, fG) = UserProfile.calculateMacroGramsForSplit(parsedCalories, pPct, cPct, fPct)
        proteinInput = pG.toString()
        carbsInput = cG.toString()
        fatInput = fG.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = FreshGreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Set Daily Calorie & Macro Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose a Macro Split Preset or customize your daily calorie and gram targets below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Macro Split Presets
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("Balanced (25/50/25)", 25, 50 to 25),
                        Triple("High Protein (35/40/25)", 35, 40 to 25),
                        Triple("Low Carb (30/30/40)", 30, 30 to 40),
                        Triple("Athletic (25/55/20)", 25, 55 to 20)
                    ).forEach { (label, pPct, cfPair) ->
                        FilterChip(
                            selected = selectedPreset == label,
                            onClick = { applyPresetSplit(label, pPct, cfPair.first, cfPair.second) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider()

                // 1. Daily Calorie Target with +/- 100 kcal Quick Steppers
                Text(
                    text = "Daily Calorie Goal (kcal)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PastelGreenCard,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                val next = (parsedCalories - 100).coerceIn(800, 6000)
                                caloriesInput = next.toString()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease 100 kcal", tint = FreshGreenPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Calories (kcal)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_calories_input")
                    )

                    Surface(
                        shape = CircleShape,
                        color = PastelGreenCard,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                val next = (parsedCalories + 100).coerceIn(800, 6000)
                                caloriesInput = next.toString()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Increase 100 kcal", tint = FreshGreenPrimary)
                        }
                    }
                }

                // 2. Protein & Carbs Gram Targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = {
                            selectedPreset = "Custom"
                            proteinInput = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("Protein (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_protein_input")
                    )

                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = {
                            selectedPreset = "Custom"
                            carbsInput = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("Carbs (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_carbs_input")
                    )
                }

                // Interactive Protein & Carbs Sliders
                Column {
                    Text(
                        text = "Protein: ${parsedProtein}g (${parsedProtein * 4} kcal) • Carbs: ${parsedCarbs}g (${parsedCarbs * 4} kcal)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = parsedProtein.toFloat(),
                        onValueChange = {
                            selectedPreset = "Custom"
                            proteinInput = it.roundToInt().toString()
                        },
                        valueRange = 30f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = ProteinColor,
                            activeTrackColor = ProteinColor
                        )
                    )
                }

                // 3. Fat & Dietary Fiber Gram Targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = {
                            selectedPreset = "Custom"
                            fatInput = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("Fat (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_fat_input")
                    )

                    OutlinedTextField(
                        value = fiberInput,
                        onValueChange = {
                            fiberInput = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("Fiber (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_fiber_input")
                    )
                }

                // Live Macro Calorie Energy Summary Card + 1-Tap Auto-Sync
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelGreenCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Macro Energy Breakdown: $macroCalculatedCalories kcal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkCharcoalText
                        )
                        Text(
                            text = "P: ${parsedProtein * 4} kcal • C: ${parsedCarbs * 4} kcal • F: ${parsedFat * 9} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkCharcoalText.copy(alpha = 0.8f)
                        )
                        OutlinedButton(
                            onClick = { caloriesInput = macroCalculatedCalories.coerceIn(800, 6000).toString() },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Match Calorie Goal to Macros ($macroCalculatedCalories kcal)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveGoals(
                        profile.copy(
                            dailyCalorieGoal = parsedCalories,
                            dailyProteinGoalG = parsedProtein,
                            dailyCarbsGoalG = parsedCarbs,
                            dailyFatGoalG = parsedFat,
                            dailyFiberGoalG = parsedFiber,
                            useCustomNutritionGoals = true
                        )
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("save_nutrition_goals_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Daily Goals", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
