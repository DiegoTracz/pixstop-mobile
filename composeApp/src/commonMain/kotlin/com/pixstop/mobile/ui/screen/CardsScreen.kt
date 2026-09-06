package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixstop.mobile.domain.model.SavedCard
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CardsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Meus cartões": os cartões que o fechamento oferece, gerenciados fora dele.
 *
 * Cartão novo só entra pelo fechamento, pagando — guardar sem usar seria
 * guardar por guardar. Aqui a pessoa escolhe o padrão e remove o que não
 * quer mais ver.
 */
@Composable
fun CardsScreen(
    onBack: () -> Unit,
    viewModel: CardsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    state.removingCard?.let { card ->
        RemoveCardDialog(
            card = card,
            onConfirm = viewModel::confirmRemove,
            onDismiss = viewModel::dismissRemove,
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Meus cartões", onBack = onBack)

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.isEmpty -> Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhum cartão guardado. Marque \"salvar cartão\" ao pagar um pedido e ele aparece aqui.",
                    style = PixTypography.bodyMuted,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.error?.let { error ->
                    item {
                        Text(text = error, style = PixTypography.errorText)
                    }
                }

                items(state.cards, key = { it.id }) { card ->
                    CardRow(
                        card = card,
                        busy = state.busyCardId == card.id,
                        onSetDefault = { viewModel.setDefault(card) },
                        onRemove = { viewModel.askRemove(card) },
                    )
                }

                item {
                    Text(
                        text = "O cartão padrão é o que o fechamento já deixa escolhido.",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardRow(card: SavedCard, busy: Boolean, onSetDefault: () -> Unit, onRemove: () -> Unit) {
    val accent = if (card.isDefault) PixColors.Cyan else PixColors.Gray600

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, accent)
            .background(if (card.isDefault) PixColors.CyanAlpha10 else PixColors.Darker)
            .clickable(enabled = !busy && !card.isDefault, onClick = onSetDefault)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(2.dp, if (card.isDefault) PixColors.Cyan else PixColors.Gray500)
                .background(if (card.isDefault) PixColors.Cyan else PixColors.Transparent),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${card.brand ?: "Cartão"} •••• ${card.lastFour}",
                color = if (card.isDefault) PixColors.Cyan else PixColors.Gray100,
            )

            Text(
                text = listOfNotNull(
                    card.expires?.let { "Vence em $it" },
                    "padrão".takeIf { card.isDefault },
                ).joinToString(" · ").ifEmpty { "Toque para tornar padrão" },
                style = PixTypography.caption,
                color = if (card.isDefault) PixColors.Cyan else PixColors.Gray400,
            )
        }

        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PixColors.Cyan)
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(2.dp, PixColors.Pink)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon = AppIconType.Delete, contentDescription = "Remover", tint = PixColors.Pink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RemoveCardDialog(card: SavedCard, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PixColors.Pink)
                .background(PixColors.Darker)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Remover cartão", style = PixTypography.sectionTitle, color = PixColors.Pink)

            Text(
                text = "O ${card.brand ?: "cartão"} terminado em ${card.lastFour} some do fechamento. " +
                    "Para usar de novo, é só informar os dados numa próxima compra.",
                style = PixTypography.bodySecondary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "Voltar",
                    onClick = onDismiss,
                    variant = PixelButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )

                PixelButton(
                    text = "Remover",
                    onClick = onConfirm,
                    variant = PixelButtonVariant.Destructive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
