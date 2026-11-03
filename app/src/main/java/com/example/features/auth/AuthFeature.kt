package com.example.features.auth

import androidx.compose.runtime.Composable
import com.example.ui.screens.AuthScreen

/**
 * Clean Architecture Feature Module: Authentication (`features/auth`)
 */
@Composable
fun AuthFeatureEntry(onAuthSuccess: () -> Unit) {
    AuthScreen(onAuthSuccess = onAuthSuccess)
}
