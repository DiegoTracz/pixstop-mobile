package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Appliance
import com.pixstop.mobile.domain.model.Presence
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Em qual geladeira a pessoa está (Fase 9.5).
 *
 * Só existe com mais de uma porta na empresa. O estoque e a porta que abre são
 * de uma geladeira: comprar sem dizer qual seria prometer um produto que pode
 * estar num andar onde a pessoa não está.
 *
 * A que está fora do ar continua na lista, e não some: some-la esconderia a
 * razão de a compra não sair ali. Quem escolhe uma offline ainda pode comprar
 * se ela tiver Bluetooth, e é o checkout que diz isso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceSwitcherSheet(
    appliances: List<Appliance>,
    currentId: Long?,
    /** Sem porta escolhida não há como fechar: a vitrine não saberia o que mostrar. */
    dismissible: Boolean,
    onSelect: (Appliance) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = sheetState,
        containerColor = PixColors.Darker,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Em qual geladeira você está?",
                style = PixTypography.sectionTitle,
                color = PixColors.Cyan,
            )

            Text(
                text = "A loja mostra o que tem dentro da porta em frente a você. " +
                    "Trocar de geladeira esvazia o carrinho: a reserva vale só onde foi feita.",
                style = PixTypography.bodyMuted,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(appliances, key = { it.id }) { appliance ->
                    ApplianceRow(
                        appliance = appliance,
                        isCurrent = appliance.id == currentId,
                        onClick = { onSelect(appliance) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplianceRow(
    appliance: Appliance,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isCurrent) PixColors.Green else PixColors.Gray700

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(if (isCurrent) PixColors.Green.copy(alpha = 0.08f) else PixColors.Dark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = appliance.name,
                style = PixTypography.bodyRegular,
                color = PixColors.Gray100,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            )

            appliance.subtitle?.let {
                Text(text = it, style = PixTypography.bodyMuted)
            }

            presenceLabel(appliance.presence)?.let {
                Text(text = it, style = PixTypography.bodyMuted, color = PixColors.Yellow)
            }
        }

        if (isCurrent) {
            AppIcon(
                icon = AppIconType.Check,
                contentDescription = "Geladeira atual",
                modifier = Modifier.size(18.dp),
                tint = PixColors.Green,
            )
        }
    }
}

/**
 * O aviso só aparece quando há o que avisar: "online" e "sem aparelho ligado"
 * não mudam nada para quem está escolhendo a porta.
 */
private fun presenceLabel(presence: Presence): String? = when (presence) {
    Presence.Offline -> "sem internet agora"
    Presence.Unreachable -> "não respondeu ao servidor"
    else -> null
}
