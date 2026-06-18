package com.omnitune.app.kizzy.gateway

import com.omnitune.app.kizzy.KizzyLogger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class DiscordWebSocket(
    private val token: String,
    private val onReady: () -> Unit = {},
    private val onEvent: (JsonObject) -> Unit = {}
) {
    private var session: WebSocketSession? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    fun connect() {
        job = scope.launch {
            try {
                client.webSocket("wss://gateway.discord.gg/?v=10&encoding=json") {
                    session = this
                    runConnectionLoop()
                }
            } catch (e: Exception) {
                KizzyLogger.error("WebSocket connection failed", e)
                delay(5000)
                connect()
            }
        }
    }

    private suspend fun DefaultWebSocketSession.runConnectionLoop() {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                val json = Json.parseToJsonElement(text).jsonObject
                val op = json["op"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue

                when (op) {
                    10 -> handleHello(json)
                    0 -> handleDispatch(json)
                }
            }
        }
    }

    private suspend fun DefaultWebSocketSession.handleHello(hello: JsonObject) {
        val interval = hello["d"]?.jsonObject
            ?.get("heartbeat_interval")?.jsonPrimitive?.content?.toLongOrNull()
            ?: 41250L

        launch {
            while (isActive) {
                try {
                    outgoing.send(Frame.Text("{\"op\":1,\"d\":null}"))
                } catch (e: Exception) {
                    KizzyLogger.error("Heartbeat send failed", e)
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
        outgoing.send(Frame.Text(Json.encodeToString(JsonObject.serializer(), payload)))
    }

    private fun handleDispatch(json: JsonObject) {
        val eventType = json["t"]?.jsonPrimitive?.content ?: return
        if (eventType == "READY") {
            onReady()
        }
        onEvent(json)
    }

    fun disconnect() {
        job?.cancel()
        scope.launch {
            try {
                session?.close()
            } catch (_: Exception) {}
            client.close()
        }
    }
}
