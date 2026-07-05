/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.together

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

object TogetherOnlineEndpoint {
    // Your secure Render URL
    private const val HTTP_URL = "https://velune-server.onrender.com"

    @Suppress("UNUSED_PARAMETER")
    fun baseUrlOrNull(
        dataStore: DataStore<Preferences>,
    ): String {
        return HTTP_URL
    }

    fun onlineWebSocketUrlOrNull(
        rawWsUrl: String,
        baseUrl: String,
    ): String? {
        // Read the exact WebSocket URL the Node server gave us (which includes the correct path!)
        val trimmed = rawWsUrl.trim()
        if (trimmed.isNotBlank() && (trimmed.startsWith("ws://") || trimmed.startsWith("wss://"))) {
            return trimmed
        }

        // Safety fallback just in case
        return "wss://velune-server.onrender.com/v1/together/ws"
    }
}
