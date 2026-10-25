package com.pixstop.mobile.core.image

/**
 * No iOS a foto ainda vai como veio: o servidor aceita até 12 MB, e a câmera
 * do iOS no app é um marcador até a Fase do iOS. Quando ela existir, a
 * redução entra aqui com `UIImage`.
 */
actual fun downscaleJpeg(bytes: ByteArray, maxSide: Int, quality: Int): ByteArray = bytes
