package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CompanyAction
import com.pixstop.mobile.ui.viewmodel.CompanyUiState

/**
 * Confirmação das ações do painel.
 *
 * Toda ação daqui mexe em dinheiro, pixels ou no acesso de alguém, e várias
 * não têm desfazer — o diálogo diz o que vai acontecer antes de acontecer.
 */
@Composable
fun CompanyActionDialog(
    action: CompanyAction,
    state: CompanyUiState,
    onAmountChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PixColors.Cyan)
                .background(PixColors.Darker)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title(action), style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(text = explanation(action), style = PixTypography.bodySecondary)

            if (state.needsAmount) {
                PixelInput(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    label = "Quantidade de pixels",
                    placeholder = "0",
                    enabled = !state.isWorking,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PixelInput(
                value = state.reason,
                onValueChange = onReasonChange,
                label = if (state.needsReason) "Motivo" else "Motivo (opcional)",
                placeholder = placeholder(action),
                enabled = !state.isWorking,
                error = "Explique em pelo menos ${CompanyUiState.MIN_REASON} letras."
                    .takeIf { state.needsReason && state.reason.isNotEmpty() && state.reason.trim().length < CompanyUiState.MIN_REASON },
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Text(text = it, style = PixTypography.errorText)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "Voltar",
                    onClick = onDismiss,
                    variant = PixelButtonVariant.Secondary,
                    enabled = !state.isWorking,
                    modifier = Modifier.weight(1f),
                )

                PixelButton(
                    text = confirmLabel(action),
                    onClick = onConfirm,
                    variant = if (isDestructive(action)) PixelButtonVariant.Destructive else PixelButtonVariant.Primary,
                    enabled = state.canConfirm,
                    isLoading = state.isWorking,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun title(action: CompanyAction): String = when (action) {
    is CompanyAction.ApproveOrder -> "Aprovar pedido"
    is CompanyAction.CancelOrder -> "Cancelar pedido"
    is CompanyAction.ToggleUser -> if (action.member.isActive) "Remover acesso" else "Liberar acesso"
    is CompanyAction.AllocatePixels -> "Alocar pixels"
    is CompanyAction.DistributePixels -> "Dar pixels"
}

/**
 * O que vai acontecer, em uma frase — inclusive o que não dá para desfazer.
 */
private fun explanation(action: CompanyAction): String = when (action) {
    is CompanyAction.ApproveOrder ->
        "O pedido ${action.order.transactionId.orEmpty()} será dado como pago sem passar pelo gateway. " +
            "Quem comprou vai ler o motivo."

    is CompanyAction.CancelOrder ->
        "O estoque volta para a vitrine e os pixels voltam para quem comprou. " +
            "O que foi pago em dinheiro é estornado pelo gateway."

    is CompanyAction.ToggleUser -> if (action.member.isActive) {
        "${action.member.name} perde o acesso a esta empresa e os pixels dela voltam para o cofre."
    } else {
        "${action.member.name} volta a ter acesso a esta empresa."
    }

    is CompanyAction.AllocatePixels ->
        "Os pixels saem do cofre da empresa e entram na verba de ${action.department.name}, " +
            "de onde o gestor distribui."

    is CompanyAction.DistributePixels ->
        "Os pixels saem do cofre e vão direto para " +
            "${action.members.size} " + (if (action.members.size == 1) "pessoa" else "pessoas") + ", sem passar por time."
}

private fun placeholder(action: CompanyAction): String = when (action) {
    is CompanyAction.ApproveOrder -> "Pagamento confirmado na maquininha"
    is CompanyAction.CancelOrder -> "Produto indisponível"
    else -> "Meta batida em setembro"
}

private fun confirmLabel(action: CompanyAction): String = when (action) {
    is CompanyAction.ApproveOrder -> "Aprovar"
    is CompanyAction.CancelOrder -> "Cancelar pedido"
    is CompanyAction.ToggleUser -> if (action.member.isActive) "Remover" else "Liberar"
    is CompanyAction.AllocatePixels -> "Alocar"
    is CompanyAction.DistributePixels -> "Distribuir"
}

private fun isDestructive(action: CompanyAction): Boolean = when (action) {
    is CompanyAction.CancelOrder -> true
    is CompanyAction.ToggleUser -> action.member.isActive
    else -> false
}
