package com.example.features.subscription

import com.example.core.constants.AppConstants
import com.example.model.UserProfile

/**
 * Clean Architecture Feature Module: Subscription & Quota Management (`features/subscription`)
 */
object SubscriptionFeature {
    fun dailyScanLimit(profile: UserProfile): Int =
        if (profile.isPremium) AppConstants.PRO_DAILY_SCAN_LIMIT else AppConstants.FREE_DAILY_SCAN_LIMIT
}
