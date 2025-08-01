package io.engst.zettels

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

interface Platform {
    val name: String

    fun httpClient(): HttpClient
}

val httpJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = true
}

expect fun getPlatform(): Platform
