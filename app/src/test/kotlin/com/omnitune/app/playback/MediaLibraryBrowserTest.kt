package com.omnitune.app.playback

import androidx.media3.session.MediaConstants
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
    fun rootContentStyleHintsMatchCarCategoryRoot() {
        assertEquals(
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            MediaLibraryBrowser.RootContentStyleHints.singleItemStyle,
        )
        assertEquals(
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            MediaLibraryBrowser.RootContentStyleHints.browsableChildrenStyle,
        )
        assertEquals(
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
            MediaLibraryBrowser.RootContentStyleHints.playableChildrenStyle,
        )
    }

    @Test
    fun categoryContentStyleHintsMatchCarCategoryRows() {
        assertEquals(
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            MediaLibraryBrowser.CategoryContentStyleHints.singleItemStyle,
        )
        assertEquals(
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
            MediaLibraryBrowser.CategoryContentStyleHints.browsableChildrenStyle,
        )
        assertEquals(
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
            MediaLibraryBrowser.CategoryContentStyleHints.playableChildrenStyle,
        )
    }

    @Test
    fun rootParamsPreserveRequestFlags() {
        val params = MediaLibraryBrowser.rootParams(
            androidx.media3.session.MediaLibraryService.LibraryParams.Builder()
                .setOffline(true)
                .setRecent(true)
                .setSuggested(true)
                .build()
        )

        assertTrue(params.isOffline)
        assertTrue(params.isRecent)
        assertTrue(params.isSuggested)
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
