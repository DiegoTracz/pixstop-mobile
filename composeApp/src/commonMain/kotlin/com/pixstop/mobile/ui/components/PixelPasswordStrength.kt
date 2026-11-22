package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.auth.PasswordRule
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

private const val CHECKS_PER_ROW = 3

/**
 * Força da senha: uma barra por regra e a lista do que ainda falta, com as
 * mesmas regras que o servidor cobra. Aparece quando a pessoa começa a digitar.
 */
@Composable
fun PixelPasswordStrength(password: String, modifier: Modifier = Modifier) {
    if (password.isEmpty()) return

    val checks = PasswordRule.entries.map { it to it.passes(password) }
    val passed = checks.count { it.second }

    val (color, label) = when {
        passed == checks.size -> PixColors.Green to "FORTE"
        passed >= 4 -> PixColors.Yellow to "BOA"
        passed >= 3 -> PixColors.Yellow to "MÉDIA"
        else -> PixColors.Pink to "FRACA"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixColors.Dark)
            .pixelBorder(PixColors.Gray700)
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "FORÇA DA SENHA", style = PixTypography.footerText, color = PixColors.Gray400)
            Text(text = label, style = PixTypography.footerText, color = color)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(checks.size) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(if (index < passed) color else PixColors.Gray700),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        checks.chunked(CHECKS_PER_ROW).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (rule, ok) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        AppIcon(
                            icon = if (ok) AppIconType.Check else AppIconType.Close,
                            contentDescription = null,
                            tint = if (ok) PixColors.Green else PixColors.Gray500,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rule.label,
                            style = PixTypography.bodyMuted,
                            color = if (ok) PixColors.Green else PixColors.Gray400,
                        )
                    }
                }
                repeat(CHECKS_PER_ROW - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
