package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixstop.mobile.domain.model.IrRemoteLayout
import com.pixstop.mobile.domain.model.RemoteKey
import com.pixstop.mobile.ui.theme.PixColors

/**
 * O controle de 24 teclas desenhado na tela (Fase 9.10, etapa A).
 *
 * A disposição é a do controle físico, tecla por tecla: quem o tem na mão
 * reconhece sem procurar. Tecla sem código no controle escolhido aparece
 * apagada — não é o controle que não a tem, é o mapeamento que ainda não
 * chegou nela.
 *
 * Apertar é mandar de verdade: a fita da geladeira acende do outro lado,
 * e é isso que faz a escolha de cor ser uma escolha e não um palpite.
 */
@Composable
fun IrRemote(
    available: List<String>,
    onPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** A tecla que está saindo agora, para o dedo ver que apertou. */
    busy: String? = null,
    /** A cor escolhida até aqui, destacada com um anel. */
    picked: String? = null,
) {
    Column(
        modifier = modifier
            .background(PixColors.Gray800, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IrRemoteLayout.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    RemoteButton(
                        key = key,
                        enabled = enabled && available.contains(key.slug),
                        busy = busy == key.slug,
                        picked = picked == key.slug,
                        onPress = { onPress(key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteButton(
    key: RemoteKey,
    enabled: Boolean,
    busy: Boolean,
    picked: Boolean,
    onPress: () -> Unit,
) {
    val face = key.swatch?.let { Color(it) } ?: PixColors.Gray600
    // Tecla clara pede letra escura; o resto é branco em cima de cor forte.
    val ink = if (key.slug == "white" || key.slug.startsWith("brightness")) PixColors.Dark else PixColors.White

    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .background(face, CircleShape)
            .border(
                width = if (picked || busy) 3.dp else 1.dp,
                color = if (picked || busy) PixColors.Cyan else PixColors.Gray500,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onPress),
        contentAlignment = Alignment.Center,
    ) {
        key.text?.let {
            Text(
                text = it,
                color = ink,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
