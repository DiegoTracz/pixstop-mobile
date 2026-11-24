package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.pixstop.mobile.core.image.toByteArray
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

private const val TAG = "PixelCamera"

/** Onde a câmera do iPhone está, do ponto de vista da pessoa que abriu a tela. */
private enum class CameraAccess { Asking, Granted, Denied }

/**
 * A câmera do app no iPhone, com `AVCapturePhotoOutput`.
 *
 * Mesmo desenho do Android: tela cheia, moldura quadrada no avatar, virar a
 * câmera, um disparo. O que o iOS pede a mais:
 *
 * - **Orientação.** O app é retrato; a conexão da foto é posta em retrato e o
 *   EXIF que vem junto é aplicado depois pelo `downscaleJpeg` de quem envia.
 * - **Espelho na frontal.** A prévia da frontal é um espelho, e o avatar sai
 *   como a pessoa se viu ao enquadrar — senão a franja troca de lado.
 * - **Permissão negada não se pede de novo.** O iOS só pergunta uma vez; depois
 *   disso o único caminho é nos Ajustes, e a tela leva para lá.
 *
 * A sessão é mexida numa fila própria: `startRunning` bloqueia, e na principal
 * congelaria a tela no tempo de a câmera acordar.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PixelCameraScreen(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
    title: String,
    instruction: String,
    startWithFront: Boolean,
    squareFrame: Boolean,
) {
    var access by remember { mutableStateOf(currentCameraAccess()) }
    var useFront by remember { mutableStateOf(startWithFront) }
    var capturing by remember { mutableStateOf(false) }
    val camera = remember { CameraController() }

    LaunchedEffect(Unit) {
        if (access == CameraAccess.Asking) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) {
                    access = if (granted) CameraAccess.Granted else CameraAccess.Denied
                }
            }
        }
    }

    LaunchedEffect(access, useFront) {
        if (access == CameraAccess.Granted) camera.use(front = useFront)
    }

    DisposableEffect(Unit) {
        onDispose { camera.stop() }
    }

    Box(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        if (access == CameraAccess.Granted) {
            UIKitView(
                factory = {
                    UIView(frame = CGRectMake(0.0, 0.0, 400.0, 800.0)).also { view ->
                        view.layer.addSublayer(camera.previewLayer)
                        camera.previewLayer.frame = view.bounds
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> camera.previewLayer.frame = view.bounds },
            )

            // A moldura do recorte: quadrada, como o avatar vai ficar.
            if (squareFrame) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(280.dp)
                        .border(4.dp, PixColors.Cyan)
                )
            }
        } else if (access == CameraAccess.Denied) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "CÂMERA NECESSÁRIA", style = PixTypography.sectionTitle, color = PixColors.Cyan)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "O acesso à câmera está desligado. Ligue em Ajustes › Pixelstop › Câmera e volte aqui.",
                    style = PixTypography.bodyMuted,
                )
                Spacer(Modifier.height(24.dp))
                PixelButton(text = "Abrir os Ajustes", onClick = ::openAppSettings)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(PixColors.Dark.copy(alpha = 0.85f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(PixColors.Gray700).clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon = AppIconType.Close, contentDescription = "Fechar", tint = PixColors.Cyan, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.width(16.dp))

            Text(text = title, style = PixTypography.sectionTitle, color = PixColors.Cyan)
        }

        if (access == CameraAccess.Granted) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(PixColors.Dark.copy(alpha = 0.85f))
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = instruction, style = PixTypography.bodyMuted)

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(PixColors.Gray700).clickable(enabled = !capturing) { useFront = !useFront },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(icon = AppIconType.CameraSwitch, contentDescription = "Virar a câmera", tint = PixColors.Cyan, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(24.dp))

                    // O botão de disparo: grande e quadrado, como tudo aqui.
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(if (capturing) PixColors.Gray700 else PixColors.Cyan)
                            .clickable(enabled = !capturing) {
                                capturing = true
                                camera.capture(mirrored = useFront) { bytes ->
                                    if (bytes == null) {
                                        capturing = false
                                    } else {
                                        onPhoto(bytes)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            icon = AppIconType.Camera,
                            contentDescription = "Tirar a foto",
                            tint = if (capturing) PixColors.Gray500 else PixColors.Dark,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(Modifier.width(24.dp))

                    Spacer(Modifier.size(44.dp))
                }
            }
        }
    }
}

private fun currentCameraAccess(): CameraAccess = when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
    AVAuthorizationStatusAuthorized -> CameraAccess.Granted
    AVAuthorizationStatusNotDetermined -> CameraAccess.Asking
    else -> CameraAccess.Denied
}

private fun openAppSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}

/**
 * A sessão de captura e o que ela precisa manter vivo.
 *
 * O AVFoundation guarda o delegate da foto por referência fraca: sem segurá-lo
 * aqui, ele seria coletado entre o disparo e a foto pronta, e o retorno nunca
 * chegaria.
 */
@OptIn(ExperimentalForeignApi::class)
private class CameraController {

    private val session = AVCaptureSession().apply { sessionPreset = AVCaptureSessionPresetPhoto }
    private val photoOutput = AVCapturePhotoOutput()
    private val queue = dispatch_queue_create("com.pixstop.mobile.camera", null)
    private var input: AVCaptureInput? = null
    private var pending: PhotoDelegate? = null

    val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    /** Liga a câmera pedida, trocando a que estiver ligada. */
    fun use(front: Boolean) = dispatch_async(queue) {
        val device = cameraAt(front)

        if (device == null) {
            AppLogger.w("Sem câmera ${if (front) "frontal" else "traseira"} neste aparelho.", tag = TAG)
            return@dispatch_async
        }

        val newInput = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return@dispatch_async

        session.beginConfiguration()
        input?.let(session::removeInput)

        if (session.canAddInput(newInput)) {
            session.addInput(newInput)
            input = newInput
        }

        if (session.outputs.isEmpty() && session.canAddOutput(photoOutput)) {
            session.addOutput(photoOutput)
        }

        session.commitConfiguration()

        if (!session.isRunning()) session.startRunning()
    }

    /** Tira a foto; `onResult` recebe o JPEG na fila principal, ou nulo se falhou. */
    fun capture(mirrored: Boolean, onResult: (ByteArray?) -> Unit) {
        val delegate = PhotoDelegate { bytes ->
            pending = null
            dispatch_async(dispatch_get_main_queue()) { onResult(bytes) }
        }
        pending = delegate

        dispatch_async(queue) {
            val connection: AVCaptureConnection? = photoOutput.connectionWithMediaType(AVMediaTypeVideo)

            if (connection == null) {
                delegate.finish(null)
                return@dispatch_async
            }

            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = AVCaptureVideoOrientationPortrait
            }

            if (connection.isVideoMirroringSupported()) {
                connection.automaticallyAdjustsVideoMirroring = false
                connection.videoMirrored = mirrored
            }

            photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), delegate)
        }
    }

    fun stop() = dispatch_async(queue) {
        if (session.isRunning()) session.stopRunning()
    }

    private fun cameraAt(front: Boolean): AVCaptureDevice? =
        AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = if (front) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack,
        ).devices.firstOrNull() as? AVCaptureDevice
}

private class PhotoDelegate(
    private val onDone: (ByteArray?) -> Unit,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    private var finished = false

    override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
        if (error != null) {
            AppLogger.w("A foto falhou: ${error.localizedDescription}", tag = TAG)
        }

        finish(if (error == null) didFinishProcessingPhoto.fileDataRepresentation()?.toByteArray() else null)
    }

    fun finish(bytes: ByteArray?) {
        if (finished) return
        finished = true
        onDone(bytes)
    }
}
