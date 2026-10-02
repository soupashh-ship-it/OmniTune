package com.omnitune.shared.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omnitune.shared.ui.components.SquircleShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SquircleShapeTest {

    private val testDensity = Density(1f)

    @Test
    fun testSquircleZeroSmoothingProducesOutline() {
        val shape = SquircleShape(cornerRadius = 16.dp, cornerSmoothing = 0.0f)
        val outline = shape.createOutline(Size(200f, 200f), LayoutDirection.Ltr, testDensity)
        assertTrue(outline is Outline.Rounded)
    }

    @Test
    fun testSquircleDefaultSmoothingCreatesGenericPathOutline() {
        val shape = SquircleShape(cornerRadius = 28.dp, cornerSmoothing = 0.6f)
        assertEquals(0.6f, shape.smoothing)
        val outline = shape.createOutline(Size(200f, 200f), LayoutDirection.Ltr, testDensity)
        assertTrue(outline is Outline.Generic)
    }

    @Test
    fun testSquircleMaximalCurvatureCreatesGenericPathOutline() {
        val shape = SquircleShape(cornerRadius = 28.dp, cornerSmoothing = 1.0f)
        assertEquals(1.0f, shape.smoothing)
        val outline = shape.createOutline(Size(200f, 200f), LayoutDirection.Ltr, testDensity)
        assertTrue(outline is Outline.Generic)
    }

    @Test
    fun testSquircleCornerSmoothingClamping() {
        val negativeSmoothing = SquircleShape(cornerRadius = 20.dp, cornerSmoothing = -0.5f)
        assertEquals(0.0f, negativeSmoothing.smoothing)

        val excessiveSmoothing = SquircleShape(cornerRadius = 20.dp, cornerSmoothing = 2.5f)
        assertEquals(1.0f, excessiveSmoothing.smoothing)
    }

    @Test
    fun testSquircleZeroRadiusReturnsRectangle() {
        val shape = SquircleShape(cornerRadius = 0.dp, cornerSmoothing = 0.6f)
        val outline = shape.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outline is Outline.Rectangle)
    }
}
