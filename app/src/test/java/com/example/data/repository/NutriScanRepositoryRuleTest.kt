package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.FoodDiaryEntry
import com.example.model.SavedRecipe
import com.example.model.SavedRecipeItem
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import org.robolectric.annotation.Config

@Config(sdk = [36], manifest = Config.NONE)
class NutriScanRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfileAndFoodDiaryEntry_validPayload_succeedsAndEmitsUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NutriScanRepository(firestore)

        val profile = UserProfile(
            userId = aliceUid,
            name = "Ananya Sen",
            email = ALICE_EMAIL,
            age = 27,
            height = 162.0,
            currentWeight = 64.0,
            targetWeight = 58.0,
            activityLevel = "Moderately Active",
            fitnessGoal = "Lose Weight",
            dietaryPreference = "Bengali Traditional",
            dailyCalorieGoal = 1750,
            dailyWaterGoalMl = 2500,
            language = "en"
        )
        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveUserProfile(profile) }
        assertTrue(profileResult.isSuccess)

        val entry = FoodDiaryEntry(
            userId = aliceUid,
            foodName = "Steamed Rice & Rohu Fish Curry",
            quantity = 300.0,
            calories = 430,
            protein = 26.4,
            carbohydrates = 50.6,
            fat = 12.9,
            fiber = 2.2,
            sugar = 1.9,
            mealType = "Lunch",
            dateKey = "2026-11-03",
            source = "AI Scan",
            confidence = 0.91,
            notes = "150g rice + 150g macher jhol"
        )
        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addFoodDiaryEntry(entry) }
        assertTrue(addResult.isSuccess)
        val entryId = addResult.getOrThrow()
        assertTrue(entryId.isNotBlank())

        val emittedEntries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFoodDiary(aliceUid).first { list -> list.any { it.id == entryId } }
        }
        assertTrue(emittedEntries.any { it.id == entryId && it.foodName.contains("Rohu Fish") })
    }

    @Test
    fun getFoodEntryById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NutriScanRepository(firestore)

        val entryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addFoodDiaryEntry(
                FoodDiaryEntry(
                    userId = aliceUid,
                    foodName = "Masoor Dal",
                    quantity = 150.0,
                    calories = 165,
                    protein = 10.2,
                    carbohydrates = 23.8,
                    fat = 3.4,
                    fiber = 4.6,
                    sugar = 1.4,
                    mealType = "Lunch",
                    dateKey = "2026-11-03",
                    source = "Database"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NutriScanRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFoodEntryById(aliceUid, entryId)
        }
        assertTrue(crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeFoodDiary_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NutriScanRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeFoodDiary("unauth_user_123").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected exception when unauthenticated", caught)
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun saveRecipe_validPayload_succeedsUpdatesAndEmitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NutriScanRepository(firestore)

        val recipe = SavedRecipe(
            userId = aliceUid,
            title = "High-Protein Dal Bhat Combo",
            category = SavedRecipe.CATEGORY_SCANNED_COMBO,
            mealType = "Lunch",
            items = listOf(
                SavedRecipeItem(
                    foodName = "Steamed White Rice",
                    quantityG = 180.0,
                    caloriesKcal = 234,
                    proteinG = 4.5,
                    carbohydratesG = 50.0,
                    fatG = 0.5,
                    fiberG = 0.8,
                    mealSlot = "Lunch"
                ),
                SavedRecipeItem(
                    foodName = "Nepali Masoor Dal",
                    quantityG = 200.0,
                    caloriesKcal = 220,
                    proteinG = 14.0,
                    carbohydratesG = 32.0,
                    fatG = 4.0,
                    fiberG = 6.2,
                    mealSlot = "Lunch"
                )
            ),
            notes = "Favorite post-workout lunch",
            isFavorite = true
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveRecipe(recipe) }
        assertTrue("Expected saveRecipe success: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val recipeId = saveResult.getOrThrow()
        assertTrue(recipeId.isNotBlank())

        val emittedRecipes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSavedRecipes(aliceUid).first { list -> list.any { it.id == recipeId } }
        }
        val saved = emittedRecipes.first { it.id == recipeId }
        assertEquals(454, saved.totalCalories)
        assertEquals(2, saved.items.size)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateSavedRecipe(saved.copy(isFavorite = false, usageCount = 2))
        }
        assertTrue("Expected updateSavedRecipe success: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val updatedEmitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSavedRecipes(aliceUid).first { list ->
                list.any { it.id == recipeId && !it.isFavorite && it.usageCount == 2 }
            }
        }
        assertTrue(updatedEmitted.any { it.id == recipeId && it.usageCount == 2 })
    }

    @Test
    fun getSavedRecipeById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NutriScanRepository(firestore)

        val recipeId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveRecipe(
                SavedRecipe(
                    userId = aliceUid,
                    title = "Alice Idli Sambar",
                    category = SavedRecipe.CATEGORY_FAVORITE_MEAL,
                    mealType = "Breakfast",
                    items = listOf(
                        SavedRecipeItem(
                            foodName = "Idli Sambar",
                            quantityG = 250.0,
                            caloriesKcal = 310,
                            proteinG = 11.0,
                            carbohydratesG = 52.0,
                            fatG = 5.5,
                            fiberG = 5.0,
                            mealSlot = "Breakfast"
                        )
                    )
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NutriScanRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getSavedRecipeById(aliceUid, recipeId)
        }
        assertTrue(crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeSavedRecipes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NutriScanRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeSavedRecipes("unauth_user_123").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected exception when unauthenticated", caught)
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@nutriscan.test"
        const val BOB_EMAIL = "bob@nutriscan.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
