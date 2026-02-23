package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * Scanner de QR Code multiplataforma.
 *
 * Android: CameraX + ML Kit Barcode Scanning
 * iOS: AVFoundation + AVCaptureMetadataOutput
 *
 * @param onCodeScanned Callback com o valor bruto do QR code escaneado
 * @param onDismiss Callback para fechar o scanner
 */
@Composable
expect fun QrCodeScannerScreen(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
)

