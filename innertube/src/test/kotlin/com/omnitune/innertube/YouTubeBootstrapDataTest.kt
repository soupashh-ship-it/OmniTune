package com.omnitune.innertube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeBootstrapDataTest {
    @Test
    fun extractsDataSyncIdFromSwJsData() {
        val body = ")]}'\n[[null,null,[\"CgtsomeVisitor\",\"Vb46d0334||\"]]]"

        assertEquals("Vb46d0334", YouTube.extractDataSyncIdFromSwJsData(body))
    }

    @Test
    fun ignoresBootstrapDataWithoutDataSyncId() {
        val body = ")]}'\n[[null,null,[\"CgtsomeVisitor\",\"plain-value\"]]]"

        assertNull(YouTube.extractDataSyncIdFromSwJsData(body))
    }
}
