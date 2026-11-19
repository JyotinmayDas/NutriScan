package com.example

import com.example.core.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguagePersistenceTest {

    @Test
    fun testLanguageCodeResolution() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
        assertEquals(AppLanguage.BENGALI, AppLanguage.fromCode("bn"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("invalid_code"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("HI"))
        assertEquals(AppLanguage.BENGALI, AppLanguage.fromCode("Bn"))
    }

    @Test
    fun testAllThreeDesignatedLanguagesHaveFlagsAndNativeNames() {
        val languages = AppLanguage.entries
        assertEquals(3, languages.size)

        val en = AppLanguage.ENGLISH
        assertEquals("en", en.code)
        assertEquals("English", en.displayName)
        assertEquals("🇬🇧", en.flagEmoji)

        val hi = AppLanguage.HINDI
        assertEquals("hi", hi.code)
        assertEquals("Hindi", hi.displayName)
        assertEquals("हिन्दी", hi.nativeName)
        assertEquals("🇮🇳", hi.flagEmoji)

        val bn = AppLanguage.BENGALI
        assertEquals("bn", bn.code)
        assertEquals("Bengali", bn.displayName)
        assertEquals("বাংলা", bn.nativeName)
        assertEquals("🇧🇩", bn.flagEmoji)
    }

    @Test
    fun testAiPromptInstructionsContainExpectedGuidelines() {
        val enPrompt = AppLanguage.ENGLISH.aiPromptInstruction
        val hiPrompt = AppLanguage.HINDI.aiPromptInstruction
        val bnPrompt = AppLanguage.BENGALI.aiPromptInstruction

        assertTrue(enPrompt.contains("English"))
        assertTrue(hiPrompt.contains("Hindi") && hiPrompt.contains("Devanagari"))
        assertTrue(bnPrompt.contains("Bengali") && bnPrompt.contains("বাংলা"))
    }
}
