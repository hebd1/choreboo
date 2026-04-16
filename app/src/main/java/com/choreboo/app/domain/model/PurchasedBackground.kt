package com.choreboo.app.domain.model

/**
 * Domain model for a background the user has purchased.
 * Mirrors [com.choreboo.app.data.local.entity.PurchasedBackgroundEntity]
 * but belongs to the domain layer so ViewModels never hold entity references.
 */
data class PurchasedBackground(
    val ownerUid: String,
    val backgroundId: String,
    val purchasedAt: Long,
)
