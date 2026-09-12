package com.pixstop.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * A câmera da geladeira, numa gaveta (Fase 9.10, etapa D).
 *
 * É a foto de agora, não o vídeo: ela responde por inteiro a pergunta da
 * instalação — "a câmera está apontada para o lugar certo?" — e chega a cada
 * dois segundos enquanto a gaveta estiver aberta.
 *
 * Fechada a gaveta, ninguém renova a janela e a geladeira para de transmitir
 * sozinha: ela não pode gastar o uplink da empresa o dia todo para ninguém.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSheet(
    /** O último quadro em bytes, buscado pelo cliente autenticado. */
    frame: ByteArray?,
    onDismiss: () -> Unit,
) {
    // Uma foto corrompida não pode derrubar a tela: sem bitmap, fica a espera.
    val bitmap = frame?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixColors.Darker,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("A câmera agora", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(
                "A imagem chega a cada dois segundos enquanto esta tela estiver aberta. " +
                    "Fechando, a geladeira para de transmitir.",
                style = PixTypography.caption,
                color = PixColors.Gray300,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(PixColors.Dark)
                    .border(2.dp, PixColors.Cyan),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "O que a câmera da geladeira está vendo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // O primeiro quadro leva alguns segundos: a geladeira
                    // precisa receber o pedido, tirar a foto e subi-la.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(color = PixColors.Cyan)
                        Text("Pedindo a primeira foto…", style = PixTypography.bodyMuted, color = PixColors.Gray300)
                    }
                }
            }

            PixelButton(
                text = "Fechar",
                onClick = onDismiss,
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
