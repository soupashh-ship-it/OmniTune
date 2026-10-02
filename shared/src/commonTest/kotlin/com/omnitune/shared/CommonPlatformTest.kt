package com.omnitune.shared

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommonPlatformTest {
    @Test
    fun testPlatformNotNull() {
        val platform = getPlatform()
        assertNotNull(platform)
        assertTrue(platform.name.isNotBlank())
    }
}
