package com.choreboo.app.ui.components

import com.choreboo.app.domain.model.ChorebooMood

/** Base directory for all fox animation assets. */
private const val FOX_ANIM_DIR = "animations/fox"

/** Asset path constants for every named fox animation. */
const val FOX_ANIM_HAPPY = "$FOX_ANIM_DIR/fox_happy.webp"
const val FOX_ANIM_HUNGRY = "$FOX_ANIM_DIR/fox_hungry.webp"
const val FOX_ANIM_SAD = "$FOX_ANIM_DIR/fox_sad.webp"
const val FOX_ANIM_IDLE = "$FOX_ANIM_DIR/fox_idle.webp"
const val FOX_ANIM_EATING = "$FOX_ANIM_DIR/fox_eating.webp"
const val FOX_ANIM_INTERACT = "$FOX_ANIM_DIR/fox_interact.webp"
const val FOX_ANIM_THUMBS_UP = "$FOX_ANIM_DIR/fox_thumbs_up.webp"
const val FOX_ANIM_START_SLEEP = "$FOX_ANIM_DIR/fox_start_sleep.webp"
const val FOX_ANIM_LOOP_SLEEPING = "$FOX_ANIM_DIR/fox_loop_sleeping.webp"

/** Number of extra iterations for IDLE (plays 3 times total). */
const val FOX_IDLE_ITERATIONS = 3

/**
 * Returns the fox animation asset path that corresponds to the given [mood].
 *
 * - HAPPY / CONTENT → [FOX_ANIM_HAPPY]
 * - HUNGRY          → [FOX_ANIM_HUNGRY]
 * - all others      → [FOX_ANIM_SAD]
 */
fun foxMoodAssetPath(mood: ChorebooMood): String = when (mood) {
    ChorebooMood.HAPPY,
    ChorebooMood.CONTENT -> FOX_ANIM_HAPPY
    ChorebooMood.HUNGRY -> FOX_ANIM_HUNGRY
    else -> FOX_ANIM_SAD
}
