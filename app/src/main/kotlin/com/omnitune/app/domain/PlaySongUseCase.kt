package com.omnitune.app.domain

import com.omnitune.app.data.MusicRepository
import com.omnitune.app.data.StreamRepository
import com.omnitune.app.db.entities.Song
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.StreamInfo
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(songId: String): AppResult<StreamInfo> {
        return streamRepository.extractWithFallbacks(songId)
    }

    suspend fun saveSong(song: Song) = musicRepository.saveSong(song)
}
