/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy.gateway.entities.op

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(OpCodeSerializer::class)
enum class OpCode(val value: Int) {
    DISPATCH(0),
    HEARTBEAT(1),
    IDENTIFY(2),
    PRESENCE_UPDATE(3),
    VOICE_STATE(4),
    RESUME(6),
    RECONNECT(7),
    REQUEST_GUILD_MEMBERS(8),
    INVALID_SESSION(9),
    HELLO(10),
    HEARTBEAT_ACK(11),
    UNKNOWN(-1),
}

object OpCodeSerializer : KSerializer<OpCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OpCode", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): OpCode =
        decoder.decodeInt().let { value ->
            OpCode.entries.firstOrNull { it.value == value } ?: OpCode.UNKNOWN
        }

    override fun serialize(encoder: Encoder, value: OpCode) =
        encoder.encodeInt(value.value)
}
