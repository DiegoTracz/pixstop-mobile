package com.pixstop.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.DoorSessionSummary
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * As últimas aberturas da porta, numa gaveta (Fase 9.10, etapa C).
 *
 * Toda abertura vira registro, com ou sem compra. Quem está de pé na frente
 * da geladeira quer uma resposta curta — a última foi normal? —, e é isso que
 * cada linha diz: quando, por quanto tempo, se houve compra e o que a análise
 * por imagem achou.
 *
 * A que merece uma olhada humana vem em amarelo: é a única que pede ação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsSheet(
    sessions: List<DoorSessionSummary>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixColors.Darker,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Últimas aberturas", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(
                "Toda vez que a porta abre fica registrado, com ou sem compra, e a câmera grava.",
                style = PixTypography.caption,
                color = PixColors.Gray300,
            )

            when {
                isLoading && sessions.isEmpty() ->
                    Text("Buscando…", style = PixTypography.bodyMuted, color = PixColors.Gray300)

                sessions.isEmpty() ->
                    Text(
                        "Esta geladeira ainda não foi aberta. A primeira abertura aparece aqui em seguida.",
                        style = PixTypography.bodyMuted,
                        color = PixColors.Gray300,
                    )

                else -> sessions.forEach { session ->
                    SensorCard(
                        icon = if (session.isOpen) AppIconType.LockOpen else AppIconType.Camera,
                        label = session.openedAt.take(16).replace("T", " às "),
                        value = session.durationLabel,
                        // O amarelo é para o que pede olho humano; o resto é
                        // histórico, e histórico não precisa gritar.
                        accent = when {
                            session.isFlag -> PixColors.Yellow
                            session.hasOrder -> PixColors.Green
                            else -> PixColors.Gray500
                        },
                        detail = session.observations ?: session.summary,
                    )
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
