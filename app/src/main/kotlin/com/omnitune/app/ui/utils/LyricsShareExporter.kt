package com.omnitune.app.ui.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.omnitune.app.models.LyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LyricsShareExporter {

    suspend fun generateImage(
        context: Context,
        lyricsLines: List<String>,
        songTitle: String,
        artistName: String,
        artworkUrl: String?,
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawImageBackground(context, canvas, width, height, artworkUrl)
            drawImageLyrics(canvas, lyricsLines, width, height)
            drawImageFooter(canvas, songTitle, artistName, width, height)
            saveBitmap(context, bitmap, songTitle)
        }.getOrNull()
    }

    suspend fun exportLrc(
        context: Context,
        lines: List<LyricsLine>,
        songTitle: String,
        artistName: String,
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildString {
                if (artistName.isNotBlank()) append("[ar:").append(artistName).append("]\n")
                if (songTitle.isNotBlank()) append("[ti:").append(songTitle).append("]\n")
                append("[tool:OmniTune]\n")
                lines.forEach { line ->
                    if (line.timestamp >= 0L) {
                        append(formatLrcTimestamp(line.timestamp))
                    }
                    append(line.text)
                    append('\n')
                }
            }.toByteArray(Charsets.UTF_8)

            val uri = createMediaUri(
                context = context,
                collection = MediaStore.Files.getContentUri("external"),
                displayName = "${safeFilePart(songTitle).ifBlank { "Lyrics" }}_${System.currentTimeMillis()}.lrc",
                mimeType = "text/plain",
                relativePath = "${Environment.DIRECTORY_DOCUMENTS}/OmniTune",
            ) ?: return@withContext null

            context.contentResolver.openOutputStream(uri)?.use { it.write(payload) } ?: return@withContext null
            uri
        }.getOrNull()
    }

    suspend fun exportPdf(
        context: Context,
        lyricsLines: List<String>,
        songTitle: String,
        artistName: String,
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val pageWidth = 595
            val pageHeight = 842
            val primaryColor = android.graphics.Color.rgb(88, 28, 135)
            val secondaryColor = android.graphics.Color.rgb(212, 58, 156)
            val accentColor = android.graphics.Color.rgb(0, 189, 214)
            val pageBackgroundColor = android.graphics.Color.rgb(252, 252, 255)
            val textColor = android.graphics.Color.rgb(33, 33, 33)
            val pdfDocument = PdfDocument()

            val backgroundPaint = Paint().apply { color = pageBackgroundColor }
            val headerPaint = Paint().apply { color = primaryColor }
            val titlePaint = TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
            val artistPaint = TextPaint().apply {
                color = android.graphics.Color.rgb(232, 191, 255)
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
            val brandingPaint = TextPaint().apply {
                color = accentColor
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val bodyPaint = TextPaint().apply {
                color = textColor
                textSize = 16f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            val footerPaint = TextPaint().apply {
                color = android.graphics.Color.GRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val contentWidth = pageWidth - 100
            val staticLayout = StaticLayout.Builder.obtain(
                lyricsLines.joinToString("\n\n"),
                0,
                lyricsLines.joinToString("\n\n").length,
                bodyPaint,
                contentWidth,
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(10f, 1.3f)
                .build()

            val headerHeight = 140
            val footerHeight = 60
            val contentHeightPerPage = pageHeight - headerHeight - footerHeight
            val totalLines = staticLayout.lineCount
            var currentLine = 0
            var pageNumber = 1

            while (currentLine < totalLines) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), backgroundPaint)
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, headerPaint)
                canvas.drawText(songTitle.ellipsizeForPdf(), 50f, 60f, titlePaint)
                canvas.drawText(artistName, 50f, 90f, artistPaint)
                canvas.drawText("OmniTune", pageWidth - 50f, 60f, brandingPaint)
                canvas.drawLine(50f, 105f, 180f, 105f, Paint().apply {
                    color = secondaryColor
                    strokeWidth = 3f
                })

                canvas.save()
                canvas.translate(50f, headerHeight.toFloat())
                var pageLinesHeight = 0
                val startLine = currentLine
                var endLine = currentLine
                while (endLine < totalLines) {
                    val lineHeight = staticLayout.getLineBottom(endLine) - staticLayout.getLineTop(endLine)
                    if (pageLinesHeight + lineHeight > contentHeightPerPage) break
                    pageLinesHeight += lineHeight
                    endLine++
                }
                if (endLine == startLine && endLine < totalLines) endLine++

                val scrollY = staticLayout.getLineTop(startLine)
                canvas.translate(0f, -scrollY.toFloat())
                canvas.clipRect(0, scrollY, contentWidth, scrollY + pageLinesHeight)
                staticLayout.draw(canvas)
                canvas.restore()

                canvas.drawLine(50f, pageHeight - 50f, pageWidth - 50f, pageHeight - 50f, Paint().apply {
                    color = android.graphics.Color.LTGRAY
                })
                canvas.drawText("Page $pageNumber", pageWidth / 2f, pageHeight - 30f, footerPaint)
                canvas.drawText("Exported from OmniTune", pageWidth / 2f, pageHeight - 15f, footerPaint.apply { textSize = 8f })

                pdfDocument.finishPage(page)
                currentLine = endLine
                pageNumber++
            }

            val uri = createMediaUri(
                context = context,
                collection = MediaStore.Files.getContentUri("external"),
                displayName = "Lyrics_${safeFilePart(songTitle).ifBlank { "Track" }}_${System.currentTimeMillis()}.pdf",
                mimeType = "application/pdf",
                relativePath = "${Environment.DIRECTORY_DOCUMENTS}/OmniTune",
            ) ?: return@withContext null

            context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) } ?: return@withContext null
            pdfDocument.close()
            uri
        }.getOrNull()
    }

    fun shareText(
        context: Context,
        lyricsLines: List<String>,
        songTitle: String,
        artistName: String,
    ): Boolean {
        val shareText = buildString {
            append(songTitle)
            if (artistName.isNotBlank()) append(" - ").append(artistName)
            append("\n\n")
            lyricsLines.forEach { line ->
                append(line)
                append('\n')
            }
            append("\nShared via OmniTune")
        }
        return launchShare(
            context = context,
            intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            },
            title = "Share Lyrics",
        )
    }

    fun shareUri(
        context: Context,
        uri: Uri,
        mimeType: String,
        title: String,
    ): Boolean =
        launchShare(
            context = context,
            intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            title = title,
        )

    private suspend fun drawImageBackground(
        context: Context,
        canvas: Canvas,
        width: Int,
        height: Int,
        artworkUrl: String?,
    ) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                intArrayOf(0xFF1A1A1A.toInt(), 0xFF000000.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        if (artworkUrl.isNullOrBlank()) return
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .size(width, height)
                .build()
            val result = (context.imageLoader.execute(request) as? SuccessResult)?.image
            if (result != null) {
                val artBitmap = result.toBitmap().let {
                    if (it.width != width || it.height != height) {
                        Bitmap.createScaledBitmap(it, width, height, true)
                    } else {
                        it
                    }
                }
                canvas.drawBitmap(artBitmap, 0f, 0f, null)
                canvas.drawColor(0x99000000.toInt())
            }
        }
    }

    private fun drawImageLyrics(
        canvas: Canvas,
        lines: List<String>,
        width: Int,
        height: Int,
    ) {
        val textPaint = TextPaint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val combinedText = lines.joinToString("\n\n")
        val contentWidth = (width * 0.8f).toInt()
        val staticLayout = StaticLayout.Builder.obtain(
            combinedText,
            0,
            combinedText.length,
            textPaint,
            contentWidth,
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .build()

        canvas.save()
        canvas.translate((width - contentWidth) / 2f, (height - staticLayout.height) / 2f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawImageFooter(
        canvas: Canvas,
        title: String,
        artist: String,
        width: Int,
        height: Int,
    ) {
        val titlePaint = TextPaint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val artistPaint = TextPaint().apply {
            color = 0xCCFFFFFF.toInt()
            textSize = 36f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val brandingPaint = TextPaint().apply {
            color = 0x80FFFFFF.toInt()
            textSize = 30f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val footerY = height - 150f
        canvas.drawText(title, width / 2f, footerY - 60f, titlePaint)
        canvas.drawText(artist, width / 2f, footerY, artistPaint)
        canvas.drawText("Shared via OmniTune", width / 2f, height - 50f, brandingPaint)
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, songTitle: String): Uri? {
        val uri = createMediaUri(
            context = context,
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            displayName = "Lyrics_${safeFilePart(songTitle).ifBlank { "Track" }}_${System.currentTimeMillis()}.png",
            mimeType = "image/png",
            relativePath = "${Environment.DIRECTORY_PICTURES}/OmniTune",
        ) ?: return null

        return if (context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            } == true) {
            uri
        } else {
            null
        }
    }

    private fun createMediaUri(
        context: Context,
        collection: Uri,
        displayName: String,
        mimeType: String,
        relativePath: String,
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        return context.contentResolver.insert(collection, values)
    }

    private fun launchShare(context: Context, intent: Intent, title: String): Boolean =
        runCatching {
            context.startActivity(Intent.createChooser(intent, title))
            true
        }.getOrDefault(false)

    private fun safeFilePart(value: String): String = value.replace(Regex("[^a-zA-Z0-9.-]"), "_")

    private fun String.ellipsizeForPdf(): String =
        if (length > 30) take(27) + "..." else this

    private fun formatLrcTimestamp(timestampMs: Long): String {
        val totalCentiseconds = timestampMs / 10
        val minutes = totalCentiseconds / 6000
        val seconds = (totalCentiseconds % 6000) / 100
        val centiseconds = totalCentiseconds % 100
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centiseconds)
    }
}
