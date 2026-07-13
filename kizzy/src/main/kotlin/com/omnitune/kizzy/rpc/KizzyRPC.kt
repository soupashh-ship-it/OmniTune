/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy.rpc

import com.omnitune.kizzy.KizzyLogger
import com.omnitune.kizzy.gateway.DiscordWebSocket
import com.omnitune.kizzy.gateway.entities.Payload
import com.omnitune.kizzy.gateway.entities.op.OpCode
import com.omnitune.kizzy.gateway.entities.presence.Activity
import com.omnitune.kizzy.gateway.entities.presence.Assets
import com.omnitune.kizzy.gateway.entities.presence.Metadata
import com.omnitune.kizzy.gateway.entities.presence.Presence
import com.omnitune.kizzy.gateway.entities.presence.Timestamps
import com.omnitune.kizzy.repository.KizzyRepository
import com.omnitune.kizzy.remote.ApiService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

enum class ActivityType(val value: Int) {
    PLAYING(0),
    STREAMING(1),
    LISTENING(2),
    WATCHING(3),
    COMPETING(5),
}

class KizzyRPC(
    private val repository: KizzyRepository,
    private val apiService: ApiService,
    private val logger: KizzyLogger,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private var webSocket: DiscordWebSocket? = null
    private var currentToken: String = ""
    private val rpcScope = CoroutineScope(Dispatchers.IO)

    fun isConnected(): Boolean = webSocket?.isConnected() ?: false

    suspend fun connect(token: String) {
        currentToken = token
        val ws = DiscordWebSocket(
            token = token,
            onReady = { logger.info("Discord RPC connected") },
            onEvent = { payload ->
                val op = payload["op"]?.jsonPrimitive?.content?.toIntOrNull()
                if (op == 9) {
                    logger.warning("Invalid session, reconnecting...")
                    rpcScope.launch { reconnect() }
                }
            },
        )
        ws.connect()
        webSocket = ws
    }

    suspend fun buildActivity(
        name: String,
        type: ActivityType = ActivityType.LISTENING,
        state: String? = null,
        details: String? = null,
        largeImage: RpcImage? = null,
        largeText: String? = null,
        smallImage: RpcImage? = null,
        smallText: String? = null,
        startTimestamp: Long? = null,
        endTimestamp: Long? = null,
        buttons: List<Pair<String, String>>? = null,
        status: String = "online",
    ) {
        var resolvedLarge: String? = null
        var resolvedSmall: String? = null
        if (largeImage != null) resolvedLarge = resolveImage(largeImage)
        if (smallImage != null) resolvedSmall = resolveImage(smallImage)

        val assets = if (resolvedLarge != null || resolvedSmall != null) {
            Assets(
                largeImage = resolvedLarge,
                largeText = largeText?.take(128),
                smallImage = resolvedSmall,
                smallText = smallText?.take(128),
            )
        } else null

        val timestamps = when {
            startTimestamp != null || endTimestamp != null ->
                Timestamps(start = startTimestamp, end = endTimestamp)
            else -> null
        }

        val buttonLabels = buttons?.map { it.first.take(32) }
        val buttonUrls = buttons?.map { it.second }

        val activity = Activity(
            name = name.take(128),
            type = type.value,
            state = state?.take(128),
            details = details?.take(128),
            assets = assets,
            timestamps = timestamps,
            buttons = buttonLabels,
            metadata = if (buttonUrls != null) Metadata(buttonUrls = buttonUrls) else null,
        )

        val presence = Presence(
            activities = listOf(activity),
            afk = false,
            since = 0,
            status = status,
        )

        updateActivity(presence)
    }

    suspend fun updateActivity(presence: Presence) {
        val ws = webSocket ?: run {
            if (currentToken.isNotBlank()) connect(currentToken)
            return
        }
        if (!ws.isConnected()) {
            logger.warning("WebSocket not connected, will retry on next cycle")
            return
        }
        ws.send(
            Payload(
                op = OpCode.PRESENCE_UPDATE,
                d = json.encodeToJsonElement(Presence.serializer(), presence),
            )
        )
    }

    suspend fun stopActivity() {
        val empty = Presence(activities = emptyList(), afk = false, status = "invisible")
        updateActivity(empty)
    }

    suspend fun closeRPC() {
        try { stopActivity() } catch (_: Exception) {}
        webSocket?.close()
        webSocket = null
    }

    private suspend fun reconnect() {
        webSocket?.close()
        if (currentToken.isNotBlank()) connect(currentToken)
    }

    private suspend fun resolveImage(image: RpcImage): String? = when (image) {
        is RpcImage.DiscordImage -> image.id
        is RpcImage.ExternalImage -> repository.getImage(image.url)
        is RpcImage.Unresolved -> null
    }

    companion object {
        suspend fun getUserInfo(
            client: HttpClient,
            token: String,
        ): Result<UserInfo> = runCatching {
            val j = Json { ignoreUnknownKeys = true }
            val body = client.get("https://discord.com/api/v10/users/@me") {
                header("Authorization", token)
            }.bodyAsText()
            j.decodeFromString<UserInfo>(body)
        }
    }
}
