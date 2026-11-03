package com.example.features.onboarding

import androidx.compose.runtime.Composable
import com.example.model.UserProfile
import com.example.ui.screens.OnboardingProfileDialog

/**
 * Clean Architecture Feature Module: Onboarding & Calorie Goal Setup (`features/onboarding`)
 */
@Composable
fun OnboardingFeatureEntry(
    currentProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onSkip: () -> Unit
) {
    OnboardingProfileDialog(
        currentProfile = currentProfile,
        onSaveProfile = onSaveProfile,
        onSkip = onSkip
    )
}
