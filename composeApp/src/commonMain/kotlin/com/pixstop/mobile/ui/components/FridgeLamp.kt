package com.pixstop.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.FridgeLight
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * A luz da geladeira, na tela (Fase 9.6).
 *
 * A fita LED dentro da geladeira diz o estado por cor, a três metros de
 * distância. Repetir a mesma cor aqui faz o celular e a geladeira falarem a
 * mesma língua: quem lê "sem internet" no telefone levanta os olhos e vê a
 * geladeira vermelha, e entende sem ninguém explicar.
 *
 * A cor é a que a geladeira está mostrando, não uma decoração: quando o app
 * não tem como saber, a lâmpada fica apagada em vez de inventar.
 */
@Composable
fun FridgeLamp(
    light: FridgeLight,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    // A troca de cor acompanha a fita, que também não pisca de um quadro para
    // o outro: um corte seco aqui pareceria defeito da tela.
    val color by animateColorAsState(targetValue = light.toPixColor(), animationSpec = tween(400))

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // O quadrado é a fita: acesa tem cor, apagada tem só a moldura.
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (light == FridgeLight.Off) PixColors.Gray400 else color)
                .background(if (light == FridgeLight.Off) Color.Transparent else color),
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = PixTypography.inputLabel, color = if (light == FridgeLight.Off) PixColors.Gray400 else color)

            hint?.let {
                Text(text = it, style = PixTypography.bodyMuted)
            }
        }
    }
}

/**
 * A cor da fita na paleta do aplicativo.
 *
 * São as cores do app, não as do LED: a fita é luz sobre o escuro da
 * geladeira, e a tela é pigmento sobre o escuro do tema. O que precisa ser
 * igual é o *significado* — vermelho é sem rede nos dois lugares.
 */
/** A cor desta luz, para quem precisa dela fora da lâmpada — uma amostra num card, por exemplo. */
@Composable
fun FridgeLight.toSwatch(): Color = toPixColor()

@Composable
private fun FridgeLight.toPixColor(): Color = when (this) {
    FridgeLight.Red -> PixColors.Pink
    FridgeLight.Green -> PixColors.Green
    FridgeLight.Cyan -> PixColors.Cyan
    FridgeLight.Blue -> PixColors.Blue
    FridgeLight.Yellow -> PixColors.Yellow
    FridgeLight.Magenta -> PixColors.Purple
    // Os arco-íris mudam de cor sozinhos; aqui vale a cor que os representa.
    FridgeLight.Fade -> PixColors.Purple
    FridgeLight.Smooth -> PixColors.Pink
    FridgeLight.White -> PixColors.White
    FridgeLight.Off -> PixColors.Gray400
}
