package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * A câmera do app: tela cheia, um alvo, um disparo.
 *
 * Nasceu para o avatar em pixel art (frontal, moldura quadrada do recorte) e
 * serve também à nota de compra (traseira, sem moldura, porque o cupom é
 * comprido e precisa caber inteiro). O padrão de cada parâmetro é o do avatar.
 *
 * @param onPhoto o JPEG capturado
 * @param onDismiss fechar sem tirar nada
 * @param title o nome da tela, no topo
 * @param instruction a frase de enquadramento, acima do disparo
 * @param startWithFront começar pela câmera frontal (o rosto) ou pela traseira
 * @param squareFrame desenhar a moldura quadrada do recorte do avatar
 */
@Composable
expect fun PixelCameraScreen(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
    title: String = "CÂMERA PIXEL ART",
    instruction: String = "Rosto dentro do quadrado, luz de frente.",
    startWithFront: Boolean = true,
    squareFrame: Boolean = true,
)
