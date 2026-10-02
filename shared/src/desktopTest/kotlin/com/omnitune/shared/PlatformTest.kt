package com.omnitune.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun testDesktopPlatform() {
        val platform = getPlatform()
        assertTrue(platform.name.contains("Desktop JVM"))
        assertFalse(platform.isIos)
    }
}
