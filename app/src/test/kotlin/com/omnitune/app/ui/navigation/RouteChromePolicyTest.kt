package com.omnitune.app.ui.navigation

import com.omnitune.app.ui.utils.DeviceFormFactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteChromePolicyTest {
    @Test
    fun `standard phone with a current song shows bottom navigation and reserves mini player space`() {
        val layout = RouteChromePolicy.resolve(
            routeMode = RouteChromeMode.Standard,
            formFactor = DeviceFormFactor.Phone,
            hasCurrentSong = true,
            isMiniPlayerDismissed = false,
            isPlayerExpanded = false,
            isBlockingOverlayVisible = false,
            isImeVisible = false,
        )

        assertTrue(layout.showNavigationChrome)
        assertTrue(layout.showBottomNavigation)
        assertFalse(layout.showNavigationRail)
        assertTrue(layout.showPlayerSheet)
        assertEquals(RouteChromePolicy.BottomNavigationHeightDp, layout.miniPlayerBottomPaddingDp)
        assertEquals(
            RouteChromePolicy.MiniPlayerHeightDp + RouteChromePolicy.SnackbarMarginDp,
            layout.contentBottomInsetDp,
        )
        assertEquals(
            RouteChromePolicy.BottomNavigationHeightDp +
                RouteChromePolicy.MiniPlayerHeightDp +
                RouteChromePolicy.SnackbarMarginDp,
            layout.snackbarBottomInsetDp,
        )
    }

    @Test
    fun `tablet uses rail chrome without reserving a fake bottom navigation bar`() {
        val layout = RouteChromePolicy.resolve(
            routeMode = RouteChromeMode.Standard,
            formFactor = DeviceFormFactor.Tablet,
            hasCurrentSong = true,
            isMiniPlayerDismissed = false,
            isPlayerExpanded = false,
            isBlockingOverlayVisible = false,
            isImeVisible = false,
        )

        assertTrue(layout.showNavigationChrome)
        assertFalse(layout.showBottomNavigation)
        assertTrue(layout.showNavigationRail)
        assertTrue(layout.showPlayerSheet)
        assertEquals(0, layout.miniPlayerBottomPaddingDp)
        assertEquals(
            RouteChromePolicy.MiniPlayerHeightDp + RouteChromePolicy.SnackbarMarginDp,
            layout.contentBottomInsetDp,
        )
    }

    @Test
    fun `immersive route hides collapsed mini player and navigation chrome`() {
        val layout = RouteChromePolicy.resolve(
            routeMode = RouteChromeMode.Immersive,
            formFactor = DeviceFormFactor.Phone,
            hasCurrentSong = true,
            isMiniPlayerDismissed = false,
            isPlayerExpanded = false,
            isBlockingOverlayVisible = false,
            isImeVisible = false,
        )

        assertFalse(layout.showNavigationChrome)
        assertFalse(layout.showBottomNavigation)
        assertFalse(layout.showNavigationRail)
        assertFalse(layout.showPlayerSheet)
        assertEquals(0, layout.contentBottomInsetDp)
    }

    @Test
    fun `expanded player can remain visible over an immersive route while app chrome stays hidden`() {
        val layout = RouteChromePolicy.resolve(
            routeMode = RouteChromeMode.Immersive,
            formFactor = DeviceFormFactor.Phone,
            hasCurrentSong = true,
            isMiniPlayerDismissed = false,
            isPlayerExpanded = true,
            isBlockingOverlayVisible = false,
            isImeVisible = false,
        )

        assertFalse(layout.showNavigationChrome)
        assertFalse(layout.showBottomNavigation)
        assertTrue(layout.showPlayerSheet)
        assertEquals(0, layout.contentBottomInsetDp)
    }

    @Test
    fun `phone IME hides collapsed player and bottom navigation`() {
        val layout = RouteChromePolicy.resolve(
            routeMode = RouteChromeMode.Standard,
            formFactor = DeviceFormFactor.Phone,
            hasCurrentSong = true,
            isMiniPlayerDismissed = false,
            isPlayerExpanded = false,
            isBlockingOverlayVisible = false,
            isImeVisible = true,
        )

        assertTrue(layout.showNavigationChrome)
        assertFalse(layout.showBottomNavigation)
        assertFalse(layout.showPlayerSheet)
        assertEquals(0, layout.contentBottomInsetDp)
    }

    @Test
    fun `blocking onboarding overlay suppresses all app chrome`() {
        val layout = RouteChromePolicy.resolve(
            routeMode = RouteChromeMode.Standard,
            formFactor = DeviceFormFactor.Phone,
            hasCurrentSong = true,
            isMiniPlayerDismissed = false,
            isPlayerExpanded = true,
            isBlockingOverlayVisible = true,
            isImeVisible = false,
        )

        assertFalse(layout.showNavigationChrome)
        assertFalse(layout.showBottomNavigation)
        assertFalse(layout.showNavigationRail)
        assertFalse(layout.showPlayerSheet)
        assertEquals(0, layout.contentBottomInsetDp)
    }
}
