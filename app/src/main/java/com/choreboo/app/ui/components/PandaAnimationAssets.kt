package com.choreboo.app.ui.components

import com.choreboo.app.domain.model.ChorebooMood

/** Base directory for all panda animation assets. */
private const val PANDA_ANIM_DIR = "animations/panda"

/** Asset path constants for every named panda animation. */
const val PANDA_ANIM_HAPPY = "$PANDA_ANIM_DIR/panda_happy.webp"
const val PANDA_ANIM_HUNGRY = "$PANDA_ANIM_DIR/panda_hungry.webp"
const val PANDA_ANIM_SAD = "$PANDA_ANIM_DIR/panda_sad.webp"
const val PANDA_ANIM_IDLE = "$PANDA_ANIM_DIR/panda_idle.webp"
const val PANDA_ANIM_EATING = "$PANDA_ANIM_DIR/panda_eating.webp"
const val PANDA_ANIM_INTERACT = "$PANDA_ANIM_DIR/panda_interact.webp"
const val PANDA_ANIM_THUMBS_UP = "$PANDA_ANIM_DIR/panda_thumbs_up.webp"
const val PANDA_ANIM_START_SLEEP = "$PANDA_ANIM_DIR/panda_start_sleep.webp"
const val PANDA_ANIM_LOOP_SLEEPING = "$PANDA_ANIM_DIR/panda_loop_sleeping.webp"

/** Number of extra iterations for IDLE (plays 3 times total). */
const val PANDA_IDLE_ITERATIONS = 3

/**
 * Returns the panda animation asset path that corresponds to the given [mood].
 *
 * - HAPPY / CONTENT → [PANDA_ANIM_HAPPY]
 * - HUNGRY          → [PANDA_ANIM_HUNGRY]
 * - all others      → [PANDA_ANIM_SAD]
 */
fun pandaMoodAssetPath(mood: ChorebooMood): String = when (mood) {
    ChorebooMood.HAPPY,
    ChorebooMood.CONTENT -> PANDA_ANIM_HAPPY
    ChorebooMood.HUNGRY -> PANDA_ANIM_HUNGRY
    else -> PANDA_ANIM_SAD
}
