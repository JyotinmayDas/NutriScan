package com.example.widgets

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.core.ads.AdMobConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Policy-compliant AdMob Native Advanced Ad card styled to match NutriScan's Material 3 feed.
 * Hides automatically for Pro users and collapses cleanly when no ad is loaded.
 */
@Composable
fun NativeAdvancedAdCard(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.nativeAdUnitId()
) {
    if (isPremium || LocalInspectionMode.current) return

    val context = LocalContext.current
    var loadedNativeAd by remember(adUnitId) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        var disposed = false
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                if (disposed) {
                    ad.destroy()
                } else {
                    loadedNativeAd?.destroy()
                    loadedNativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!disposed) {
                        loadedNativeAd?.destroy()
                        loadedNativeAd = null
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            disposed = true
            loadedNativeAd?.destroy()
            loadedNativeAd = null
        }
    }

    val nativeAd = loadedNativeAd ?: return
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_native_advanced_card")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(
                    R.layout.view_native_advanced_ad,
                    null,
                    false
                ) as NativeAdView
            },
            update = { adView ->
                bindNativeAdToView(
                    nativeAd = nativeAd,
                    adView = adView,
                    headlineColor = onSurfaceArgb,
                    secondaryColor = onSurfaceVariantArgb
                )
            }
        )
    }
}

private fun bindNativeAdToView(
    nativeAd: NativeAd,
    adView: NativeAdView,
    headlineColor: Int,
    secondaryColor: Int
) {
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val ctaButton = adView.findViewById<Button>(R.id.ad_call_to_action)

    adView.iconView = iconView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView
    adView.mediaView = mediaView
    adView.bodyView = bodyView
    adView.callToActionView = ctaButton

    headlineView.text = nativeAd.headline.orEmpty()
    headlineView.setTextColor(headlineColor)

    val advertiserText = nativeAd.advertiser ?: nativeAd.store
    if (advertiserText.isNullOrBlank()) {
        advertiserView.visibility = View.GONE
    } else {
        advertiserView.visibility = View.VISIBLE
        advertiserView.text = advertiserText
        advertiserView.setTextColor(secondaryColor)
    }

    val iconDrawable = nativeAd.icon?.drawable
    if (iconDrawable == null) {
        iconView.visibility = View.GONE
    } else {
        iconView.visibility = View.VISIBLE
        iconView.setImageDrawable(iconDrawable)
    }

    val mediaContent = nativeAd.mediaContent
    if (mediaContent != null && (mediaContent.hasVideoContent() || mediaContent.mainImage != null)) {
        mediaView.visibility = View.VISIBLE
        mediaView.mediaContent = mediaContent
    } else {
        mediaView.visibility = View.GONE
    }

    if (nativeAd.body.isNullOrBlank()) {
        bodyView.visibility = View.GONE
    } else {
        bodyView.visibility = View.VISIBLE
        bodyView.text = nativeAd.body
        bodyView.setTextColor(secondaryColor)
    }

    if (nativeAd.callToAction.isNullOrBlank()) {
        ctaButton.visibility = View.GONE
    } else {
        ctaButton.visibility = View.VISIBLE
        ctaButton.text = nativeAd.callToAction
    }

    adView.setNativeAd(nativeAd)
}
