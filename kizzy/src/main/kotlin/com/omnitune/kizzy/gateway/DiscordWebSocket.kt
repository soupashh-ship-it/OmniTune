/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy.gateway

import com.omnitune.kizzy.KizzyLoggers
import com.omnitune.kizzy.gateway.entities.Payload
import com.omnitune.kizzy.gateway.entities.op.OpCode
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

class DiscordWebSocket(
    private val token: String,
    private val onReady: () -> Unit = {},
    private val onEvent: (JsonObject) -> Unit = {},
) {
    private var session: WebSocketSession? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }
    private val _connected = AtomicBoolean(false)
    private val json = Json

    fun connect() {
        if (_connected.get()) return
        job = scope.launch {
            try {
                client.webSocket("wss://gateway.discord.gg/?v=10&encoding=json") {
                    session = this
                    _connected.set(true)
                    runConnectionLoop()
                }
            } catch (e: Exception) {
                KizzyLoggers.error("WebSocket connection failed", e)
                _connected.set(false)
                delay(5000)
                connect()
            } finally {
                _connected.set(false)
                session = null
            }
        }
    }

    fun isConnected(): Boolean = _connected.get()

    suspend fun send(payload: Payload) {
        val s = session ?: return
        try {
            val jsonStr = json.encodeToString(Payload.serializer(), payload)
            s.outgoing.send(Frame.Text(jsonStr))
        } catch (e: Exception) {
            KizzyLoggers.error("WebSocket send failed", e)
        }
    }

    fun close() {
        _connected.set(false)
        job?.cancel()
        scope.launch {
            try {
                session?.close()
            } catch (_: Exception) {}
            client.close()
        }
    }

    private suspend fun DefaultWebSocketSession.runConnectionLoop() {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                try {
                    val jsonObj = json.parseToJsonElement(text).jsonObject
                    val op = jsonObj["op"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue

                    when (op) {
                        10 -> handleHello(jsonObj)
                        0 -> handleDispatch(jsonObj)
                    }
                } catch (e: Exception) {
                    KizzyLoggers.error("Failed to parse gateway message", e)
                }
            }
        }
    }

    private suspend fun DefaultWebSocketSession.handleHello(hello: JsonObject) {
        val interval = hello["d"]?.jsonObject
            ?.get("heartbeat_interval")?.jsonPrimitive?.content?.toLongOrNull()
            ?: 41250L

        launch {
            while (isActive && _connected.get()) {
                try {
                    outgoing.send(Frame.Text("{\"op\":1,\"d\":null}"))
                } catch (e: Exception) {
                    KizzyLoggers.error("Heartbeat send failed", e)
                }
                delay(interval)
            }
        }

        identify()
    }

    private suspend fun DefaultWebSocketSession.identify() {
        val payload = buildJsonObject {
            put("op", 2)
            putJsonObject("d") {
                put("token", token)
                put("intents", 0)
                putJsonObject("properties") {
                    put("os", "android")
                    put("browser", "omnitune")
                    put("device", "omnitune")
                }
            }
        }
        outgoing.send(Frame.Text(json.encodeToString(JsonObject.serializer(), payload)))
    }

    private fun handleDispatch(json: JsonObject) {
        val eventType = json["t"]?.jsonPrimitive?.content ?: return
        if (eventType == "READY") {
            onReady()
        }
        onEvent(json)
    }
}
