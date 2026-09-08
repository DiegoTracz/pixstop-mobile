package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * Ainda não no iOS: falta o `PHPickerViewController` e um aparelho para
 * provar que o recorte e a orientação saem certos.
 */
@Composable
actual fun rememberPhotoPicker(onPhoto: (ByteArray) -> Unit): () -> Unit = {}
