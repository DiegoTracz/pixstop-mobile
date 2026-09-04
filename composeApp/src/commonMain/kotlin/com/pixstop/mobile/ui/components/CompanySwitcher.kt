package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.CompanyRole
import com.pixstop.mobile.domain.model.Membership
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Lista de empresas para trocar a ativa.
 *
 * Só aparece com mais de um vínculo. Vínculo desativado continua na lista, mas
 * apagado e sem toque: some-lo faria a pessoa achar que perdeu o histórico
 * daquela empresa, quando na verdade só perdeu o acesso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySwitcherSheet(
    memberships: List<Membership>,
    isSwitching: Boolean,
    onSelect: (Membership) -> Unit,
    onJoinCompany: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixColors.Darker,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Suas empresas",
                style = PixTypography.sectionTitle,
                color = PixColors.Cyan,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memberships, key = { it.id }) { membership ->
                    CompanyRow(
                        membership = membership,
                        enabled = membership.isActive && !isSwitching,
                        onClick = { onSelect(membership) },
                    )
                }
            }

            PixelButton(
                text = "Entrar em outra empresa",
                onClick = onJoinCompany,
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CompanyRow(
    membership: Membership,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        membership.isCurrent -> PixColors.Cyan
        !membership.isActive -> PixColors.Gray700
        else -> PixColors.Gray600
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .background(if (membership.isCurrent) PixColors.CyanAlpha10 else PixColors.Dark)
            .clickable(enabled = enabled && !membership.isCurrent, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                text = membership.name,
                color = if (membership.isActive) PixColors.Gray100 else PixColors.Gray400,
                fontWeight = if (membership.isCurrent) FontWeight.Bold else FontWeight.Normal,
            )

            Text(
                text = membership.roleLabel(),
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }

        Text(
            text = when {
                !membership.isActive -> "sem acesso"
                membership.isCurrent -> "atual"
                else -> "${membership.pixelAvailable} px"
            },
            style = PixTypography.caption,
            color = when {
                !membership.isActive -> PixColors.Gray500
                membership.isCurrent -> PixColors.Cyan
                else -> PixColors.Yellow
            },
        )
    }
}

private fun Membership.roleLabel(): String = when (role) {
    CompanyRole.Admin -> "Administrador"
    CompanyRole.Manager -> "Gestor"
    CompanyRole.Member -> "Membro"
}
