package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Uma leitura da geladeira, com a cara do sistema (Fase 9.10).
 *
 * Cada carta é um mostrador: o ícone à esquerda num quadrado da cor do
 * estado, o nome pequeno em cima e o valor grande embaixo. A cor não é
 * enfeite — ela é a própria informação, e é o que faz quem passa os olhos
 * ver "amarelo" e saber que tem porta aberta antes de ler a palavra.
 *
 * A carta que leva a algum lugar mostra a seta; a que só informa, não.
 */
@Composable
fun SensorCard(
    icon: AppIconType,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    /** A cor do estado: verde é bom, amarelo pede atenção, vermelho é problema. */
    accent: Color = PixColors.Cyan,
    /** A linha miúda embaixo: o detalhe que explica o valor. */
    detail: String? = null,
    /** Uma amostra de cor no lugar do ícone, para a fita LED. */
    swatch: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier.pixelShadow(color = accent, offsetX = 4.dp, offsetY = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixColors.Darker)
                .border(2.dp, accent)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // O quadrado do ícone repete a cor do estado: é o que se enxerga
            // de longe, antes de qualquer palavra.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(swatch ?: accent.copy(alpha = 0.15f))
                    .border(2.dp, accent),
                contentAlignment = Alignment.Center,
            ) {
                if (swatch == null) {
                    AppIcon(icon = icon, contentDescription = null, tint = accent)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label.uppercase(), style = PixTypography.inputLabel, color = PixColors.Gray400)
                Text(value, style = PixTypography.sectionTitle, color = accent)

                detail?.let {
                    Text(it, style = PixTypography.caption, color = PixColors.Gray300)
                }
            }

            if (onClick != null) {
                AppIcon(icon = AppIconType.ChevronDown, contentDescription = null, tint = PixColors.Gray400)
            }
        }
    }
}
