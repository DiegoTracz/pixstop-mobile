package com.pixstop.mobile.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual fun downscaleJpeg(bytes: ByteArray, maxSide: Int, quality: Int): ByteArray = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    val rotation = rotationOf(bytes)

    if (longest <= 0 || (longest <= maxSide && rotation == 0)) {
        return@runCatching bytes
    }

    // Decodifica já reduzido em potência de dois (barato) e acerta o resto
    // com a matriz, junto com a rotação do EXIF.
    var sample = 1
    while (longest / (sample * 2) >= maxSide) sample *= 2

    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return@runCatching bytes

    val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation.toFloat())
    }
    val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

    ByteArrayOutputStream().use { out ->
        result.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (result !== decoded) result.recycle()
        decoded.recycle()
        out.toByteArray()
    }
}.getOrDefault(bytes)

private fun rotationOf(bytes: ByteArray): Int = runCatching {
    when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}.getOrDefault(0)
