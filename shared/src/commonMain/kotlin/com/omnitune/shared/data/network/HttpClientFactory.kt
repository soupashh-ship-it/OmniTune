package com.omnitune.shared.data.network

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient

expect fun currentTimeMillis(): Long
