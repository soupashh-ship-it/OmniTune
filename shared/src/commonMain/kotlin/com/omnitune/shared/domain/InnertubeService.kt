package com.omnitune.shared.domain

import com.omnitune.shared.domain.models.AlbumItem
import com.omnitune.shared.domain.models.ArtistItem
import com.omnitune.shared.domain.models.SearchFilter
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.domain.models.StreamInfo

interface InnertubeService {
    suspend fun search(query: String, filter: SearchFilter = SearchFilter.ALL): Result<List<SongItem>>
    suspend fun getSearchSuggestions(query: String): Result<List<String>>
    suspend fun getAlbum(browseId: String): Result<AlbumItem>
    suspend fun getArtist(browseId: String): Result<ArtistItem>
    suspend fun resolveStreamUrl(videoId: String): Result<StreamInfo>
}
