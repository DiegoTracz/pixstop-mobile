package com.pixstop.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.MascotFace
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Tela vazia, ou a mesma tela quando o carregamento falhou.
 *
 * Vazio é o mascote no azul de marca, com o rosto do contexto: vazio não é
 * erro. Falha é o mascote vermelho assustado, e a ação costuma ser tentar de
 * novo. As telas do app mostram as duas coisas no mesmo lugar, por isso as
 * duas moram aqui. Mesmo desenho do `PixelEmptyState.vue`.
 */
@Composable
fun PixelEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    face: MascotFace = MascotFace.Normal,
    size: Dp = 64.dp,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isError) {
            PixelMascot(state = MascotState.Error, size = size, decorative = true)
        } else {
            PixelMascot(state = MascotState.Idle, face = face, size = size, decorative = true)
        }

        Text(
            text = message,
            style = if (isError) PixTypography.errorText else PixTypography.bodyMuted,
            textAlign = TextAlign.Center,
        )

        action?.invoke()
    }
}
