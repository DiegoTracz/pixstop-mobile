package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ForgotPasswordViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Esqueci a senha", aberto do login: pede o link por e-mail e explica o
 * resto do caminho, que é na página do site aberta pelo link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordSheet(
    initialEmail: String,
    onDismiss: (sentTo: String?) -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.start(initialEmail) }

    ModalBottomSheet(
        onDismissRequest = { onDismiss(state.sentTo) },
        sheetState = sheetState,
        containerColor = PixColors.Darker,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("REDEFINIR SENHA", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            val sentTo = state.sentTo
            if (sentTo == null) {
                Text(
                    "Digite o e-mail da sua conta. Mandamos um link para você criar uma senha nova.",
                    style = PixTypography.bodySecondary,
                )

                PixelInput(
                    value = state.email,
                    onValueChange = viewModel::onEmail,
                    label = "E-MAIL",
                    placeholder = "seu@email.com",
                    enabled = !state.isSending,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                    leadingIcon = {
                        AppIcon(icon = AppIconType.Email, contentDescription = null, tint = PixColors.Gray400, modifier = Modifier.size(16.dp))
                    },
                )

                state.error?.let { Text(it, style = PixTypography.errorText) }

                PixelButton(
                    text = "ENVIAR LINK",
                    onClick = viewModel::send,
                    enabled = state.canSend,
                    isLoading = state.isSending,
                    loadingText = "ENVIANDO...",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text("Se $sentTo estiver cadastrado, o link já está a caminho.", color = PixColors.Green)
                Text(
                    "Abra o e-mail, toque em \"Redefinir senha\" e crie a senha nova no site. " +
                        "Depois volte aqui e entre com ela. Não chegou? Olhe a caixa de spam.",
                    style = PixTypography.bodySecondary,
                )

                PixelButton(
                    text = "VOLTAR AO LOGIN",
                    onClick = { onDismiss(sentTo) },
                    variant = PixelButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
