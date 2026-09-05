package com.pixstop.mobile.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlin.io.encoding.Base64

/**
 * Decodifica uma imagem que veio como base64 na resposta da API.
 *
 * É o caso do QR do PIX, que o gateway devolve embutido no pedido em vez de
 * uma URL. Devolve `null` em vez de estourar: um QR corrompido não pode
 * derrubar a tela de um pedido que já foi criado — a pessoa ainda tem o código
 * copia e cola para pagar.
 */
fun decodeBase64Image(value: String?): ImageBitmap? {
    if (value.isNullOrBlank()) {
        return null
    }

    // Alguns gateways mandam o prefixo `data:image/png;base64,`; outros só o
    // conteúdo.
    val payload = value.substringAfterLast("base64,").trim()

    return runCatching { Base64.decode(payload).decodeToImageBitmap() }.getOrNull()
}
