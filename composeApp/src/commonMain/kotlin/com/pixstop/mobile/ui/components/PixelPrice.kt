package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Preço em reais e em pixels, lado a lado.
 *
 * Os dois aparecem sempre porque a compra pode misturar as duas moedas — quem
 * só vir um dos números não consegue decidir.
 */
@Composable
fun PixelPrice(
    priceMoney: Double,
    priceMoneyOriginal: Double,
    pricePixels: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMoney(priceMoney),
                style = PixTypography.sectionTitle,
                color = PixColors.Green,
            )

            // O preço cheio só aparece quando há desconto, riscado ao lado.
            if (priceMoneyOriginal > priceMoney) {
                Text(
                    text = formatMoney(priceMoneyOriginal),
                    style = PixTypography.caption,
                    color = PixColors.Gray500,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelCoin(size = 12.dp)

            Text(text = pricePixels.toString(), style = PixTypography.caption, color = PixColors.Yellow)
        }
    }
}

/**
 * Selo de desconto, no canto do produto.
 */
@Composable
fun DiscountTag(percentage: Int, modifier: Modifier = Modifier) {
    Text(
        text = "-$percentage%",
        style = PixTypography.badgeText,
        color = PixColors.Dark,
        modifier = modifier.background(PixColors.Pink).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * Reais no formato brasileiro, sem depender de biblioteca de localização.
 */
fun formatMoney(value: Double): String {
    val cents = (value * 100).toLong()
    val reais = cents / 100
    val rest = (cents % 100).toString().padStart(2, '0')

    return "R$ $reais,$rest"
}
