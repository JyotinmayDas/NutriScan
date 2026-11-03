package com.example.features.food_scanner

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import com.example.ui.screens.AiScannerScreen
import com.example.viewmodel.ScanUiState

/**
 * Clean Architecture Feature Module: Smart Food Scanner (`features/food_scanner`)
 */
@Composable
fun FoodScannerFeatureEntry(
    scanUiState: ScanUiState,
    remainingScansToday: Int,
    isPremium: Boolean,
    onAnalyzeBitmap: (Bitmap, String) -> Unit,
    onUpdateScannedItem: (Int, String, Double) -> Unit,
    onConfirmMealToDiary: (String) -> Unit,
    onResetScan: () -> Unit,
    onGrantBonusScan: () -> Unit
) {
    AiScannerScreen(
        scanUiState = scanUiState,
        remainingScansToday = remainingScansToday,
        isPremium = isPremium,
        onAnalyzeBitmap = onAnalyzeBitmap,
        onUpdateScannedItem = onUpdateScannedItem,
        onConfirmMealToDiary = onConfirmMealToDiary,
        onResetScan = onResetScan,
        onGrantBonusScan = onGrantBonusScan
    )
}
