package com.pixstop.mobile.core.notification

import android.os.Build

internal actual val devicePlatform: String = "android"

internal actual fun deviceName(): String? = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { null }
