package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriScanRepository
import com.example.model.AiMealScanResponse
import com.example.model.FoodDatabaseItem
import com.example.model.FoodDiaryEntry
import com.example.model.SavedRecipe
import com.example.model.ScannedFoodItem
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.todayDateKey
import com.example.ui.theme.ThemePreferenceManager
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "NutriScanVM"

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Analyzing : ScanUiState
    data class Success(
        val result: AiMealScanResponse,
        val baseResult: AiMealScanResponse = result,
        val portionMultiplier: Float = 1.0f
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class NutriScanViewModel(
    private val repository: NutriScanRepository,
    private val currentUserId: String,
    private val userEmail: String = "",
    private val userDisplayName: String = ""
) : ViewModel() {

    companion object {
        const val GUEST_USER_ID = "guest_local_user"

        /**
         * Safely parses and validates structured JSON returned by Gemini food calorie analysis.
         * Handles optional whitespace/fences, NaN/null/string numbers, and always computes exact totals.
         */
        fun parseAndValidateScanJson(rawResponse: String): AiMealScanResponse {
            val cleaned = extractJsonObject(rawResponse)
            val json = JSONObject(cleaned)
            val status = json.optString("status", "success").ifBlank { "success" }
            val mealName = json.optString("meal_name", "Scanned Meal").ifBlank { "Scanned Meal" }
            val itemsArray = json.optJSONArray("food_items")
            val parsedItems = mutableListOf<ScannedFoodItem>()

            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i) ?: continue
                    val name = itemObj.optString("food_name", "Food Item").ifBlank { "Food Item" }
                    val qty = itemObj.optSafeDouble("quantity_g", 100.0).coerceIn(1.0, 5000.0)
                    val kcal = itemObj.optSafeInt("calories_kcal", 0).coerceIn(0, 5000)
                    val pro = itemObj.optSafeDouble("protein_g", 0.0).coerceIn(0.0, 500.0)
                    val carb = itemObj.optSafeDouble("carbohydrates_g", 0.0).coerceIn(0.0, 1000.0)
                    val fat = itemObj.optSafeDouble("fat_g", 0.0).coerceIn(0.0, 500.0)
                    val fib = itemObj.optSafeDouble("fiber_g", 0.0).coerceIn(0.0, 200.0)
                    val sug = itemObj.optSafeDouble("sugar_g", 0.0).coerceIn(0.0, 500.0)
                    val conf = itemObj.optSafeDouble("confidence", 0.85).coerceIn(0.0, 1.0)
                    val assumptions = itemObj.optString("assumptions", "Estimated serving size")
                        .ifBlank { "Estimated serving size" }

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
            }

            if (parsedItems.isEmpty()) {
                val totalObj = json.optJSONObject("total_nutrition")
                val fallbackCalories = totalObj?.optSafeInt("calories_kcal", 0)
                    ?: json.optSafeInt("calories_kcal", 0)
                if (fallbackCalories > 0) {
                    parsedItems.add(
                        ScannedFoodItem(
                            foodName = mealName,
                            quantityG = (totalObj?.optSafeDouble("total_weight_g", 200.0) ?: 200.0)
                                .coerceIn(1.0, 5000.0),
                            caloriesKcal = fallbackCalories.coerceIn(0, 5000),
                            proteinG = (totalObj?.optSafeDouble("protein_g", 0.0) ?: 0.0).coerceIn(0.0, 500.0),
                            carbohydratesG = (totalObj?.optSafeDouble("carbohydrates_g", 0.0) ?: 0.0).coerceIn(0.0, 1000.0),
                            fatG = (totalObj?.optSafeDouble("fat_g", 0.0) ?: 0.0).coerceIn(0.0, 500.0),
                            fiberG = (totalObj?.optSafeDouble("fiber_g", 0.0) ?: 0.0).coerceIn(0.0, 200.0),
                            sugarG = (totalObj?.optSafeDouble("sugar_g", 0.0) ?: 0.0).coerceIn(0.0, 500.0),
                            confidence = 0.75,
                            assumptions = "Estimated overall meal portion"
                        )
                    )
                }
            }

            if (parsedItems.isEmpty()) {
                throw IllegalArgumentException("No food detected in this photo. Try a clearer photo or type your dish name.")
            }

            val recalculatedTotals = AiMealScanResponse.recalculateTotals(parsedItems)
            val notes = json.optString(
                "analysis_notes",
                "Approximate nutrition estimate. Adjust portion slider if needed."
            ).ifBlank { "Approximate nutrition estimate." }

            return AiMealScanResponse(
                status = status,
                mealName = mealName,
                foodItems = parsedItems,
                totalNutrition = recalculatedTotals,
                analysisNotes = notes,
                requiresUserConfirmation = true
            )
        }

        private fun extractJsonObject(raw: String): String {
            var trimmed = raw.trim()
            if (trimmed.startsWith("```")) {
                trimmed = trimmed
                    .removePrefix("```json")
                    .removePrefix("```JSON")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
            }
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
        }

        private fun JSONObject.optSafeDouble(key: String, defaultValue: Double): Double {
            if (!has(key) || isNull(key)) return defaultValue
            val raw = opt(key)
            val parsed = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull()
                else -> null
            }
            return if (parsed != null && parsed.isFinite()) parsed else defaultValue
        }

        private fun JSONObject.optSafeInt(key: String, defaultValue: Int): Int {
            val dbl = optSafeDouble(key, defaultValue.toDouble())
            return if (dbl.isFinite()) dbl.roundToInt() else defaultValue
        }
    }

    val isGuestMode: Boolean = (currentUserId == GUEST_USER_ID)

    // Compact, reliable schema for Gemini Food Calorie Analysis (totals calculated locally in Kotlin)
    private val foodItemSchema = Schema.obj(
        properties = mapOf(
            "food_name" to Schema.string(description = "Name of the detected food item"),
            "quantity_g" to Schema.double(description = "Estimated portion weight in grams"),
            "calories_kcal" to Schema.integer(description = "Estimated calories in kcal"),
            "protein_g" to Schema.double(description = "Estimated protein in grams"),
            "carbohydrates_g" to Schema.double(description = "Estimated carbohydrates in grams"),
            "fat_g" to Schema.double(description = "Estimated fat in grams"),
            "fiber_g" to Schema.double(description = "Estimated fiber in grams"),
            "sugar_g" to Schema.double(description = "Estimated sugar in grams"),
            "confidence" to Schema.double(description = "Confidence score from 0.0 to 1.0"),
            "assumptions" to Schema.string(description = "Brief portion or cooking note")
        ),
        optionalProperties = listOf("fiber_g", "sugar_g", "confidence", "assumptions")
    )

    private val foodScanSchema = Schema.obj(
        properties = mapOf(
            "meal_name" to Schema.string(description = "Short overall meal name"),
            "food_items" to Schema.array(
                items = foodItemSchema,
                description = "List of food items with estimated weight, calories, and macros"
            ),
            "analysis_notes" to Schema.string(description = "Brief note on portion estimate")
        ),
        optionalProperties = listOf("analysis_notes")
    )

    private val scannerGenerationConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = foodScanSchema
            temperature = 0.2f
        }
    }

    // Lazy initialization of Gemini models via Firebase AI Logic
    private val scannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = scannerGenerationConfig
        )
    }

    private val scannerFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = scannerGenerationConfig
        )
    }

    // Selected date for Diary & Dashboard
    private val _selectedDateKey = MutableStateFlow(todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    // Dark theme & UI banners
    private val _isDarkMode = MutableStateFlow(ThemePreferenceManager.isDarkModeOverride.value)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _needsProfileSetup = MutableStateFlow(false)
    val needsProfileSetup: StateFlow<Boolean> = _needsProfileSetup.asStateFlow()

    // Guest Mode Local Session StateFlows
    private val _guestProfileState = MutableStateFlow<UserProfile?>(
        repository.getLocalUserProfile(GUEST_USER_ID) ?: UserProfile(
            userId = GUEST_USER_ID,
            name = userDisplayName.ifBlank { "Guest" },
            email = "",
            language = com.example.core.AppLanguageManager.getLanguageCode(repository.appContext),
            lastScanDate = todayDateKey()
        )
    )
    private val _guestDiaryEntries = MutableStateFlow<List<FoodDiaryEntry>>(emptyList())
    private val _guestWaterHistory = MutableStateFlow<List<WaterRecord>>(emptyList())
    private val _guestSavedRecipes = MutableStateFlow<List<SavedRecipe>>(
        repository.getLocalGuestRecipes(GUEST_USER_ID)
    )

    val userProfileState: StateFlow<UserProfile?> = if (isGuestMode) {
        _guestProfileState.asStateFlow()
    } else {
        repository.observeUserProfile(currentUserId)
            .catch { e ->
                Log.w(TAG, "Error observing profile", e)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = repository.getLocalUserProfile(currentUserId) ?: UserProfile(
                    userId = currentUserId,
                    name = userDisplayName.ifBlank { "User" },
                    email = userEmail,
                    language = com.example.core.AppLanguageManager.getLanguageCode(repository.appContext)
                )
            )
    }

    val foodDiaryEntries: StateFlow<List<FoodDiaryEntry>> = if (isGuestMode) {
        _guestDiaryEntries.asStateFlow()
    } else {
        repository.observeFoodDiary(currentUserId)
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
    }

    val waterHistory: StateFlow<List<WaterRecord>> = if (isGuestMode) {
        _guestWaterHistory.asStateFlow()
    } else {
        repository.observeWaterHistory(currentUserId)
            .catch { e ->
                Log.w(TAG, "Error observing water history", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    }

    val savedRecipes: StateFlow<List<SavedRecipe>> = if (isGuestMode) {
        _guestSavedRecipes.asStateFlow()
    } else {
        repository.observeSavedRecipes(currentUserId)
            .catch { e ->
                Log.w(TAG, "Error observing saved recipes", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    }

    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    init {
        if (isGuestMode) {
            if (repository.getLocalUserProfile(GUEST_USER_ID) == null) {
                _needsProfileSetup.value = true
            }
        } else {
            ensureDefaultProfileInitialized()
        }
    }

    private fun ensureDefaultProfileInitialized() {
        if (isGuestMode) return
        viewModelScope.launch {
            val res = repository.getUserProfile(currentUserId)
            val remoteProfile = res.getOrNull()
            val localProfile = repository.getLocalUserProfile(currentUserId)
            if (res.isSuccess && remoteProfile == null && localProfile == null) {
                _needsProfileSetup.value = true
            } else if (remoteProfile != null) {
                repository.saveLocalUserProfile(remoteProfile)
                _needsProfileSetup.value = false
            }
        }
    }

    fun dismissProfileSetup() {
        _needsProfileSetup.value = false
        val current = userProfileState.value ?: return
        if (isGuestMode) {
            if (repository.getLocalUserProfile(GUEST_USER_ID) == null) {
                repository.saveLocalUserProfile(current.copy(userId = GUEST_USER_ID))
            }
        } else {
            viewModelScope.launch {
                val withUid = current.copy(userId = currentUserId, lastScanDate = todayDateKey())
                repository.saveLocalUserProfile(withUid)
                repository.saveUserProfile(withUid)
            }
        }
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        ThemePreferenceManager.setDarkMode(null, enabled)
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateProfile(updated: UserProfile) {
        _needsProfileSetup.value = false
        if (isGuestMode) {
            val guestUpdated = updated.copy(userId = GUEST_USER_ID)
            _guestProfileState.value = guestUpdated
            repository.saveLocalUserProfile(guestUpdated)
            _statusBanner.value = "Profile saved"
            return
        }
        viewModelScope.launch {
            val withUid = updated.copy(userId = currentUserId)
            repository.saveLocalUserProfile(withUid)
            val result = repository.saveUserProfile(withUid)
            _statusBanner.value = if (result.isSuccess) {
                "Profile saved"
            } else {
                "Saved on device"
            }
        }
    }

    fun setLanguage(langCode: String) {
        val normalized = if (langCode in listOf("en", "hi", "bn")) langCode else "en"
        repository.appContext?.let { ctx ->
            com.example.core.AppLanguageManager.setLanguage(ctx, normalized)
        }
        val current = userProfileState.value ?: UserProfile(userId = currentUserId)
        updateProfile(current.copy(language = normalized))
    }

    // --- AI FOOD CALORIE SCANNER (IMAGE & TEXT) ---

    fun analyzeFoodImage(bitmap: Bitmap, customHint: String = "") {
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Analyzing
            try {
                val preparedBitmap = withContext(Dispatchers.Default) {
                    prepareBitmapForInference(bitmap)
                }
                val promptText = buildFoodScanPrompt(
                    language = userProfileState.value?.language ?: "en",
                    customHint = customHint,
                    isTextOnly = false
                )
                val requestContent = content {
                    image(preparedBitmap)
                    text(promptText)
                }
                val jsonString = generateScanJsonWithFallback(requestContent)
                val parsedResult = parseAndValidateScanJson(jsonString)
                _scanUiState.value = ScanUiState.Success(parsedResult)
                recordScanCountSafely()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Food image scan timed out", e)
                _scanUiState.value = ScanUiState.Error(
                    "Scan timed out. Please check your internet connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Food image scan failed", e)
                _scanUiState.value = ScanUiState.Error(formatFriendlyScanError(e))
            }
        }
    }

    fun analyzeFoodText(description: String) {
        val cleaned = description.trim()
        if (cleaned.isBlank()) {
            _scanUiState.value = ScanUiState.Error("Please enter a meal or food item to estimate calories.")
            return
        }
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Analyzing
            try {
                val promptText = buildFoodScanPrompt(
                    language = userProfileState.value?.language ?: "en",
                    customHint = cleaned,
                    isTextOnly = true
                )
                val requestContent = content { text(promptText) }
                val jsonString = generateScanJsonWithFallback(requestContent)
                val parsedResult = parseAndValidateScanJson(jsonString)
                _scanUiState.value = ScanUiState.Success(parsedResult)
                recordScanCountSafely()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Food text estimate timed out", e)
                _scanUiState.value = ScanUiState.Error(
                    "Request timed out. Please check your internet connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Food text estimate failed", e)
                _scanUiState.value = ScanUiState.Error(formatFriendlyScanError(e))
            }
        }
    }

    private suspend fun generateScanJsonWithFallback(requestContent: Content): String {
        return try {
            val primaryResponse = withTimeout(45_000L) {
                scannerModel.generateContent(requestContent)
            }
            primaryResponse.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty response from primary nutrition model.")
        } catch (primaryError: Exception) {
            if (primaryError is CancellationException && primaryError !is TimeoutCancellationException) {
                throw primaryError
            }
            Log.w(TAG, "Primary scanner model failed (${primaryError.message}); retrying with fallback model")
            val fallbackResponse = withTimeout(45_000L) {
                scannerFallbackModel.generateContent(requestContent)
            }
            fallbackResponse.text?.takeIf { it.isNotBlank() }
                ?: throw primaryError
        }
    }

    private fun buildFoodScanPrompt(
        language: String,
        customHint: String,
        isTextOnly: Boolean
    ): String {
        val langInstruction = when (language) {
            "bn" -> "Include Bengali and English food names (e.g., 'সাদা ভাত / Steamed Rice')."
            "hi" -> "Include Hindi and English food names (e.g., 'रोटी / Whole Wheat Roti')."
            else -> "Use clear common names (include Indian/Bengali dish names when relevant)."
        }
        return buildString {
            append("You are an expert nutritionist for Indian, Bengali, Asian, and global foods. ")
            if (isTextOnly) {
                append("Estimate the calories (kcal), portion weight (g), protein (g), carbohydrates (g), fat (g), fiber (g), and sugar (g) for each food item in this meal description: \"$customHint\". ")
            } else {
                append("Identify every visible food item in this photo and estimate portion weight (g), calories (kcal), protein (g), carbohydrates (g), fat (g), fiber (g), and sugar (g). ")
                if (customHint.isNotBlank()) {
                    append("User note: $customHint. ")
                }
            }
            append(langInstruction)
            append(" Return accurate realistic nutrition numbers.")
        }
    }

    private fun prepareBitmapForInference(source: Bitmap): Bitmap {
        val softwareBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            source.config == Bitmap.Config.HARDWARE
        ) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: source
        } else {
            source
        }
        val maxDim = 960
        val width = softwareBitmap.width.coerceAtLeast(1)
        val height = softwareBitmap.height.coerceAtLeast(1)
        if (width <= maxDim && height <= maxDim) {
            return softwareBitmap
        }
        val scale = maxDim.toFloat() / maxOf(width, height).toFloat()
        val newW = (width * scale).roundToInt().coerceAtLeast(1)
        val newH = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(softwareBitmap, newW, newH, true)
    }

    private fun formatFriendlyScanError(error: Exception): String {
        val msg = error.localizedMessage.orEmpty()
        return when {
            msg.contains("App Check", ignoreCase = true) || msg.contains("403", ignoreCase = true) ->
                "Unable to verify app session right now ($msg). Please restart the app and try again."
            msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("UnknownHostException", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true) ->
                "No internet connection. Please connect to the internet and tap Retry."
            msg.isNotBlank() -> msg
            else -> "Could not analyze food right now. Please try again."
        }
    }

    private fun recordScanCountSafely() {
        viewModelScope.launch {
            runCatching {
                val profile = userProfileState.value ?: UserProfile(userId = currentUserId)
                val today = todayDateKey()
                val usedToday = if (profile.lastScanDate == today) profile.dailyScansUsed + 1 else 1
                val updated = profile.copy(dailyScansUsed = usedToday, lastScanDate = today)
                if (isGuestMode) {
                    _guestProfileState.value = updated
                    repository.saveLocalUserProfile(updated)
                } else {
                    repository.saveUserProfile(updated)
                }
            }.onFailure { err ->
                Log.w(TAG, "Non-fatal scan count update failed: ${err.message}")
            }
        }
    }

    fun updateScanPortionMultiplier(multiplier: Float) {
        val currentState = _scanUiState.value as? ScanUiState.Success ?: return
        val clamped = ((multiplier.coerceIn(0.25f, 3.0f) * 20f).roundToInt() / 20f)
        val scaledResult = currentState.baseResult.scaledBy(clamped)
        _scanUiState.value = currentState.copy(
            result = scaledResult,
            portionMultiplier = clamped
        )
    }

    fun updateScannedFoodItem(index: Int, newName: String, newQuantityG: Double) {
        val currentState = _scanUiState.value as? ScanUiState.Success ?: return
        val baseItems = currentState.baseResult.foodItems.toMutableList()
        if (index !in baseItems.indices) return

        val mult = currentState.portionMultiplier.coerceIn(0.25f, 3.0f).toDouble()
        val oldBaseItem = baseItems[index]
        val newBaseQuantityG = (newQuantityG / mult).coerceIn(1.0, 5000.0)
        val scaleFactor = if (oldBaseItem.quantityG > 0.0) (newBaseQuantityG / oldBaseItem.quantityG) else 1.0
        val updatedBaseItem = oldBaseItem.copy(
            foodName = newName.ifBlank { oldBaseItem.foodName },
            quantityG = newBaseQuantityG,
            caloriesKcal = (oldBaseItem.caloriesKcal * scaleFactor).roundToInt().coerceIn(0, 5000),
            proteinG = ((oldBaseItem.proteinG * scaleFactor) * 10).roundToInt() / 10.0,
            carbohydratesG = ((oldBaseItem.carbohydratesG * scaleFactor) * 10).roundToInt() / 10.0,
            fatG = ((oldBaseItem.fatG * scaleFactor) * 10).roundToInt() / 10.0,
            fiberG = ((oldBaseItem.fiberG * scaleFactor) * 10).roundToInt() / 10.0,
            sugarG = ((oldBaseItem.sugarG * scaleFactor) * 10).roundToInt() / 10.0
        )
        baseItems[index] = updatedBaseItem
        val updatedBaseResult = currentState.baseResult.copy(
            foodItems = baseItems,
            totalNutrition = AiMealScanResponse.recalculateTotals(baseItems)
        )
        _scanUiState.value = currentState.copy(
            result = updatedBaseResult.scaledBy(currentState.portionMultiplier),
            baseResult = updatedBaseResult
        )
    }

    fun confirmScannedMealToDiary(mealType: String) {
        val currentState = _scanUiState.value as? ScanUiState.Success ?: return
        val dateKey = _selectedDateKey.value
        val newEntries = currentState.result.foodItems.mapIndexed { idx, item ->
            FoodDiaryEntry(
                id = if (isGuestMode) "guest_scan_${System.currentTimeMillis()}_$idx" else "",
                userId = if (isGuestMode) GUEST_USER_ID else currentUserId,
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
                source = if (isGuestMode) "Scan" else "AI Scan",
                confidence = item.confidence,
                notes = item.assumptions
            )
        }
        if (isGuestMode) {
            _guestDiaryEntries.value = _guestDiaryEntries.value + newEntries
            _scanUiState.value = ScanUiState.Idle
            _statusBanner.value = "Added ${newEntries.size} item(s) to $mealType"
            return
        }
        viewModelScope.launch {
            newEntries.forEach { entry ->
                repository.addFoodDiaryEntry(entry)
            }
            _scanUiState.value = ScanUiState.Idle
            _statusBanner.value = "Added ${currentState.result.foodItems.size} item(s) to $mealType"
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
            id = if (isGuestMode) "guest_db_${System.currentTimeMillis()}" else "",
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
        if (isGuestMode) {
            _guestDiaryEntries.value = _guestDiaryEntries.value + entry
            _statusBanner.value = "Added ${item.nameEn} to $mealType"
            return
        }
        viewModelScope.launch {
            repository.addFoodDiaryEntry(entry)
            _statusBanner.value = "Added ${item.nameEn} to $mealType"
        }
    }

    fun addManualFoodEntry(entry: FoodDiaryEntry) {
        if (isGuestMode) {
            val guestEntry = entry.copy(
                id = "guest_manual_${System.currentTimeMillis()}",
                userId = GUEST_USER_ID,
                dateKey = _selectedDateKey.value,
                source = "Manual"
            )
            _guestDiaryEntries.value = _guestDiaryEntries.value + guestEntry
            _statusBanner.value = "Added ${entry.foodName} to ${entry.mealType}"
            return
        }
        viewModelScope.launch {
            repository.addFoodDiaryEntry(
                entry.copy(
                    userId = currentUserId,
                    dateKey = _selectedDateKey.value,
                    source = "Manual"
                )
            )
            _statusBanner.value = "Added ${entry.foodName} to ${entry.mealType}"
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
        if (isGuestMode) {
            _guestDiaryEntries.value = _guestDiaryEntries.value.map { if (it.id == entry.id) updated else it }
            _statusBanner.value = "Updated ${updated.foodName}"
            return
        }
        viewModelScope.launch {
            repository.updateFoodDiaryEntry(updated)
            _statusBanner.value = "Updated ${updated.foodName}"
        }
    }

    fun deleteDiaryEntry(entryId: String) {
        if (isGuestMode) {
            _guestDiaryEntries.value = _guestDiaryEntries.value.filterNot { it.id == entryId }
            _statusBanner.value = "Removed entry"
            return
        }
        viewModelScope.launch {
            repository.deleteFoodDiaryEntry(entryId)
            _statusBanner.value = "Removed entry"
        }
    }

    // --- WATER TRACKING ---

    fun logWaterIntake(amountMl: Int) {
        val dateKey = _selectedDateKey.value
        if (isGuestMode) {
            val record = WaterRecord(
                id = "guest_water_${System.currentTimeMillis()}",
                userId = GUEST_USER_ID,
                quantity = amountMl,
                dateKey = dateKey
            )
            _guestWaterHistory.value = _guestWaterHistory.value + record
            return
        }
        viewModelScope.launch {
            repository.addWaterRecord(amountMl, dateKey)
        }
    }

    fun decrementWaterIntake(stepMl: Int = 250) {
        val dateKey = _selectedDateKey.value
        val safeStep = stepMl.coerceIn(10, 5000)
        val todayLogs = waterHistory.value.filter { it.dateKey == dateKey }
        if (todayLogs.isEmpty()) return

        if (isGuestMode) {
            val mutableAll = _guestWaterHistory.value.toMutableList()
            var remainingToSubtract = safeStep
            val dayIndices = mutableAll.indices.filter { mutableAll[it].dateKey == dateKey }.reversed()
            for (idx in dayIndices) {
                if (remainingToSubtract <= 0) break
                val item = mutableAll[idx]
                if (item.quantity <= remainingToSubtract) {
                    remainingToSubtract -= item.quantity
                    mutableAll.removeAt(idx)
                } else {
                    val newQty = item.quantity - remainingToSubtract
                    remainingToSubtract = 0
                    mutableAll[idx] = item.copy(quantity = newQty)
                }
            }
            _guestWaterHistory.value = mutableAll
            return
        }

        viewModelScope.launch {
            repository.decrementDailyWaterIntake(safeStep, dateKey, todayLogs)
        }
    }

    // --- MY RECIPES ---

    fun saveCustomRecipe(recipe: SavedRecipe) {
        val normalized = recipe.copy(userId = currentUserId).withRecalculatedTotals()
        if (isGuestMode) {
            val withId = if (normalized.id.isBlank()) {
                normalized.copy(id = "guest_recipe_${System.currentTimeMillis()}")
            } else {
                normalized
            }
            val updatedList = listOf(withId) + _guestSavedRecipes.value.filterNot { it.id == withId.id }
            _guestSavedRecipes.value = updatedList
            repository.saveLocalGuestRecipes(GUEST_USER_ID, updatedList)
            _statusBanner.value = "Saved '${withId.title}' to My Recipes"
            return
        }
        viewModelScope.launch {
            val result = if (normalized.id.isBlank()) {
                repository.saveRecipe(normalized).map { Unit }
            } else {
                repository.updateSavedRecipe(normalized)
            }
            if (result.isSuccess) {
                _statusBanner.value = "Saved '${normalized.title}' to My Recipes"
            } else {
                _statusBanner.value = "Could not save recipe: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun saveScannedMealToRecipes(customTitle: String, mealType: String, notes: String = "") {
        val currentState = _scanUiState.value as? ScanUiState.Success ?: return
        val recipe = SavedRecipe.fromScannedMeal(
            userId = currentUserId,
            customTitle = customTitle,
            scan = currentState.result,
            mealType = mealType,
            notes = notes
        )
        saveCustomRecipe(recipe)
    }

    fun saveDiaryEntriesToRecipes(
        title: String,
        mealType: String,
        entries: List<FoodDiaryEntry>,
        category: String = SavedRecipe.CATEGORY_FAVORITE_MEAL,
        notes: String = ""
    ) {
        if (entries.isEmpty()) return
        val recipe = SavedRecipe.fromDiaryEntries(
            userId = currentUserId,
            title = title,
            mealType = mealType,
            entries = entries,
            category = category,
            notes = notes
        )
        saveCustomRecipe(recipe)
    }

    fun toggleSavedRecipeFavorite(recipe: SavedRecipe) {
        val toggled = recipe.copy(isFavorite = !recipe.isFavorite)
        if (isGuestMode) {
            val updated = _guestSavedRecipes.value.map { if (it.id == recipe.id) toggled else it }
            _guestSavedRecipes.value = updated
            repository.saveLocalGuestRecipes(GUEST_USER_ID, updated)
            return
        }
        viewModelScope.launch {
            repository.updateSavedRecipe(toggled)
        }
    }

    fun deleteSavedRecipe(recipeId: String) {
        if (isGuestMode) {
            val updated = _guestSavedRecipes.value.filterNot { it.id == recipeId }
            _guestSavedRecipes.value = updated
            repository.saveLocalGuestRecipes(GUEST_USER_ID, updated)
            _statusBanner.value = "Removed recipe"
            return
        }
        viewModelScope.launch {
            repository.deleteSavedRecipe(recipeId)
            _statusBanner.value = "Removed recipe"
        }
    }

    fun logSavedRecipeToDiary(recipe: SavedRecipe, targetMealTypeOverride: String? = null) {
        val dateKey = _selectedDateKey.value
        val entriesToLog = recipe.toFoodDiaryEntries(
            dateKey = dateKey,
            targetMealTypeOverride = targetMealTypeOverride
        )
        if (entriesToLog.isEmpty()) return

        val incrementedRecipe = recipe.copy(usageCount = recipe.usageCount + 1)

        if (isGuestMode) {
            val guestStamped = entriesToLog.mapIndexed { idx, entry ->
                entry.copy(
                    id = "guest_recipe_log_${System.currentTimeMillis()}_$idx",
                    userId = GUEST_USER_ID
                )
            }
            _guestDiaryEntries.value = _guestDiaryEntries.value + guestStamped
            val updatedRecipes = _guestSavedRecipes.value.map {
                if (it.id == recipe.id) incrementedRecipe else it
            }
            _guestSavedRecipes.value = updatedRecipes
            repository.saveLocalGuestRecipes(GUEST_USER_ID, updatedRecipes)
            _statusBanner.value = "Logged '${recipe.title}'"
            return
        }

        viewModelScope.launch {
            entriesToLog.forEach { entry ->
                repository.addFoodDiaryEntry(entry)
            }
            if (recipe.id.isNotBlank()) {
                repository.updateSavedRecipe(incrementedRecipe)
            }
            _statusBanner.value = "Logged '${recipe.title}'"
        }
    }

    fun deleteAccountData(onDone: () -> Unit) {
        if (isGuestMode) {
            _guestDiaryEntries.value = emptyList()
            _guestWaterHistory.value = emptyList()
            _guestSavedRecipes.value = emptyList()
            repository.saveLocalGuestRecipes(GUEST_USER_ID, emptyList())
            onDone()
            return
        }
        viewModelScope.launch {
            repository.deleteUserAccountData()
            onDone()
        }
    }
}
