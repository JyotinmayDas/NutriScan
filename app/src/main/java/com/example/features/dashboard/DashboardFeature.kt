package com.example.features.dashboard

import androidx.compose.runtime.Composable
import com.example.core.AppStrings
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.ui.screens.HomeDashboardScreen

/**
 * Clean Architecture Feature Module: Home Dashboard (`features/dashboard`)
 */
@Composable
fun DashboardFeatureEntry(
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
    HomeDashboardScreen(
        strings = strings,
        profile = profile,
        selectedDateKey = selectedDateKey,
        diaryEntries = diaryEntries,
        waterRecords = waterRecords,
        weightRecords = weightRecords,
        remainingScansToday = remainingScansToday,
        onNavigateToScan = onNavigateToScan,
        onNavigateToDiary = onNavigateToDiary,
        onNavigateToCoach = onNavigateToCoach,
        onQuickAddWater = onQuickAddWater,
        onOpenOnboardingEdit = onOpenOnboardingEdit
    )
}
