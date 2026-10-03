package com.pixstop.mobile.core.notification

/** `android` ou `ios`: o servidor só aceita esses dois. */
internal expect val devicePlatform: String

/** O nome do aparelho, para o servidor saber qual dos aparelhos de uma pessoa é qual. */
internal expect fun deviceName(): String?
