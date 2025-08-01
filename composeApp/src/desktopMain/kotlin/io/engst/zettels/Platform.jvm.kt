package io.engst.zettels

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class JVMPlatform : Platform {

   override val name: String = "Java ${System.getProperty("java.version")}"

   override fun httpClient(): HttpClient = HttpClient(CIO) {
      install(ContentNegotiation) {
         json(httpJson)
      }
   }
}

actual fun getPlatform(): Platform = JVMPlatform()
