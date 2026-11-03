package com.example.core.constants

object AppConstants {
    const val APP_NAME = "NutriScan AI"
    const val APP_TAGLINE = "Scan Your Food. Know Your Nutrition."

    // Gemini Model configured for Firebase AI Logic
    const val GEMINI_VISION_MODEL = "gemini-3.8-flash"
    const val AI_TIMEOUT_MS = 60_000L

    // Freemium & Pro Plan Quotas
    const val FREE_DAILY_SCAN_LIMIT = 5
    const val PRO_DAILY_SCAN_LIMIT = 50
    const val FREE_DAILY_COACH_LIMIT = 10

    // Meal Sections
    val MEAL_TYPES = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    // Fitness Goals
    val FITNESS_GOALS = listOf("Lose Weight", "Maintain Weight", "Gain Weight")

    // Activity Levels
    val ACTIVITY_LEVELS = listOf(
        "Sedentary",
        "Lightly Active",
        "Moderately Active",
        "Very Active",
        "Extra Active"
    )

    // Dietary Preferences
    val DIETARY_PREFERENCES = listOf(
        "Bengali Traditional",
        "Indian Vegetarian",
        "Non-Vegetarian",
        "Vegan",
        "Eggetarian"
    )

    // Supported Localization Languages
    val SUPPORTED_LANGUAGES = listOf(
        "en" to "English",
        "bn" to "বাংলা (Bengali)",
        "hi" to "हिन्दी (Hindi)"
    )
}
