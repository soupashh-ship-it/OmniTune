package com.omnitune.app.ui.component

import androidx.compose.runtime.Composable
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.toDomainSong

@Composable
fun TrackMenuProvider(
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    mediaMetadata: MediaMetadata,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onViewArtist: (() -> Unit)? = null,
    onViewAlbum: (() -> Unit)? = null,
    onMoreLikeThis: (() -> Unit)? = null
) {
    if (!showMenu) return

    val song = mediaMetadata.toDomainSong()
    SongMenuBottomSheet(
        isVisible = showMenu,
        onDismiss = onDismissMenu,
        song = song,
        onPlayNext = onPlayNext?.let { callback -> { callback(); onDismissMenu() } },
        onAddToQueue = onAddToQueue?.let { callback -> { callback(); onDismissMenu() } },
        onAddToPlaylist = onAddToPlaylist?.let { callback -> { callback(); onDismissMenu() } },
        onDownload = onDownload?.let { callback -> { callback(); onDismissMenu() } },
        onShare = onShare?.let { callback -> { callback(); onDismissMenu() } },
        onRemoveFromPlaylist = onRemoveFromPlaylist?.let { cb -> { cb(); onDismissMenu() } },
        onMoveUp = onMoveUp?.let { cb -> { cb(); onDismissMenu() } },
        onMoveDown = onMoveDown?.let { cb -> { cb(); onDismissMenu() } },
        onViewArtist = onViewArtist?.let { cb -> { cb(); onDismissMenu() } },
        onViewAlbum = onViewAlbum?.let { cb -> { cb(); onDismissMenu() } },
        showShare = onShare != null
    )
}
