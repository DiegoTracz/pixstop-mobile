package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.PixelInviteViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * O convite com pixels (Fase 14): o presente que cadastra.
 *
 * Chega pelo link. Mostra a empresa e o valor, pede o telefone quando o
 * convite foi por telefone — é como se sabe que o presente é desta pessoa —
 * e aceita uma vez.
 */
@Composable
fun PixelInviteScreen(
    code: String,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
    viewModel: PixelInviteViewModel = koinViewModel(parameters = { parametersOf(code) }),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.acceptedCompany) {
        if (state.acceptedCompany != null) {
            onAccepted()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Presente", onBack = onBack)

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.invite == null -> Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Convite não encontrado.", style = PixTypography.errorText, textAlign = TextAlign.Center)
            }

            else -> {
                val invite = state.invite!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, PixColors.Green)
                            .background(PixColors.GreenAlpha20)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        invite.recipientName?.let {
                            Text(text = "Olá, $it!", style = PixTypography.bodySecondary, color = PixColors.Gray100)
                        }

                        Text(text = invite.companyName.uppercase(), style = PixTypography.badgeText, color = PixColors.Yellow)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PixelCoin(size = 20.dp)
                            Text(text = "${invite.pixels} pixels", style = PixTypography.pageTitle, color = PixColors.Yellow)
                        }

                        Text(text = "valem ${invite.reais}", style = PixTypography.caption, color = PixColors.Gray300)

                        invite.message?.let {
                            Text(text = "“$it”", style = PixTypography.bodySecondary, color = PixColors.Gray100, textAlign = TextAlign.Center)
                        }
                    }

                    if (!invite.isOpen) {
                        Text(
                            text = "Convite ${invite.statusLabel?.lowercase() ?: "encerrado"}. Peça um novo a ${invite.companyName}.",
                            style = PixTypography.bodyMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        if (invite.needsPhone) {
                            PixelInput(
                                value = state.phone,
                                onValueChange = viewModel::onPhoneChange,
                                label = "Seu WhatsApp",
                                placeholder = "11 99999-0000",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Text(
                                text = "O número que recebeu o convite — é como sabemos que o presente é seu.",
                                style = PixTypography.caption,
                                color = PixColors.Gray400,
                            )
                        }

                        state.error?.let {
                            Text(text = it, style = PixTypography.errorText, color = PixColors.Pink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        PixelButton(
                            text = "Receber ${invite.pixels} pixels",
                            onClick = viewModel::accept,
                            enabled = state.canAccept,
                            isLoading = state.isAccepting,
                            loadingText = "Recebendo...",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        PixelButton(
                            text = "Agora não",
                            onClick = onBack,
                            variant = PixelButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
