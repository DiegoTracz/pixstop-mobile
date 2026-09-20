package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Menos, número, mais — com alvos de toque de 48dp.
 *
 * Existe porque, de pé na frente de uma prateleira com uma mão na porta,
 * apertar "+" cinco vezes é mais rápido que digitar; com trinta latas é o
 * contrário, e por isso o número também abre o teclado ao ser tocado.
 *
 * `value` nulo é "ainda não contado", e aparece como um traço: zero é uma
 * afirmação ("olhei, não tem"), e não pode ser o estado de partida.
 */
@Composable
fun QuantityStepper(
    value: Int?,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int? = null,
    enabled: Boolean = true,
    onNumberClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = AppIconType.Remove,
            description = "Menos um",
            enabled = enabled && value != null && value > min,
            onClick = { onChange((value ?: min) - 1) },
        )

        Text(
            text = value?.toString() ?: "—",
            style = PixTypography.sectionTitle,
            color = if (value == null) PixColors.Gray500 else PixColors.Cyan,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 40.dp)
                .then(if (onNumberClick != null && enabled) Modifier.clickable(onClick = onNumberClick) else Modifier),
        )

        StepperButton(
            icon = AppIconType.Add,
            description = "Mais um",
            enabled = enabled && (max == null || (value ?: min) < max),
            onClick = { onChange((value ?: min - 1) + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: AppIconType,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = if (enabled) PixColors.Cyan else PixColors.Gray600

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(PixColors.Darker)
            .border(2.dp, color)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon = icon, contentDescription = description, tint = color)
    }
}
