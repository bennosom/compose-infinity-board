package io.engst.zettels

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform