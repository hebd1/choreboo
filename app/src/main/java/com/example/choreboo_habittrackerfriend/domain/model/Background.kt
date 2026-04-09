package com.example.choreboo_habittrackerfriend.domain.model

/**
 * Pricing tier for a background item.
 * COMMON = 50 points, RARE = 100 points.
 * (The old PREMIUM tier has been replaced by the subscription-gated requiresPremium flag.)
 */
enum class BackgroundTier(val cost: Int) {
    COMMON(50),
    RARE(100),
    PREMIUM(200), // Legacy — kept for potential future use; see requiresPremium below.
}

/**
 * A single purchasable (or free) background for the pet card.
 *
 * @param id               Unique stable string identifier (used as the cloud key).
 * @param assetPath        Path inside `assets/backgrounds/` (e.g. "sunset.webp"), or null for the
 *                         special "Default" mood-gradient background.
 * @param tier             Pricing tier for star-point purchase; null means the background is always
 *                         free. Ignored when [requiresPremium] is true.
 * @param emoji            Emoji shown in the picker grid until/if an asset is present.
 * @param requiresPremium  When true, this background is only accessible to premium subscribers —
 *                         it cannot be purchased with star points. Currently applies to Galaxy,
 *                         Underwater, and Aurora.
 */
data class BackgroundItem(
    val id: String,
    val assetPath: String?,
    val tier: BackgroundTier?,
    val emoji: String,
    val requiresPremium: Boolean = false,
) {
    /** True for the special free "Default" entry that shows the mood-based gradient. */
    val isDefault: Boolean get() = id == BACKGROUND_DEFAULT_ID
    val cost: Int get() = tier?.cost ?: 0
    val isFree: Boolean get() = tier == null && !requiresPremium
}

/** The stable ID used for the always-free "Default" mood-gradient option. */
const val BACKGROUND_DEFAULT_ID = "default"

/**
 * The full catalogue of available backgrounds.
 * Order matters — the Default entry must always be first (shown first in the picker).
 *
 * To add a new background:
 *  1. Drop the asset into `app/src/main/assets/backgrounds/`.
 *  2. Add a new [BackgroundItem] entry here.
 *  3. Add string resources for the label (bg_label_<id>) in values/strings.xml and translations.
 */
val BACKGROUND_REGISTRY: List<BackgroundItem> = listOf(
    BackgroundItem(
        id = BACKGROUND_DEFAULT_ID,
        assetPath = null,
        tier = null,
        emoji = "🌈",
    ),
    BackgroundItem(
        id = "meadow",
        assetPath = "backgrounds/meadow.webp",
        tier = BackgroundTier.COMMON,
        emoji = "🌿",
    ),
    BackgroundItem(
        id = "sunset",
        assetPath = "backgrounds/sunset.webp",
        tier = BackgroundTier.COMMON,
        emoji = "🌅",
    ),
    BackgroundItem(
        id = "ocean",
        assetPath = "backgrounds/ocean.webp",
        tier = BackgroundTier.COMMON,
        emoji = "🌊",
    ),
    BackgroundItem(
        id = "night_sky",
        assetPath = "backgrounds/night_sky.webp",
        tier = BackgroundTier.RARE,
        emoji = "🌙",
    ),
    BackgroundItem(
        id = "autumn",
        assetPath = "backgrounds/autumn.webp",
        tier = BackgroundTier.RARE,
        emoji = "🍂",
    ),
    BackgroundItem(
        id = "cherry_blossom",
        assetPath = "backgrounds/cherry_blossom.webp",
        tier = BackgroundTier.RARE,
        emoji = "🌸",
    ),
    BackgroundItem(
        id = "galaxy",
        assetPath = "backgrounds/galaxy.webp",
        tier = null,
        emoji = "🔭",
        requiresPremium = true,
    ),
    BackgroundItem(
        id = "underwater",
        assetPath = "backgrounds/underwater.webp",
        tier = null,
        emoji = "🐠",
        requiresPremium = true,
    ),
    BackgroundItem(
        id = "aurora",
        assetPath = "backgrounds/aurora.webp",
        tier = null,
        emoji = "✨",
        requiresPremium = true,
    ),
)

/** Quick lookup by id — returns null for unknown ids. */
fun backgroundById(id: String?): BackgroundItem? =
    BACKGROUND_REGISTRY.firstOrNull { it.id == id }

/** Returns the Default background item (always the first entry). */
val defaultBackground: BackgroundItem get() = BACKGROUND_REGISTRY.first()
