package com.example.repositories

import android.content.Context
import com.example.data.FoodDatabaseCatalog
import com.example.data.repository.NutriScanRepository
import com.example.model.AiCoachMessage
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture Repository contracts (`repositories/` package)
 * separating domain data access from UI presentation.
 */
interface NutritionRepositoryContract {
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    fun observeFoodDiary(): Flow<List<FoodDiaryEntry>>
    suspend fun addFoodDiaryEntry(entry: FoodDiaryEntry): Result<String>
    suspend fun updateFoodDiaryEntry(entry: FoodDiaryEntry): Result<Unit>
    suspend fun deleteFoodDiaryEntry(entryId: String): Result<Unit>
    fun observeWaterIntake(): Flow<List<WaterRecord>>
    suspend fun addWaterRecord(quantityMl: Int, dateKey: String): Result<String>
    suspend fun deleteWaterRecord(recordId: String): Result<Unit>
    fun observeWeightHistory(): Flow<List<WeightRecord>>
    suspend fun addWeightRecord(weightKg: Double, dateKey: String, note: String): Result<String>
    fun observeAiHistory(): Flow<List<AiCoachMessage>>
    fun observeMealPlans(): Flow<List<MealPlanEntry>>
    fun searchCatalog(query: String, category: String): List<FoodDatabaseItem>
}

class DefaultNutritionRepository(
    private val delegate: NutriScanRepository
) : NutritionRepositoryContract {
    constructor(context: Context) : this(NutriScanRepository(context))

    override fun observeUserProfile(): Flow<UserProfile?> = delegate.observeUserProfile()
    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = delegate.saveUserProfile(profile)
    override fun observeFoodDiary(): Flow<List<FoodDiaryEntry>> = delegate.observeFoodDiary()
    override suspend fun addFoodDiaryEntry(entry: FoodDiaryEntry): Result<String> = delegate.addFoodDiaryEntry(entry)
    override suspend fun updateFoodDiaryEntry(entry: FoodDiaryEntry): Result<Unit> = delegate.updateFoodDiaryEntry(entry)
    override suspend fun deleteFoodDiaryEntry(entryId: String): Result<Unit> = delegate.deleteFoodDiaryEntry(entryId)
    override fun observeWaterIntake(): Flow<List<WaterRecord>> = delegate.observeWaterHistory()
    override suspend fun addWaterRecord(quantityMl: Int, dateKey: String): Result<String> = delegate.addWaterRecord(quantityMl, dateKey)
    override suspend fun deleteWaterRecord(recordId: String): Result<Unit> = delegate.deleteWaterRecord(recordId)
    override fun observeWeightHistory(): Flow<List<WeightRecord>> = delegate.observeWeightHistory()
    override suspend fun addWeightRecord(weightKg: Double, dateKey: String, note: String): Result<String> =
        delegate.addWeightRecord(weightKg, dateKey, note)
    override fun observeAiHistory(): Flow<List<AiCoachMessage>> = delegate.observeAiHistory()
    override fun observeMealPlans(): Flow<List<MealPlanEntry>> = delegate.observeMealPlans()
    override fun searchCatalog(query: String, category: String): List<FoodDatabaseItem> =
        FoodDatabaseCatalog.search(query, category)
}
