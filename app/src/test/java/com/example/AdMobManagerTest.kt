package com.example

import com.example.core.ads.AdMobConfig
import com.example.core.ads.AppOpenAdPolicy
import com.example.core.ads.InterstitialFrequencyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMobManagerTest {

    @Test
    fun adMobConfig_debugModeAlwaysReturnsOfficialGoogleTestIds() {
        assertEquals(AdMobConfig.TEST_BANNER_AD_UNIT_ID, AdMobConfig.bannerAdUnitId(isDebug = true))
        assertEquals(AdMobConfig.TEST_INTERSTITIAL_AD_UNIT_ID, AdMobConfig.interstitialAdUnitId(isDebug = true))
        assertEquals(AdMobConfig.TEST_APP_OPEN_AD_UNIT_ID, AdMobConfig.appOpenAdUnitId(isDebug = true))
        assertEquals(AdMobConfig.TEST_NATIVE_AD_UNIT_ID, AdMobConfig.nativeAdUnitId(isDebug = true))
        assertEquals(AdMobConfig.TEST_REWARDED_AD_UNIT_ID, AdMobConfig.rewardedAdUnitId(isDebug = true))
    }

    @Test
    fun adMobConfig_releaseModeReturnsConfiguredLivePublisherIds() {
        assertEquals("ca-app-pub-7344344105416382~9014548996", AdMobConfig.APP_ID)
        assertEquals("ca-app-pub-7344344105416382/9409687706", AdMobConfig.bannerAdUnitId(isDebug = false))
        assertEquals("ca-app-pub-7344344105416382/9162275693", AdMobConfig.interstitialAdUnitId(isDebug = false))
        assertEquals("ca-app-pub-7344344105416382/1878295007", AdMobConfig.appOpenAdUnitId(isDebug = false))
        assertEquals("ca-app-pub-7344344105416382/1435441875", AdMobConfig.nativeAdUnitId(isDebug = false))
        assertNull(AdMobConfig.rewardedAdUnitId(isDebug = false))
    }

    @Test
    fun interstitialFrequencyPolicy_blocksPremiumUsersAndUnloadedAds() {
        assertFalse(
            InterstitialFrequencyPolicy.shouldShowInterstitial(
                isPremium = true,
                hasLoadedAd = true,
                lastShownTimestampMs = 0L,
                nowMs = 100_000L
            )
        )
        assertFalse(
            InterstitialFrequencyPolicy.shouldShowInterstitial(
                isPremium = false,
                hasLoadedAd = false,
                lastShownTimestampMs = 0L,
                nowMs = 100_000L
            )
        )
    }

    @Test
    fun interstitialFrequencyPolicy_enforcesCooldownWindow() {
        assertTrue(
            InterstitialFrequencyPolicy.shouldShowInterstitial(
                isPremium = false,
                hasLoadedAd = true,
                lastShownTimestampMs = 0L,
                nowMs = 10_000L
            )
        )
        assertFalse(
            InterstitialFrequencyPolicy.shouldShowInterstitial(
                isPremium = false,
                hasLoadedAd = true,
                lastShownTimestampMs = 100_000L,
                nowMs = 150_000L,
                cooldownMs = 90_000L
            )
        )
        assertTrue(
            InterstitialFrequencyPolicy.shouldShowInterstitial(
                isPremium = false,
                hasLoadedAd = true,
                lastShownTimestampMs = 100_000L,
                nowMs = 190_000L,
                cooldownMs = 90_000L
            )
        )
    }

    @Test
    fun appOpenAdPolicy_enforcesFreshnessCooldownAndOverlapGuard() {
        val loadTime = 1_000_000L
        assertTrue(
            AppOpenAdPolicy.shouldShowAppOpenAd(
                isPremium = false,
                hasLoadedAd = true,
                isAnyFullScreenAdShowing = false,
                loadTimestampMs = loadTime,
                lastShownTimestampMs = 0L,
                nowMs = loadTime + 60_000L
            )
        )
        // Expired after 4 hours
        assertFalse(
            AppOpenAdPolicy.shouldShowAppOpenAd(
                isPremium = false,
                hasLoadedAd = true,
                isAnyFullScreenAdShowing = false,
                loadTimestampMs = loadTime,
                lastShownTimestampMs = 0L,
                nowMs = loadTime + AppOpenAdPolicy.MAX_AD_AGE_MS
            )
        )
        // Cooldown active within 2 minutes
        assertFalse(
            AppOpenAdPolicy.shouldShowAppOpenAd(
                isPremium = false,
                hasLoadedAd = true,
                isAnyFullScreenAdShowing = false,
                loadTimestampMs = loadTime,
                lastShownTimestampMs = loadTime + 10_000L,
                nowMs = loadTime + 60_000L
            )
        )
        // Blocked when another full-screen ad is showing
        assertFalse(
            AppOpenAdPolicy.shouldShowAppOpenAd(
                isPremium = false,
                hasLoadedAd = true,
                isAnyFullScreenAdShowing = true,
                loadTimestampMs = loadTime,
                lastShownTimestampMs = 0L,
                nowMs = loadTime + 300_000L
            )
        )
    }
}
