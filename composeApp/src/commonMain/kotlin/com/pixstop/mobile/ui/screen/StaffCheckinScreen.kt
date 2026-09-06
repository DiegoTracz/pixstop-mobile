package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.data.remote.dto.StaffMemberDto
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.QrCodeScannerScreen
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.StaffCheckinViewModel
import org.koin.compose.viewmodel.koinViewModel

private val kinds = listOf("visit" to "Visita", "service" to "Serviço", "sale" to "Venda")

/**
 * O balcão (Fase 14, V6): achar a pessoa e registrar a visita.
 *
 * É o "pedido pago" dos negócios de serviço. Lê o QR da pessoa ou aceita o
 * código, nome, e-mail ou telefone; diz o que houve e por quanto; e mostra na
 * hora o que a visita rendeu.
 */
@Composable
fun StaffCheckinScreen(
    onBack: () -> Unit,
    viewModel: StaffCheckinViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var scannerOpen by remember { mutableStateOf(false) }

    if (scannerOpen) {
        QrCodeScannerScreen(
            onCodeScanned = { raw ->
                scannerOpen = false
                // O QR diz o que é: `/u/` é a pessoa, `/v/` é um voucher.
                if (raw.contains("/v/")) viewModel.onVoucherCodeChange(raw) else viewModel.onQueryChange(raw)
            },
            onDismiss = { scannerOpen = false },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Registrar visita", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.lastResult?.let {
                Text(
                    text = it,
                    color = PixColors.Green,
                    modifier = Modifier.fillMaxWidth().border(2.dp, PixColors.Green).background(PixColors.GreenAlpha20).padding(12.dp),
                )
            }

            val selected = state.selected

            if (selected == null) {
                PixelInput(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = "Quem está aqui?",
                    placeholder = "Código, nome, e-mail ou telefone",
                    modifier = Modifier.fillMaxWidth(),
                )

                PixelButton(
                    text = "Ler QR code",
                    onClick = { scannerOpen = true },
                    variant = PixelButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )

                state.members.forEach { member ->
                    MemberRow(member = member, onClick = { viewModel.select(member) })
                }

                if (state.members.isEmpty() && state.query.length >= 2 && !state.isSearching) {
                    Text(text = "Ninguém com \"${state.query}\" nesta empresa.", style = PixTypography.bodyMuted)
                }

                Text(text = "Validar voucher", style = PixTypography.sectionTitle, color = PixColors.Cyan, modifier = Modifier.padding(top = 8.dp))

                PixelInput(
                    value = state.voucherCode,
                    onValueChange = viewModel::onVoucherCodeChange,
                    label = "Código do voucher",
                    placeholder = "ABC123XY",
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let {
                    Text(text = it, style = PixTypography.errorText, color = PixColors.Pink)
                }

                PixelButton(
                    text = "Validar",
                    onClick = viewModel::validateVoucher,
                    enabled = state.voucherCode.length >= 4 && !state.isValidating,
                    isLoading = state.isValidating,
                    loadingText = "Validando...",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().border(2.dp, PixColors.Cyan).background(PixColors.Darker).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = selected.name, style = PixTypography.sectionTitle, color = PixColors.Cyan)
                        Text(
                            text = "trocar",
                            style = PixTypography.caption,
                            color = PixColors.Gray300,
                            modifier = Modifier.clickable { viewModel.select(null) }.padding(4.dp),
                        )
                    }
                    Text(text = selected.email.orEmpty(), style = PixTypography.caption, color = PixColors.Gray400)
                }

                Text(text = "O que houve", style = PixTypography.caption, color = PixColors.Gray400)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kinds.forEach { (value, label) ->
                        val active = state.kind == value
                        Text(
                            text = label,
                            color = if (active) PixColors.Cyan else PixColors.Gray300,
                            modifier = Modifier
                                .weight(1f)
                                .border(2.dp, if (active) PixColors.Cyan else PixColors.Gray600)
                                .background(if (active) PixColors.CyanAlpha10 else PixColors.Darker)
                                .clickable { viewModel.onKindChange(value) }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }

                PixelInput(
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = "Valor (R$, opcional)",
                    placeholder = "0,00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                PixelInput(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = "Descrição (opcional)",
                    placeholder = "ex.: corte + barba",
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let {
                    Text(text = it, style = PixTypography.errorText, color = PixColors.Pink)
                }

                PixelButton(
                    text = "Registrar",
                    onClick = viewModel::register,
                    enabled = state.canRegister,
                    isLoading = state.isRegistering,
                    loadingText = "Registrando...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.today.isNotEmpty()) {
                Text(text = "Hoje, por você", style = PixTypography.sectionTitle, color = PixColors.Cyan, modifier = Modifier.padding(top = 8.dp))

                state.today.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().border(2.dp, PixColors.Gray700).background(PixColors.Darker).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = entry.name, color = PixColors.Gray100)
                            Text(
                                text = listOfNotNull(entry.kindLabel, entry.description).joinToString(" · "),
                                style = PixTypography.caption,
                                color = PixColors.Gray400,
                            )
                        }
                        entry.amount?.let {
                            Text(text = "R$ ${it.toString().replace('.', ',')}", style = PixTypography.caption, color = PixColors.Yellow)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: StaffMemberDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray700)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = member.name, color = PixColors.Gray100)
            Text(text = listOfNotNull(member.email, member.phone).joinToString(" · "), style = PixTypography.caption, color = PixColors.Gray400)
        }

        member.memberCode?.let {
            Text(text = it, style = PixTypography.caption, color = PixColors.Cyan)
        }
    }
}
