package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.AccountUser
import com.pixstop.mobile.domain.model.ActiveCompany
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Painel do jogador: quem é, em que empresa está e quantos pixels tem.
 *
 * É a versão do `PlayerHUD` da web — retrato à direita, nome, empresa em
 * amarelo e o saldo numa caixa com a moeda. Mantém as mesmas cores e a mesma
 * ordem para quem usa os dois reconhecer na hora.
 */
@Composable
fun PlayerHud(
    user: AccountUser,
    company: ActiveCompany?,
    modifier: Modifier = Modifier,
    showEmail: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(name = user.name, size = 56.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = user.firstName.uppercase(),
                style = PixTypography.sectionTitle,
                color = PixColors.Gray100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (showEmail) {
                Text(
                    text = user.email,
                    style = PixTypography.caption,
                    color = PixColors.Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = company?.name?.uppercase() ?: "SEM EMPRESA",
                style = PixTypography.caption,
                color = PixColors.Yellow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // O selo de nível: status que se carrega, não só se consulta.
            company?.progression?.let { progression ->
                Text(
                    text = "NV. ${progression.level} · ${progression.displayTitle.uppercase()}",
                    style = PixTypography.caption,
                    color = PixColors.Cyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            PixelBalance(pixels = company?.pixelAvailable ?: 0)
        }
    }
}

/**
 * Saldo de pixels na caixa de borda amarela, com a moeda ao lado.
 */
@Composable
fun PixelBalance(pixels: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(2.dp, PixColors.YellowDim)
            .background(PixColors.Dark)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelCoin(size = 14.dp)

        Text(
            text = pixels.toString(),
            style = PixTypography.badgeText,
            color = PixColors.Yellow,
        )
    }
}

/**
 * Cabeçalho do menu: o rótulo e os três quadradinhos, como na web.
 */
@Composable
fun PlayerMenuHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PixColors.Dark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = PixTypography.badgeText, color = PixColors.Cyan)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(PixColors.Pink, PixColors.Yellow, PixColors.Green).forEach { color ->
                Box(modifier = Modifier.size(8.dp).background(color))
            }
        }
    }
}
