package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.pixstop.mobile.core.logging.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private const val TAG = "PhotoPicker"

/** O tipo uniforme que cobre qualquer imagem — JPEG, PNG, HEIC. */
private const val IMAGE_UTI = "public.image"

/**
 * O seletor de fotos do sistema.
 *
 * O `PHPickerViewController` roda fora do processo do app: a pessoa navega na
 * fototeca inteira, e só a foto escolhida atravessa. Por isso ele **não pede
 * permissão** nem exige `NSPhotoLibraryUsageDescription` — o app nunca recebe
 * acesso à biblioteca, recebe um arquivo.
 */
@Composable
actual fun rememberPhotoPicker(onPhoto: (ByteArray) -> Unit): () -> Unit {
    // O UIKit guarda o delegate por referência fraca; sem segurá-lo aqui ele
    // seria coletado entre abrir o seletor e a pessoa escolher a foto, e o
    // retorno nunca chegaria.
    val delegate = remember { PhotoPickerDelegate() }

    DisposableEffect(delegate, onPhoto) {
        delegate.onPhoto = onPhoto
        onDispose { delegate.onPhoto = null }
    }

    return remember(delegate) {
        {
            val presenter = topViewController()

            if (presenter == null) {
                AppLogger.w("Sem tela para apresentar o seletor de fotos.", tag = TAG)
            } else {
                val configuration = PHPickerConfiguration().apply {
                    setFilter(PHPickerFilter.imagesFilter)
                    setSelectionLimit(1)
                }

                val picker = PHPickerViewController(configuration).apply {
                    setDelegate(delegate)
                }

                presenter.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class PhotoPickerDelegate : NSObject(), PHPickerViewControllerDelegateProtocol {

    var onPhoto: ((ByteArray) -> Unit)? = null

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        // Fechar é responsabilidade de quem apresentou, e vale também para a
        // desistência: sem isto o seletor fica na tela.
        picker.dismissViewControllerAnimated(true, completion = null)

        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
            ?: return

        if (!provider.hasItemConformingToTypeIdentifier(IMAGE_UTI)) {
            AppLogger.w("O item escolhido não é uma imagem.", tag = TAG)
            return
        }

        provider.loadDataRepresentationForTypeIdentifier(IMAGE_UTI) { data: NSData?, error: NSError? ->
            val bytes = data?.toByteArray()

            // A carga termina numa fila de fundo; o estado do Compose só pode
            // ser tocado na principal.
            dispatch_async(dispatch_get_main_queue()) {
                if (bytes != null && bytes.isNotEmpty()) {
                    onPhoto?.invoke(bytes)
                } else {
                    AppLogger.w("Não deu para ler a foto: ${error?.localizedDescription}", tag = TAG)
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    return ByteArray(size).also { target ->
        target.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}

/**
 * O controlador que está na frente.
 *
 * Apresentar sobre a raiz falharia enquanto outra folha estiver aberta — e é
 * exatamente o caso aqui: o avatar é escolhido de dentro de uma.
 */
private fun topViewController(): UIViewController? {
    val windows = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>()
    val window = windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull()

    var controller = window?.rootViewController

    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }

    return controller
}
