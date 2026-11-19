package com.example.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported app languages for NutriScan UI and Gemini AI responses.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val summaryText: String,
    val aiPromptInstruction: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        flagEmoji = "🇬🇧",
        summaryText = "English AI responses for coaching, scan analysis, and recipes",
        aiPromptInstruction = "Respond primarily in English. Keep nutritional guidance clear, culturally supportive, and accessible."
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        flagEmoji = "🇮🇳",
        summaryText = "हिन्दी में AI पोषण सलाह, वॉइस लॉगिंग, इमेज एनालिसिस और रेसिपी",
        aiPromptInstruction = "Respond primarily in Hindi (हिन्दी) using natural conversational Devanagari script for advice, ingredient lists, and meal notes. Include English food names in brackets where standard (e.g. रोटी / Whole Wheat Roti, दाल / Dal)."
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা",
        flagEmoji = "🇧🇩",
        summaryText = "বাংলায় AI পুষ্টি পরামর্শ, ভয়েস লগিং, মিল স্ক্যানিং ও সুস্বাদু রেসিপি",
        aiPromptInstruction = "Respond primarily in Bengali (বাংলা) using natural, warm conversational Bengali script for advice, ingredient lists, and meal notes. Include English dish names in brackets where helpful (e.g. সাদা ভাত / Steamed Rice, মাছের ঝোল / Fish Curry)."
    );

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

/**
 * Persistent Language Preference Manager for NutriScan.
 * Stores language preference locally in SharedPreferences so it persists across app restarts,
 * offline sessions, and guest mode, directly governing all Gemini AI responses (Coach, Food Scanner,
 * Spoken Voice Logger, and Smart Recipe Suggestions).
 */
object AppLanguageManager {
    private const val PREFS_NAME = "nutriscan_language_prefs"
    private const val KEY_LANGUAGE_CODE = "selected_language_code"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, AppLanguage.ENGLISH.code)
        _currentLanguage.value = AppLanguage.fromCode(savedCode)
    }

    fun setLanguage(context: Context?, code: String): AppLanguage {
        val selected = AppLanguage.fromCode(code)
        _currentLanguage.value = selected
        context?.applicationContext?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE_CODE, selected.code)
                .apply()
        }
        return selected
    }

    fun getLanguageCode(context: Context?): String {
        if (context == null) return _currentLanguage.value.code
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_CODE, _currentLanguage.value.code) ?: _currentLanguage.value.code
    }
}
