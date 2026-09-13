package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * O corpo do controle e a borda das teclas não vêm do tema: o controle é um
 * objeto na mesa, e é o mesmo nos dois modos. Puxá-los do tema fazia o corpo
 * virar branco no modo claro, e aí a tecla branca e as de brilho sumiam.
 */
private val RemoteBody = Color(0xFF2C313B)
private val RemoteEdge = Color(0xFF6B7280)
private val PixelShadow = Color(0xFF1A1D24)

/**
 * O controle de 24 teclas desenhado na tela (Fase 9.10, etapa A).
 *
 * A disposição é a do controle físico, tecla por tecla: quem o tem na mão
 * reconhece sem procurar. O acabamento é o do resto do Pixelstop — borda reta,
 * sombra sólida sem desfoque, e a tecla **afunda** quando apertada, que é o
 * que dá peso de teclado 8-bit a um controle desenhado.
 *
 * Tecla sem código no controle escolhido aparece apagada: não é o controle
 * que não a tem, é o mapeamento que ainda não chegou nela.
 *
 * Apertar é mandar de verdade: a fita da geladeira acende do outro lado, e é
 * isso que faz a escolha de cor ser uma escolha e não um palpite.
 */
@Composable
fun IrRemote(
    available: List<String>,
    onPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** A tecla que está saindo agora, para o dedo ver que apertou. */
    busy: String? = null,
    /** A cor escolhida até aqui, destacada pela borda. */
    picked: String? = null,
    /** A última tecla que saiu, mesmo que não sirva de repouso. */
    lastSent: String? = null,
) {
    Box(modifier = modifier.pixelShadow(color = PixelShadow, offsetX = 6.dp, offsetY = 6.dp)) {
        Column(
            modifier = Modifier
                .background(RemoteBody)
                .border(2.dp, RemoteEdge)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IrRemoteLayout.rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        RemoteButton(
                            key = key,
                            enabled = enabled && available.contains(key.slug),
                            // Uma tecla que não serve de repouso também precisa
                            // dizer que saiu: sem isso, apertar FLASH parece um
                            // botão quebrado.
                            down = busy == key.slug || (lastSent == key.slug && picked != key.slug),
                            picked = picked == key.slug,
                            onPress = { onPress(key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteButton(
    key: RemoteKey,
    enabled: Boolean,
    down: Boolean,
    picked: Boolean,
    onPress: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Apertada, a tecla desce até o lugar da sombra e a sombra some — é o
    // mesmo movimento do botão pixel do resto do aplicativo.
    val sunk = pressed || down
    val shadow = if (sunk || !enabled) 0.dp else 3.dp
    val shift = if (sunk && enabled) 3.dp else 0.dp

    // As teclas de programa são cinza-claro no controle de verdade; as de
    // cor têm a cor delas.
    val face = key.swatch?.let { Color(it) } ?: Color(0xFFAEB4BE)
    val light = key.slug == "white" || key.slug.startsWith("brightness") || key.swatch == null
    val ink = if (light) Color(0xFF111827) else Color(0xFFFFFFFF)

    // A fonte pixel é larga: "SMOOTH" só cabe bem pequeno.
    val size = if ((key.text?.length ?: 0) > 3) 5.sp else 10.sp

    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .pixelShadow(color = PixelShadow, offsetX = shadow, offsetY = shadow),
    ) {
        Box(
            modifier = Modifier
                .offset(x = shift, y = shift)
                .size(48.dp)
                .background(face)
                // A cor escolhida fica com a borda em ciano, que é a cor de
                // seleção do Pixelstop inteiro.
                .border(2.dp, if (picked) PixColors.Cyan else Color(0x66000000))
                .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onPress),
            contentAlignment = Alignment.Center,
        ) {
            key.text?.let {
                Text(
                    text = it,
                    color = ink,
                    fontSize = size,
                    fontFamily = PixTypography.pixelFontFamily,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
