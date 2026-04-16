package com.choreboo.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.choreboo.app.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive banner ad composable for free-tier users.
 *
 * Renders nothing when [isPremium] is true — no layout space is consumed.
 *
 * The ad unit ID is sourced from [BuildConfig.AD_UNIT_BANNER], set per build type in
 * app/build.gradle.kts. Update the release buildConfigField to the real unit ID before shipping.
 */
@Composable
fun BannerAdView(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.AD_UNIT_BANNER,
) {
    if (isPremium) return

    val context: Context = LocalContext.current
    val adView = remember(context, adUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    // Destroy the AdView when this composable leaves composition to prevent Activity leak.
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    )
}
