package com.example

import com.example.data.FoodDatabaseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceFoodInputTest {

    @Test
    fun voiceSearch_exactAndPartialFoodMatches_returnsCatalogItems() {
        // Test spoken words like "rice", "dal", "chicken", "fish"
        val riceMatches = FoodDatabaseCatalog.search("rice")
        assertTrue(riceMatches.isNotEmpty())
        assertTrue(riceMatches.any { it.nameEn.contains("Rice", ignoreCase = true) })

        val fishMatches = FoodDatabaseCatalog.search("fish")
        assertTrue(fishMatches.isNotEmpty())
        assertTrue(fishMatches.any { it.nameEn.contains("Fish", ignoreCase = true) })

        val dalMatches = FoodDatabaseCatalog.search("dal")
        assertTrue(dalMatches.isNotEmpty())
        assertTrue(dalMatches.any { it.nameEn.contains("Dal", ignoreCase = true) })
    }

    @Test
    fun voiceSearch_multilingualQuery_matchesBengaliNames() {
        val bengaliMatches = FoodDatabaseCatalog.search("ভাত") // Bhaat / Rice
        assertTrue(bengaliMatches.isNotEmpty())
        assertTrue(bengaliMatches.any { it.nameBn.contains("ভাত") })
    }

    @Test
    fun voiceSearch_emptyQuery_returnsAllFoods() {
        val all = FoodDatabaseCatalog.search("")
        assertEquals(FoodDatabaseCatalog.allFoods.size, all.size)
    }
}
