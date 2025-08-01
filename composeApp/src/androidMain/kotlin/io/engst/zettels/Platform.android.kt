package io.engst.zettels

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AndroidPlatform : Platform {
   override val name: String = "Android ${Build.VERSION.SDK_INT}"

   override fun httpClient(): HttpClient = HttpClient(OkHttp) {
      install(ContentNegotiation) {
         json(httpJson)
      }
   }
}

actual fun getPlatform(): Platform = AndroidPlatform()
