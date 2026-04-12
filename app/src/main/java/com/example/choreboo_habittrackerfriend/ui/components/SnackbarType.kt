package com.example.choreboo_habittrackerfriend.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals

/** Discriminates the visual style of a [StitchSnackbar]. */
sealed class SnackbarType {
    data object Success     : SnackbarType()
    data object Error       : SnackbarType()
    data object Info        : SnackbarType()
    data object Achievement : SnackbarType()
}

/** [SnackbarVisuals] implementation that carries a [SnackbarType]. */
data class StitchSnackbarVisuals(
    override val message: String,
    val type: SnackbarType,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
) : SnackbarVisuals

/** Convenience extension so call sites don't construct [StitchSnackbarVisuals] manually. */
suspend fun SnackbarHostState.showStitchSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.Info,
    duration: SnackbarDuration = SnackbarDuration.Short,
) = showSnackbar(StitchSnackbarVisuals(message = message, type = type, duration = duration))
