package com.pixstop.mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform