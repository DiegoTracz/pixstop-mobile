package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.AccountUser
import androidx.compose.ui.window.Dialog
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelDivider
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelPasswordInput
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ProfileUiState
import com.pixstop.mobile.ui.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Dados da conta e troca de senha.
 *
 * As duas metades salvam separado: são chamadas diferentes no servidor e
 * falhar numa não pode desfazer a outra.
 */
@Composable
fun ProfileScreen(
    user: AccountUser?,
    onProfileSaved: () -> Unit,
    modifier: Modifier = Modifier,
    onAccountDeleted: () -> Unit = {},
    onOpenCards: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(user?.id) {
        user?.let { viewModel.start(it.name, it.email) }
    }

    // Conta excluída: não há mais nada para esta tela mostrar.
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onAccountDeleted()
        }
    }

    if (state.isDeleteOpen) {
        DeleteAccountDialog(
            password = state.deletePassword,
            isDeleting = state.isDeleting,
            canDelete = state.canDelete,
            error = state.deleteError,
            onPasswordChange = viewModel::onDeletePasswordChange,
            onConfirm = viewModel::deleteAccount,
            onDismiss = viewModel::dismissDelete,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Seus dados", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        PixelInput(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = "Nome",
            enabled = !state.isSavingProfile,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )

        PixelInput(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "E-mail",
            enabled = !state.isSavingProfile,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        Feedback(message = state.profileMessage, error = state.profileError)

        PixelButton(
            text = "Salvar",
            onClick = { viewModel.saveProfile(onProfileSaved) },
            enabled = state.canSaveProfile,
            isLoading = state.isSavingProfile,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelDivider(text = "")

        Text(text = "Trocar a senha", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        PixelPasswordInput(
            value = state.currentPassword,
            onValueChange = viewModel::onCurrentPasswordChange,
            label = "Senha atual",
            enabled = !state.isSavingPassword,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelPasswordInput(
            value = state.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            label = "Nova senha",
            enabled = !state.isSavingPassword,
            error = "Use ao menos ${ProfileUiState.MIN_PASSWORD} caracteres."
                .takeIf { state.newPassword.isNotEmpty() && state.newPassword.length < ProfileUiState.MIN_PASSWORD },
            modifier = Modifier.fillMaxWidth(),
        )

        PixelPasswordInput(
            value = state.confirmation,
            onValueChange = viewModel::onConfirmationChange,
            label = "Confirme a nova senha",
            enabled = !state.isSavingPassword,
            error = "As senhas não são iguais.".takeIf { state.confirmation.isNotEmpty() && !state.passwordsMatch },
            modifier = Modifier.fillMaxWidth(),
        )

        Feedback(message = state.passwordMessage, error = state.passwordError)

        PixelButton(
            text = "Alterar senha",
            onClick = viewModel::savePassword,
            enabled = state.canSavePassword,
            isLoading = state.isSavingPassword,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelDivider(text = "")

        Text(text = "Meus cartões", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        Text(
            text = "Os cartões guardados no fechamento: escolha o padrão ou remova o que não quer mais ver.",
            style = PixTypography.bodySecondary,
        )

        PixelButton(
            text = "Gerenciar cartões",
            onClick = onOpenCards,
            variant = PixelButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelDivider(text = "")

        Text(text = "Excluir a conta", style = PixTypography.sectionTitle, color = PixColors.Pink)

        Text(
            text = "Sua conta sai do ar na hora. Os dados são apagados em definitivo " +
                "depois de ${state.purgeAfterDays} dias, e até lá dá para voltar atrás.",
            style = PixTypography.bodySecondary,
        )

        PixelButton(
            text = "Excluir minha conta",
            onClick = viewModel::openDelete,
            variant = PixelButtonVariant.Destructive,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Confirmação da exclusão.
 *
 * Pede a senha porque é o que separa o pedido do titular de alguém que pegou
 * o celular destravado.
 */
@Composable
private fun DeleteAccountDialog(
    password: String,
    isDeleting: Boolean,
    canDelete: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PixColors.Pink)
                .background(PixColors.Darker)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Excluir a conta", style = PixTypography.sectionTitle, color = PixColors.Pink)

            Text(
                text = "Você perde o acesso às empresas, ao saldo e aos pixels. " +
                    "Confirme com a sua senha.",
                style = PixTypography.bodySecondary,
            )

            PixelPasswordInput(
                value = password,
                onValueChange = onPasswordChange,
                label = "Senha",
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(text = it, style = PixTypography.errorText)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "Voltar",
                    onClick = onDismiss,
                    variant = PixelButtonVariant.Secondary,
                    enabled = !isDeleting,
                    modifier = Modifier.weight(1f),
                )

                PixelButton(
                    text = "Excluir",
                    onClick = onConfirm,
                    variant = PixelButtonVariant.Destructive,
                    enabled = canDelete,
                    isLoading = isDeleting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Feedback(message: String?, error: String?) {
    error?.let {
        Text(text = it, style = PixTypography.errorText)
        return
    }

    message?.let {
        Text(text = it, style = PixTypography.caption, color = PixColors.Green)
    }
}
