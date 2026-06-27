import re

code = """
        val effectiveMode =
            when (qualityMode) {
                PlaybackQualityMode.AUTO -> if (networkMetered) PlaybackQualityMode.BALANCED else PlaybackQualityMode.BEST_AVAILABLE
                else -> qualityMode
            }

        val preferHigher =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
                .thenByDescending { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val preferLower =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
                .thenBy { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val candidates = when (effectiveMode) {
            PlaybackQualityMode.BEST_AVAILABLE -> audioFormats.sortedWith(preferHigher)
            PlaybackQualityMode.DATA_SAVER -> audioFormats.sortedWith(preferLower)
            else -> { // BALANCED
                // try to find bitrates around 128k-160k
                val midRange = audioFormats.filter { it.bitrate in 100_000..180_000 }
                if (midRange.isNotEmpty()) {
                    midRange.sortedWith(preferHigher) + audioFormats.filter { it !in midRange }.sortedWith(preferHigher)
                } else {
                    // pick middle of list
                    val sorted = audioFormats.sortedWith(preferHigher)
                    val midIndex = sorted.size / 2
                    listOf(sorted[midIndex]) + sorted.filterIndexed { index, _ -> index != midIndex }
                }
            }
        }
"""
