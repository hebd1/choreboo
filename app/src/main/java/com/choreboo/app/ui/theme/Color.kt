package com.choreboo.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Stitch Design System – Light Theme ──────────────────────────────────────
// Primary: deep growth green
val StitchPrimary            = Color(0xFF006E1C)
val StitchOnPrimary          = Color(0xFFFFFFFF)
val StitchPrimaryContainer   = Color(0xFF4CAF50)
val StitchOnPrimaryContainer = Color(0xFF003C0B)
val StitchPrimaryFixed       = Color(0xFF94F990)
val StitchPrimaryFixedDim    = Color(0xFF78DC77)
val StitchOnPrimaryFixed     = Color(0xFF002204)
val StitchOnPrimaryFixedVariant = Color(0xFF005313)

// Secondary: achievement / points (orange axis)
val StitchSecondary               = Color(0xFF8B5000)
val StitchOnSecondary             = Color(0xFFFFFFFF)
val StitchSecondaryContainer      = Color(0xFFFF9800)
val StitchOnSecondaryContainer    = Color(0xFF653900)
val StitchSecondaryFixed          = Color(0xFFFFDCBE)
val StitchSecondaryFixedDim       = Color(0xFFFFB870)
val StitchOnSecondaryFixed        = Color(0xFF2C1600)
val StitchOnSecondaryFixedVariant = Color(0xFF693C00)

// Tertiary: XP / leveling / lore (deep purple)
val StitchTertiary               = Color(0xFF6833EA)
val StitchOnTertiary             = Color(0xFFFFFFFF)
val StitchTertiaryContainer      = Color(0xFFA488FF)
val StitchOnTertiaryContainer    = Color(0xFF39009C)
val StitchTertiaryFixed          = Color(0xFFE8DEFF)
val StitchTertiaryFixedDim       = Color(0xFFCDBDFF)
val StitchOnTertiaryFixed        = Color(0xFF20005F)
val StitchOnTertiaryFixedVariant = Color(0xFF4F00D0)

// Surface hierarchy (depth-first tonal layering)
val StitchBackground              = Color(0xFFF6FAFE)
val StitchOnBackground            = Color(0xFF171C1F)
val StitchSurface                 = Color(0xFFF6FAFE)
val StitchOnSurface               = Color(0xFF171C1F)
val StitchSurfaceBright           = Color(0xFFF6FAFE)
val StitchSurfaceDim              = Color(0xFFD6DADE)
val StitchSurfaceVariant          = Color(0xFFDFE3E7)
val StitchOnSurfaceVariant        = Color(0xFF3F4A3C)
val StitchSurfaceContainerLowest  = Color(0xFFFFFFFF)
val StitchSurfaceContainerLow     = Color(0xFFF0F4F8)
val StitchSurfaceContainer        = Color(0xFFEAEEF2)
val StitchSurfaceContainerHigh    = Color(0xFFE4E9ED)
val StitchSurfaceContainerHighest = Color(0xFFDFE3E7)
val StitchInverseSurface          = Color(0xFF2C3134)
val StitchInverseOnSurface        = Color(0xFFEDF1F5)
val StitchInversePrimary          = Color(0xFF78DC77)

// Outline & borders
val StitchOutline        = Color(0xFF6F7A6B)
val StitchOutlineVariant = Color(0xFFBECAB9)
// StitchSurfaceTint intentionally mirrors StitchPrimary (0xFF006E1C): M3 spec calls for
// the tint color to match the primary color so that surface tonal elevation layers look correct.
val StitchSurfaceTint    = Color(0xFF006E1C)

// Error
val StitchError            = Color(0xFFBA1A1A)
val StitchOnError          = Color(0xFFFFFFFF)
val StitchErrorContainer   = Color(0xFFFFDAD6)
val StitchOnErrorContainer = Color(0xFF93000A)

// ── Stitch Design System – Dark Theme ────────────────────────────────────────
val StitchDarkPrimary            = Color(0xFF78DC77)
val StitchDarkOnPrimary          = Color(0xFF003910)
val StitchDarkPrimaryContainer   = Color(0xFF005313)
val StitchDarkOnPrimaryContainer = Color(0xFF94F990)

val StitchDarkSecondary               = Color(0xFFFFB870)
val StitchDarkOnSecondary             = Color(0xFF4A2800)
val StitchDarkSecondaryContainer      = Color(0xFF693C00)
val StitchDarkOnSecondaryContainer    = Color(0xFFFFDCBE)

val StitchDarkTertiary               = Color(0xFFCDBDFF)
val StitchDarkOnTertiary             = Color(0xFF380096)
val StitchDarkTertiaryContainer      = Color(0xFF5018CE)
val StitchDarkOnTertiaryContainer    = Color(0xFFE8DEFF)

val StitchDarkBackground              = Color(0xFF0F1416)
val StitchDarkOnBackground            = Color(0xFFE1E3E6)
val StitchDarkSurface                 = Color(0xFF0F1416)
val StitchDarkOnSurface               = Color(0xFFE1E3E6)
val StitchDarkSurfaceBright           = Color(0xFF1B2023) // elevated surface for dark mode
val StitchDarkSurfaceDim              = Color(0xFF0F1416)
val StitchDarkSurfaceVariant          = Color(0xFF3A4640)
val StitchDarkOnSurfaceVariant        = Color(0xFFBBC8BB)
val StitchDarkSurfaceContainerLowest  = Color(0xFF0A0F11)
val StitchDarkSurfaceContainerLow     = Color(0xFF171C1F)
val StitchDarkSurfaceContainer        = Color(0xFF1B2023)
val StitchDarkSurfaceContainerHigh    = Color(0xFF252B2E)
val StitchDarkSurfaceContainerHighest = Color(0xFF303639)
val StitchDarkInverseSurface          = Color(0xFFE1E3E6)
val StitchDarkInverseOnSurface        = Color(0xFF2C3134)
val StitchDarkInversePrimary          = Color(0xFF006E1C)

val StitchDarkOutline        = Color(0xFF8A9389)
val StitchDarkOutlineVariant = Color(0xFF3A4640)

val StitchDarkError            = Color(0xFFFFB4AB)
val StitchDarkOnError          = Color(0xFF690005)
val StitchDarkErrorContainer   = Color(0xFF93000A)
val StitchDarkOnErrorContainer = Color(0xFFFFDAD6)

// ── Pet mood gradient colors — Light ─────────────────────────────────────────
val PetMoodHappyStart   = Color(0xFFFFF9C4)  // yellow radial center
val PetMoodHappyEnd     = Color(0xFF4CAF50)  // primaryContainer green edge
val PetMoodHungryStart  = Color(0xFFFFF3E0)
val PetMoodHungryEnd    = Color(0xFFFF9800)
val PetMoodTiredStart   = Color(0xFFE3F2FD)
val PetMoodTiredEnd     = Color(0xFF90CAF9)
val PetMoodSadStart     = Color(0xFFECEFF1)
val PetMoodSadEnd       = Color(0xFFB0BEC5)
val PetMoodContentStart = Color(0xFFE8F5E9)
val PetMoodContentEnd   = Color(0xFF66BB6A)
val PetMoodIdleStart    = StitchSurfaceContainerLow  // neutral fallback (= 0xFFF0F4F8 light)

// ── Pet mood gradient colors — Dark ──────────────────────────────────────────
val PetMoodDarkHappyStart   = Color(0xFF3A3A00)  // muted yellow-olive
val PetMoodDarkHappyEnd     = Color(0xFF1B5E20)  // dark green
val PetMoodDarkHungryStart  = Color(0xFF3E2800)
val PetMoodDarkHungryEnd    = Color(0xFFE65100)
val PetMoodDarkTiredStart   = Color(0xFF0D253A)
val PetMoodDarkTiredEnd     = Color(0xFF1565C0)
val PetMoodDarkSadStart     = Color(0xFF1C2126)
val PetMoodDarkSadEnd       = Color(0xFF455A64)
val PetMoodDarkContentStart = Color(0xFF1B2E1C)
val PetMoodDarkContentEnd   = Color(0xFF2E7D32)
val PetMoodDarkIdleStart    = StitchDarkSurfaceContainerLow  // = 0xFF171C1F dark

// ── Semantic accent colors ────────────────────────────────────────────────────
val StreakFlame = Color(0xFFFF6D00)  // streak fire icon / text
val GoldGlow    = Color(0xFFFFD54F) // legendary item highlight

// ── Heatmap colors (calendar) ──────────────────────────────────────────────────
val HeatmapHigh = StitchPrimaryContainer   // 4+ tasks: primary green at 30% alpha
val HeatmapLow  = Color(0xFFFFC107)        // 1-3 tasks: amber yellow at 30% alpha
val HeatmapNone = StitchSurfaceVariant     // No tasks: surface variant

// ── Legacy aliases (semantic aliases for PetScreen gradient use) ──────────────
// These are intentional re-exports of theme tokens so that PetScreen gradient
// code can use descriptive names without importing full theme color names.
// XpPurple, PetHappyGreen, PetHungryOrange → map to tertiary / container tokens.
// PetTiredBlue, PetSadGrey → standalone accent values not covered by the token system.
val XpPurple       = StitchTertiary
val PetHappyGreen  = StitchPrimaryContainer
val PetHungryOrange = StitchSecondaryContainer
val PetTiredBlue   = Color(0xFF90CAF9)
val PetSadGrey     = Color(0xFFB0BEC5)
