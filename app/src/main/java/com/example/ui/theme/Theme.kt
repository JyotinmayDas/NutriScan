package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val NutriScanLightColorScheme = lightColorScheme(
    primary = FreshGreenPrimary,
    onPrimary = FreshGreenOnPrimary,
    primaryContainer = FreshGreenPrimaryContainer,
    onPrimaryContainer = FreshGreenOnPrimaryContainer,
    secondary = SoftMintPrimaryTone,
    onSecondary = SoftMintOnSecondary,
    secondaryContainer = SoftMintContainer,
    onSecondaryContainer = SoftMintOnContainer,
    tertiary = WarmAmberTertiary,
    onTertiary = WarmAmberOnTertiary,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = WarmAmberOnContainer,
    background = CleanWhiteBackground,
    onBackground = DarkCharcoalText,
    surface = PureWhiteSurface,
    onSurface = DarkCharcoalText,
    surfaceVariant = SoftNeutralCard,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = CleanWhiteBackground,
    surfaceContainerLow = SoftNeutralCardLow,
    surfaceContainer = SoftNeutralCard,
    surfaceContainerHigh = SoftNeutralCardHigh,
    surfaceContainerHighest = SoftNeutralCardHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorRedLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

val NutriScanDarkColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = DarkBackground,
    primaryContainer = FreshGreenDark,
    onPrimaryContainer = SoftMintSecondary,
    secondary = EmeraldVibrant,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = CarbsColor,
    onTertiary = DarkBackground,
    tertiaryContainer = WarmAmberOnContainer,
    onTertiaryContainer = WarmAmberContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkCardSurface,
    surfaceContainerHigh = DarkCardSurfaceHigh,
    surfaceContainerHighest = DarkCardSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val LightExtendedColors = NutriScanExtendedColors(
    softMintBadge = SoftMintSecondary,
    neutralCardBackground = SoftNeutralCard
)

private val DarkExtendedColors = NutriScanExtendedColors(
    softMintBadge = FreshGreenDark,
    neutralCardBackground = DarkCardSurface
)

/**
 * Custom Material Design 3 theme component for NutriScan AI.
 * Provides primary Fresh Green, secondary Soft Mint Green, Clean White backgrounds,
 * Dark Charcoal typography, rounded card shapes, and dark mode support.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NutriScanDarkColorScheme else NutriScanLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalNutriScanExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

object AppTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val extendedColors: NutriScanExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNutriScanExtendedColors.current
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppTheme(darkTheme = darkTheme, content = content)
}
