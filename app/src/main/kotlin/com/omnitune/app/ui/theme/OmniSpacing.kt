package com.omnitune.app.ui.theme

import androidx.compose.ui.unit.dp

object OmniSpacing {
    // Core 4-base rhythm. Every new UI must use these; raw dp literals are audited by
    // scripts/check-spacing.ps1.
    val micro = 4.dp
    val compact = 8.dp
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp
    val section = 24.dp
    val hero = 32.dp
    val screen = 40.dp
    val xl = 48.dp
    val xxl = 64.dp

    val screenHorizontal = section
    // The reference Home hero bleeds wider than its text rails while retaining
    // comfortable touch spacing inside those rails.
    val screenHorizontalCompact = 15.dp
    val sectionGap = section
    val chapterGap = hero

    val rowVertical = small
    val rowHorizontal = medium
    val cardPadding = medium
    val cardPaddingLarge = large

    val chip = 10.dp
    val headerTop = 12.dp

    /** Minimum interactive target on any axis (accessibility floor). */
    val touchTarget = 44.dp
}
