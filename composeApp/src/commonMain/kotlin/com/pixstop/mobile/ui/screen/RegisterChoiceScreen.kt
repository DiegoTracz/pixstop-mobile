package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.components.*
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Empresa ou pessoa: a mesma escolha que abre o cadastro na web.
 */
@Composable
fun RegisterChoiceScreen(
    onChooseCompany: () -> Unit,
    onChoosePerson: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    PixelAuthCard(headerTitle = "SYSTEM.REGISTER", centerContent = true) {
        PixelAuthHeader(
            icon = AppIconType.PersonAdd,
            title = "CRIAR CONTA",
            subtitle = "Escolha o tipo de cadastro",
            themeColor = PixColors.Green,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ChoiceCard(
            icon = AppIconType.Person,
            title = "PESSOA",
            description = "Crie sua conta para comprar na geladeira da sua empresa",
            color = PixColors.Cyan,
            onClick = onChoosePerson,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChoiceCard(
            icon = AppIconType.Business,
            title = "EMPRESA",
            description = "Cadastre sua empresa e teste 30 dias grátis",
            color = PixColors.Pink,
            onClick = onChooseCompany,
        )

        PixelDivider()

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Já tem conta? ", style = PixTypography.bodySecondary)
            Text(
                text = "ENTRAR",
                style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { onNavigateToLogin() },
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: AppIconType,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.Dark)
            .pixelBorder(color)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(color),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon = icon, contentDescription = null, tint = PixColors.Dark, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PixTypography.sectionTitle, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = PixTypography.bodyMuted)
        }

        AppIcon(icon = AppIconType.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}
