package com.pixstop.mobile.ui.components

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
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async

/**
 * Holds references to AVFoundation objects so they aren't garbage-collected.
 */
private class QrScannerHolder {
    var captureSession: AVCaptureSession? = null
    var delegate: QrCodeDelegate? = null
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    fun stop() {
        captureSession?.stopRunning()
        captureSession = null
        delegate = null
        previewLayer = null
    }
}

/**
 * Delegate that receives AVFoundation metadata when a QR code is detected.
 */
private class QrCodeDelegate(
    private val onScanned: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        for (obj in didOutputMetadataObjects) {
            val readable = obj as? AVMetadataMachineReadableCodeObject ?: continue
            if (readable.type == AVMetadataObjectTypeQRCode) {
                readable.stringValue?.let { value ->
                    onScanned(value)
                }
            }
        }
    }
}

/**
 * Implementação iOS do scanner QR Code usando AVFoundation.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrCodeScannerScreen(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hasScanned by remember { mutableStateOf(false) }
    val holder = remember { QrScannerHolder() }

    DisposableEffect(Unit) {
        onDispose {
            holder.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
    ) {
        // Camera preview
        UIKitView(
            factory = {
                val containerView = UIView(frame = CGRectMake(0.0, 0.0, 400.0, 800.0))

                val captureSession = AVCaptureSession()
                captureSession.sessionPreset = AVCaptureSessionPresetHigh
                holder.captureSession = captureSession

                val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                if (device != null) {
                    val input = try {
                        AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                    } catch (_: Exception) { null }

                    if (input != null && captureSession.canAddInput(input)) {
                        captureSession.addInput(input)
                    }

                    val metadataOutput = AVCaptureMetadataOutput()
                    if (captureSession.canAddOutput(metadataOutput)) {
                        captureSession.addOutput(metadataOutput)

                        val delegate = QrCodeDelegate { rawValue ->
                            if (!hasScanned) {
                                hasScanned = true
                                onCodeScanned(rawValue)
                            }
                        }
                        holder.delegate = delegate

                        metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
                        metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
                    }

                    val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
                    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                    previewLayer.frame = containerView.bounds
                    containerView.layer.addSublayer(previewLayer)
                    holder.previewLayer = previewLayer

                    dispatch_async(dispatch_get_main_queue()) {
                        captureSession.startRunning()
                    }
                }

                containerView
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                holder.previewLayer?.frame = view.bounds
            }
        )

        // Top bar
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


