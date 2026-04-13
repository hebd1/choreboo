package com.example.choreboo_habittrackerfriend.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Composable for playing animated WebP files (VP8+alpha converted via libvpx).
 * Uses Android's AnimatedImageDrawable (API 28+) for native alpha transparency support.
 * On API 24-27, renders a transparent placeholder — callers should show an emoji fallback.
 *
 * Phase-transition correctness is enforced by the caller (PetScreen): each onComplete
 * callback is guarded by a `if (phase == animPhase)` check so stale callbacks from
 * Crossfade's outgoing slot are silently ignored instead of causing incorrect transitions.
 * We deliberately do NOT call unregisterAnimationCallback() here: AnimatedImageDrawable
 * posts the onAnimationEnd invocation as a Runnable and may NPE if the callback list is
 * nullified between the post and the run.
 *
 * @param assetPath Path to the .webp file in assets (e.g., "animations/fox/fox_happy.webp")
 * @param iterations Number of times to loop (default 1 = play once). Use Int.MAX_VALUE for infinite.
 * @param onComplete Callback invoked when the animation finishes (not called for infinite loops).
 * @param modifier Composable modifier.
 */
@Composable
fun WebmAnimationView(
    assetPath: String,
    iterations: Int = 1,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        // AnimatedImageDrawable requires API 28+; caller shows emoji fallback for older devices.
        Box(modifier = modifier.background(Color.Transparent))
        return
    }
    AnimatedWebpViewImpl(
        assetPath = assetPath,
        iterations = iterations,
        onComplete = onComplete,
        modifier = modifier,
    )
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun AnimatedWebpViewImpl(
    assetPath: String,
    iterations: Int,
    onComplete: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    // Use applicationContext so the ImageView does not hold a reference to the Activity.
    val imageView = remember { ImageView(context.applicationContext) }
    // Capture the latest onComplete lambda without restarting the LaunchedEffect on every recompose.
    val currentOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(assetPath) {
        try {
            // Stop and clear the previous animation before loading the new one.
            (imageView.drawable as? AnimatedImageDrawable)?.stop()
            imageView.setImageDrawable(null)

            Timber.d("Loading animated WebP: $assetPath")
            val drawable = withContext(Dispatchers.IO) {
                val source = ImageDecoder.createSource(context.assets, assetPath)
                ImageDecoder.decodeDrawable(source)
            } as? AnimatedImageDrawable

            if (drawable == null) {
                Timber.e("Decoded drawable is not AnimatedImageDrawable: $assetPath")
                return@LaunchedEffect
            }

            // repeatCount = extra plays after the first: 0 = play once, 1 = play twice, etc.
            drawable.repeatCount = when {
                iterations == Int.MAX_VALUE -> AnimatedImageDrawable.REPEAT_INFINITE
                else -> (iterations - 1).coerceAtLeast(0)
            }

            drawable.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(d: Drawable?) {
                    Timber.d("Animation complete: $assetPath")
                    currentOnComplete()
                }
            })

            imageView.setImageDrawable(drawable)
            drawable.start()
            Timber.d("Animation started: $assetPath")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load animated WebP: $assetPath")
        }
    }

    // Pause/resume based on lifecycle.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        (imageView.drawable as? AnimatedImageDrawable)?.stop()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        (imageView.drawable as? AnimatedImageDrawable)?.start()
    }

    // Stop the animation when the composable leaves composition (e.g., Crossfade slot removed).
    // We do NOT unregisterAnimationCallback here: AnimatedImageDrawable posts onAnimationEnd
    // as a Runnable, and nullifying the callback list between the post and the run causes an
    // NPE in AnimatedImageDrawable.postOnAnimationEnd (Android framework bug). Any stale
    // onComplete invocations are harmlessly dropped by the phase guards in PetScreen.
    DisposableEffect(Unit) {
        onDispose {
            (imageView.drawable as? AnimatedImageDrawable)?.stop()
            imageView.setImageDrawable(null)
            Timber.d("ImageView cleaned up")
        }
    }

    Box(
        modifier = modifier.background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { imageView },
            modifier = Modifier.matchParentSize(),
        )
    }
}
