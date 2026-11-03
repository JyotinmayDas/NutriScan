package com.example.features.ai_coach

import androidx.compose.runtime.Composable
import com.example.model.AiCoachMessage
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.UserProfile
import com.example.model.WeightRecord
import com.example.ui.screens.AiCoachAndMealPlannerScreen
import com.example.viewmodel.CoachUiState
import com.example.viewmodel.MealPlanUiState

/**
 * Clean Architecture Feature Module: Nutrition Coach (`features/ai_coach`)
 */
@Composable
fun NutritionCoachFeatureEntry(
    profile: UserProfile,
    diaryEntries: List<FoodDiaryEntry>,
    weightHistory: List<WeightRecord>,
    mealPlans: List<MealPlanEntry>,
    aiHistory: List<AiCoachMessage>,
    coachUiState: CoachUiState,
    mealPlanUiState: MealPlanUiState,
    onAskCoach: (String) -> Unit,
    onGenerateMealPlan: (String) -> Unit,
    onLogWeight: (Double, String) -> Unit
) {
    AiCoachAndMealPlannerScreen(
        profile = profile,
        diaryEntries = diaryEntries,
        weightHistory = weightHistory,
        mealPlans = mealPlans,
        aiHistory = aiHistory,
        coachUiState = coachUiState,
        mealPlanUiState = mealPlanUiState,
        onAskCoach = onAskCoach,
        onGenerateMealPlan = onGenerateMealPlan,
        onLogWeight = onLogWeight
    )
}
