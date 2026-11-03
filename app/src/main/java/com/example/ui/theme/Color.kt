package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Primary Leaf & Lime Green Palette (Matching Uploaded UI Designs) ---
val FreshGreenPrimary = Color(0xFF16A34A)
val FreshGreenOnPrimary = Color(0xFFFFFFFF)
val FreshGreenPrimaryContainer = Color(0xFFDCFCE7)
val FreshGreenOnPrimaryContainer = Color(0xFF052E16)
val FreshGreenDark = Color(0xFF15803D)
val EmeraldVibrant = Color(0xFF22C55E)

// --- Lime & Sage Card Accents (Image 1, 2, 8, 9 Hero Cards & Buttons) ---
val LimeAccent = Color(0xFFCBEF79)
val LimeAccentStrong = Color(0xFF84CC16)
val SageHeroCardStart = Color(0xFFD8F3DC)
val SageHeroCardEnd = Color(0xFFECFCCB)
val SoftMintSecondary = Color(0xFFD1FAE5)
val SoftMintPrimaryTone = Color(0xFF0D9488)
val SoftMintOnSecondary = Color(0xFFFFFFFF)
val SoftMintContainer = Color(0xFFDDF7E3)
val SoftMintOnContainer = Color(0xFF064E3B)
val MintAccent = Color(0xFF4ADE80)

// --- Pastel Macro & Stat Card Surfaces (Image 1, 3, 4) ---
val PastelBlueCard = Color(0xFFE0F2FE)
val PastelBlueAccent = Color(0xFF0284C7)
val PastelGreenCard = Color(0xFFECFCCB)
val PastelGreenAccent = Color(0xFF65A30D)
val PastelPeachCard = Color(0xFFFFEDD5)
val PastelPeachAccent = Color(0xFFEA580C)
val PastelCreamSheet = Color(0xFFFDFBF7)
val StreakBannerDark = Color(0xFF1C1917)

// --- Tertiary Warm Nutrition Accent Palette ---
val WarmAmberTertiary = Color(0xFFF59E0B)
val WarmAmberOnTertiary = Color(0xFFFFFFFF)
val WarmAmberContainer = Color(0xFFFEF3C7)
val WarmAmberOnContainer = Color(0xFF451A03)

// --- Light Mode Background, Surface & Charcoal Text ---
val CleanWhiteBackground = Color(0xFFF6F8F5)
val PureWhiteSurface = Color(0xFFFFFFFF)
val SoftNeutralCard = Color(0xFFEEF3EC)
val SoftNeutralCardLow = Color(0xFFF4F7F2)
val SoftNeutralCardHigh = Color(0xFFE4ECE2)
val DarkCharcoalText = Color(0xFF111827)
val MutedSlateText = Color(0xFF526058)
val LightOutline = Color(0xFFD5E0D7)
val LightOutlineVariant = Color(0xFFE5ECE6)

// --- Dark Mode Background, Surface & Text ---
val DarkBackground = Color(0xFF0B1410)
val DarkSurface = Color(0xFF12211A)
val DarkCardSurface = Color(0xFF1A2E25)
val DarkCardSurfaceHigh = Color(0xFF223B30)
val DarkTextPrimary = Color(0xFFF0FDF4)
val DarkTextSecondary = Color(0xFFBBF7D0)
val DarkOutline = Color(0xFF2D5243)
val DarkOutlineVariant = Color(0xFF1E3A2E)

// --- Error Semantic Tokens ---
val ErrorRedLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorRedDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFECACA)

// --- Nutrition Macro & Telemetry Colors ---
val ProteinColor = Color(0xFF0EA5E9)
val CarbsColor = Color(0xFFF59E0B)
val FatColor = Color(0xFF10B981)
val FiberColor = Color(0xFF8B5CF6)
val SugarColor = Color(0xFFEC4899)
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
