package com.pixstop.mobile.core.image

/**
 * Reduz um JPEG para caber em `maxSide` no lado maior, já em pé.
 *
 * A foto da câmera sai com 12 megapixels e a rotação só anotada no EXIF; a
 * nota de compra lida inteira pelo servidor tinha 900×1600. Mandar a foto
 * cheia pela rede do estacionamento do atacado é esperar à toa
 * (docs/plans/COMPRAS_MOBILE.md, passo 2). Foto que já cabe, ou que não dá
 * para decodificar, volta como veio.
 */
expect fun downscaleJpeg(bytes: ByteArray, maxSide: Int = 1600, quality: Int = 85): ByteArray
