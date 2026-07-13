/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy.gateway.entities.presence

import com.omnitune.kizzy.gateway.entities.op.OpCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Presence(
    val activities: List<Activity>? = null,
    val afk: Boolean = false,
    val since: Long = 0,
    val status: String = "online",
)

@Serializable
data class PresenceOp(
    val op: OpCode = OpCode.PRESENCE_UPDATE,
    val d: Presence? = null,
)
