package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * A câmera do avatar em pixel art.
 *
 * É a mesma ideia do scanner de QR — tela cheia, moldura, um alvo — mas o que
 * se mira aqui é o próprio rosto, e o que sai é um JPEG para o servidor
 * transformar em retrato 8-bit.
 *
 * @param onPhoto o JPEG capturado
 * @param onDismiss fechar sem tirar nada
 */
@Composable
expect fun PixelCameraScreen(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
)
