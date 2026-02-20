package com.pixstop.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image as SkiaImage
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImageSymbolConfiguration
import platform.posix.memcpy

/**
 * Implementação iOS usando SF Symbols nativos da Apple,
 * renderizados como ImageBitmap puro do Compose.
 *
 * Converte UIImage (SF Symbol) → PNG data → Skia Image → ImageBitmap,
 * mantendo tudo na camada Compose. Isso garante:
 * - Transparência correta sobre qualquer fundo (botões, FABs, etc.)
 * - Cliques não são interceptados (sem UIKitView)
 * - Tinting via ColorFilter do Compose
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppIcon(
    icon: AppIconType,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color?
) {
    val sfSymbolName = remember(icon) { icon.toSFSymbolName() }
    val effectiveTint = tint ?: LocalContentColor.current

    val imageBitmap = remember(sfSymbolName) {
        sfSymbolToImageBitmap(sfSymbolName)
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier.size(24.dp),
            colorFilter = ColorFilter.tint(effectiveTint)
        )
    }
}

/**
 * Converte um SF Symbol para ImageBitmap do Compose.
 *
 * Renderiza o símbolo como imagem preta sobre fundo transparente,
 * depois o tinting é aplicado via ColorFilter no Compose.
 */
@OptIn(ExperimentalForeignApi::class)
private fun sfSymbolToImageBitmap(symbolName: String): ImageBitmap? {
    val config = UIImageSymbolConfiguration.configurationWithPointSize(
        pointSize = 48.0
    )
    val symbol = UIImage.systemImageNamed(symbolName, config)
        ?: UIImage.systemImageNamed("questionmark.circle", config)
        ?: return null

    // Renderiza o SF Symbol com cor preta sobre fundo transparente
    val renderSize = 96.0
    val size = CGSizeMake(renderSize, renderSize)

    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)

    UIColor.blackColor.setFill()
    symbol.drawInRect(CGRectMake(0.0, 0.0, renderSize, renderSize))

    val renderedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (renderedImage == null) return null

    val pngData = UIImagePNGRepresentation(renderedImage) ?: return null

    val length = pngData.length.toInt()
    if (length == 0) return null

    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
    }

    return try {
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

/**
 * Mapeia AppIconType para nomes de SF Symbols da Apple.
 * Referência: https://developer.apple.com/sf-symbols/
 */
private fun AppIconType.toSFSymbolName(): String = when (this) {
    // Navigation
    AppIconType.Menu -> "line.3.horizontal"
    AppIconType.Home -> "house.fill"
    AppIconType.Search -> "magnifyingglass"
    AppIconType.Notifications -> "bell.fill"
    AppIconType.Settings -> "gearshape.fill"

    // User
    AppIconType.Person -> "person.fill"
    AppIconType.Logout -> "rectangle.portrait.and.arrow.right"

    // Auth
    AppIconType.Lock -> "lock.fill"
    AppIconType.Visibility -> "eye.fill"
    AppIconType.VisibilityOff -> "eye.slash.fill"

    // Actions
    AppIconType.Refresh -> "arrow.clockwise"
    AppIconType.Add -> "plus"
    AppIconType.Edit -> "pencil"
    AppIconType.Delete -> "trash.fill"
    AppIconType.Share -> "square.and.arrow.up"

    // Status
    AppIconType.Check -> "checkmark"
    AppIconType.Close -> "xmark"
    AppIconType.Info -> "info.circle.fill"
    AppIconType.Warning -> "exclamationmark.triangle.fill"
}
