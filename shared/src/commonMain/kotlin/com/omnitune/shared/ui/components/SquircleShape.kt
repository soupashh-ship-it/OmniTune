package com.omnitune.shared.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Continuous curvature Squircle Shape implementing Apple/Figma-style corner smoothing.
 *
 * @param cornerRadius The base corner radius.
 * @param cornerSmoothing Corner smoothing factor:
 *   - 0.0: Standard rectangle / no smoothing
 *   - 0.6: iOS-grade continuous curvature squircle
 *   - 1.0: Maximal hyperellipse curvature
 */
class SquircleShape(
    val cornerRadius: Dp = 28.dp,
    val cornerSmoothing: Float = 0.6f
) : Shape {

    val smoothing: Float = cornerSmoothing.coerceIn(0f, 1f)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rPx = with(density) { cornerRadius.toPx() }
        val maxR = minOf(size.width, size.height) / 2f
        val radius = rPx.coerceIn(0f, maxR)

        if (radius <= 0f || smoothing == 0f && radius == 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }

        if (smoothing == 0f) {
            return Outline.Rounded(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(radius, radius)
                )
            )
        }

        val path = Path().apply {
            val w = size.width
            val h = size.height
            val smoothFactor = smoothing
            val p = radius * (1f + smoothFactor * 0.5f).coerceAtMost(maxR)

            moveTo(p, 0f)
            // Top edge
            lineTo(w - p, 0f)
            // Top-right squircle corner
            cubicTo(
                w - p + p * (1f - smoothFactor * 0.4f), 0f,
                w, p * (1f - smoothFactor * 0.4f),
                w, p
            )
            // Right edge
            lineTo(w, h - p)
            // Bottom-right squircle corner
            cubicTo(
                w, h - p + p * (1f - smoothFactor * 0.4f),
                w - p + p * (1f - smoothFactor * 0.4f), h,
                w - p, h
            )
            // Bottom edge
            lineTo(p, h)
            // Bottom-left squircle corner
            cubicTo(
                p * (1f - smoothFactor * 0.4f), h,
                0f, h - p + p * (1f - smoothFactor * 0.4f),
                0f, h - p
            )
            // Left edge
            lineTo(0f, p)
            // Top-left squircle corner
            cubicTo(
                0f, p * (1f - smoothFactor * 0.4f),
                p * (1f - smoothFactor * 0.4f), 0f,
                p, 0f
            )
            close()
        }

        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return cornerRadius == other.cornerRadius && cornerSmoothing == other.cornerSmoothing
    }

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + cornerSmoothing.hashCode()
        return result
    }

    companion object {
        const val DEFAULT_CORNER_SMOOTHING = 0.6f
        val Default = SquircleShape(cornerRadius = 28.dp, cornerSmoothing = 0.6f)
    }
}
