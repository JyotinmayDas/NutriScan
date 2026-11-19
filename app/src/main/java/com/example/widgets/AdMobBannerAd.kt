package com.example.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.ads.AdMobConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Lifecycle-aware, adaptive AdMob Banner Ad designed to dock cleanly above the bottom bar.
 * Automatically hides for Pro users and collapses gracefully when an ad fails to load.
 */
@Composable
fun AnchoredAdaptiveBannerAd(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.bannerAdUnitId()
) {
    if (isPremium || LocalInspectionMode.current) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var adFailed by remember(adUnitId) { mutableStateOf(false) }

    if (adFailed) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("admob_adaptive_banner"),
        contentAlignment = Alignment.Center
    ) {
        val availableWidthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(availableWidthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
        }
        val adView = remember(adUnitId, availableWidthDp) {
            AdView(context).apply {
                setAdUnitId(adUnitId)
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adFailed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adSize.height.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
