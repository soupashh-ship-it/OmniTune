/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.utils.DeviceFormFactor

internal enum class RouteChromeMode {
    Standard,
    Immersive,
}

@Immutable
internal data class RouteChromeLayout(
    val showNavigationChrome: Boolean,
    val showBottomNavigation: Boolean,
    val showNavigationRail: Boolean,
    val showPlayerSheet: Boolean,
    val miniPlayerBottomPaddingDp: Int,
    val contentBottomInsetDp: Int,
    val snackbarBottomInsetDp: Int,
)

internal object RouteChromePolicy {
    const val BottomNavigationHeightDp = 80
    const val MiniPlayerHeightDp = 64
    const val SnackbarMarginDp = 16

    fun resolve(
        routeMode: RouteChromeMode,
        formFactor: DeviceFormFactor,
        hasCurrentSong: Boolean,
        isMiniPlayerDismissed: Boolean,
        isPlayerExpanded: Boolean,
        isBlockingOverlayVisible: Boolean,
        isImeVisible: Boolean,
    ): RouteChromeLayout {
        val routeAllowsChrome = routeMode == RouteChromeMode.Standard && !isBlockingOverlayVisible
        val showNavigationChrome = routeAllowsChrome && !isPlayerExpanded
        val showBottomNavigation = showNavigationChrome && formFactor.isPhoneLike && !isImeVisible
        val showNavigationRail = showNavigationChrome && !formFactor.isPhoneLike

        val routeAllowsCollapsedPlayer = routeMode == RouteChromeMode.Standard && !isBlockingOverlayVisible
        val showPlayerSheet = hasCurrentSong &&
            !isMiniPlayerDismissed &&
            !isBlockingOverlayVisible &&
            (routeAllowsCollapsedPlayer || isPlayerExpanded) &&
            (!isImeVisible || isPlayerExpanded)

        val miniPlayerCollapsed = showPlayerSheet && !isPlayerExpanded
        val bottomNavigationHeight = if (showBottomNavigation) BottomNavigationHeightDp else 0
        val miniPlayerHeight = if (miniPlayerCollapsed) MiniPlayerHeightDp else 0
        val contentBottomInset = if (routeAllowsChrome && !isImeVisible && !isPlayerExpanded) {
            miniPlayerHeight + SnackbarMarginDp
        } else {
            0
        }

        return RouteChromeLayout(
            showNavigationChrome = showNavigationChrome,
            showBottomNavigation = showBottomNavigation,
            showNavigationRail = showNavigationRail,
            showPlayerSheet = showPlayerSheet,
            miniPlayerBottomPaddingDp = bottomNavigationHeight,
            contentBottomInsetDp = contentBottomInset,
            snackbarBottomInsetDp = bottomNavigationHeight + miniPlayerHeight + SnackbarMarginDp,
        )
    }
}

@Immutable
data class RouteChromeInsets(
    val contentBottomPadding: Dp = (RouteChromePolicy.MiniPlayerHeightDp + RouteChromePolicy.SnackbarMarginDp).dp,
    val snackbarBottomPadding: Dp = (
        RouteChromePolicy.BottomNavigationHeightDp +
            RouteChromePolicy.MiniPlayerHeightDp +
            RouteChromePolicy.SnackbarMarginDp
        ).dp,
    val imeVisible: Boolean = false,
)

val LocalRouteChromeInsets = staticCompositionLocalOf { RouteChromeInsets() }
