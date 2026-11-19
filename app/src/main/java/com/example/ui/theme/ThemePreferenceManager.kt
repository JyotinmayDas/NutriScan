package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    LIGHT(
        id = "LIGHT",
        title = "Light",
        subtitle = "Soft Sage & Daylight Emerald",
        icon = Icons.Default.LightMode
    ),
    DARK(
        id = "DARK",
        title = "Dark",
        subtitle = "Low-Light Deep Forest & Mint",
        icon = Icons.Default.DarkMode
    ),
    SYSTEM(
        id = "SYSTEM",
        title = "System",
        subtitle = "Follows device display setting",
        icon = Icons.Default.BrightnessAuto
    );

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LIGHT
        }
    }
}

/**
 * Global reactive theme state manager persisted to SharedPreferences so switching between
 * Light, Dark, and System modes immediately updates every screen in NutriScan (including Auth,
 * Dashboard, Scanner, Diary, Coach, and Profile) and survives app restarts.
 */
object ThemePreferenceManager {
    private const val PREFS_NAME = "nutriscan_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DARK_BOOLEAN = "is_dark_mode"

    private val _themeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isDarkModeOverride = MutableStateFlow(false)
    val isDarkModeOverride: StateFlow<Boolean> = _isDarkModeOverride.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = AppThemeMode.fromId(prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.id))
        val savedDark = prefs.getBoolean(KEY_DARK_BOOLEAN, savedMode == AppThemeMode.DARK)
        _themeMode.value = savedMode
        _isDarkModeOverride.value = savedDark
    }

    fun setDarkMode(context: Context?, enabled: Boolean) {
        val mode = if (enabled) AppThemeMode.DARK else AppThemeMode.LIGHT
        _themeMode.value = mode
        _isDarkModeOverride.value = enabled
        context?.applicationContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_THEME_MODE, mode.id)
            ?.putBoolean(KEY_DARK_BOOLEAN, enabled)
            ?.apply()
    }

    fun setThemeMode(context: Context?, mode: AppThemeMode) {
        val isDark = when (mode) {
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
            AppThemeMode.SYSTEM -> _isDarkModeOverride.value
        }
        _themeMode.value = mode
        _isDarkModeOverride.value = isDark
        context?.applicationContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_THEME_MODE, mode.id)
            ?.putBoolean(KEY_DARK_BOOLEAN, isDark)
            ?.apply()
    }

    @Composable
    fun resolveIsDarkTheme(
        mode: AppThemeMode,
        explicitDarkFlag: Boolean
    ): Boolean {
        return when (mode) {
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> explicitDarkFlag
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }
}

/**
 * Compact 1-tap Light / Dark Theme Toggle Button for top app headers (Home Dashboard, Auth, etc.).
 */
@Composable
fun QuickThemeToggleButton(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (isDarkMode) DarkCardSurfaceHigh else MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable { onToggleDarkMode(!isDarkMode) }
            .border(
                width = 1.dp,
                color = if (isDarkMode) MintAccent.copy(alpha = 0.5f) else FreshGreenPrimary.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .testTag("quick_theme_toggle_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                tint = if (isDarkMode) LimeAccent else DarkCharcoalText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Comprehensive Theme & Low-Light Readability Switcher Card for Profile & Settings and Dashboard.
 */
@Composable
fun ThemeAppearanceSelectorCard(
    isDarkMode: Boolean,
    currentThemeMode: AppThemeMode = if (isDarkMode) AppThemeMode.DARK else AppThemeMode.LIGHT,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit = { mode ->
        onToggleDarkMode(mode == AppThemeMode.DARK)
    },
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_appearance_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        shape = CircleShape,
                        color = if (isDarkMode) DarkCardSurfaceHigh else PastelPeachCard,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isDarkMode) LimeAccent else DarkCharcoalText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App Theme & Low-Light Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isDarkMode) {
                                "Active: Deep Emerald Dark Mode (Optimized for low-light)"
                            } else {
                                "Active: Soft Sage Light Mode (High daylight clarity)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onToggleDarkMode,
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }

            // 3-Way Segmented Selector: Light | Dark | System
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = when (mode) {
                        AppThemeMode.DARK -> isDarkMode && currentThemeMode != AppThemeMode.SYSTEM
                        AppThemeMode.LIGHT -> !isDarkMode && currentThemeMode != AppThemeMode.SYSTEM
                        AppThemeMode.SYSTEM -> currentThemeMode == AppThemeMode.SYSTEM
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            FreshGreenPrimary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectThemeMode(mode)
                            }
                            .testTag("theme_mode_option_${mode.id.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.title,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Contrast & Readability Preview Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkMode) DarkCardSurfaceHigh else PastelGreenCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarkMode) {
                            "Eye-comfort dark surfaces reduce glare in dim & night environments."
                        } else {
                            "Tap the Moon icon in the top bar anytime for instant Dark Mode."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) DarkTextPrimary else DarkCharcoalText
                    )
                }
            }
        }
    }
}
