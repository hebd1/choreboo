package com.example.choreboo_habittrackerfriend.di

import android.content.Context
import android.util.Log
import com.airbnb.lottie.LottieCompositionFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LottiePreloadManager"

/**
 * Preloads all Lottie animation JSON files at application startup.
 *
 * Animations are split into two tiers:
 * - **Critical** (4 mood animations): must be ready before PetScreen renders so the pet is
 *   never shown as a blank. [isCriticalReady] fires when these finish.
 * - **Deferred** (5 action animations): loaded in the background after startup. They are
 *   triggered only by user actions (feed, tap, sleep) so a short delay is acceptable.
 *
 * [isReady] fires when ALL 9 animations are parsed (for callers that need the full set).
 */
@Singleton
class LottiePreloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _isCriticalReady = MutableStateFlow(false)

    /** `true` once the 4 critical mood animations are parsed and in Lottie's LRU cache. */
    val isCriticalReady: StateFlow<Boolean> = _isCriticalReady.asStateFlow()

    private val _isReady = MutableStateFlow(false)

    /** `true` once all 9 animations (critical + deferred) are parsed. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Kick off async parsing for all fox Lottie animations. Each [LottieTask]
     * adds a success/failure listener that decrements a counter; when the counter
     * hits zero, the corresponding ready flag is set to `true`.
     *
     * Safe to call multiple times — subsequent calls are no-ops once already started.
     */
    fun preloadAll() {
        if (_isReady.value) return

        // Mood/state animations — shown immediately when PetScreen opens
        val criticalAnimations = listOf(
            "animations/fox/fox_happy_lottie.json",
            "animations/fox/fox_hungry_lottie.json",
            "animations/fox/fox_sad_lottie.json",
            "animations/fox/fox_idle_lottie.json",
            "animations/fox/fox_loop_sleeping_lottie.json",
        )

        // Action-triggered animations — only play on user interaction (feed, tap, sleep)
        val deferredAnimations = listOf(
            "animations/fox/fox_eating_lottie.json",
            "animations/fox/fox_interact_lottie.json",
            "animations/fox/fox_thumbs_up_lottie.json",
            "animations/fox/fox_start_sleep_lottie.json",
        )

        val criticalRemaining = AtomicInteger(criticalAnimations.size)
        val totalRemaining = AtomicInteger(criticalAnimations.size + deferredAnimations.size)

        fun onAnimationDone(path: String, isCritical: Boolean) {
            if (isCritical) {
                if (criticalRemaining.decrementAndGet() == 0) {
                    Log.d(TAG, "All ${criticalAnimations.size} critical Lottie animations preloaded")
                    _isCriticalReady.value = true
                }
            }
            if (totalRemaining.decrementAndGet() == 0) {
                Log.d(TAG, "All ${criticalAnimations.size + deferredAnimations.size} Lottie animations preloaded")
                _isReady.value = true
            }
        }

        criticalAnimations.forEach { path ->
            val task = LottieCompositionFactory.fromAsset(context, path)
            task.addListener {
                Log.d(TAG, "Preloaded (critical): $path")
                onAnimationDone(path, isCritical = true)
            }
            task.addFailureListener { error ->
                Log.w(TAG, "Failed to preload (critical): $path", error)
                onAnimationDone(path, isCritical = true)
            }
        }

        deferredAnimations.forEach { path ->
            val task = LottieCompositionFactory.fromAsset(context, path)
            task.addListener {
                Log.d(TAG, "Preloaded (deferred): $path")
                onAnimationDone(path, isCritical = false)
            }
            task.addFailureListener { error ->
                Log.w(TAG, "Failed to preload (deferred): $path", error)
                onAnimationDone(path, isCritical = false)
            }
        }
    }
}
