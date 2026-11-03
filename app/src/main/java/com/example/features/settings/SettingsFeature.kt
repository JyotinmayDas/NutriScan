package com.example.features.settings

import androidx.compose.runtime.Composable
import com.example.model.UserProfile
import com.example.ui.screens.ProfileAndSettingsScreen

/**
 * Clean Architecture Feature Module: User Profile, Localization & Settings (`features/settings`)
 */
@Composable
fun SettingsFeatureEntry(
    profile: UserProfile,
    remainingScansToday: Int,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onTogglePremiumPlan: (Boolean) -> Unit,
    onGrantBonusScan: () -> Unit,
    onOpenEditProfileDialog: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit = {},
    onDeleteAccountData: () -> Unit,
    onSignOut: () -> Unit
) {
    ProfileAndSettingsScreen(
        profile = profile,
        remainingScansToday = remainingScansToday,
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        onSelectLanguage = onSelectLanguage,
        onTogglePremiumPlan = onTogglePremiumPlan,
        onGrantBonusScan = onGrantBonusScan,
        onOpenEditProfileDialog = onOpenEditProfileDialog,
        onSaveProfile = onSaveProfile,
        onDeleteAccountData = onDeleteAccountData,
        onSignOut = onSignOut
    )
}
