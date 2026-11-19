package com.example.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized Google AdMob configuration for NutriScan AI.
 *
 * Debug builds automatically use Google's official test ad unit IDs to protect your live AdMob
 * account from invalid test traffic. Release builds use your live production AdMob Console IDs.
 */
object AdMobConfig {
    // Live AdMob Application ID (also configured in app/build.gradle.kts manifestPlaceholders)
    const val APP_ID = "ca-app-pub-7344344105416382~9014548996"

    // Official Google Mobile Ads Test Ad Unit IDs (auto-used in DEBUG builds)
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Live Production AdMob Console Ad Unit IDs (auto-used in RELEASE builds)
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-7344344105416382/9409687706"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7344344105416382/9162275693"
    const val PROD_APP_OPEN_AD_UNIT_ID = "ca-app-pub-7344344105416382/1878295007"
    const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-7344344105416382/1435441875"
    const val PROD_REWARDED_AD_UNIT_ID = ""

    fun bannerAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String =
        if (isDebug) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    fun interstitialAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String =
        if (isDebug) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    fun appOpenAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String =
        if (isDebug) TEST_APP_OPEN_AD_UNIT_ID else PROD_APP_OPEN_AD_UNIT_ID

    fun nativeAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String =
        if (isDebug) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID

    fun rewardedAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String? =
        if (isDebug) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID.takeIf { it.isNotBlank() }
}

/**
 * Policy-compliant frequency cap evaluator for interstitial ads so users are never spammed.
 */
internal object InterstitialFrequencyPolicy {
    const val MIN_COOLDOWN_MS = 90_000L

    fun shouldShowInterstitial(
        isPremium: Boolean,
        hasLoadedAd: Boolean,
        lastShownTimestampMs: Long,
        nowMs: Long,
        cooldownMs: Long = MIN_COOLDOWN_MS,
        isAnyFullScreenAdShowing: Boolean = false
    ): Boolean {
        if (isPremium || !hasLoadedAd || isAnyFullScreenAdShowing) return false
        if (lastShownTimestampMs <= 0L) return true
        return (nowMs - lastShownTimestampMs) >= cooldownMs
    }
}

/**
 * Policy-compliant freshness (4-hour AdMob expiry) and cooldown evaluator for App Open ads.
 */
internal object AppOpenAdPolicy {
    const val MAX_AD_AGE_MS = 4 * 60 * 60 * 1000L // 4 hours per AdMob best practices
    const val MIN_COOLDOWN_MS = 120_000L // 2 minutes between warm foreground resumes

    fun isAdFresh(loadTimestampMs: Long, nowMs: Long, maxAgeMs: Long = MAX_AD_AGE_MS): Boolean {
        if (loadTimestampMs <= 0L) return false
        val age = nowMs - loadTimestampMs
        return age in 0 until maxAgeMs
    }

    fun shouldShowAppOpenAd(
        isPremium: Boolean,
        hasLoadedAd: Boolean,
        isAnyFullScreenAdShowing: Boolean,
        loadTimestampMs: Long,
        lastShownTimestampMs: Long,
        nowMs: Long,
        cooldownMs: Long = MIN_COOLDOWN_MS
    ): Boolean {
        if (isPremium || !hasLoadedAd || isAnyFullScreenAdShowing) return false
        if (!isAdFresh(loadTimestampMs, nowMs)) return false
        if (lastShownTimestampMs <= 0L) return true
        return (nowMs - lastShownTimestampMs) >= cooldownMs
    }
}

/**
 * Manages Google Mobile Ads SDK initialization, preloading, and lifecycle-safe display for
 * App Open, Interstitial, and Rewarded ads across NutriScan AI.
 */
object AdMobManager {
    private const val TAG = "AdMobManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isPremiumUser = false

    @Volatile
    private var isFullScreenAdShowing = false

    @Volatile
    private var isLoadingInterstitial = false

    @Volatile
    private var isLoadingAppOpen = false

    @Volatile
    private var isLoadingRewarded = false

    @Volatile
    private var lastInterstitialShownMs: Long = 0L

    @Volatile
    private var appOpenLoadTimestampMs: Long = 0L

    @Volatile
    private var lastAppOpenShownMs: Long = 0L

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    fun updatePremiumStatus(isPremium: Boolean) {
        isPremiumUser = isPremium
    }

    /**
     * Initializes the Google Mobile Ads SDK off the main thread and preloads full-screen ads.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        isInitialized = true
        scope.launch {
            try {
                MobileAds.initialize(appContext) {
                    Log.d(TAG, "Google Mobile Ads SDK initialized.")
                    scope.launch(Dispatchers.Main) {
                        preloadAppOpenAd(appContext)
                        preloadInterstitial(appContext)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "MobileAds.initialize skipped or failed: ${e.message}")
            }
        }
    }

    // --- APP OPEN ADS ---

    fun preloadAppOpenAd(context: Context) {
        if (isPremiumUser || isLoadingAppOpen) return
        val now = System.currentTimeMillis()
        if (appOpenAd != null && AppOpenAdPolicy.isAdFresh(appOpenLoadTimestampMs, now)) return

        isLoadingAppOpen = true
        val appContext = context.applicationContext
        try {
            AppOpenAd.load(
                appContext,
                AdMobConfig.appOpenAdUnitId(),
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        appOpenLoadTimestampMs = System.currentTimeMillis()
                        isLoadingAppOpen = false
                        Log.d(TAG, "App Open ad loaded.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        appOpenLoadTimestampMs = 0L
                        isLoadingAppOpen = false
                        Log.d(TAG, "App Open ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isLoadingAppOpen = false
            Log.w(TAG, "App Open preload error: ${e.message}")
        }
    }

    /**
     * Shows a preloaded App Open Ad when returning to the foreground if fresh (< 4h) and outside
     * the cooldown window. Never blocks UI or overlaps another full-screen ad.
     */
    fun showAppOpenAdIfReady(
        activity: Activity?,
        isPremium: Boolean = isPremiumUser,
        onContinue: () -> Unit = {}
    ) {
        val ad = appOpenAd
        val now = System.currentTimeMillis()
        if (activity == null ||
            activity.isFinishing ||
            activity.isDestroyed ||
            !AppOpenAdPolicy.shouldShowAppOpenAd(
                isPremium = isPremium,
                hasLoadedAd = ad != null,
                isAnyFullScreenAdShowing = isFullScreenAdShowing,
                loadTimestampMs = appOpenLoadTimestampMs,
                lastShownTimestampMs = lastAppOpenShownMs,
                nowMs = now
            )
        ) {
            if (!isPremium && activity != null && (ad == null || !AppOpenAdPolicy.isAdFresh(appOpenLoadTimestampMs, now))) {
                appOpenAd = null
                preloadAppOpenAd(activity.applicationContext)
            }
            onContinue()
            return
        }

        appOpenAd = null
        appOpenLoadTimestampMs = 0L
        lastAppOpenShownMs = now
        isFullScreenAdShowing = true
        val appContext = activity.applicationContext

        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isFullScreenAdShowing = false
                preloadAppOpenAd(appContext)
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isFullScreenAdShowing = false
                Log.w(TAG, "App Open ad failed to show: ${adError.message}")
                preloadAppOpenAd(appContext)
                onContinue()
            }
        }
        ad?.show(activity)
    }

    // --- INTERSTITIAL ADS ---

    fun preloadInterstitial(context: Context) {
        if (isPremiumUser || interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        val appContext = context.applicationContext
        try {
            InterstitialAd.load(
                appContext,
                AdMobConfig.interstitialAdUnitId(),
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                        Log.d(TAG, "Interstitial ad loaded.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                        Log.d(TAG, "Interstitial ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isLoadingInterstitial = false
            Log.w(TAG, "Interstitial preload error: ${e.message}")
        }
    }

    /**
     * Shows a preloaded interstitial ad at natural transition points while respecting premium
     * status and a 90-second cooldown. Always invokes [onContinue] so navigation is never blocked.
     */
    fun showInterstitialIfReady(
        activity: Activity?,
        isPremium: Boolean,
        onContinue: () -> Unit
    ) {
        val ad = interstitialAd
        val now = System.currentTimeMillis()
        if (activity == null ||
            activity.isFinishing ||
            activity.isDestroyed ||
            !InterstitialFrequencyPolicy.shouldShowInterstitial(
                isPremium = isPremium,
                hasLoadedAd = ad != null,
                lastShownTimestampMs = lastInterstitialShownMs,
                nowMs = now,
                isAnyFullScreenAdShowing = isFullScreenAdShowing
            )
        ) {
            if (!isPremium && ad == null && activity != null) {
                preloadInterstitial(activity.applicationContext)
            }
            onContinue()
            return
        }

        interstitialAd = null
        lastInterstitialShownMs = now
        isFullScreenAdShowing = true
        val appContext = activity.applicationContext

        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isFullScreenAdShowing = false
                preloadInterstitial(appContext)
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isFullScreenAdShowing = false
                Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                preloadInterstitial(appContext)
                onContinue()
            }
        }
        ad?.show(activity)
    }

    // --- REWARDED ADS ---

    fun preloadRewarded(context: Context) {
        val unitId = AdMobConfig.rewardedAdUnitId() ?: return
        if (rewardedAd != null || isLoadingRewarded) return
        isLoadingRewarded = true
        val appContext = context.applicationContext
        try {
            RewardedAd.load(
                appContext,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isLoadingRewarded = false
                        _isRewardedReady.value = true
                        Log.d(TAG, "Rewarded ad loaded.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        isLoadingRewarded = false
                        _isRewardedReady.value = false
                        Log.d(TAG, "Rewarded ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isLoadingRewarded = false
            _isRewardedReady.value = false
            Log.w(TAG, "Rewarded preload error: ${e.message}")
        }
    }

    /**
     * Shows a preloaded Rewarded Ad so users can earn a bonus AI meal scan.
     */
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onAdUnavailable: (String) -> Unit
    ) {
        if (AdMobConfig.rewardedAdUnitId() == null) {
            onAdUnavailable("Rewarded ads are not configured for this build.")
            return
        }
        if (activity == null || activity.isFinishing || activity.isDestroyed || isFullScreenAdShowing) {
            onAdUnavailable("Unable to display ad right now. Please try again.")
            return
        }
        val ad = rewardedAd
        val appContext = activity.applicationContext
        if (ad == null) {
            preloadRewarded(appContext)
            onAdUnavailable("Loading rewarded ad... Please tap again in a few seconds.")
            return
        }

        rewardedAd = null
        _isRewardedReady.value = false
        isFullScreenAdShowing = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isFullScreenAdShowing = false
                preloadRewarded(appContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isFullScreenAdShowing = false
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                preloadRewarded(appContext)
                onAdUnavailable("Ad could not be shown right now. Please try again shortly.")
            }
        }

        ad.show(activity) {
            onRewardEarned()
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
