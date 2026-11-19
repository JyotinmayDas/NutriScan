package com.example.widgets

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDiaryEntry
import com.example.model.UserProfile
import com.example.model.todayDateKey
import com.example.ui.theme.CarbsColor
import com.example.ui.theme.DarkCharcoalText
import com.example.ui.theme.FatColor
import com.example.ui.theme.FreshGreenPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPeachCard
import com.example.ui.theme.ProteinColor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "SmartRecipeGenerator"

data class DailyNutritionGaps(
    val remainingCaloriesKcal: Int,
    val proteinGapG: Int,
    val carbsGapG: Int,
    val fatGapG: Int,
    val fiberGapG: Int,
    val proteinDeficitPct: Int,
    val carbsDeficitPct: Int,
    val fatDeficitPct: Int,
    val fiberDeficitPct: Int,
    val priorityFocusTitle: String,
    val priorityFocusSubtitle: String
)

data class SmartRecipeIdea(
    val id: String,
    val recipeName: String,
    val mealSlot: String,
    val prepTimeMinutes: Int,
    val portionGrams: Int,
    val caloriesKcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val gapMatchSummary: String,
    val ingredients: List<String>,
    val cookingSteps: List<String>,
    val isAiGenerated: Boolean = false
) {
    fun toFoodDiaryEntry(dateKey: String = todayDateKey()): FoodDiaryEntry {
        return FoodDiaryEntry(
            dateKey = dateKey,
            mealType = if (mealSlot in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealSlot else "Dinner",
            foodName = "$recipeName (Smart Recipe)",
            quantity = portionGrams.toDouble().coerceAtLeast(50.0),
            calories = caloriesKcal,
            protein = proteinG,
            carbohydrates = carbsG,
            fat = fatG,
            fiber = fiberG,
            sugar = 3.0,
            source = "smart_recipe"
        )
    }
}

object SmartRecipeEngine {

    private val recipeItemSchema = Schema.obj(
        properties = mapOf(
            "recipe_name" to Schema.string(description = "Name of the healthy recipe"),
            "meal_slot" to Schema.enumeration(
                values = listOf("Breakfast", "Lunch", "Dinner", "Snacks"),
                description = "Recommended meal slot"
            ),
            "prep_time_minutes" to Schema.integer(description = "Total preparation and cooking time in minutes"),
            "portion_grams" to Schema.integer(description = "Serving weight in grams"),
            "calories_kcal" to Schema.integer(description = "Calories per serving in kcal"),
            "protein_g" to Schema.double(description = "Protein in grams per serving"),
            "carbs_g" to Schema.double(description = "Carbohydrates in grams per serving"),
            "fat_g" to Schema.double(description = "Total fat in grams per serving"),
            "fiber_g" to Schema.double(description = "Dietary fiber in grams per serving"),
            "gap_match_summary" to Schema.string(description = "1-sentence explanation of how this recipe closes the user's specific remaining calorie/macro gaps"),
            "ingredients" to Schema.array(
                items = Schema.string(),
                description = "List of 4 to 7 ingredients with exact household & gram measurements"
            ),
            "cooking_steps" to Schema.array(
                items = Schema.string(),
                description = "List of 3 to 5 clear step-by-step cooking instructions"
            )
        )
    )

    private val smartRecipesResponseSchema = Schema.obj(
        properties = mapOf(
            "gap_strategy_headline" to Schema.string(description = "Short summary of the nutritional gap strategy for the rest of the day"),
            "recipes" to Schema.array(
                items = recipeItemSchema,
                description = "3 tailored healthy recipes designed around the user's remaining nutrition gaps"
            )
        )
    )

    private val smartRecipeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = smartRecipesResponseSchema
            }
        )
    }

    fun calculateNutritionGaps(
        profile: UserProfile,
        todayEntries: List<FoodDiaryEntry>
    ): DailyNutritionGaps {
        val targetCal = profile.dailyCalorieGoal.coerceAtLeast(800)
        val targetProtein = profile.effectiveProteinGoalG().coerceAtLeast(30)
        val targetCarbs = profile.effectiveCarbsGoalG().coerceAtLeast(50)
        val targetFat = profile.effectiveFatGoalG().coerceAtLeast(20)
        val targetFiber = profile.effectiveFiberGoalG().coerceAtLeast(15)

        val eatenCal = todayEntries.sumOf { it.calories }
        val eatenPro = todayEntries.sumOf { it.protein }.roundToInt()
        val eatenCarbs = todayEntries.sumOf { it.carbohydrates }.roundToInt()
        val eatenFat = todayEntries.sumOf { it.fat }.roundToInt()
        val eatenFiber = todayEntries.sumOf { it.fiber }.roundToInt()

        val remCal = (targetCal - eatenCal).coerceAtLeast(0)
        val gapPro = (targetProtein - eatenPro).coerceAtLeast(0)
        val gapCarbs = (targetCarbs - eatenCarbs).coerceAtLeast(0)
        val gapFat = (targetFat - eatenFat).coerceAtLeast(0)
        val gapFiber = (targetFiber - eatenFiber).coerceAtLeast(0)

        val proDefPct = ((gapPro.toDouble() / targetProtein) * 100).roundToInt().coerceIn(0, 100)
        val carbsDefPct = ((gapCarbs.toDouble() / targetCarbs) * 100).roundToInt().coerceIn(0, 100)
        val fatDefPct = ((gapFat.toDouble() / targetFat) * 100).coerceIn(0.0, 100.0).roundToInt()
        val fiberDefPct = ((gapFiber.toDouble() / targetFiber) * 100).roundToInt().coerceIn(0, 100)

        val (focusTitle, focusSubtitle) = when {
            remCal == 0 -> "Calorie Target Reached • Light Recovery Focus" to
                "You've met your calorie goal today. Choose ultra-light, hydrating, fiber-rich greens or herbal infusions."
            proDefPct >= 45 && fiberDefPct >= 45 -> "High-Protein & High-Fiber Gap Priority" to
                "You still need ${gapPro}g protein and ${gapFiber}g fiber within your remaining $remCal kcal budget."
            proDefPct >= 40 -> "Lean Protein Deficit Focus (+${gapPro}g Needed)" to
                "Prioritize lean protein sources (fish, paneer, dal, eggs, tofu, Greek yogurt) to hit your ${targetProtein}g target."
            fiberDefPct >= 45 -> "Micronutrient & Dietary Fiber Boost (+${gapFiber}g Needed)" to
                "Add whole lentils, leafy greens, oats, or vegetables to close your ${gapFiber}g fiber gap."
            else -> "Balanced Macro Completion ($remCal kcal Left)" to
                "Well-paced day! Balance your remaining ${gapPro}g protein, ${gapCarbs}g carbs, and ${gapFat}g healthy fats."
        }

        return DailyNutritionGaps(
            remainingCaloriesKcal = remCal,
            proteinGapG = gapPro,
            carbsGapG = gapCarbs,
            fatGapG = gapFat,
            fiberGapG = gapFiber,
            proteinDeficitPct = proDefPct,
            carbsDeficitPct = carbsDefPct,
            fatDeficitPct = fatDefPct,
            fiberDeficitPct = fiberDefPct,
            priorityFocusTitle = focusTitle,
            priorityFocusSubtitle = focusSubtitle
        )
    }

    /**
     * Curated nutrition-gap-aware recipes that immediately adapt to whether the user has a
     * High Protein gap, High Fiber gap, Low Remaining Calorie budget, or specific dietary preference.
     */
    fun getInstantGapMatchedRecipes(
        gaps: DailyNutritionGaps,
        preferredMealSlot: String,
        dietaryPreference: String = "Bengali Traditional"
    ): List<SmartRecipeIdea> {
        val slot = if (preferredMealSlot in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) {
            preferredMealSlot
        } else {
            when {
                gaps.remainingCaloriesKcal > 1100 -> "Lunch"
                gaps.remainingCaloriesKcal in 350..1100 -> "Dinner"
                else -> "Snacks"
            }
        }

        val isVegOrVegan = dietaryPreference.contains("Veg", ignoreCase = true) || dietaryPreference.contains("Vegan", ignoreCase = true)
        val isKeto = dietaryPreference.contains("Keto", ignoreCase = true) || dietaryPreference.contains("Low", ignoreCase = true)

        val firstRecipe = if (isKeto) {
            SmartRecipeIdea(
                id = "smart_rec_keto",
                recipeName = "Spiced Paneer & Spinach Sauté with Crushed Almonds",
                mealSlot = slot,
                prepTimeMinutes = 15,
                portionGrams = 220,
                caloriesKcal = 320,
                proteinG = 22.0,
                carbsG = 7.5,
                fatG = 22.0,
                fiberG = 5.2,
                gapMatchSummary = "Keto-friendly 7.5g net carbs, delivers 22g protein within your ${gaps.remainingCaloriesKcal} kcal budget.",
                ingredients = listOf(
                    "100g low-fat fresh paneer or firm tofu cubes",
                    "80g baby spinach leaves",
                    "15g roasted crushed almonds",
                    "1 tbsp cold-pressed olive or mustard oil",
                    "Garlic, green chili, rock salt, and toasted cumin"
                ),
                cookingSteps = listOf(
                    "Heat oil with minced garlic and green chili in a non-stick skillet.",
                    "Lightly brown paneer cubes on medium heat for 3 minutes.",
                    "Toss in baby spinach leaves and cook 90 seconds until wilted; garnish with crushed almonds."
                )
            )
        } else {
            SmartRecipeIdea(
                id = "smart_rec_1",
                recipeName = "Grilled Paneer & Palak Moong Dal Chilla Bowl",
                mealSlot = slot,
                prepTimeMinutes = 18,
                portionGrams = 260,
                caloriesKcal = 340,
                proteinG = 24.5,
                carbsG = 31.0,
                fatG = 11.2,
                fiberG = 8.4,
                gapMatchSummary = "Vegetarian high-protein option that fits your ${gaps.remainingCaloriesKcal} kcal remaining calorie budget.",
                ingredients = listOf(
                    "80g soaked yellow moong dal (ground into smooth batter)",
                    "75g low-fat paneer cubes (lightly seared with turmeric & black pepper)",
                    "60g fresh spinach (palak) finely chopped",
                    "1/2 medium onion, 1 green chili & grated ginger",
                    "1 tsp cold-pressed mustard or olive oil"
                ),
                cookingSteps = listOf(
                    "Blend soaked moong dal with ginger, green chili, and 1/4 cup water into a smooth batter.",
                    "Fold finely chopped spinach and a pinch of rock salt into the batter.",
                    "Spread on a hot tawa with 1/2 tsp oil, top with paneer cubes, and cook 3 minutes per side."
                )
            )
        }

        val secondRecipe = if (isVegOrVegan) {
            SmartRecipeIdea(
                id = "smart_rec_veg_2",
                recipeName = "Bengali Shorshe Potol & Masoor Dal Rice Bowl",
                mealSlot = if (slot == "Snacks") "Dinner" else slot,
                prepTimeMinutes = 20,
                portionGrams = 280,
                caloriesKcal = 360,
                proteinG = 18.5,
                carbsG = 48.0,
                fatG = 9.5,
                fiberG = 9.0,
                gapMatchSummary = "Plant-based Bengali traditional meal packing 9g fiber within your remaining calorie budget.",
                ingredients = listOf(
                    "100g pointed gourd (potol) lightly scored and sautéed",
                    "1 tbsp light yellow mustard paste with green chilies",
                    "90g cooked red rice or brown basmati",
                    "1/2 cup yellow masoor dal with turmeric and kalonji",
                    "1 tsp raw cold-pressed mustard oil"
                ),
                cookingSteps = listOf(
                    "Lightly steam or sauté pointed gourd with turmeric and a pinch of salt.",
                    "Simmer with mild mustard paste and green chilies for 5 minutes.",
                    "Serve hot alongside 90g red rice and nourishing masoor dal."
                )
            )
        } else {
            SmartRecipeIdea(
                id = "smart_rec_2",
                recipeName = "Bengali Steamed Rohu Fish (Bhapa Mach) & Red Rice Greens",
                mealSlot = if (slot == "Snacks") "Dinner" else slot,
                prepTimeMinutes = 22,
                portionGrams = 300,
                caloriesKcal = 385,
                proteinG = 31.0,
                carbsG = 34.0,
                fatG = 12.5,
                fiberG = 6.2,
                gapMatchSummary = "Delivers 31g high-bioavailability omega-3 protein within your remaining ${gaps.remainingCaloriesKcal} kcal budget.",
                ingredients = listOf(
                    "120g Rohu or Bhetki fish fillet",
                    "1 tbsp mustard & poppy seed (posto) paste + 2 tbsp hung curd",
                    "100g cooked red/brown rice or quinoa",
                    "80g steamed broccoli, French beans & carrots",
                    "2 slit green chilies & 1 tsp raw mustard oil"
                ),
                cookingSteps = listOf(
                    "Marinate fish fillet with hung curd, lightly ground mustard-posto paste, turmeric, and slit chilies.",
                    "Steam in a covered steel tiffin box for 12–14 minutes until flaky and aromatic.",
                    "Serve warm alongside 100g fiber-rich red rice and steamed seasonal vegetables."
                )
            )
        }

        val thirdRecipe = SmartRecipeIdea(
            id = "smart_rec_3",
            recipeName = "Roasted Kala Chana, Sprouted Moong & Pomelo Salad",
            mealSlot = "Snacks",
            prepTimeMinutes = 10,
            portionGrams = 220,
            caloriesKcal = 235,
            proteinG = 14.8,
            carbsG = 33.5,
            fatG = 4.2,
            fiberG = 10.6,
            gapMatchSummary = "Packs 10.6g dietary fiber and 14.8g plant protein with only 235 kcal to fit comfortably into your remaining budget.",
            ingredients = listOf(
                "70g boiled kala chana (black chickpeas)",
                "60g steamed sprouted green moong",
                "50g diced cucumber, tomato & pomegranate arils",
                "1 tbsp roasted peanuts & fresh coriander leaves",
                "Juice of 1/2 Gondhoraj lemon & roasted cumin (bhaja masala)"
            ),
            cookingSteps = listOf(
                "Toss warm boiled kala chana and steamed moong sprouts in a mixing bowl.",
                "Add crunchy diced cucumber, tomato, pomegranate, and chopped coriander.",
                "Dress with fresh lemon juice and roasted cumin powder; serve immediately."
            )
        )

        return listOf(firstRecipe, secondRecipe, thirdRecipe)
    }

    /**
     * Calls Gemini 3.8 Flash via Firebase AI Logic with structured JSON output to recommend
     * healthy recipes tailored to the user's remaining calorie budget and common dietary preferences.
     */
    suspend fun generateSmartRecipesWithAi(
        profile: UserProfile,
        gaps: DailyNutritionGaps,
        todayEntries: List<FoodDiaryEntry>,
        selectedMealSlot: String,
        selectedCuisineFilter: String,
        pantryIngredients: String
    ): Result<Pair<String, List<SmartRecipeIdea>>> {
        return try {
            val loggedDishNames = if (todayEntries.isEmpty()) {
                "No meals logged yet today"
            } else {
                todayEntries.joinToString(", ") { "${it.foodName} (${it.calories} kcal)" }
            }

            val prompt = """
                You are an expert clinical nutritionist and healthy recipe chef.
                Recommend 3 healthy, delicious, and easy-to-cook meal suggestions tailored specifically to the user's REMAINING CALORIE BUDGET and DIETARY PREFERENCE:
                - REMAINING CALORIE BUDGET: ${gaps.remainingCaloriesKcal} kcal (Daily Calorie Target: ${profile.dailyCalorieGoal} kcal)
                - DIETARY PREFERENCE: $selectedCuisineFilter
                - Preferred Meal Slot: $selectedMealSlot
                - User Fitness Goal: ${profile.fitnessGoal}
                - Meals Already Consumed Today: $loggedDishNames
                - Remaining Protein Target: ${gaps.proteinGapG} g (Goal: ${profile.effectiveProteinGoalG()} g)
                - Remaining Carbs Target: ${gaps.carbsGapG} g (Goal: ${profile.effectiveCarbsGoalG()} g)
                - Remaining Fat Target: ${gaps.fatGapG} g (Goal: ${profile.effectiveFatGoalG()} g)
                - Remaining Fiber Target: ${gaps.fiberGapG} g (Goal: ${profile.effectiveFiberGoalG()} g)
                - Optional Pantry Ingredients Available: ${pantryIngredients.ifBlank { "Everyday accessible ingredients" }}

                Rules:
                1. STRICT CALORIE BUDGET: Each recipe's calories_kcal MUST strictly fit within the user's remaining daily calorie budget (${gaps.remainingCaloriesKcal.coerceAtLeast(200)} kcal per serving).
                2. STRICT DIETARY PREFERENCE: Strictly adhere to the chosen dietary preference ($selectedCuisineFilter). If Vegetarian/Vegan, never include meat, fish, or poultry. If Keto/Low-Carb, keep net carbs minimal. If Bengali Traditional, use authentic healthy Bengali ingredients.
                3. NUTRITION QUALITY: Maximize protein and fiber, minimize refined sugars and excess saturated fat.
                4. REALISTIC: Provide realistic prep times (in minutes), portion size (in grams), clear measured ingredients, and step-by-step instructions.
                5. LANGUAGE DIRECTIVE: ${when (profile.language) {
                    "bn" -> "Write recipe dish names, summary, ingredients, and instructions primarily in Bengali (বাংলা) with English dish name subtitle where helpful."
                    "hi" -> "Write recipe dish names, summary, ingredients, and instructions primarily in Hindi (हिन्दी) with English dish name subtitle where helpful."
                    else -> "Write recipes in English."
                }}
            """.trimIndent()

            val response = withTimeout(60_000L) {
                smartRecipeModel.generateContent(prompt)
            }

            val jsonString = response.text ?: error("Model returned an empty recipe response.")
            val root = JSONObject(jsonString)
            val headline = root.optString(
                "gap_strategy_headline",
                "Healthy meal suggestions tailored to your ${gaps.remainingCaloriesKcal} kcal calorie budget & $selectedCuisineFilter diet"
            )
            val arr = root.optJSONArray("recipes")
            val parsedRecipes = mutableListOf<SmartRecipeIdea>()

            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val ingArr = obj.optJSONArray("ingredients")
                    val ingredients = buildList {
                        if (ingArr != null) {
                            for (j in 0 until ingArr.length()) {
                                add(ingArr.optString(j))
                            }
                        }
                    }
                    val stepsArr = obj.optJSONArray("cooking_steps")
                    val steps = buildList {
                        if (stepsArr != null) {
                            for (j in 0 until stepsArr.length()) {
                                add(stepsArr.optString(j))
                            }
                        }
                    }

                    parsedRecipes.add(
                        SmartRecipeIdea(
                            id = "ai_smart_recipe_${System.currentTimeMillis()}_$i",
                            recipeName = obj.optString("recipe_name", "Healthy Balanced Meal"),
                            mealSlot = obj.optString("meal_slot", if (selectedMealSlot == "Auto") "Dinner" else selectedMealSlot),
                            prepTimeMinutes = obj.optInt("prep_time_minutes", 20).coerceIn(5, 120),
                            portionGrams = obj.optInt("portion_grams", 250).coerceIn(50, 1000),
                            caloriesKcal = obj.optInt("calories_kcal", 350).coerceIn(80, gaps.remainingCaloriesKcal.coerceAtLeast(400)),
                            proteinG = obj.optDouble("protein_g", 22.0).coerceAtLeast(0.0),
                            carbsG = obj.optDouble("carbs_g", 30.0).coerceAtLeast(0.0),
                            fatG = obj.optDouble("fat_g", 10.0).coerceAtLeast(0.0),
                            fiberG = obj.optDouble("fiber_g", 6.0).coerceAtLeast(0.0),
                            gapMatchSummary = obj.optString(
                                "gap_match_summary",
                                "Fits your ${gaps.remainingCaloriesKcal} kcal budget with wholesome $selectedCuisineFilter ingredients."
                            ),
                            ingredients = ingredients.ifEmpty { listOf("Fresh seasonal ingredients") },
                            cookingSteps = steps.ifEmpty { listOf("Prepare and cook with minimal oil.") },
                            isAiGenerated = true
                        )
                    )
                }
            }

            if (parsedRecipes.isEmpty()) {
                error("No recipes were returned by the AI model.")
            }

            Result.success(headline to parsedRecipes)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Healthy recipe generation timed out", e)
            Result.failure(Exception("Healthy Recipe generation timed out after 60s. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Healthy recipe generation failed", e)
            Result.failure(e)
        }
    }
}

/**
 * Public alias for the 'Healthy Recipe Suggestion' card requested by user.
 */
@Composable
fun HealthyRecipeSuggestionCard(
    profile: UserProfile,
    todayEntries: List<FoodDiaryEntry>,
    onLogSmartRecipeToDiary: (FoodDiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    SmartRecipeGeneratorCard(
        profile = profile,
        todayEntries = todayEntries,
        onLogSmartRecipeToDiary = onLogSmartRecipeToDiary,
        modifier = modifier
    )
}

/**
 * Interactive 'Healthy Recipe Suggestion' Card that uses the user's remaining calorie budget
 * and common dietary preferences to recommend meals via the Gemini API.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartRecipeGeneratorCard(
    profile: UserProfile,
    todayEntries: List<FoodDiaryEntry>,
    onLogSmartRecipeToDiary: (FoodDiaryEntry) -> Unit,
    onSaveSmartRecipeToMyRecipes: ((SmartRecipeIdea) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val gaps = remember(profile, todayEntries) {
        SmartRecipeEngine.calculateNutritionGaps(profile, todayEntries)
    }

    var selectedMealSlot by remember { mutableStateOf("Auto") }
    var selectedCuisineStyle by remember(profile.dietaryPreference) {
        val initialStyle = when {
            profile.dietaryPreference.contains("Veg", ignoreCase = true) && !profile.dietaryPreference.contains("Non", ignoreCase = true) -> "Indian Vegetarian"
            profile.dietaryPreference.contains("Vegan", ignoreCase = true) -> "Vegan"
            profile.dietaryPreference.contains("Keto", ignoreCase = true) || profile.dietaryPreference.contains("Low Carb", ignoreCase = true) -> "Low Carb / Keto"
            profile.dietaryPreference.contains("High Protein", ignoreCase = true) -> "High Protein"
            profile.dietaryPreference.isNotBlank() -> profile.dietaryPreference
            else -> "Bengali Traditional"
        }
        mutableStateOf(initialStyle)
    }
    var pantryHint by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var aiStrategyHeadline by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loggedFeedback by remember { mutableStateOf<String?>(null) }

    var aiGeneratedRecipes by remember { mutableStateOf<List<SmartRecipeIdea>>(emptyList()) }
    val activeRecipes = remember(aiGeneratedRecipes, gaps, selectedMealSlot, selectedCuisineStyle) {
        aiGeneratedRecipes.ifEmpty {
            SmartRecipeEngine.getInstantGapMatchedRecipes(gaps, selectedMealSlot, selectedCuisineStyle)
        }
    }

    fun triggerAiSmartRecipeGeneration() {
        isGenerating = true
        errorMessage = null
        loggedFeedback = null
        coroutineScope.launch {
            SmartRecipeEngine.generateSmartRecipesWithAi(
                profile = profile,
                gaps = gaps,
                todayEntries = todayEntries,
                selectedMealSlot = selectedMealSlot,
                selectedCuisineFilter = selectedCuisineStyle,
                pantryIngredients = pantryHint
            ).onSuccess { (headline, recipes) ->
                isGenerating = false
                aiStrategyHeadline = headline
                aiGeneratedRecipes = recipes
            }.onFailure { err ->
                isGenerating = false
                errorMessage = err.localizedMessage ?: "Failed to generate AI healthy recipe suggestions."
            }
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("healthy_recipe_suggestion_card")
            .testTag("smart_recipe_generator_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row
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
                        shape = RoundedCornerShape(14.dp),
                        color = PastelGreenCard,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                tint = FreshGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Healthy Recipe Suggestions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "AI meals tailored to your remaining calorie budget & dietary preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Remaining Calorie Budget Spotlight Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PastelGreenCard,
                                PastelBlueCard.copy(alpha = 0.65f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        FreshGreenPrimary.copy(alpha = 0.28f),
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("remaining_calorie_budget_display")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remaining Calorie Budget",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkCharcoalText.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${gaps.remainingCaloriesKcal} kcal left today",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = DarkCharcoalText
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = DarkCharcoalText
                        ) {
                            Text(
                                text = "${todayEntries.sumOf { it.calories }} / ${profile.dailyCalorieGoal} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Linear calorie meter
                    val consumedRatio = (todayEntries.sumOf { it.calories }.toFloat() / profile.dailyCalorieGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { consumedRatio },
                        color = FreshGreenPrimary,
                        trackColor = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Text(
                        text = aiStrategyHeadline ?: gaps.priorityFocusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkCharcoalText.copy(alpha = 0.85f)
                    )

                    // 4 Nutrient Gap Badges (Protein, Carbs, Fat, Fiber)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NutrientGapBadge(
                            label = "Protein Gap",
                            gapGrams = gaps.proteinGapG,
                            deficitPct = gaps.proteinDeficitPct,
                            accentColor = ProteinColor,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientGapBadge(
                            label = "Carbs Gap",
                            gapGrams = gaps.carbsGapG,
                            deficitPct = gaps.carbsDeficitPct,
                            accentColor = CarbsColor,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientGapBadge(
                            label = "Fat Gap",
                            gapGrams = gaps.fatGapG,
                            deficitPct = gaps.fatDeficitPct,
                            accentColor = FatColor,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientGapBadge(
                            label = "Fiber Gap",
                            gapGrams = gaps.fiberGapG,
                            deficitPct = gaps.fiberDeficitPct,
                            accentColor = FreshGreenPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Common Dietary Preferences & Meal Slot Filter Chips
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Common Dietary Preferences",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val commonDietaryPreferences = listOf(
                        "Bengali Traditional",
                        "Indian Vegetarian",
                        "High Protein",
                        "Low Carb / Keto",
                        "Mediterranean / Balanced",
                        "Vegan"
                    )
                    commonDietaryPreferences.forEach { pref ->
                        val isSelected = selectedCuisineStyle == pref
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCuisineStyle = pref },
                            label = { Text(pref, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FreshGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("dietary_pref_${pref.lowercase().replace(" ", "_").replace("/", "_")}")
                        )
                    }
                }

                Text(
                    text = "Target Meal Slot",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Breakfast", "Lunch", "Dinner", "Snacks").forEach { slot ->
                        val isSelected = selectedMealSlot == slot
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMealSlot = slot },
                            label = {
                                Text(
                                    text = if (slot == "Auto") "Auto (Best Fit)" else slot,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkCharcoalText,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("smart_recipe_slot_${slot.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = pantryHint,
                    onValueChange = { pantryHint = it },
                    label = { Text("Ingredients on hand (optional, e.g. 'paneer, eggs, spinach, oats')") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_recipe_pantry_input")
                )

                Button(
                    onClick = { triggerAiSmartRecipeGeneration() },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = DarkCharcoalText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("suggest_healthy_recipes_button")
                        .testTag("generate_smart_recipes_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = DarkCharcoalText,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing Budget & Dietary Preferences…",
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Suggest Healthy Recipes (Gemini AI)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PastelPeachCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (loggedFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PastelBlueCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FreshGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loggedFeedback.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoalText
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4. Generated Smart Recipe Idea Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activeRecipes.forEachIndexed { idx, recipe ->
                    SmartRecipeIdeaCard(
                        recipe = recipe,
                        defaultExpanded = idx == 0,
                        onLogRecipe = {
                            val entry = recipe.toFoodDiaryEntry()
                            onLogSmartRecipeToDiary(entry)
                            loggedFeedback = "Logged '${recipe.recipeName}' (${recipe.caloriesKcal} kcal, ${recipe.proteinG.roundToInt()}g protein) to ${entry.mealType}!"
                        },
                        onSaveToMyRecipes = onSaveSmartRecipeToMyRecipes?.let { saver ->
                            {
                                saver(recipe)
                                loggedFeedback = "Saved '${recipe.recipeName}' to My Recipes!"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NutrientGapBadge(
    label: String,
    gapGrams: Int,
    deficitPct: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.92f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkCharcoalText.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (gapGrams > 0) "+${gapGrams}g" else "Met ✓",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (gapGrams > 0) accentColor else FreshGreenPrimary
            )
            LinearProgressIndicator(
                progress = { ((100 - deficitPct) / 100f).coerceIn(0.1f, 1f) },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun SmartRecipeIdeaCard(
    recipe: SmartRecipeIdea,
    defaultExpanded: Boolean,
    onLogRecipe: () -> Unit,
    onSaveToMyRecipes: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val dishDrawable = remember(recipe.recipeName) {
        FoodDatabaseCatalog.foodImageResFor(recipe.recipeName)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                FreshGreenPrimary.copy(alpha = 0.25f),
                RoundedCornerShape(22.dp)
            )
            .clickable { expanded = !expanded }
            .testTag("smart_recipe_item_${recipe.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = dishDrawable),
                    contentDescription = recipe.recipeName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PastelGreenCard
                        ) {
                            Text(
                                text = recipe.mealSlot,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshGreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${recipe.prepTimeMinutes} min • ${recipe.portionGrams}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recipe.recipeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${recipe.caloriesKcal} kcal • P ${recipe.proteinG.roundToInt()}g • C ${recipe.carbsG.roundToInt()}g • F ${recipe.fatG.roundToInt()}g • Fiber ${recipe.fiberG.roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshGreenPrimary
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse Recipe" else "Expand Recipe",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Why this matches today's gap
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PastelGreenCard.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Gap Match: ${recipe.gapMatchSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkCharcoalText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    recipe.ingredients.forEach { ing ->
                        Text(
                            text = "• $ing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Quick Cooking Steps",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    recipe.cookingSteps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLogRecipe,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("log_smart_recipe_button_${recipe.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = FreshGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log '${recipe.recipeName.take(20)}' (${recipe.caloriesKcal} kcal)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreenPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onSaveToMyRecipes != null) {
                    IconButton(
                        onClick = onSaveToMyRecipes,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("save_smart_recipe_button_${recipe.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save to My Recipes",
                            tint = FreshGreenPrimary
                        )
                    }
                }
            }
        }
    }
}
