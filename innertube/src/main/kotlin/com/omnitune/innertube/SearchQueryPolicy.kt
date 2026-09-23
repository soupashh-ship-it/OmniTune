package com.omnitune.innertube

/** Bounds and normalizes user-provided text before it reaches search requests. */
object SearchQueryPolicy {
    const val MAX_QUERY_CODE_POINTS = 200

    fun limitInput(raw: String): String {
        val output = StringBuilder(minOf(raw.length, MAX_QUERY_CODE_POINTS))
        var index = 0
        var codePoints = 0

        while (index < raw.length && codePoints < MAX_QUERY_CODE_POINTS) {
            val codePoint = raw.codePointAt(index)
            val charCount = Character.charCount(codePoint)
            index += charCount
            codePoints++

            if (charCount == 1 && codePoint in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code) continue
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) continue

            output.appendCodePoint(codePoint)
        }

        return output.toString()
    }

    fun normalize(raw: String): String {
        val input = limitInput(raw)
        val output = StringBuilder(input.length)
        var index = 0
        var pendingSpace = false

        while (index < input.length) {
            val codePoint = input.codePointAt(index)
            index += Character.charCount(codePoint)

            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || Character.isISOControl(codePoint)) {
                pendingSpace = output.isNotEmpty()
                continue
            }

            if (pendingSpace) output.append(' ')
            output.appendCodePoint(codePoint)
            pendingSpace = false
        }

        return output.toString()
    }
}
