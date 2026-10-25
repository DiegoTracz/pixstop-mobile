package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * No iOS, por enquanto, o texto vai para a área de transferência; a folha de
 * compartilhamento (`UIActivityViewController`) entra junto com a câmera.
 */
@Composable
actual fun rememberShareText(): (String) -> Boolean {
    val clipboard = LocalClipboardManager.current

    return remember(clipboard) {
        { text ->
            clipboard.setText(AnnotatedString(text))
            false
        }
    }
}
