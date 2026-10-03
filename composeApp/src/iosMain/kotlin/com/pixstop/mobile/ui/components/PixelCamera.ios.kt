package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Ainda não no iOS.
 *
 * A captura de foto pede `AVCapturePhotoOutput` e um aparelho para provar que
 * a orientação, o espelhamento da câmera frontal e a permissão funcionam.
 * Escrever isso sem aparelho seria entregar código que só parece pronto — a
 * tela diz a verdade até lá.
 *
 * O caminho que ela indica existe: desde 22/09 o `PhotoPicker.ios.kt` abre o
 * `PHPickerViewController` de verdade, e o retrato 8-bit sai da foto da
 * galeria igual ao do Android.
 */
@Composable
actual fun PixelCameraScreen(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(PixColors.Dark).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "CÂMERA PIXEL ART", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

        Text(
            text = "Ainda disponível só no Android. No iPhone, escolha a foto pela galeria.",
            style = PixTypography.bodyMuted,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

        PixelButton(text = "Voltar", onClick = onDismiss)
    }
}
