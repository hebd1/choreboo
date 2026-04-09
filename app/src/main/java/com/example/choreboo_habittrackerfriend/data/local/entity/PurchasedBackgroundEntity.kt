package com.example.choreboo_habittrackerfriend.data.local.entity

import androidx.room.Entity

/**
 * Room entity for the purchased_backgrounds table.
 *
 * One row per background that the current user has unlocked by spending star points.
 * [backgroundId] matches a [com.example.choreboo_habittrackerfriend.domain.model.BackgroundItem.id]
 * from [com.example.choreboo_habittrackerfriend.domain.model.BACKGROUND_REGISTRY].
 *
 * The composite primary key (ownerUid, backgroundId) mirrors the cloud table structure and
 * prevents duplicate purchases in the local cache.
 *
 * This table is a read-only cloud cache plus an optimistic local write: purchases are written
 * locally first (instant feedback) and then synced to Data Connect. Cleared on sign-out and
 * account reset.
 */
@Entity(
    tableName = "purchased_backgrounds",
    primaryKeys = ["ownerUid", "backgroundId"],
)
data class PurchasedBackgroundEntity(
    val ownerUid: String,
    val backgroundId: String,
    val purchasedAt: Long = System.currentTimeMillis(),
)
