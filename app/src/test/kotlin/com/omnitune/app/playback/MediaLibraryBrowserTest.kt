package com.omnitune.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryBrowserTest {
    @Test
    fun rootAndCategoriesDeclareBrowseabilityHonestly() {
        val root = MediaLibraryBrowser.rootItem()
        assertEquals(MediaLibraryBrowser.ROOT_ID, root.mediaId)
        assertTrue(root.mediaMetadata.isBrowsable == true)
        assertFalse(root.mediaMetadata.isPlayable == true)

        val downloads = MediaLibraryBrowser.categoryItem(MediaLibraryBrowser.DOWNLOADS_ID)
        assertEquals("Downloads", downloads.mediaMetadata.title.toString())
        assertTrue(downloads.mediaMetadata.isBrowsable == true)
        assertFalse(downloads.mediaMetadata.isPlayable == true)
    }

    @Test
    fun pageItemsHandlesBoundsWithoutThrowing() {
        val values = listOf("a", "b", "c", "d", "e")

        assertEquals(listOf("a", "b"), MediaLibraryBrowser.pageItems(values, page = 0, pageSize = 2))
        assertEquals(listOf("c", "d"), MediaLibraryBrowser.pageItems(values, page = 1, pageSize = 2))
        assertEquals(listOf("e"), MediaLibraryBrowser.pageItems(values, page = 2, pageSize = 2))
        assertEquals(emptyList<String>(), MediaLibraryBrowser.pageItems(values, page = 3, pageSize = 2))
        assertEquals(emptyList<String>(), MediaLibraryBrowser.pageItems(values, page = -1, pageSize = 2))
        assertEquals(emptyList<String>(), MediaLibraryBrowser.pageItems(values, page = 0, pageSize = 0))
    }
}
