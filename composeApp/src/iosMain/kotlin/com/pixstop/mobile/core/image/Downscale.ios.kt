package com.pixstop.mobile.core.image

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.posix.memcpy

/**
 * A mesma redução do Android, com `UIImage`.
 *
 * O `UIImage` já lê a orientação do EXIF, e desenhar num renderizador aplica
 * essa rotação aos pixels: o JPEG que sai fica em pé sem depender de quem o
 * mostra. A escala do renderizador é 1 para `maxSide` valer em pixels, não em
 * pontos — numa tela 3x a foto sairia com o triplo do tamanho.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun downscaleJpeg(bytes: ByteArray, maxSide: Int, quality: Int): ByteArray = runCatching {
    if (bytes.isEmpty()) return@runCatching bytes

    val image = UIImage.imageWithData(bytes.toNSData()) ?: return@runCatching bytes

    val (width, height) = image.size.useContents { width to height }
    val longest = maxOf(width, height)
    val upright = image.imageOrientation == UIImageOrientation.UIImageOrientationUp

    if (longest <= 0.0 || (longest <= maxSide && upright)) {
        return@runCatching bytes
    }

    val scale = minOf(1.0, maxSide / longest)
    val targetWidth = (width * scale).toInt().toDouble()
    val targetHeight = (height * scale).toInt().toDouble()

    val format = UIGraphicsImageRendererFormat.preferredFormat().apply {
        setScale(1.0)
        setOpaque(true)
    }

    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
    val resized = renderer.imageWithActions { _ ->
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    }

    UIImageJPEGRepresentation(resized, quality / 100.0)?.toByteArray() ?: bytes
}.getOrDefault(bytes)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    return ByteArray(size).also { target ->
        target.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}
