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
 * @param instruction O que apontar. Por padrão é o QR da geladeira, que é onde a
 *   pessoa encontra o código; o balcão lê o QR da pessoa e diz isso.
 */
@Composable
expect fun QrCodeScannerScreen(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    instruction: String = "Aponte a câmera para o QR Code na geladeira",
)

