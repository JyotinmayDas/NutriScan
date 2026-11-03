package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriScanRepository
import com.example.model.AiCoachMessage
import com.example.model.AiMealScanResponse
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.ScannedFoodItem
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.example.model.todayDateKey
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import kotlin.math.roundToInt

private const val TAG = "NutriScanVM"
private const val FREE_DAILY_SCAN_LIMIT = 5
private const val PRO_DAILY_SCAN_LIMIT = 50

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Analyzing : ScanUiState
    data class Success(val result: AiMealScanResponse) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface CoachUiState {
    data object Idle : CoachUiState
    data object Loading : CoachUiState
    data class Streaming(val partialText: String) : CoachUiState
    data class Success(val responseText: String) : CoachUiState
    data class Error(val message: String) : CoachUiState
}

sealed interface MealPlanUiState {
    data object Idle : MealPlanUiState
    data object Generating : MealPlanUiState
    data class Success(val plan: MealPlanEntry) : MealPlanUiState
    data class Error(val message: String) : MealPlanUiState
}

class NutriScanViewModel(
    private val repository: NutriScanRepository,
    private val currentUserId: String,
    private val userEmail: String = "",
    private val userDisplayName: String = ""
) : ViewModel() {

    // Schema for Gemini Multimodal AI Food Scanner (matches Master Prompt Section 5)
    private val foodItemSchema = Schema.obj(
        properties = mapOf(
            "food_name" to Schema.string(description = "Name of the detected food item in English and local Indian/Bengali name if applicable"),
            "quantity_g" to Schema.double(description = "Estimated portion weight in grams"),
            "calories_kcal" to Schema.integer(description = "Estimated calories in kcal for this portion"),
            "protein_g" to Schema.double(description = "Estimated protein in grams"),
            "carbohydrates_g" to Schema.double(description = "Estimated carbohydrates in grams"),
            "fat_g" to Schema.double(description = "Estimated fat in grams"),
            "fiber_g" to Schema.double(description = "Estimated dietary fiber in grams"),
            "sugar_g" to Schema.double(description = "Estimated sugar in grams"),
            "confidence" to Schema.double(description = "Confidence score between 0.0 and 1.0"),
            "assumptions" to Schema.string(description = "Cooking method and portion estimation assumptions")
        )
    )

    private val totalNutritionSchema = Schema.obj(
        properties = mapOf(
            "total_weight_g" to Schema.double(),
            "calories_kcal" to Schema.integer(),
            "protein_g" to Schema.double(),
            "carbohydrates_g" to Schema.double(),
            "fat_g" to Schema.double(),
            "fiber_g" to Schema.double(),
            "sugar_g" to Schema.double()
        )
    )

    private val foodScanSchema = Schema.obj(
        properties = mapOf(
            "status" to Schema.enumeration(values = listOf("success", "uncertain", "unrecognized")),
            "meal_name" to Schema.string(description = "Overall name of the meal"),
            "food_items" to Schema.array(items = foodItemSchema, description = "List of detected food items"),
            "total_nutrition" to totalNutritionSchema,
            "analysis_notes" to Schema.string(description = "Overall nutrition analysis, cooking methods, and uncertainty notes"),
            "requires_user_confirmation" to Schema.boolean(description = "Always true for image-based nutrition estimates")
        )
    )

    private val mealPlanSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Meal plan title"),
            "dietType" to Schema.string(description = "Dietary style"),
            "targetCalories" to Schema.integer(description = "Target daily calories"),
            "breakfastSummary" to Schema.string(description = "Breakfast items with portion and approximate kcal"),
            "lunchSummary" to Schema.string(description = "Lunch items with portion and approximate kcal"),
            "dinnerSummary" to Schema.string(description = "Dinner items with portion and approximate kcal"),
            "snacksSummary" to Schema.string(description = "Healthy snacks with approximate kcal"),
            "coachTips" to Schema.string(description = "Practical nutrition and hydration tips")
        )
    )

    // Lazy initialization of Gemini 3.8 Flash models via Firebase AI Logic
    private val scannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = foodScanSchema
            }
        )
    }

    private val mealPlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealPlanSchema
            }
        )
    }

    private val coachModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    // Selected date for Diary & Dashboard
    private val _selectedDateKey = MutableStateFlow(todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    // Dark theme & UI messages
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Reactive Firestore Streams with Two-Tier Error Handling
    val userProfileState: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing profile", e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserProfile(
                userId = currentUserId,
                name = userDisplayName.ifBlank { "NutriScan Explorer" },
                email = userEmail
            )
        )

    val foodDiaryEntries: StateFlow<List<FoodDiaryEntry>> = repository.observeFoodDiary(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing Firestore diary; falling back to Room cache", e)
            val cachedFlow = repository.observeOfflineCachedDiary(currentUserId)
            if (cachedFlow != null) {
                cachedFlow.collect { emit(it) }
            } else {
                emit(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val waterHistory: StateFlow<List<WaterRecord>> = repository.observeWaterHistory(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing water history", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val weightHistory: StateFlow<List<WeightRecord>> = repository.observeWeightHistory(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing weight history", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val mealPlans: StateFlow<List<MealPlanEntry>> = repository.observeMealPlans(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing meal plans", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val aiHistory: StateFlow<List<AiCoachMessage>> = repository.observeAiHistory(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing AI coach history", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    private val _coachUiState = MutableStateFlow<CoachUiState>(CoachUiState.Idle)
    val coachUiState: StateFlow<CoachUiState> = _coachUiState.asStateFlow()

    private val _mealPlanUiState = MutableStateFlow<MealPlanUiState>(MealPlanUiState.Idle)
    val mealPlanUiState: StateFlow<MealPlanUiState> = _mealPlanUiState.asStateFlow()

    init {
        ensureDefaultProfileInitialized()
    }

    private fun ensureDefaultProfileInitialized() {
        viewModelScope.launch {
            val res = repository.getUserProfile(currentUserId)
            if (res.isSuccess && res.getOrNull() == null) {
                val defaultProfile = UserProfile(
                    userId = currentUserId,
                    name = userDisplayName.ifBlank { "NutriScan Explorer" },
                    email = userEmail,
                    lastScanDate = todayDateKey()
                )
                repository.saveUserProfile(defaultProfile)
            }
        }
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateProfile(updated: UserProfile) {
        viewModelScope.launch {
            val result = repository.saveUserProfile(updated.copy(userId = currentUserId))
            if (result.isSuccess) {
                _statusBanner.value = "Profile & nutrition targets updated!"
            } else {
                _statusBanner.value = "Failed to save profile: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun setLanguage(langCode: String) {
        val current = userProfileState.value ?: UserProfile(userId = currentUserId)
        updateProfile(current.copy(language = langCode))
    }

    fun togglePremiumPlan(enablePro: Boolean) {
        val current = userProfileState.value ?: UserProfile(userId = currentUserId)
        updateProfile(current.copy(isPremium = enablePro))
    }

    fun grantBonusScanFromReward() {
        val current = userProfileState.value ?: return
        val newUsed = (current.dailyScansUsed - 1).coerceAtLeast(0)
        updateProfile(current.copy(dailyScansUsed = newUsed, lastScanDate = todayDateKey()))
        _statusBanner.value = "+1 Bonus Meal Scan unlocked!"
    }

    fun getRemainingScansToday(): Int {
        val profile = userProfileState.value ?: UserProfile()
        val limit = if (profile.isPremium) PRO_DAILY_SCAN_LIMIT else FREE_DAILY_SCAN_LIMIT
        val usedToday = if (profile.lastScanDate == todayDateKey()) profile.dailyScansUsed else 0
        return (limit - usedToday).coerceAtLeast(0)
    }

    // --- SMART FOOD SCANNER (MULTIMODAL + STRUCTURED JSON VALIDATION) ---

    fun analyzeFoodImage(bitmap: Bitmap, customHint: String = "") {
        val remaining = getRemainingScansToday()
        if (remaining <= 0) {
            _scanUiState.value = ScanUiState.Error(
                "Daily meal scan quota reached (${if (userProfileState.value?.isPremium == true) PRO_DAILY_SCAN_LIMIT else FREE_DAILY_SCAN_LIMIT} scans/day). Upgrade to NutriScan Pro or unlock a bonus scan."
            )
            return
        }

        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Analyzing
            try {
                // Compress and scale bitmap to keep inline payload well under 20MB limit
                val optimizedBitmap = compressBitmapForInference(bitmap)
                val lang = userProfileState.value?.language ?: "en"
                val langInstruction = when (lang) {
                    "bn" -> "Include Bengali & English names in food_name (e.g., 'সাদা ভাত / Steamed Rice')."
                    "hi" -> "Include Hindi & English names in food_name (e.g., 'रोटी / Whole Wheat Roti')."
                    else -> "Include Indian/Bengali & English names in food_name where relevant (e.g., 'Rohu Fish Curry (Macher Jhol)')."
                }

                val promptText = buildString {
                    append("You are NutriScan AI, an expert nutritionist specializing in Indian, Bengali, Asian, and international dishes. ")
                    append("Analyze this food image carefully. Identify all visible food items, estimate portion weight in grams, ")
                    append("calories (kcal), protein (g), carbohydrates (g), fat (g), fiber (g), and sugar (g). ")
                    append("Identify possible cooking methods and explain your assumptions and any ambiguity. ")
                    append("Do not claim exact identification if ambiguous; set an honest confidence score between 0.0 and 1.0. ")
                    append(langInstruction)
                    if (customHint.isNotBlank()) {
                        append(" User note about the meal: $customHint")
                    }
                }

                val response = withTimeout(60_000L) {
                    scannerModel.generateContent(
                        content {
                            image(optimizedBitmap)
                            text(promptText)
                        }
                    )
                }

                val jsonString = response.text ?: throw IllegalStateException("Nutrition analysis service returned an empty response.")
                val parsedResult = parseAndValidateScanJson(jsonString)
                _scanUiState.value = ScanUiState.Success(parsedResult)

                // Increment daily scan counter in profile
                val profile = userProfileState.value ?: UserProfile(userId = currentUserId)
                val today = todayDateKey()
                val usedToday = if (profile.lastScanDate == today) profile.dailyScansUsed + 1 else 1
                repository.saveUserProfile(profile.copy(dailyScansUsed = usedToday, lastScanDate = today))
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Food scan timed out", e)
                _scanUiState.value = ScanUiState.Error("Meal analysis timed out after 60 seconds. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Food scan failed", e)
                _scanUiState.value = ScanUiState.Error(e.localizedMessage ?: "Failed to analyze food image.")
            }
        }
    }

    private fun compressBitmapForInference(source: Bitmap): Bitmap {
        val maxDim = 1024
        val width = source.width
        val height = source.height
        val scaled = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newW = if (ratio > 1f) maxDim else (maxDim * ratio).roundToInt().coerceAtLeast(1)
            val newH = if (ratio > 1f) (maxDim / ratio).roundToInt().coerceAtLeast(1) else maxDim
            Bitmap.createScaledBitmap(source, newW, newH, true)
        } else {
            source
        }
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        val bytes = bos.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: scaled
    }

    internal fun parseAndValidateScanJson(jsonString: String): AiMealScanResponse {
        val json = JSONObject(jsonString)
        val status = json.optString("status", "success")
        val mealName = json.optString("meal_name", "Scanned Meal").ifBlank { "Scanned Meal" }
        val itemsArray = json.getJSONArray("food_items")
        val parsedItems = mutableListOf<ScannedFoodItem>()

        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val name = itemObj.optString("food_name", "Food Item").ifBlank { "Food Item" }
            val qty = itemObj.optDouble("quantity_g", 100.0).coerceIn(1.0, 5000.0)
            val kcal = itemObj.optInt("calories_kcal", 100).coerceIn(0, 5000)
            val pro = itemObj.optDouble("protein_g", 0.0).coerceIn(0.0, 500.0)
            val carb = itemObj.optDouble("carbohydrates_g", 0.0).coerceIn(0.0, 1000.0)
            val fat = itemObj.optDouble("fat_g", 0.0).coerceIn(0.0, 500.0)
            val fib = itemObj.optDouble("fiber_g", 0.0).coerceIn(0.0, 200.0)
            val sug = itemObj.optDouble("sugar_g", 0.0).coerceIn(0.0, 500.0)
            val conf = itemObj.optDouble("confidence", 0.85).coerceIn(0.0, 1.0)
            val assumptions = itemObj.optString("assumptions", "Estimated portion from visual analysis")

            parsedItems.add(
                ScannedFoodItem(
                    foodName = name,
                    quantityG = qty,
                    caloriesKcal = kcal,
                    proteinG = pro,
                    carbohydratesG = carb,
                    fatG = fat,
                    fiberG = fib,
                    sugarG = sug,
                    confidence = conf,
                    assumptions = assumptions
                )
            )
        }

        if (parsedItems.isEmpty()) {
            throw IllegalArgumentException("No recognizable food items were detected in the image.")
        }

        // Always dynamically compute totals from the validated individual food items
        val recalculatedTotals = AiMealScanResponse.recalculateTotals(parsedItems)
        val notes = json.optString("analysis_notes", "Approximate image-based nutritional estimates. Please confirm portion sizes.")
        return AiMealScanResponse(
            status = status,
            mealName = mealName,
            foodItems = parsedItems,
            totalNutrition = recalculatedTotals,
            analysisNotes = notes,
            requiresUserConfirmation = true
        )
    }

    fun updateScannedFoodItem(index: Int, newName: String, newQuantityG: Double) {
        val currentState = _scanUiState.value as? ScanUiState.Success ?: return
        val currentItems = currentState.result.foodItems.toMutableList()
        if (index !in currentItems.indices) return

        val oldItem = currentItems[index]
        val scaleFactor = if (oldItem.quantityG > 0.0) (newQuantityG / oldItem.quantityG) else 1.0
        val updatedItem = oldItem.copy(
            foodName = newName.ifBlank { oldItem.foodName },
            quantityG = newQuantityG.coerceIn(1.0, 5000.0),
            caloriesKcal = (oldItem.caloriesKcal * scaleFactor).roundToInt().coerceIn(0, 5000),
            proteinG = ((oldItem.proteinG * scaleFactor) * 10).roundToInt() / 10.0,
            carbohydratesG = ((oldItem.carbohydratesG * scaleFactor) * 10).roundToInt() / 10.0,
            fatG = ((oldItem.fatG * scaleFactor) * 10).roundToInt() / 10.0,
            fiberG = ((oldItem.fiberG * scaleFactor) * 10).roundToInt() / 10.0,
            sugarG = ((oldItem.sugarG * scaleFactor) * 10).roundToInt() / 10.0
        )
        currentItems[index] = updatedItem
        val newTotals = AiMealScanResponse.recalculateTotals(currentItems)
        _scanUiState.value = ScanUiState.Success(
            currentState.result.copy(
                foodItems = currentItems,
                totalNutrition = newTotals
            )
        )
    }

    fun confirmScannedMealToDiary(mealType: String) {
        val currentState = _scanUiState.value as? ScanUiState.Success ?: return
        val dateKey = _selectedDateKey.value
        viewModelScope.launch {
            currentState.result.foodItems.forEach { item ->
                val entry = FoodDiaryEntry(
                    userId = currentUserId,
                    foodName = item.foodName,
                    quantity = item.quantityG,
                    calories = item.caloriesKcal,
                    protein = item.proteinG,
                    carbohydrates = item.carbohydratesG,
                    fat = item.fatG,
                    fiber = item.fiberG,
                    sugar = item.sugarG,
                    mealType = mealType,
                    dateKey = dateKey,
                    source = "AI Scan",
                    confidence = item.confidence,
                    notes = item.assumptions
                )
                repository.addFoodDiaryEntry(entry)
            }
            _scanUiState.value = ScanUiState.Idle
            _statusBanner.value = "Added ${currentState.result.foodItems.size} scanned item(s) to $mealType!"
        }
    }

    fun resetScanState() {
        _scanUiState.value = ScanUiState.Idle
    }

    // --- FOOD DIARY OPERATIONS ---

    fun addDatabaseFoodToDiary(item: FoodDatabaseItem, grams: Double, mealType: String) {
        val ratio = if (item.servingSizeG > 0.0) grams / item.servingSizeG else 1.0
        val lang = userProfileState.value?.language ?: "en"
        val entry = FoodDiaryEntry(
            userId = currentUserId,
            foodName = item.localizedName(lang),
            quantity = grams.coerceIn(1.0, 5000.0),
            calories = (item.calories * ratio).roundToInt(),
            protein = ((item.protein * ratio) * 10).roundToInt() / 10.0,
            carbohydrates = ((item.carbohydrates * ratio) * 10).roundToInt() / 10.0,
            fat = ((item.fat * ratio) * 10).roundToInt() / 10.0,
            fiber = ((item.fiber * ratio) * 10).roundToInt() / 10.0,
            sugar = ((item.sugar * ratio) * 10).roundToInt() / 10.0,
            mealType = mealType,
            dateKey = _selectedDateKey.value,
            source = "Database",
            confidence = 1.0,
            notes = item.cookingMethod
        )
        viewModelScope.launch {
            repository.addFoodDiaryEntry(entry)
            _statusBanner.value = "Added ${item.nameEn} to $mealType!"
        }
    }

    fun addManualFoodEntry(entry: FoodDiaryEntry) {
        viewModelScope.launch {
            repository.addFoodDiaryEntry(entry.copy(userId = currentUserId, dateKey = _selectedDateKey.value, source = "Manual"))
            _statusBanner.value = "Added ${entry.foodName} to ${entry.mealType}!"
        }
    }

    fun updateDiaryEntryPortion(entry: FoodDiaryEntry, newFoodName: String, newQuantityG: Double) {
        val factor = if (entry.quantity > 0.0) newQuantityG / entry.quantity else 1.0
        val updated = entry.copy(
            foodName = newFoodName.ifBlank { entry.foodName },
            quantity = newQuantityG.coerceIn(1.0, 5000.0),
            calories = (entry.calories * factor).roundToInt().coerceIn(0, 5000),
            protein = ((entry.protein * factor) * 10).roundToInt() / 10.0,
            carbohydrates = ((entry.carbohydrates * factor) * 10).roundToInt() / 10.0,
            fat = ((entry.fat * factor) * 10).roundToInt() / 10.0,
            fiber = ((entry.fiber * factor) * 10).roundToInt() / 10.0,
            sugar = ((entry.sugar * factor) * 10).roundToInt() / 10.0
        )
        viewModelScope.launch {
            repository.updateFoodDiaryEntry(updated)
            _statusBanner.value = "Updated ${updated.foodName}"
        }
    }

    fun deleteDiaryEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteFoodDiaryEntry(entryId)
            _statusBanner.value = "Removed entry from diary"
        }
    }

    fun copyEntriesFromDate(sourceDateKey: String, targetDateKey: String) {
        val entriesToCopy = foodDiaryEntries.value.filter { it.dateKey == sourceDateKey }
        if (entriesToCopy.isEmpty()) {
            _statusBanner.value = "No meals found on $sourceDateKey to copy."
            return
        }
        viewModelScope.launch {
            entriesToCopy.forEach { oldEntry ->
                repository.addFoodDiaryEntry(
                    oldEntry.copy(
                        id = "",
                        dateKey = targetDateKey,
                        source = "Copied"
                    )
                )
            }
            _statusBanner.value = "Copied ${entriesToCopy.size} meal entries to $targetDateKey!"
        }
    }

    // --- WATER & WEIGHT TRACKING ---

    fun logWaterIntake(amountMl: Int) {
        viewModelScope.launch {
            repository.addWaterRecord(amountMl, _selectedDateKey.value)
            _statusBanner.value = "+${amountMl}ml water logged!"
        }
    }

    fun deleteWaterEntry(recordId: String) {
        viewModelScope.launch {
            repository.deleteWaterRecord(recordId)
        }
    }

    fun logWeightUpdate(weightKg: Double, note: String) {
        viewModelScope.launch {
            repository.addWeightRecord(weightKg, todayDateKey(), note)
            val currentProfile = userProfileState.value
            if (currentProfile != null) {
                val updatedTargetKcal = UserProfile.calculateDailyCalorieTarget(
                    age = currentProfile.age,
                    heightCm = currentProfile.height,
                    weightKg = weightKg,
                    gender = currentProfile.gender,
                    activityLevel = currentProfile.activityLevel,
                    fitnessGoal = currentProfile.fitnessGoal
                )
                repository.saveUserProfile(
                    currentProfile.copy(
                        currentWeight = weightKg,
                        dailyCalorieGoal = updatedTargetKcal
                    )
                )
            }
            _statusBanner.value = "Weight updated to ${weightKg}kg!"
        }
    }

    // --- AI NUTRITION COACH (STREAMING GEMINI 3.8 FLASH) ---

    fun askAiNutritionCoach(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _coachUiState.value = CoachUiState.Loading
            val accumulated = StringBuilder()
            try {
                val profile = userProfileState.value ?: UserProfile(userId = currentUserId)
                val todayEntries = foodDiaryEntries.value.filter { it.dateKey == _selectedDateKey.value }
                val totalCaloriesToday = todayEntries.sumOf { it.calories }
                val totalProteinToday = todayEntries.sumOf { it.protein }.roundToInt()
                val mealListSummary = if (todayEntries.isEmpty()) {
                    "No food logged yet today."
                } else {
                    todayEntries.joinToString("; ") { "${it.mealType}: ${it.foodName} (${it.quantity.roundToInt()}g, ${it.calories} kcal)" }
                }

                val languageName = when (profile.language) {
                    "bn" -> "Bengali (বাংলা)"
                    "hi" -> "Hindi (हिन्दी)"
                    else -> "English"
                }

                val systemContextPrompt = """
                    You are NutriScan AI Coach, a warm, knowledgeable Indian & Bengali nutrition assistant.
                    Respond primarily in $languageName (unless the user explicitly asks in another language).
                    
                    User Profile Context:
                    - Goal: ${profile.fitnessGoal} | Diet Preference: ${profile.dietaryPreference}
                    - Current Weight: ${profile.currentWeight}kg | Target Weight: ${profile.targetWeight}kg
                    - Daily Calorie Target: ${profile.dailyCalorieGoal} kcal
                    - Today's Diary (${_selectedDateKey.value}): $totalCaloriesToday kcal, ${totalProteinToday}g protein.
                    - Today's Meals: $mealListSummary
                    
                    Safety Rules:
                    - Provide practical, culturally authentic Indian/Bengali/balanced food suggestions.
                    - Never provide medical diagnoses, extreme crash diets, or unsafe weight-loss advice.
                    - Remind the user gently when relevant that recommendations are educational estimates.
                    
                    User Question: $question
                """.trimIndent()

                withTimeout(60_000L) {
                    coachModel.generateContentStream(systemContextPrompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _coachUiState.value = CoachUiState.Streaming(accumulated.toString())
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    _coachUiState.value = CoachUiState.Success(finalReply)
                    repository.saveAiCoachMessage(
                        question = question,
                        response = finalReply,
                        language = profile.language
                    )
                } else {
                    _coachUiState.value = CoachUiState.Error("Nutrition Coach returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Nutrition Coach timed out", e)
                _coachUiState.value = CoachUiState.Error("Nutrition Coach timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nutrition Coach generation failed", e)
                _coachUiState.value = CoachUiState.Error(e.localizedMessage ?: "Nutrition Coach failed to respond.")
            }
        }
    }

    // --- AI MEAL PLANNER (STRUCTURED JSON VIA GEMINI 3.8 FLASH) ---

    fun generatePersonalizedMealPlan(dietOverride: String? = null) {
        viewModelScope.launch {
            _mealPlanUiState.value = MealPlanUiState.Generating
            try {
                val profile = userProfileState.value ?: UserProfile(userId = currentUserId)
                val dietStyle = dietOverride ?: profile.dietaryPreference
                val langName = when (profile.language) {
                    "bn" -> "Bengali (বাংলা) and English"
                    "hi" -> "Hindi (हिन्दी) and English"
                    else -> "English with Indian/Bengali dish names"
                }

                val prompt = """
                    Generate a realistic, delicious 1-day personalized meal plan for an Indian user.
                    - Dietary Preference: $dietStyle
                    - Fitness Goal: ${profile.fitnessGoal}
                    - Daily Calorie Target: ${profile.dailyCalorieGoal} kcal
                    - Language: $langName
                    Include authentic dishes (such as Roti, Dal, Rice, Fish/Paneer/Chicken/Vegetables, Curd, Healthy Snacks) with portion sizes in grams and approximate calories for Breakfast, Lunch, Dinner, and Snacks.
                    Present as general wellness suggestions, not a medical prescription.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    mealPlannerModel.generateContent(prompt)
                }

                val jsonText = response.text ?: throw IllegalStateException("Empty meal plan response.")
                val json = JSONObject(jsonText)
                val plan = MealPlanEntry(
                    userId = currentUserId,
                    title = json.optString("title", "$dietStyle ${profile.dailyCalorieGoal} kcal Plan"),
                    dietType = json.optString("dietType", dietStyle),
                    targetCalories = json.optInt("targetCalories", profile.dailyCalorieGoal),
                    breakfastSummary = json.optString("breakfastSummary", "Oats porridge + 2 boiled eggs / paneer"),
                    lunchSummary = json.optString("lunchSummary", "150g steamed rice, masoor dal, mixed sabzi & fish/paneer"),
                    dinnerSummary = json.optString("dinnerSummary", "2 whole wheat rotis, tadka dal & cucumber salad"),
                    snacksSummary = json.optString("snacksSummary", "Roasted chana / jhalmuri (light oil) & green tea"),
                    coachTips = json.optString("coachTips", "Drink ${profile.dailyWaterGoalMl}ml water daily. General wellness suggestion, not medical advice."),
                    dateKey = _selectedDateKey.value
                )
                repository.saveMealPlan(plan)
                _mealPlanUiState.value = MealPlanUiState.Success(plan)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Meal planner timed out", e)
                _mealPlanUiState.value = MealPlanUiState.Error("Meal plan generation timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Meal plan generation failed", e)
                _mealPlanUiState.value = MealPlanUiState.Error(e.localizedMessage ?: "Failed to generate meal plan.")
            }
        }
    }

    fun deleteAccountData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUserAccountData()
            onDone()
        }
    }
}
