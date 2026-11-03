package com.example.features.food_diary

import androidx.compose.runtime.Composable
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.ui.screens.FoodDiaryAndDatabaseScreen

/**
 * Clean Architecture Feature Module: Daily Food Diary (`features/food_diary`)
 */
@Composable
fun FoodDiaryFeatureEntry(
    language: String,
    selectedDateKey: String,
    diaryEntries: List<FoodDiaryEntry>,
    onSelectDate: (String) -> Unit,
    onAddDatabaseFood: (FoodDatabaseItem, Double, String) -> Unit,
    onAddManualFood: (FoodDiaryEntry) -> Unit,
    onUpdateDiaryEntry: (FoodDiaryEntry, String, Double) -> Unit,
    onDeleteDiaryEntry: (String) -> Unit,
    onCopyPreviousDayMeals: (String, String) -> Unit
) {
    FoodDiaryAndDatabaseScreen(
        language = language,
        selectedDateKey = selectedDateKey,
        diaryEntries = diaryEntries,
        onSelectDate = onSelectDate,
        onAddDatabaseFood = onAddDatabaseFood,
        onAddManualFood = onAddManualFood,
        onUpdateDiaryEntry = onUpdateDiaryEntry,
        onDeleteDiaryEntry = onDeleteDiaryEntry,
        onCopyPreviousDayMeals = onCopyPreviousDayMeals
    )
}
