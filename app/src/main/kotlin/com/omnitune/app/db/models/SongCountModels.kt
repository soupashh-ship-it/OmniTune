package com.omnitune.app.db.models

data class SongPlayCount(val songId: String, val playCount: Int)
data class SongSkipCount(val songId: String, val skipCount: Int)
