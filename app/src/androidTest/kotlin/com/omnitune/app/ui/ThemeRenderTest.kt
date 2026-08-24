/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.ui.theme.OmniTuneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First Compose UI test: proves the app theme renders content and establishes the
 * createComposeRule-based pattern for future UI tests (see TEST_INFRA.md).
 *
 * Runs on-device via `connectedDebugAndroidTest`; CI compiles this source set.
 */
@RunWith(AndroidJUnit4::class)
class ThemeRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeRendersContentInLightMode() {
        composeRule.setContent {
            OmniTuneTheme(darkTheme = false) {
                Text("omnitune-theme-smoke")
            }
        }
        composeRule.onNodeWithText("omnitune-theme-smoke").assertIsDisplayed()
    }

    @Test
    fun themeRendersContentInDarkMode() {
        composeRule.setContent {
            OmniTuneTheme(darkTheme = true, pureBlack = true) {
                Text("omnitune-dark-smoke")
            }
        }
        composeRule.onNodeWithText("omnitune-dark-smoke").assertIsDisplayed()
    }

    @Test
    fun themeAcceptsSeedPaletteWithoutCrashing() {
        composeRule.setContent {
            OmniTuneTheme(darkTheme = false, dynamicColor = true) {
                Text("omnitune-dynamic-smoke")
            }
        }
        composeRule.onNodeWithText("omnitune-dynamic-smoke").assertIsDisplayed()
    }
}
