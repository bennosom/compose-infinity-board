package io.engst.zettels

import io.ktor.client.HttpClient
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
   override val name: String =
      UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

   override fun httpClient(): HttpClient = HttpClient(Darwin) {
      install(ContentNegotiation) {
         json(httpJson)
      }
   }
}

actual fun getPlatform(): Platform = IOSPlatform()
