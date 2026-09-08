package com.pixstop.mobile.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import java.util.concurrent.Executors

/**
 * A câmera do avatar, no Android, com CameraX.
 *
 * Nasce na frontal porque o que se vai fotografar é o próprio rosto, e a
 * moldura quadrada existe para a pessoa saber o que vai virar avatar: o
 * recorte final é quadrado, e um retrato enquadrado na horizontal perderia a
 * testa ou o queixo.
 */
@Composable
actual fun PixelCameraScreen(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var useFront by remember { mutableStateOf(true) }
    var capturing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        if (hasPermission) {
            CameraPreview(useFront = useFront, imageCapture = imageCapture)

            // A moldura do recorte: quadrada, como o avatar vai ficar.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(280.dp)
                    .border(4.dp, PixColors.Cyan)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "CÂMERA NECESSÁRIA", style = PixTypography.sectionTitle, color = PixColors.Cyan)
                Spacer(Modifier.height(12.dp))
                Text(text = "Permita o acesso à câmera para fotografar o seu avatar.", style = PixTypography.bodyMuted)
                Spacer(Modifier.height(24.dp))
                PixelButton(text = "Permitir", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
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

            Text(text = "CÂMERA PIXEL ART", style = PixTypography.sectionTitle, color = PixColors.Cyan)
        }

        if (hasPermission) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(PixColors.Dark.copy(alpha = 0.85f))
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Rosto dentro do quadrado, luz de frente.",
                    style = PixTypography.bodyMuted,
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(PixColors.Gray700).clickable { useFront = !useFront },
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
                                imageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bytes = image.toJpegBytes()
                                            image.close()
                                            onPhoto(bytes)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            capturing = false
                                        }
                                    },
                                )
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

@Composable
private fun CameraPreview(useFront: Boolean, imageCapture: ImageCapture) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
        update = { view ->
            val providerFuture = ProcessCameraProvider.getInstance(context)

            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                }
            }, ContextCompat.getMainExecutor(context))
        },
    )
}

/** O JPEG que a CameraX já entrega pronto no primeiro plano do buffer. */
private fun ImageProxy.toJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    return bytes
}
