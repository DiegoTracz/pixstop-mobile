package com.pixstop.mobile.core.notification

import platform.UIKit.UIDevice

internal actual val devicePlatform: String = "ios"

internal actual fun deviceName(): String? = UIDevice.currentDevice.model
