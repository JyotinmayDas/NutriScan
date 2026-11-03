package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AiCoachMessage
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.UserProfile
import com.example.model.WeightRecord
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachAccent
import com.example.ui.theme.PastelPeachCard
import com.example.ui.theme.ProteinColor
import com.example.viewmodel.CoachUiState
import com.example.viewmodel.MealPlanUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCoachAndMealPlannerScreen(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    weightHistory: List<WeightRecord>,
    mealPlans: List<MealPlanEntry>,
    @Suppress("UNUSED_PARAMETER") aiHistory: List<AiCoachMessage>,
    @Suppress("UNUSED_PARAMETER") coachUiState: CoachUiState,
    mealPlanUiState: MealPlanUiState,
    @Suppress("UNUSED_PARAMETER") onAskCoach: (String) -> Unit,
    onGenerateMealPlan: (String) -> Unit,
    onLogWeight: (Double, String) -> Unit
) {
    // 0 = Progress & Weight Reports, 1 = Daily Meal Plan (Coach option details removed)
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)
    ) {
        // Modern 2-Tab Segmented Pill Bar: Progress & Meal Plan
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(46.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                listOf(
                    Triple(0, "Progress", "subtab_weight_reports"),
                    Triple(1, "Meal Plan", "subtab_meal_planner")
                ).forEach { (idx, label, tag) ->
                    val isSelected = selectedSubTab == idx
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) DarkCharcoalText else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedSubTab = idx }
                            .testTag(tag)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                // PROGRESS, WEIGHT TRACKER & MACRO REPORTS (Matching Uploaded Image 1 Right)
                var newWeightStr by remember(profile) { mutableStateOf(profile.currentWeight.toString()) }
                var weightNote by remember { mutableStateOf("") }

                val totalProteinG = diaryEntries.sumOf { it.protein }.toFloat().coerceAtLeast(1f)
                val totalCarbsG = diaryEntries.sumOf { it.carbohydrates }.toFloat().coerceAtLeast(1f)
                val totalFatG = diaryEntries.sumOf { it.fat }.toFloat().coerceAtLeast(1f)
                val macroSum = totalProteinG + totalCarbsG + totalFatG

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header ("Progress • You're off to a great start!" - Image 1 Right)
                    item {
                        Column {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "You're off to a great start! Tracking consistency builds lasting results.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Lime Green "Total progress" Card (Image 1 Right)
                    item {
                        Card(
                            shape = RoundedCornerShape(26.dp),
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
                                            text = "${profile.currentWeight} → ${profile.targetWeight} kg",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { 0.65f },
                                    color = DarkCharcoalText,
                                    trackColor = DarkCharcoalText.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Balanced meals fuel energy, boost focus, and drive consistent progress.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkCharcoalText.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Weight Logger Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MonitorWeight,
                                        contentDescription = null,
                                        tint = FreshGreenPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Log Weight Entry",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = newWeightStr,
                                        onValueChange = { newWeightStr = it },
                                        label = { Text("Weight (kg)") },
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = weightNote,
                                        onValueChange = { weightNote = it },
                                        label = { Text("Note (e.g. Morning)") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val w = newWeightStr.toDoubleOrNull() ?: profile.currentWeight
                                        onLogWeight(w, weightNote)
                                        weightNote = ""
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkCharcoalText),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("log_weight_button")
                                ) {
                                    Text("Log Weight Entry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Macronutrient Distribution Donut Ring Card (Matching Image 7 Right)
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Macronutrient Distribution",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(110.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(96.dp)) {
                                            val stroke = 14.dp.toPx()
                                            val pSweep = (totalProteinG / macroSum) * 360f
                                            val cSweep = (totalCarbsG / macroSum) * 360f
                                            val fSweep = (totalFatG / macroSum) * 360f
                                            drawArc(
                                                color = ProteinColor,
                                                startAngle = -90f,
                                                sweepAngle = pSweep,
                                                useCenter = false,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt),
                                                size = Size(size.width, size.height)
                                            )
                                            drawArc(
                                                color = CarbsColor,
                                                startAngle = -90f + pSweep,
                                                sweepAngle = cSweep,
                                                useCenter = false,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt),
                                                size = Size(size.width, size.height)
                                            )
                                            drawArc(
                                                color = FatColor,
                                                startAngle = -90f + pSweep + cSweep,
                                                sweepAngle = fSweep,
                                                useCenter = false,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt),
                                                size = Size(size.width, size.height)
                                            )
                                        }
                                        Text(
                                            text = "${profile.dailyCalorieGoal}\nkcal",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "● Protein: ${((totalProteinG / macroSum) * 100).roundToInt()}% (${totalProteinG.roundToInt()}g)",
                                            color = ProteinColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "● Carbs: ${((totalCarbsG / macroSum) * 100).roundToInt()}% (${totalCarbsG.roundToInt()}g)",
                                            color = CarbsColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "● Fat: ${((totalFatG / macroSum) * 100).roundToInt()}% (${totalFatG.roundToInt()}g)",
                                            color = FatColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (weightHistory.isNotEmpty()) {
                        item {
                            Text("Weight Log History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        items(weightHistory, key = { it.id.ifBlank { it.dateKey + it.weight } }) { record ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${record.weight} kg",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${record.dateKey} ${if (record.note.isNotBlank()) "• ${record.note}" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // PERSONAL COOKBOOK & MEAL PLANNER (Matching Uploaded Image 5 Right)
                var selectedDietStyle by remember(profile) { mutableStateOf(profile.dietaryPreference) }
                val dietOptions = listOf(
                    "Bengali Traditional",
                    "Indian Vegetarian",
                    "Non-Vegetarian",
                    "Vegan",
                    "High-Protein Indian"
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${profile.dailyCalorieGoal} kcal/day • ${profile.fitnessGoal}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your personal cookbook",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tailored Daily Menu",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dietOptions.forEach { style ->
                                FilterChip(
                                    selected = selectedDietStyle == style,
                                    onClick = { selectedDietStyle = style },
                                    label = { Text(style) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DarkCharcoalText,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { onGenerateMealPlan(selectedDietStyle) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeAccent,
                                contentColor = DarkCharcoalText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("generate_meal_plan_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate $selectedDietStyle Plan",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    when (mealPlanUiState) {
                        is MealPlanUiState.Idle -> {}
                        is MealPlanUiState.Generating -> {
                            item {
                                Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FreshGreenPrimary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Crafting your personalized cookbook meal plan...")
                                    }
                                }
                            }
                        }
                        is MealPlanUiState.Error -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = mealPlanUiState.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        is MealPlanUiState.Success -> {
                            item {
                                CookbookMealPlanCard(plan = mealPlanUiState.plan)
                            }
                        }
                    }

                    if (mealPlans.isNotEmpty()) {
                        items(mealPlans.take(5), key = { it.id.ifBlank { it.title } }) { plan ->
                            CookbookMealPlanCard(plan = plan)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cookbook-style Meal Plan Card matching Uploaded Image 5 Right.
 */
@Composable
private fun CookbookMealPlanCard(plan: MealPlanEntry) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Personalized Daily Menu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PastelGreenCard
                ) {
                    Text(
                        text = "${plan.targetCalories} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            CookbookMealRow(
                mealType = "Breakfast",
                description = plan.breakfastSummary,
                kcalText = "${(plan.targetCalories * 0.25).roundToInt()} kcal",
                badgeBg = PastelGreenCard,
                badgeTint = FreshGreenPrimary
            )
            CookbookMealRow(
                mealType = "Lunch",
                description = plan.lunchSummary,
                kcalText = "${(plan.targetCalories * 0.35).roundToInt()} kcal",
                badgeBg = PastelPeachCard,
                badgeTint = PastelPeachAccent
            )
            CookbookMealRow(
                mealType = "Dinner",
                description = plan.dinnerSummary,
                kcalText = "${(plan.targetCalories * 0.28).roundToInt()} kcal",
                badgeBg = PastelGreenCard,
                badgeTint = FreshGreenPrimary
            )
            CookbookMealRow(
                mealType = "Snacks",
                description = plan.snacksSummary,
                kcalText = "${(plan.targetCalories * 0.12).roundToInt()} kcal",
                badgeBg = PastelPeachCard,
                badgeTint = PastelPeachAccent
            )

            if (plan.coachTips.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nutrition Note: ${plan.coachTips}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CookbookMealRow(
    mealType: String,
    description: String,
    kcalText: String,
    badgeBg: Color,
    badgeTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = CircleShape,
            color = badgeBg,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mealType,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = kcalText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
