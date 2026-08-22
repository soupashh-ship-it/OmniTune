package com.omnitune.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackNotificationContractTest {

    @Test
    fun presentationUsesTrackMetadataThenAlbumFallbacks() {
        assertEquals(
            PlaybackNotificationPresentation(title = "Fixture song", artist = "Fixture artist"),
            PlaybackNotificationContract.presentation(
                title = "Fixture song",
                artist = "Fixture artist",
                albumArtist = "Album artist",
                albumTitle = "Album title",
                fallbackTitle = "OmniTune",
            ),
        )
        assertEquals(
            PlaybackNotificationPresentation(title = "OmniTune", artist = "Album artist"),
            PlaybackNotificationContract.presentation(
                title = " ",
                artist = null,
                albumArtist = "Album artist",
                albumTitle = "Album title",
                fallbackTitle = "OmniTune",
            ),
        )
        assertEquals(
            PlaybackNotificationPresentation(title = "OmniTune", artist = "Playing"),
            PlaybackNotificationContract.presentation(
                title = null,
                artist = " ",
                albumArtist = null,
                albumTitle = null,
                fallbackTitle = "OmniTune",
            ),
        )
    }

    @Test
    fun actionsKeepTransportOrderAndUseCorrectPlayPauseCommand() {
        val paused = PlaybackNotificationContract.actions(isPlaying = false, isLiked = false)
        assertEquals(
            listOf(
                PlaybackNotificationManager.ACTION_PREVIOUS,
                PlaybackNotificationManager.ACTION_PLAY,
                PlaybackNotificationManager.ACTION_NEXT,
                PlaybackNotificationManager.ACTION_LIKE,
                PlaybackNotificationManager.ACTION_REPEAT,
                PlaybackNotificationManager.ACTION_STOP,
            ),
            paused.map(PlaybackNotificationActionSpec::action),
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6), paused.map(PlaybackNotificationActionSpec::requestCode))
        assertEquals("Play", paused[1].title)

        val playing = PlaybackNotificationContract.actions(isPlaying = true, isLiked = true)
        assertEquals(PlaybackNotificationManager.ACTION_PAUSE, playing[1].action)
        assertEquals("Pause", playing[1].title)
        assertEquals("Like", playing[3].title)
    }
}
