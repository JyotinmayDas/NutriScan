package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- NutriScan Official Logo Palette (Extracted from Uploaded Logo) ---
// Vibrant Camera-Scanner Emerald Green & Leaf Green
val FreshGreenPrimary = Color(0xFF129A1B)
val FreshGreenOnPrimary = Color(0xFFFFFFFF)
val FreshGreenPrimaryContainer = Color(0xFFDCFAE0)
val FreshGreenOnPrimaryContainer = Color(0xFF073B0C)
val FreshGreenDark = Color(0xFF0B6E12)
val EmeraldVibrant = Color(0xFF28B822)

// --- Logo Leaf Lime & Frosted Mint Accents ---
val LimeAccent = Color(0xFF8BE83F)
val LimeAccentStrong = Color(0xFF4ED122)
val SageHeroCardStart = Color(0xFFDCFAE0)
val SageHeroCardEnd = Color(0xFFE9FCEB)
val SoftMintSecondary = Color(0xFFD6F9DC)
val SoftMintPrimaryTone = Color(0xFF158A2A)
val SoftMintOnSecondary = Color(0xFFFFFFFF)
val SoftMintContainer = Color(0xFFDEFBE2)
val SoftMintOnContainer = Color(0xFF084210)
val MintAccent = Color(0xFF5CE130)

// --- Logo Pastel & Flame Orange Accents ("AI" & "kcal" Flame Icon) ---
val PastelBlueCard = Color(0xFFE0F7FA)
val PastelBlueAccent = Color(0xFF00838F)
val PastelGreenCard = Color(0xFFE2FBE5)
val PastelGreenAccent = Color(0xFF129A1B)
val PastelPeachCard = Color(0xFFFFEAD5)
val PastelPeachAccent = Color(0xFFFF6200)
val PastelCreamSheet = Color(0xFFFDFEFD)
val StreakBannerDark = Color(0xFF0A3D0E)

// --- Tertiary Logo Flame Orange Palette ---
val WarmAmberTertiary = Color(0xFFFF6200)
val WarmAmberOnTertiary = Color(0xFFFFFFFF)
val WarmAmberContainer = Color(0xFFFFE6D5)
val WarmAmberOnContainer = Color(0xFF4D1B00)

// --- Mockup Warm Sand/Cream Background, Pure White Surface & Deep Charcoal Typography ---
val CleanWhiteBackground = Color(0xFFF5F3EF) // Warm organic sand/cream from mockup
val PureWhiteSurface = Color(0xFFFFFFFF)
val SoftNeutralCard = Color(0xFFEDE9E1)
val SoftNeutralCardLow = Color(0xFFF2EFE8)
val SoftNeutralCardHigh = Color(0xFFE5E0D5)
val DarkCharcoalText = Color(0xFF191C19) // Deep modern charcoal
val MutedSlateText = Color(0xFF6E736D) // Sophisticated muted charcoal
val LightOutline = Color(0xFFE5E0D6)
val LightOutlineVariant = Color(0xFFECE7DD)

// Mockup Semantic Macro Indicator Colors (Sprout Green, Amber Droplet, Coral Cube)
val MockupCarbsGreen = Color(0xFF38A139)
val MockupCarbsBg = Color(0xFFEBF8EC)
val MockupFatAmber = Color(0xFFF59E0B)
val MockupFatBg = Color(0xFFFFF7E6)
val MockupSugarCoral = Color(0xFFEF4444)
val MockupSugarBg = Color(0xFFFEEAEA)
val MockupNeonScan = Color(0xFF70E000)

// --- Dark Mode Background, Surface & Text ---
val DarkBackground = Color(0xFF0A130C)
val DarkSurface = Color(0xFF121F15)
val DarkCardSurface = Color(0xFF182A1C)
val DarkCardSurfaceHigh = Color(0xFF213826)
val DarkTextPrimary = Color(0xFFF0F6F1)
val DarkTextSecondary = Color(0xFFA7C7AE)
val DarkOutline = Color(0xFF2B4D32)
val DarkOutlineVariant = Color(0xFF1E3724)

// --- Error Semantic Tokens ---
val ErrorRedLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorRedDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFECACA)

// --- Nutrition Macro & Telemetry Colors (Synced with Logo Green & Flame Orange) ---
val ProteinColor = Color(0xFF129A1B)
val CarbsColor = Color(0xFFFF6200)
val FatColor = Color(0xFF0284C7)
val FiberColor = Color(0xFF4ED122)
val SugarColor = Color(0xFFF97316)
val WaterBlue = Color(0xFF0284C7)

@Immutable
data class NutriScanExtendedColors(
    val protein: Color = ProteinColor,
    val carbs: Color = CarbsColor,
    val fat: Color = FatColor,
    val fiber: Color = FiberColor,
    val sugar: Color = SugarColor,
    val water: Color = WaterBlue,
    val softMintBadge: Color = SoftMintSecondary,
    val neutralCardBackground: Color = SoftNeutralCard,
    val limePill: Color = LimeAccent,
    val pastelBlueCard: Color = PastelBlueCard,
    val pastelGreenCard: Color = PastelGreenCard,
    val pastelPeachCard: Color = PastelPeachCard
)

val LocalNutriScanExtendedColors = staticCompositionLocalOf {
    NutriScanExtendedColors()
}
