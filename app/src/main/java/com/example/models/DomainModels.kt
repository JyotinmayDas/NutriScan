package com.example.models

import com.example.model.AiCoachMessage
import com.example.model.AiMealScanResponse
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.ScannedFoodItem
import com.example.model.TotalNutritionSummary
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord

/**
 * Clean Architecture Domain Models barrel (`models/` package)
 * exposing all core domain entities for NutriScan.
 */
typealias DomainUserProfile = UserProfile
typealias DomainFoodDiaryEntry = FoodDiaryEntry
typealias DomainFoodDatabaseItem = FoodDatabaseItem
typealias DomainScannedFoodItem = ScannedFoodItem
typealias DomainTotalNutritionSummary = TotalNutritionSummary
typealias DomainMealScanResponse = AiMealScanResponse
typealias DomainWaterRecord = WaterRecord
typealias DomainWeightRecord = WeightRecord
typealias DomainCoachMessage = AiCoachMessage
typealias DomainMealPlanEntry = MealPlanEntry
