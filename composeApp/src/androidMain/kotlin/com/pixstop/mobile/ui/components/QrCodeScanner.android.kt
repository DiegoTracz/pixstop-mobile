package com.pixstop.mobile.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import java.util.concurrent.Executors

/**
 * Implementação Android do scanner QR Code usando CameraX + ML Kit.
 */
@Composable
actual fun QrCodeScannerScreen(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
    ) {
        if (hasCameraPermission) {
            CameraPreviewWithScanner(
                onCodeScanned = onCodeScanned
            )
        } else {
            // Mensagem de permissão negada
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CÂMERA NECESSÁRIA",
                    style = PixTypography.sectionTitle
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permita o acesso à câmera para escanear o QR Code.",
                    style = PixTypography.bodySecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                PixelButton(
                    text = "Permitir",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }

        // Top bar with close button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(PixColors.Dark.copy(alpha = 0.8f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PixColors.Gray700)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    icon = AppIconType.Close,
                    contentDescription = "Fechar",
                    tint = PixColors.Cyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "ESCANEAR QR CODE",
                style = PixTypography.sectionTitle
            )
        }

        // Bottom instruction
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(PixColors.Dark.copy(alpha = 0.8f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aponte a câmera para o QR Code da empresa",
                style = PixTypography.bodyMuted
            )
        }

        // Scanning frame overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .drawBehind {
                    val cornerLen = 30.dp.toPx()
                    val strokeW = 4.dp.toPx()
                    val cyanColor = PixColors.Cyan

                    // Top-left
                    drawRect(cyanColor, Offset.Zero, Size(cornerLen, strokeW))
                    drawRect(cyanColor, Offset.Zero, Size(strokeW, cornerLen))
                    // Top-right
                    drawRect(cyanColor, Offset(size.width - cornerLen, 0f), Size(cornerLen, strokeW))
                    drawRect(cyanColor, Offset(size.width - strokeW, 0f), Size(strokeW, cornerLen))
                    // Bottom-left
                    drawRect(cyanColor, Offset(0f, size.height - strokeW), Size(cornerLen, strokeW))
                    drawRect(cyanColor, Offset(0f, size.height - cornerLen), Size(strokeW, cornerLen))
                    // Bottom-right
                    drawRect(cyanColor, Offset(size.width - cornerLen, size.height - strokeW), Size(cornerLen, strokeW))
                    drawRect(cyanColor, Offset(size.width - strokeW, size.height - cornerLen), Size(strokeW, cornerLen))
                }
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithScanner(
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasScanned by remember { mutableStateOf(false) }

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = remember { BarcodeScanning.getClient(options) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !hasScanned) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { value ->
                                        if (!hasScanned) {
                                            hasScanned = true
                                            onCodeScanned(value)
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScanner", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}





