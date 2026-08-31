/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.utils

object SharedTransitionKeys {
    fun playerArtwork(songId: String): String = "player_artwork_$songId"
    fun songTitle(songId: String): String = "song_title_$songId"
    fun songArtist(songId: String): String = "song_artist_$songId"
}
